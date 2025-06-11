package com.aigroup.aigroupmobile.data.models

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appStringResource
import io.realm.kotlin.ext.backlinks
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Ignore
import io.realm.kotlin.types.annotations.PersistedName
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class MessageChat : RealmObject {
  @PrimaryKey
  @PersistedName("_id")
  var id = ObjectId()

  var sender: MessageSenderInclusive? = null
  var parts: RealmList<MessageChatData> = realmListOf()

  var pluginId: String? = null

  // TODO: 未来考虑更通用的 Bytes？
  var pluginExtra: String? = null

  val session by backlinks<ChatSession>(ChatSession::messages)
}

class MessageChatData : EmbeddedRealmObject {
  var error: MessageChatError? = null

  var textItem: MessageTextItem? = null
  var imageItem: MessageImageItem? = null
  var videoItem: MessageVideoItem? = null
  var docItem: MessageDocItem? = null
}

class MessageChatError(
  var message: String,
  var errorCode: String? = null
) : EmbeddedRealmObject {
  constructor() : this("")
}

val MessageChatData.specific: MessageItem?
  get() = when {
    textItem != null -> textItem
    imageItem != null -> imageItem
    videoItem != null -> videoItem
    docItem != null -> docItem
    error != null -> null
    else -> throw IllegalArgumentException("Unknown message item type")
  }

val MessageChatData.mediaItem: MediaItem?
  get() = when (val detail = specific) {
    is MessageImageItem -> detail.image
    is MessageVideoItem -> detail.video
    is MessageDocItem -> detail.document
    else -> null
  }

val MessageChat.readableText: String
  get() {
    return parts.joinToString(separator = "") {
      when (val specific = it.specific) {
        is MessageTextItem -> specific.text.replace(System.lineSeparator(), " ")
        is MessageImageItem -> appStringResource(R.string.label_message_chat_readable_text_image, specific.helpText)
        is MessageVideoItem -> appStringResource(R.string.label_message_chat_readable_text_video, specific.helpText)
        is MessageDocItem -> appStringResource(R.string.label_message_chat_readable_text_doc, "") // TODO: should include in context? and show help text
        else -> ""
      }
    }
  }