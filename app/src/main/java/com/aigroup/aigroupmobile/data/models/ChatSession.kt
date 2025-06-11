package com.aigroup.aigroupmobile.data.models

import androidx.compose.runtime.Composable
import com.aigroup.aigroupmobile.Constants
import com.aigroup.aigroupmobile.connect.voice.VoiceCode
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.knowledge.KnowledgeBase
import com.aigroup.aigroupmobile.data.utils.LocalDateTimeAdapter
import com.aigroup.aigroupmobile.data.utils.VoiceCodeAdapter
import com.aigroup.aigroupmobile.utils.common.localDateTime
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Ignore
import io.realm.kotlin.types.annotations.PersistedName
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.datetime.LocalDateTime
import org.mongodb.kbson.ObjectId

class ChatSession: RealmObject {
    @PrimaryKey
    @PersistedName("_id")
    var id = ObjectId()

    // TODO: senders store user?

    // TODO: 需要手动保持 set 的唯一性，使用 list 是为了标注 primaryBot
    var senders: RealmList<MessageSenderInclusive> = realmListOf()

    var summaryLastMsgId: ObjectId? = null
    var summaryContent: String? = null
    var title: String? = null
    var pinned: Boolean = false

    var messages: RealmList<MessageChat> = realmListOf()
    var knowledgeBases: RealmList<KnowledgeBase> = realmListOf()

    // TODO: find better way in RQL in sort!

    @PersistedName("lastModified")
    var lastChatAtInstant = RealmInstant.now()

    @Ignore
    var lastChatAt: LocalDateTime by LocalDateTimeAdapter(ChatSession::lastChatAtInstant)

    /**
     * 上下文消息数量，null 为不限制
     */
    var historyInclude: Int? = null
    var plugins = realmListOf<String>()

    @Ignore
    var voiceCode: VoiceCode by VoiceCodeAdapter(ChatSession::voiceCodeString)

    @PersistedName("voiceCode")
    var voiceCodeString: String = AppPreferencesDefaults.defaultVoiceCode.fullCode()
}

data class ChatSummary(
    var lastMessageId: ObjectId,
    var content: String,
)

val ChatSession.summary: ChatSummary?
    get() {
        if (summaryLastMsgId == null || summaryContent == null) {
            return null
        }
        return ChatSummary(summaryLastMsgId!!, summaryContent!!)
    }

val ChatSession.lastMessage: MessageChat?
    get() {
        return messages.lastOrNull()
    }

val ChatSession.lastModifyDateTime: LocalDateTime
    get() {
        return lastMessage?.id?.localDateTime ?: id.localDateTime
    }

val ChatSession.unifiedTitle: String
    get() {
        return title?.ifEmpty { null } ?: summaryContent ?: Constants.UntitledChat
    }

/**
 * 用于在会话列表显示的标题，优先级为 title > summaryContent > lastMessage
 */
val ChatSession.sessionDisplayTitle: String
    @Composable
    get() {
        return title?.ifEmpty { null }
            ?: summaryContent
            ?: lastMessage?.readableText?.ifEmpty { null }
            ?: Constants.UntitledChat
    }

/**
 * 用于在会话列表显示的副标题，作为 [sessionDisplayTitle] 的补充，如果 [sessionDisplayTitle] 足够清晰，则为空白
 */
val ChatSession.sessionDisplaySubtitle: String?
    @Composable
    get() {
        return when {
            title?.ifEmpty { null } != null -> summaryContent ?: lastMessage?.readableText
            summaryContent != null -> null
            // TODO: 优化这里的判断
            sessionDisplayTitle == Constants.UntitledChat -> lastMessage?.readableText?.ifEmpty { null }
            else -> null
        }
    }

// TODO: 未来支持在头像区域展示多个 Sender
// TODO: 添加自定义参数 (temp 等) 机器人到 realm, 额外的机器人实体？
val ChatSession.primaryBot: LargeLangBot?
    get() {
        return senders.firstNotNullOfOrNull { it.botSender }?.langBot
    }

// TODO: should return MessageSenderBot
val ChatSession.primaryBotSender: MessageSenderInclusive?
    get() {
        return senders.firstOrNull { it.botSender != null }
    }

val ChatSession.botSenders: List<MessageSenderBot>
    get() {
        return senders.filter { it.botSender != null }.mapNotNull { it.botSender }
    }