package com.aigroup.aigroupmobile.data.dao

import android.util.Log
import androidx.datastore.core.DataStore
import com.aigroup.aigroupmobile.data.AppDatabase
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.LargeLangBot
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.data.models.MessageChatData
import com.aigroup.aigroupmobile.data.models.MessageChatError
import com.aigroup.aigroupmobile.data.models.MessageImageItem
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.data.models.MessageSenderInclusive
import com.aigroup.aigroupmobile.data.models.MessageSenderUser
import com.aigroup.aigroupmobile.data.models.MessageTextItem
import com.aigroup.aigroupmobile.data.models.MessageVideoItem
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.models.VideoMediaItem
import com.aigroup.aigroupmobile.connect.voice.VoiceCode
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.extensions.applyPreferenceProperties
import com.aigroup.aigroupmobile.data.models.BotAssistant
import com.aigroup.aigroupmobile.data.models.model
import com.aigroup.aigroupmobile.data.models.parseAssistantScheme
import com.aigroup.aigroupmobile.data.models.primaryBot
import com.aigroup.aigroupmobile.data.models.primaryBotSender
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.toRealmList
import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.BsonObjectId
import javax.inject.Inject
import kotlin.math.max

class ChatConversationDao @Inject constructor(
  private val appDatabase: AppDatabase,

  // TODO: its corret to using dataStore here? (REFACTOR) remove this!
  private val dataStore: DataStore<AppPreferences>
) {

  companion object {
    const val TAG = "ChatConversationDao"
  }

  fun countChatSession(): Flow<Int> {
    return appDatabase.realm.query<ChatSession>().asFlow().map { it.list.count() }
  }

  fun getCommonlyModelCode(): Flow<List<String>> {
    return appDatabase.realm.query<ChatSession>()
      .asFlow()
      .map {
        it.list
          .mapNotNull { it.primaryBot?.model }.groupBy { it.code }
          .toList()
          .sortedBy { (_, value) ->
            value.size
          }
          .map { (key, _) -> key }
          .reversed()
      }
  }

  fun getNormalChatSession(
    timeRange: LongRange? = null,
    query: String? = null
  ): Flow<ResultsChange<ChatSession>> {
    // TODO: The query use right thread or dispatcher?
    // see [RealmConfiguration.Builder.notificationDispatcher].
    return appDatabase.realm
      .query<ChatSession>("pinned = $0", false)
      .let {
        if (timeRange != null) {
          val startId = BsonObjectId(max(0, timeRange.first.toInt()), 0, 0, 0)
          val endId = BsonObjectId(timeRange.last.toInt(), 0, 0, 0)
          it.query("id >= $0 && id <= $1", startId, endId)
        } else {
          it
        }
      }
      .let {
        if (!query.isNullOrEmpty()) {
          val rql = "(SUBQUERY(messages, \$msg, \$msg.parts.textItem.text CONTAINS[c] \"$query\").@count > 0) || " +
              "(title CONTAINS[c] \"$query\") || " +
              "(summaryContent CONTAINS[c] \"$query\")"
          Log.d(TAG, "Query with search: $rql")
          it.query(rql)
        } else {
          it
        }
      }
      .sort("lastModified", Sort.DESCENDING)
      .asFlow();
  }

  fun getPinnedChatSession(): Flow<ResultsChange<ChatSession>> {
    return appDatabase.realm
      .query<ChatSession>("pinned = $0", true)
      .sort("lastModified", Sort.DESCENDING)
      .asFlow();
  }

  // TODO: 跟 getNormalChatSession 合并? and getPinnedChatSession
  fun getChatSessionByAssistant(assistant: BotAssistant): Flow<ResultsChange<ChatSession>> {
    return appDatabase.realm
      .query<ChatSession>()
      .query("senders.botSender.assistant.id == $0", assistant.id)
      .sort("lastModified", Sort.DESCENDING)
      .asFlow()
  }

  suspend fun ensureEmptySession(modelCode: ModelCode, exceptSession: ChatSession? = null): ChatSession {
    val current = appDatabase.realm.query<ChatSession>()
      .query("messages.@size == 0")
      .let {
        if (exceptSession != null) {
          it.query("id != $0", exceptSession.id)
        } else {
          it
        }
      }
      .first().find()

    return current ?: createChatSession(modelCode)
  }

  // TODO: 合并 createChatSession, 逻辑类似
  suspend fun addAssistantToChatSession(session: ChatSession, assistant: BotAssistant, backupModel: ModelCode): MessageSenderInclusive {
    val prop = dataStore.data.map { it.defaultModelProperties }.first()

    val assistantScheme = assistant.parseAssistantScheme()
    val defaultModelCode = assistantScheme.configuration.preferredModelCode?.let {
      ModelCode.fromFullCode(it)
    } ?: backupModel

    return appDatabase.realm.write {
      val session = findLatest(session)
      require(session != null) { "ChatSession not found" }

      val botSender = MessageSenderBot(
        name = assistantScheme.metadata.title,
        description = assistantScheme.metadata.description,
        langBot = LargeLangBot(defaultModelCode.fullCode()).applyPreferenceProperties(prop),
        assistant = findLatest(assistant)
      ).createInclusive()
      session.senders.add(botSender)

      return@write session.senders.last()
    }
  }

  // TODO: 合并两个 createChatSession
  suspend fun createChatSession(assistant: BotAssistant, backupModel: ModelCode): ChatSession {
    val prop = dataStore.data.map { it.defaultModelProperties }.first()
    val voiceCode = dataStore.data.map { VoiceCode.fromFullCode(it.voiceCode) }.first()

    val assistantScheme = assistant.parseAssistantScheme()
    val defaultModelCode = assistantScheme.configuration.preferredModelCode?.let {
      ModelCode.fromFullCode(it)
    } ?: backupModel

    return appDatabase.realm.write {
      val session = ChatSession().apply {
        val botSender = MessageSenderBot(
          name = assistantScheme.metadata.title,
          description = assistantScheme.metadata.description,
          langBot = LargeLangBot(defaultModelCode.fullCode()).applyPreferenceProperties(prop),
          assistant = findLatest(assistant)
        ).createInclusive()
        this.senders.add(botSender)
        this.voiceCode = voiceCode
      }
      return@write copyToRealm(session)
    }
  }

  suspend fun createChatSession(defaultModelCode: ModelCode?): ChatSession {
    val prop = dataStore.data.map { it.defaultModelProperties }.first()
    val voiceCode = dataStore.data.map { VoiceCode.fromFullCode(it.voiceCode) }.first()

    return appDatabase.realm.write {
      val session = ChatSession().apply {
        if (defaultModelCode != null) {
          val botSender = MessageSenderBot(
            name = defaultModelCode.toString(),
            description = "模型 $defaultModelCode",
            langBot = LargeLangBot(defaultModelCode.fullCode()).applyPreferenceProperties(prop),
          ).createInclusive()
          this.senders.add(botSender)
        }

        this.voiceCode = voiceCode
      }
      return@write copyToRealm(session)
    }
  }


  suspend fun updateChatSession(session: ChatSession, updater: ChatSession.() -> Unit) {
    appDatabase.realm.write {
      val session = findLatest(session)
      require(session != null) { "ChatSession not found" }
      updater.invoke(session)
    }
  }

  private suspend fun createAndUpdateSessionPrimaryBot(session: ChatSession, model: ModelCode) {
    val prop = dataStore.data.map { it.defaultModelProperties }.first()
    appDatabase.realm.write {
      val session = findLatest(session)
      require(session != null) { "ChatSession not found" }

      val botSender = MessageSenderBot(
        name = model.toString(),
        description = "模型 $model",
        langBot = LargeLangBot(model.fullCode()).applyPreferenceProperties(prop)
      ).createInclusive()

      session.senders.add(0, botSender)
    }
  }

  suspend fun checkAndSwitchSessionPrimaryBot(session: ChatSession, model: ModelCode) {
    val success = appDatabase.realm.write {
      val session = findLatest(session)
      require(session != null) { "ChatSession not found" }

      // if current primary bot is a assistant, switch it directly
      if (session.primaryBotSender?.botSender?.assistant != null) {
        session.primaryBotSender!!.botSender!!.langBot!!.largeLangModelCode = model.fullCode()
        return@write true
      }

      val botSender = session.senders.firstOrNull { it.botSender?.langBot?.model == model }
      if (botSender != null) {
        session.senders.remove(botSender)
        session.senders.add(0, botSender)
        return@write true
      } else {
        return@write false
      }
    }

    if (!success) {
      createAndUpdateSessionPrimaryBot(session, model)
    }
  }

  suspend fun switchSessionPrimaryBot(session: ChatSession, bot: MessageSenderBot) {
    val targetId = bot.inclusive.id
    appDatabase.realm.write {
      val session = findLatest(session)
      require(session != null) { "ChatSession not found" }

      val botSender = session.senders.firstOrNull { it.id == targetId }
      require(botSender != null) { "BotSender not found" }

      session.senders.remove(botSender)
      session.senders.add(0, botSender)
    }
  }

  suspend fun deleteChatSession(session: ChatSession) {
    appDatabase.realm.write {
      findLatest(session)?.also { delete(it) }
    }
  }

  suspend fun cloneChatSession(session: ChatSession) {
    appDatabase.realm.write {
      val userSender = query<MessageSenderUser>().first().find()
      require(userSender != null) { "UserSender not found" }

      // TODO: add to extension method and better solution
      val session = findLatest(session)
      require(session != null) { "ChatSession not found" }

      val senderIdMap = mutableMapOf<String, String>()

      val sendersCopy = session.senders.map { old ->
        MessageSenderInclusive().apply {
          senderIdMap[old.id.toHexString()] = id.toHexString()

          role = old.role

          when {
            old.userSender != null -> {
              this.userSender = old.userSender
            }

            old.botSender != null -> {
              val bot = old.botSender!!
              botSender = MessageSenderBot().apply {
                name = bot.name
                description = bot.description
                langBot = LargeLangBot().apply {
                  largeLangModelCode = bot.langBot!!.largeLangModelCode
                  temperature = bot.langBot?.temperature
                  maxTokens = bot.langBot?.maxTokens
                  topP = bot.langBot?.topP
                  presencePenalty = bot.langBot?.presencePenalty
                  frequencyPenalty = bot.langBot?.frequencyPenalty
                }
              }
            }
          }
        }
      }.toRealmList()
      val newSessionCopy = ChatSession().apply {
        summaryLastMsgId = session.summaryLastMsgId
        summaryContent = session.summaryContent
        title = session.title
        pinned = false
        lastChatAtInstant = session.lastChatAtInstant
        senders = sendersCopy
        historyInclude = session.historyInclude
        plugins = session.plugins.toList().toRealmList()
      }

      val newSession = copyToRealm(newSessionCopy)

      session.messages.forEach { old ->
        val newMessage = MessageChat().apply {
          // plugin
          pluginId = old.pluginId
          pluginExtra = old.pluginExtra

          // sender
          if (old.sender != null) {
            if (old.sender!!.userSender != null) {
              println("setup user sender when clone")
              sender = userSender.inclusive
            } else {
              val botSender =
                sendersCopy.firstOrNull { it.id.toHexString() == senderIdMap[old.sender!!.id.toHexString()] }
              if (botSender != null) {
                sender = botSender
              }
            }
          }

          // parts
          parts = old.parts.map { part ->
            MessageChatData().apply {
              if (part.error != null) {
                error = MessageChatError(part.error!!.message)
              }

              when {
                part.textItem != null -> {
                  textItem = MessageTextItem(part.textItem!!.text)
                }

                part.imageItem != null -> {
                  imageItem = MessageImageItem().apply {
                    if (part.imageItem!!.image != null) {
                      image = ImageMediaItem(part.imageItem!!.image!!.url)
                    }
                    helpText = part.imageItem!!.helpText
                  }
                }

                part.videoItem != null -> {
                  videoItem = MessageVideoItem().apply {
                    if (part.videoItem!!.video != null) {
                      video = VideoMediaItem(
                        part.videoItem!!.video!!.url,
                        null
                      ).apply {
                        if (part.videoItem!!.video!!.snapshot != null) {
                          snapshot = ImageMediaItem(
                            part.videoItem!!.video!!.snapshot!!.url
                          )
                        }
                      }
                    }
                    helpText = part.videoItem!!.helpText
                  }
                }
              }
            }
          }.toRealmList()
        }
        newSession.messages.add(newMessage)
      }
    }
  }

}