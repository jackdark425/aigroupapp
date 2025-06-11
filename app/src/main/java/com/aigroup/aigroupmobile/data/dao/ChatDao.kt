package com.aigroup.aigroupmobile.data.dao

import android.util.Log
import com.aigroup.aigroupmobile.data.AppDatabase
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.data.models.LargeLangBot
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.data.models.MessageChatData
import com.aigroup.aigroupmobile.data.models.MessageChatError
import com.aigroup.aigroupmobile.data.models.MessageItem
import com.aigroup.aigroupmobile.data.models.MessageSender
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.data.models.MessageTextItem
import com.aigroup.aigroupmobile.data.models.primaryBot
import com.aigroup.aigroupmobile.data.models.primaryBotSender
import com.aigroup.aigroupmobile.data.models.specific
import com.aigroup.aigroupmobile.utils.common.now
import com.aigroup.aigroupmobile.utils.common.simulateTextAnimate
import io.realm.kotlin.MutableRealm
import io.realm.kotlin.ext.query
import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.notifications.SingleQueryChange
import io.realm.kotlin.query.Sort
import io.realm.kotlin.types.BaseRealmObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.LocalDateTime
import org.mongodb.kbson.ObjectId
import javax.inject.Inject

private const val BSON_OBJECT_ID_LENGTH: Int = 12

class ChatDao @Inject constructor(private val appDatabase: AppDatabase) {

  private fun getConversationById(chatId: ObjectId): Flow<SingleQueryChange<ChatSession>> {
    // its too depth: https://www.mongodb.com/zh-cn/docs/atlas/device-sdks/sdk/kotlin/realm-database/react-to-changes/#observe-nested-key-paths

    // TODO: 单独抽出 priamryBot 作为观察，这样损失了其他不超过 depth 限制的字段的观察
    return appDatabase.realm
      .query<ChatSession>("id = $0", chatId).first()
      .asFlow(
        listOf(
          "senders.botSender.langBot.*",
          "historyInclude",
          "plugins",
          "title",
          "summaryLastMsgId",
          "summaryContent",
          "voiceCodeString"
        )
      )
  }

  fun getConversationById(chatId: String): Flow<SingleQueryChange<ChatSession>> {
    val id = ObjectId(chatId)
    return getConversationById(id)
  }

  fun getMessages(chatId: String): Flow<ResultsChange<MessageChat>> {
    val id = ObjectId(chatId)
    return getMessages(id)
  }

  fun getMessages(chatId: ObjectId): Flow<ResultsChange<MessageChat>> {
    return appDatabase.realm
      .query<MessageChat>()
      .query("@links.ChatSession.messages.id = $0", chatId)
      .sort("id", Sort.DESCENDING)
      .asFlow()
  }

  suspend fun <T> createMessage(
    session: ChatSession,
    sender: T,
    data: MessageItem
  ): MessageChat where T : MessageSender, T : BaseRealmObject {
    require(!data.isManaged()) { "Should using new object instead of managed object." }
    return appDatabase.realm.write {
      return@write innerCreateMessage(session, sender, listOf(data))
    }
  }

  suspend fun <T> createMessage(
    session: ChatSession,
    sender: T,
    data: List<MessageItem>
  ): MessageChat where T : MessageSender, T : BaseRealmObject {
    require(data.all { !it.isManaged() }) { "Should using new object instead of managed object." }
    return appDatabase.realm.write {
      return@write innerCreateMessage(session, sender, data)
    }
  }

  suspend fun createBotMessage(session: ChatSession, data: MessageItem): MessageChat {
    val sender = session.primaryBotSender?.botSender ?: error("No primary bot found")
    return createMessage(session, sender, data)
  }

  suspend fun createBotMessage(session: ChatSession, data: List<MessageItem>): MessageChat {
    val sender = session.primaryBotSender?.botSender ?: error("No primary bot found")
    return createMessage(session, sender, data)
  }

  // TODO: 优化这个 api, 叫做 empty 其实没有 empty，由于 updateMessageLastTextOrCreate 可以自动创建，似乎可以不用在这里创建一个空 part
  suspend fun createEmptyBotMessage(session: ChatSession, botSender: MessageSenderBot? = null): MessageChat {
    return appDatabase.realm.write {
      // TODO: check bot sender exists?

      val latest = findLatest(session) ?: error("No chat session found")
      val sender = botSender?.let { // TODO: why created by chatviewmodel cause [Unmanaged objects don't support backlinks.]
        val sender = findLatest(it)?.inclusive
        require(session.senders.any { it.id == sender?.id }) { "Sender not found in session" }
        sender
      } ?: latest.primaryBotSender ?: error("No primary bot found")

      val chatData = MessageTextItem().createInclusive()
      val msg = copyToRealm(  // TODO: we need this??
        MessageChat().apply {
          this.sender = sender
          parts.add(chatData)
        }
      )

      latest.messages.add(msg)

      return@write msg
    }
  }

