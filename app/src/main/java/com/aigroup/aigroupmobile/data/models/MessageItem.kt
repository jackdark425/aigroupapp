package com.aigroup.aigroupmobile.data.models

import io.realm.kotlin.ext.isManaged
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject

sealed interface MessageItem {
  fun createInclusive(): MessageChatData {
    return MessageChatData().also {
      when (this) {
        is MessageTextItem -> it.textItem = this
        is MessageImageItem -> it.imageItem = this
        is MessageVideoItem -> it.videoItem = this
        is MessageDocItem -> it.docItem = this
      }
    }
  }

  // Realm Plugin will check class implements RealmObject so cannot make MessageItem to implement RealmObject
  fun isManaged(): Boolean
}

// TODO: 不要用多态

class MessageTextItem(
  var text: String
) : EmbeddedRealmObject, MessageItem {
  var translatedText: String? = null

  var references: RealmList<MessageTextItemReference> = realmListOf()

  constructor() : this("")

  override fun isManaged(): Boolean {
    return (this as EmbeddedRealmObject).isManaged()
  }
}

class MessageTextItemReference(
  var selectionStart: Int,
  var selectionEnd: Int,
): EmbeddedRealmObject {
  var url: String = ""

  constructor() : this(-1, -1)

  val hasSelection: Boolean
    get() = selectionStart >= 0 && selectionEnd >= 0
}

// TODO: design MessageMediaItem

class MessageImageItem : EmbeddedRealmObject, MessageItem {
  var image: ImageMediaItem? = null

  /**
   * 用于帮助 LLM 在不支持的情况下使用辅助上下文
   */
  var helpText: String = ""

  override fun isManaged(): Boolean {
    return (this as EmbeddedRealmObject).isManaged()
  }
}

class MessageVideoItem : EmbeddedRealmObject, MessageItem {
  var video: VideoMediaItem? = null

  /**
   * 用于帮助 LLM 在不支持的情况下使用辅助上下文
   */
  var helpText: String = ""

  override fun isManaged(): Boolean {
    return (this as EmbeddedRealmObject).isManaged()
  }
}

class MessageDocItem: EmbeddedRealmObject, MessageItem {
  var document: DocumentMediaItem? = null

  override fun isManaged(): Boolean {
    return (this as EmbeddedRealmObject).isManaged()
  }
}