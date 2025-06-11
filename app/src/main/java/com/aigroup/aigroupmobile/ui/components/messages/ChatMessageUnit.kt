package com.aigroup.aigroupmobile.ui.components.messages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.data.models.MessageChatData
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginRunScopeRunner
import com.aigroup.aigroupmobile.viewmodels.ChatViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface ChatMessageContentScope {
  // TODO: 剥离 ViewModel ?
  val chatViewModel: ChatViewModel
  val coroutineScope: CoroutineScope
  val isLoading: Boolean

  suspend fun previewMedia(media: MediaItem)
  fun baseClickBehavior()

  fun withPluginExecutor(
    run: ChatPluginRunScopeRunner
  )
}

abstract class ChatMessagePresentUnit(val message: MessageChat) {
  abstract val id: String

  // TODO: using kotlin lambda scope?
  @Composable
  abstract fun Content(modifier: Modifier, scope: ChatMessageContentScope)
}

interface ChatMessagePresent {
  fun processMessage(msg: MessageChat, isLoading: Boolean): List<ChatMessagePresentUnit>
}

object BubbleChatMessagePresent : ChatMessagePresent {
  override fun processMessage(msg: MessageChat, loading: Boolean): List<ChatMessagePresentUnit> {
    return msg.parts.mapIndexedNotNull { idx, part ->
      // Avoid render empty text parts
      if (!loading && part.textItem?.text?.isEmpty() == true) {
        return@mapIndexedNotNull null
      }

      BubbleMessagePresentUint(msg, part, idx)
    }
  }

  class BubbleMessagePresentUint(
    message: MessageChat,
    val item: MessageChatData,
    private val partIdx: Int,
    private val disableLoading: Boolean = false
  ) : ChatMessagePresentUnit(message) {
    override val id: String
      get() = message.id.toHexString() + "-" + partIdx

    @Composable
    override fun Content(modifier: Modifier, scope: ChatMessageContentScope) {
      val voiceCode = scope.chatViewModel.session.value?.voiceCode

      ChatMessageBubble(
        message = message,
        part = item,
        loading = if (disableLoading) false else scope.isLoading,
        onRetry = {
          scope.chatViewModel.retryMessage(message)
        },
        onEdit = {
          scope.chatViewModel.editMessage(message, partIdx, it)
        },
        onPreviewMedia = { media ->
          scope.coroutineScope.launch {
            scope.previewMedia(media)
          }
        },
        onTranslateContent = { part, locale ->
          scope.chatViewModel.translateMessage(message, partIdx, locale)
        },
        voiceCode = voiceCode ?: AppPreferencesDefaults.defaultVoiceCode,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 10.dp)
          .then(modifier)
      )
    }
  }
}
