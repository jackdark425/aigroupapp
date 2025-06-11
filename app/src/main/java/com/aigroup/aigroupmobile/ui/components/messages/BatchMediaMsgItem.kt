package com.aigroup.aigroupmobile.ui.components.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.LargeLangBot
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.data.models.MessageChatData
import com.aigroup.aigroupmobile.data.models.MessageImageItem
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.data.models.specific
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginRunScopeRunner
import com.aigroup.aigroupmobile.ui.components.MediaGrid
import com.aigroup.aigroupmobile.ui.components.MediaItemViewInteractionScope
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.viewmodels.ChatViewModel
import io.realm.kotlin.ext.realmListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 将消息中的多个媒体内容渲染到一个消息单位，可用于文生图交互
 */
abstract class BatchMediaMsgPresent : ChatMessagePresent {
  companion object {
    const val TAG = "BatchMediaMsgPresent"
  }

  @Composable
  abstract fun ColumnScope.ItemMenuContent(
    message: MessageChat,
    part: MessageChatData,
    partIdx: Int,
    scope: ChatMessageContentScope,
    interactionScope: MediaItemViewInteractionScope
  )

  override fun processMessage(msg: MessageChat, isLoading: Boolean): List<ChatMessagePresentUnit> {
    val mediaItems = msg.parts.filter {
      (it.imageItem?.image ?: it.videoItem?.video) != null
    }
    if (mediaItems.isEmpty()) {
      return emptyList()
    }

    val restItems = msg.parts.filter {
      (it.imageItem?.image ?: it.videoItem?.video) == null
    }

    return listOf(
      BatchMediaMsgItem(
        msg, mediaItems,
        itemMenuContent = { msg, it, idx, scope, interactionScope ->
          ItemMenuContent(msg, it, idx, scope = scope, interactionScope = interactionScope)
        }
      )
    ) + restItems.mapIndexed { idx, it ->
      BubbleChatMessagePresent.BubbleMessagePresentUint(msg, it, idx, disableLoading = true)
    }
  }

  class BatchMediaMsgItem(
    msg: MessageChat,
    private val mediaParts: List<MessageChatData>, // TODO: ensure type is media in type system

    private val itemMenuContent: @Composable ColumnScope.(
      MessageChat,
      MessageChatData,
      Int,
      ChatMessageContentScope,
      MediaItemViewInteractionScope
    ) -> Unit = { _, _, _, _, _ -> },
  ) : ChatMessagePresentUnit(msg) {
    override val id: String
      get() = "batch-media-${message.id.toHexString()}"

    @Composable
    override fun Content(modifier: Modifier, scope: ChatMessageContentScope) {
      require(mediaParts.all { (it.imageItem ?: it.videoItem) != null }) {
        "All parts should be media items"
      }

      val isBot = message.sender?.specific is MessageSenderBot

      ChatMessageBubbleBase(
        message = message,
        showLoadingBadge = scope.isLoading,
        content = {
          BoxWithConstraints(
            modifier = Modifier
              .clip(MaterialTheme.shapes.medium)
              .background(MaterialTheme.colorScheme.surfaceContainerLowest)
              .padding(5.dp)
          ) {

            val fraction = if (isBot) 0.95f else 0.85f
            Box(
              Modifier
                .widthIn(0.dp, maxWidth * fraction)
                .heightIn(max = 200.dp)
            ) {
              // create a layout for mediaItems, the count of mediaItems should be 1-4
              val mediaItemsCount = mediaParts.count()
              val itemsInGrid = mediaParts.take(4)

              Column {
                MediaGrid(
                  items = itemsInGrid.withIndex().toList(),
                  mediaItem = { (it.value.imageItem?.image ?: it.value.videoItem?.video)!! },
                  onClick = { item ->
                    scope.baseClickBehavior()

                    val mediaItem = item.value.imageItem?.image ?: item.value.videoItem?.video
                    if (!mediaItem?.url.isNullOrEmpty()) {
                      scope.coroutineScope.launch {
                        scope.previewMedia(mediaItem!!)
                      }
                    }
                  },
                  onLongPress = { _item ->
                    scope.baseClickBehavior()
                    if (!scope.isLoading) {
                      toggleMenu()
                    }
                  },
                  menuContent = { it, interactionScope ->
                    itemMenuContent(message, it.value, it.index, scope, interactionScope)
                  },
                  modifier = Modifier.fillMaxSize(),
                )

                if (mediaItemsCount > 4) {
                  // TODO: show more button
                }
              }
            }
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 10.dp)
          .then(modifier)
      )
    }
  }
}

// PREVIEW

private object BatchMediaMsgPresentBasic : BatchMediaMsgPresent() {
  @Composable
  override fun ColumnScope.ItemMenuContent(
    message: MessageChat,
    item: MessageChatData,
    idx: Int,
    scope: ChatMessageContentScope,
    interactionScope: MediaItemViewInteractionScope
  ) {
  }
}

private val ImageLinks = listOf(
  "https://unsplash.com/photos/a-mountain-with-a-fire-in-the-middle-of-it--TE9BIVa1Zo",
  "https://unsplash.com/photos/a-view-of-the-grand-canyon-from-a-plane-EuTlfLqYWp8",
  "https://unsplash.com/photos/a-couple-of-people-standing-on-top-of-a-sandy-beach-bp_sR7TIo9s",
//  "https://unsplash.com/photos/a-view-of-the-grand-canyon-from-a-plane-EuTlfLqYWp8",
)

@Preview(showSystemUi = true)
@Composable
fun PreviewBatchMediaMsgItem() {
  val message = MessageChat().apply {
    sender = MessageSenderBot().apply {
      name = "AI"
      langBot = LargeLangBot().apply {
        largeLangModelCode = ModelCode("gpt-4o", ChatServiceProvider.OFFICIAL).fullCode()
      }
    }.createInclusive()
    parts = realmListOf(
      MessageImageItem().apply { image = ImageMediaItem(ImageLinks[0]) }.createInclusive(),
      MessageImageItem().apply { image = ImageMediaItem(ImageLinks[1]) }.createInclusive(),
      MessageImageItem().apply { image = ImageMediaItem(ImageLinks[2]) }.createInclusive()
    )
  }

  val items = BatchMediaMsgPresentBasic.processMessage(
    message,
    false,
  )
  val coroutineScope = rememberCoroutineScope()

  AIGroupAppTheme {
    LazyColumn(
      Modifier
        .fillMaxSize()
        .background(AppCustomTheme.colorScheme.groupedBackground)
    ) {
      items(items, key = { it.id }) {
        val scope = object : ChatMessageContentScope {
          override val chatViewModel: ChatViewModel
            get() = error("Preview not support chat view model")
          override val coroutineScope: CoroutineScope
            get() = coroutineScope
          override val isLoading: Boolean
            get() = false

          override suspend fun previewMedia(media: MediaItem) {
            println("Preview media: $media")
          }

          override fun baseClickBehavior() {

          }

          override fun withPluginExecutor(
            run: ChatPluginRunScopeRunner
          ) {

          }
        }

        it.Content(Modifier.fillMaxWidth(), scope)
      }
    }
  }
}