  suspend fun clearMessageParts(chat: MessageChat) {
    appDatabase.realm.write {
      val chatData = findLatest(chat)
      require(chatData != null) { "No chat data found" }

      chatData.parts.clear()
    }
  }

  suspend fun appendParts(chat: MessageChat, data: List<MessageItem>, simulateTextAnimation: Boolean = false) {
    var lenOffset = chat.parts.count()

    appDatabase.realm.write {
      val chatData = findLatest(chat)
      require(chatData != null) { "No chat data found" }
      lenOffset = chatData.parts.count()

      if (simulateTextAnimation) {
        chatData.parts.addAll(
          data.map {
            when (it) {
              is MessageTextItem -> {
                MessageTextItem().createInclusive()
              }

              else -> it.createInclusive()
            }
          }
        )
      } else {
        chatData.parts.addAll(data.map { it.createInclusive() })
      }
    }

    if (simulateTextAnimation) {
      val textItems = data.withIndex().filterIsInstance<IndexedValue<MessageTextItem>>()
      for ((partIdx, item) in textItems) {
        item.text.simulateTextAnimate().collect { prt ->
          updateMessage(chat) {
            val item = this.parts[partIdx + lenOffset].textItem!!
            item.text += prt
          }
        }
      }
    }
  }

  suspend fun updateMessageLastTextOrCreate(
    chat: MessageChat,
    updater: MessageTextItem.() -> Unit
  ) {
    appDatabase.realm.write {
      val chatData = findLatest(chat)
      require(chatData != null) { "No chat data found" }

      val lastTextItem = chatData.parts.lastOrNull { it.specific is MessageTextItem }
      if (lastTextItem != null) {
        updater.invoke(lastTextItem.specific as MessageTextItem)
      } else {
        val textItem = MessageTextItem().apply(updater)
        chatData.parts.add(textItem.createInclusive())
      }
    }
  }

  suspend fun updateMessage(chat: MessageChat, updater: MessageChat.() -> Unit) {
    appDatabase.realm.write {
      val chatData = findLatest(chat)
      require(chatData != null) { "No chat data found" }
      updater.invoke(chatData)
    }
  }

  suspend fun updateMessageError(
    chat: MessageChat,
    errorMessage: String
  ) {
    appDatabase.realm.write {
      val chatData = findLatest(chat)
      require(chatData != null) { "No chat data found" }

      // remove last part if it's a text and empty
      val lastTextItem = chatData.parts.lastOrNull { it.specific is MessageTextItem }
      if (lastTextItem != null && lastTextItem.textItem!!.text.isEmpty()) {
        chatData.parts.remove(lastTextItem)
      }

      // insert error message
      val errorPart = MessageChatData().apply {
        error = MessageChatError(errorMessage)
      }
      chatData.parts.add(errorPart)
    }
  }

  suspend fun updatePrimaryBot(session: ChatSession, updater: LargeLangBot.() -> Unit) {
    appDatabase.realm.write {
      val latest = findLatest(session) ?: error("No chat session found")
      val primaryBot = latest.primaryBot ?: error("No primary bot found")
      updater.invoke(primaryBot)
    }
  }

  suspend fun updateSession(session: ChatSession, updater: ChatSession.() -> Unit) {
    appDatabase.realm.write {
      val latest = findLatest(session) ?: error("No chat session found")
      updater.invoke(latest)
    }
  }

  private fun <T> MutableRealm.innerCreateMessage(
    session: ChatSession,
    sender: T,
    data: List<MessageItem>
  ): MessageChat where T : MessageSender, T : BaseRealmObject {
    val latest = findLatest(session) ?: error("No chat session found")
    val sender = findLatest(sender) ?: error("No sender found")

    val msg = copyToRealm(  // TODO: we need this??
      MessageChat().apply {
        this.sender = sender.inclusive
        this.parts.addAll(data.map { it.createInclusive() })
      }
    )

    latest.messages.add(msg)
    latest.lastChatAt = LocalDateTime.now

    return msg
  }
}

