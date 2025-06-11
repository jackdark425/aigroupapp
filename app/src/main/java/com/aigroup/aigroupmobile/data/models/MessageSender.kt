package com.aigroup.aigroupmobile.data.models

import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.embedBackLink
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PersistedName
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

sealed interface MessageSender {
    fun createInclusive(): MessageSenderInclusive {
        val role = when (this) {
            is MessageSenderUser -> "user"
            is MessageSenderBot -> "assistant"
        }
        return MessageSenderInclusive().also {
            it.role = role
            when (this@MessageSender) {
                is MessageSenderUser -> it.userSender = this
                is MessageSenderBot -> it.botSender = this
            }
        }
    }

    val inclusive: MessageSenderInclusive

    val username: String
        get() = when (this) {
            is MessageSenderUser -> userProfile?.username ?: ""
            is MessageSenderBot -> name
        }
}

class MessageSenderInclusive : RealmObject {
    @PrimaryKey
    @PersistedName("_id")
    var id = ObjectId()
    var role: String = ""

    var userSender: MessageSenderUser? = null
    var botSender: MessageSenderBot? = null
}

class MessageSenderUser : EmbeddedRealmObject, MessageSender {
    var userProfile: UserProfile? = null
    override val inclusive: MessageSenderInclusive by embedBackLink(MessageSenderInclusive::userSender)
}

val MessageSenderInclusive.specific: MessageSender
    get() = when {
        userSender != null -> userSender!!
        botSender != null -> botSender!!
        else -> throw IllegalArgumentException("Unknown sender type")
    }

