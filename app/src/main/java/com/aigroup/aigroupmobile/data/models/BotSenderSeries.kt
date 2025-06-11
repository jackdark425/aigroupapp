package com.aigroup.aigroupmobile.data.models

import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.embedBackLink
import com.aigroup.aigroupmobile.data.models.knowledge.KnowledgeBase
import com.aigroup.aigroupmobile.repositories.AssistantRepository
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PersistedName
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId
import java.time.LocalDateTime

class MessageSenderBot(
  var name: String,
  var description: String?,
  var langBot: LargeLangBot?,
  var assistant: BotAssistant? = null,
) : EmbeddedRealmObject, MessageSender {
  constructor() : this("", "", LargeLangBot())
  override val inclusive: MessageSenderInclusive by embedBackLink(MessageSenderInclusive::botSender)
}

class BotAssistant: RealmObject {
  @PrimaryKey
  @PersistedName("_id")
  var id = ObjectId()

  var storeIdentifier: String = ""

  // TODO: better solution
  var avatar: ImageMediaItem? = null
  var avatarEmoji: String? = null

  var tags = realmListOf<String>()
  var presetsPrompt: String = ""
  var assistantSchemeStr: String = ""

  var knowledgeBases: RealmList<KnowledgeBase> = realmListOf()
}

class LargeLangBot(
  // TODO: using realm adapter
  var largeLangModelCode: String
) : EmbeddedRealmObject {
  var temperature: Double? = null
  var maxTokens: Int? = null
  var topP: Double? = null
  var presencePenalty: Double? = null
  var frequencyPenalty: Double? = null

  constructor() : this("")
}

val LargeLangBot.model: ModelCode
  get() = ModelCode.fromFullCode(largeLangModelCode)

fun BotAssistant.parseAssistantScheme(): RemoteAssistant {
  return AssistantRepository.Json.decodeFromString(assistantSchemeStr)
}