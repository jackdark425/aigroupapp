package com.aigroup.aigroupmobile.services.chat

import android.util.Log
import com.aallam.openai.api.chat.ContentPart
import com.aallam.openai.api.chat.ImagePart
import com.aallam.openai.api.chat.TextPart
import com.aallam.openai.api.chat.VideoPart
import com.aigroup.aigroupmobile.Prompts
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.data.models.MessageChatData
import com.aigroup.aigroupmobile.data.models.MessageVideoItem
import com.aigroup.aigroupmobile.data.models.VideoMediaItem
import com.aigroup.aigroupmobile.data.models.specific
import com.aigroup.aigroupmobile.services.FileUploader
import com.aigroup.aigroupmobile.utils.common.readAsBase64
import com.aigroup.aigroupmobile.viewmodels.ChatViewModel.Companion.TAG
import javax.inject.Inject

data class UpdateMediaItemResult(
  val onlineLink: String,
  val requestHeaders: Map<String, String>
)

sealed interface ChatMediaFileManager<in M : MediaItem> {

  /**
   * @return The online link of the media item. If null, means decided to skip upload.
   */
  suspend fun uploadMediaItem(mediaItem: M): UpdateMediaItemResult?

  /**
   * 检查媒体项目是否可用，如果返回 false，调度器会根据情况调用 uploadMediaItem
   */
  suspend fun checkMediaItemAvailable(mediaItem: M): Boolean

  fun toBotChatPart(mediaItem: M): ContentPart

  fun toChatPart(mediaItem: M, model: ModelCode): ContentPart?

  fun toCompatibleChatPart(
    mediaItem: M,
    chatItem: MessageChatData, // TODO: move help text to MediaItem
    modelCode: ModelCode,
    compatibilityHistory: MediaCompatibilityHistory
  ): List<ContentPart>

}

internal data object MediaFileManagerImage : ChatMediaFileManager<ImageMediaItem> {
  override suspend fun uploadMediaItem(mediaItem: ImageMediaItem): UpdateMediaItemResult? {
    return null
  }

  override suspend fun checkMediaItemAvailable(mediaItem: ImageMediaItem): Boolean {
    return true
  }

  override fun toChatPart(mediaItem: ImageMediaItem, model: ModelCode): ContentPart? {
    if (!model.supportImage) {
      return null
    }
    return mediaItem.readAsBase64Part()
  }

  override fun toCompatibleChatPart(
    mediaItem: ImageMediaItem,
    chatItem: MessageChatData, // TODO: move help text to MediaItem
    modelCode: ModelCode,
    compatibilityHistory: MediaCompatibilityHistory
  ): List<ContentPart> {
    return when (compatibilityHistory) {
      MediaCompatibilityHistory.SKIP -> emptyList()
      MediaCompatibilityHistory.HELP_PROMPT -> {
        // TODO: consider the empty help text string
        chatItem.imageItem?.helpText?.let {
          val item = TextPart(Prompts.imagePlaceholder(it))
          listOf(item)
        } ?: emptyList()
      }
    }
  }

  // TODO: save helpText for bot sent image and video
  override fun toBotChatPart(mediaItem: ImageMediaItem): ContentPart {
    return TextPart("[Bot Sent Image]")
  }
}

internal class MediaFileManagerVideo @Inject constructor(
  private val fileUploader: FileUploader
) : ChatMediaFileManager<VideoMediaItem> {

  override suspend fun uploadMediaItem(mediaItem: VideoMediaItem): UpdateMediaItemResult {
    val fileUriToUpload = mediaItem.uri.path

    val (onlineLink, options) = fileUploader.uploadFile(fileUriToUpload!!)

    return UpdateMediaItemResult(
      onlineLink = onlineLink,
      requestHeaders = options.headers
    )
  }

  override suspend fun checkMediaItemAvailable(mediaItem: VideoMediaItem): Boolean {
    // TODO: 文件会过期吗，需要重新上传吗？
    return mediaItem.onlineLink != null
  }

  override fun toChatPart(mediaItem: VideoMediaItem, model: ModelCode): ContentPart? {
    if (!model.supportVideo) {
      return null
    }
    return mediaItem.onlineLink?.let {
      VideoPart(VideoPart.VideoURL(url = it))
    }
  }

  override fun toCompatibleChatPart(
    mediaItem: VideoMediaItem,
    chatItem: MessageChatData,
    modelCode: ModelCode,
    compatibilityHistory: MediaCompatibilityHistory
  ): List<ContentPart> {
    // TODO: consider the empty help text string
    val helpText = chatItem.videoItem?.helpText?.let {
      if (modelCode.supportImage) {
        val item = TextPart(Prompts.videoImagePlaceholder(it))
        listOf(item)
      } else {
        val item = TextPart(Prompts.videoTextPlaceholder(it))
        listOf(item)
      }
    } ?: emptyList()

    return when {
      compatibilityHistory == MediaCompatibilityHistory.SKIP -> emptyList()
      compatibilityHistory == MediaCompatibilityHistory.HELP_PROMPT && modelCode.supportImage -> {
        helpText + (mediaItem.snapshot?.let { listOf(it.readAsBase64Part()) } ?: emptyList())
      }

      compatibilityHistory == MediaCompatibilityHistory.HELP_PROMPT && !modelCode.supportImage -> {
        helpText
      }

      else -> emptyList()
    }
  }

  // TODO: save helpText for bot sent image and video
  override fun toBotChatPart(mediaItem: VideoMediaItem): ContentPart {
    return TextPart("[Bot Sent Video]")
  }

}

private fun ImageMediaItem.readAsBase64Part(): ImagePart {
  return ImagePart(
    ImagePart.ImageURL(url = this.readAsBase64())
  )
}