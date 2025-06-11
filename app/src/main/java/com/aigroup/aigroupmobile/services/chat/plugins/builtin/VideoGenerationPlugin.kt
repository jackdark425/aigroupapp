package com.aigroup.aigroupmobile.services.chat.plugins.builtin

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.aallam.openai.api.chat.ToolBuilder
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appStringResource
import com.aigroup.aigroupmobile.connect.video.VideoGenerator
import com.aigroup.aigroupmobile.connect.video.VideoModelCode
import com.aigroup.aigroupmobile.connect.video.videoAI
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.data.models.MessageTextItem
import com.aigroup.aigroupmobile.data.models.MessageVideoItem
import com.aigroup.aigroupmobile.data.models.PreviewMediaItem
import com.aigroup.aigroupmobile.data.models.VideoMediaItem
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPlugin
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginDescription
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginEffectRunScope
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginRunScope
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginUpdateScope
import com.aigroup.aigroupmobile.ui.components.messages.BubbleChatMessagePresent
import com.aigroup.aigroupmobile.ui.components.messages.ChatMessagePresentUnit
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.utils.common.simulateTextAnimate
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import io.ktor.http.Url
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class VideoGenerationPlugin : ChatPlugin() {
  @Serializable
  private data class VideoGenerationExtra(
    val taskId: String,
    val model: String,
    val status: Status,
    val prompt: String? = null
  ) {
    enum class Status {
      Processing,
      Success,
      Fail
    }
  }

  companion object : ChatPluginDescription<VideoGenerationPlugin> {
    const val TAG = "VideoGenerationPlugin"

    override val name = "video-generation"
    override val displayName: String
      @Composable
      get() = stringResource(R.string.label_plugin_name_video_generation)
    override val icon = @Composable { ImageVector.vectorResource(R.drawable.ic_video_camera_icon) }
    override val tintColor: Color = MaterialColors.Purple[100]
    override val iconColor: Color = MaterialColors.Purple[900]

    override val builder: ToolBuilder.() -> Unit = {
      function(
        name = name,
        description = "Generate a video based on the entered prompt"
      ) {
        put("type", "object")
        putJsonObject("properties") {
          putJsonObject("prompt") {
            put("type", "string")
            put("description", "The prompt for video generation")
          }
        }
        putJsonArray("required") {
          add("prompt")
        }
      }
    }

    override fun create(): VideoGenerationPlugin {
      return VideoGenerationPlugin()
    }
  }

  private fun ChatPluginRunScope.createGenerator(videoModelCode: VideoModelCode): VideoGenerator {
    return userPreferences.videoAI(videoModelCode)
  }

  private fun ChatPluginEffectRunScope.createGenerator(videoModelCode: VideoModelCode): VideoGenerator {
    return userPreferences.videoAI(videoModelCode)
  }

  /**
   * @return return the task id
   */
  private suspend fun ChatPluginRunScope.createSingleVideo(
    prompt: String,
    model: VideoModelCode? = null,
  ): String {
    val videoModel = model ?: VideoModelCode.fromFullCode(userPreferences.defaultVideoModel)

    val generator = createGenerator(videoModel)
    val taskId = generator.createVideo(
      modelCode = videoModel.code,
      prompt = prompt,
    )

    return taskId
  }

  override suspend fun execute(
    args: JsonObject,
    botMessage: MessageChat,
    run: suspend (suspend ChatPluginRunScope.() -> Unit) -> Unit,
    updater: suspend (suspend ChatPluginUpdateScope.() -> Unit) -> Unit
  ) {
    val prompt = args.getValue("prompt").jsonPrimitive.content
    Log.i(TAG, "Generate video: $prompt")

    run {
      val videoCode = VideoModelCode.fromFullCode(userPreferences.defaultVideoModel)

      updater {
        chatDao.clearMessageParts(botMessage)

        // create placeholder
        // TODO: 不要用空 url 表达 placeHolder
        // TODO: show error message using toast or show on ui

        chatDao.appendParts(
          botMessage,
          listOf(
            MessageVideoItem().apply {
              this.video = VideoMediaItem()
              this.helpText = prompt
            }
          )
        )

        Log.i(
          TAG,
          "Generate video (batch mode) by ${videoCode.fullCode()}: $prompt"
        )
        try {
          val taskId = createSingleVideo(prompt)
          val status = VideoGenerationExtra(
            taskId = taskId,
            model = videoCode.fullCode(),
            status = VideoGenerationExtra.Status.Processing,
            prompt = prompt
          )
          updatePluginExtraString(botMessage, Json.encodeToString(status))
        } catch (e: Exception) {
          Log.e(TAG, "Failed to generate images (commit task)", e)
        }

        chatDao.appendParts(
          botMessage,
          listOf(
            MessageTextItem().apply { this.text = appStringResource(R.string.chat_message_video_generation_working, prompt) }
          ),
          simulateTextAnimation = true
        )
      }
    }
  }

  override fun shouldReRunEffect(oldExtra: String?, newExtra: String?): Boolean {
    val extra = newExtra?.let {
      Json.decodeFromString<VideoGenerationExtra>(it)
    }
    // 状态为处理中需要重新执行 effect
    return extra?.status == VideoGenerationExtra.Status.Processing
  }

  override fun shouldRunEffect(present: ChatMessagePresentUnit): Boolean {
    val extra = present.message.pluginExtra?.let {
      Json.decodeFromString<VideoGenerationExtra>(it)
    }
    if (extra?.status == VideoGenerationExtra.Status.Success) {
      return false
    }

    // 只处理视频块
    if (present is BubbleChatMessagePresent.BubbleMessagePresentUint) {
      return present.item.videoItem != null && present.message.pluginExtra != null
    }
    return false
  }

  override suspend fun launchEffect(
    present: ChatMessagePresentUnit,
    userInteraction: Boolean,
    run: suspend (suspend ChatPluginEffectRunScope.() -> Unit) -> Unit,
    updater: suspend (suspend ChatPluginUpdateScope.() -> Unit) -> Unit
  ): EffectDispatch {
    val botMessage = present.message

    val extra = botMessage.pluginExtra?.let {
      Json.decodeFromString<VideoGenerationExtra>(it)
    }

    if (extra == null) {
      // TODO: 如果请求错误导致内存泄漏，加一个 maxTime？
      return EffectDispatch.Next(2000) // 2s 后检查状态
    }

    // TODO: 优化写法
    var dispatch: EffectDispatch? = null

    run {
      val model = VideoModelCode.fromFullCode(extra.model)
      val generator = createGenerator(model)

      dispatch = try {
        Log.i(TAG, "Retrieve video generation status: ${extra.taskId}")

        val response = generator.retrieveTaskStatus(extra.taskId)
        response?.let {
          Log.i(TAG, "Video generation got: $it")

          updater {
            onVideoGenerationDone(extra, botMessage, response)
          }
          EffectDispatch.Done
        } ?: EffectDispatch.Next(5000)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to retrieve task status", e)
        EffectDispatch.Done
      }
    }

    return dispatch!!
  }

  @Composable
  override fun PreviewLeadingTopContent(
    previewMedia: PreviewMediaItem
  ) {
    val modelCode = remember {
      // TODO: decode json in UI thread is not good !!
      val obj = previewMedia.plugin?.extra?.let {
        Json.decodeFromString<VideoGenerationExtra>(it)
      }
      obj?.model?.let { VideoModelCode.fromFullCode(it) }
    }

    modelCode?.let {
      Box(
        Modifier
          .clip(MaterialTheme.shapes.medium)
          .background(Color.White)
          .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Text(
          stringResource(R.string.label_video_generated_by, it.fullDisplayName()),
          style = MaterialTheme.typography.labelSmall,
          color = AppCustomTheme.colorScheme.secondaryLabel
        )
      }
    }
  }

  private suspend fun ChatPluginUpdateScope.onVideoGenerationDone(
    extra: VideoGenerationExtra,
    botMessage: MessageChat,
    video: VideoGenerator.VideoGeneration
  ) {
    val (localUri, _media) = pathManager.downloadMediaToStorage(Url(video.videoLink))
    Log.i(TAG, "Downloaded video to: $localUri")

    val (coverLocalUri, _coverMedia) = pathManager.downloadMediaToStorage(Url(video.coverLink))
    Log.i(TAG, "Downloaded cover to: $coverLocalUri")

    chatDao.updateMessage(botMessage) {
      // find video item
      val videoItem = parts.find { it.videoItem != null }?.videoItem
      videoItem?.let {
        it.video = VideoMediaItem(
          localUri.toString(),
          snapshot = ImageMediaItem(coverLocalUri.toString())
        )
      }

      // find text item
      val textItem = parts.find { it.textItem != null }?.textItem
      textItem?.let {
        it.text = ""
      }
    }

    val answer = if (extra.prompt.isNullOrEmpty())
      appStringResource(R.string.chat_message_video_generation_generated_message)
    else
      appStringResource(R.string.chat_message_video_generation_generated_message_with_prompt, extra.prompt)

    answer.simulateTextAnimate().collect {
      chatDao.updateMessageLastTextOrCreate(botMessage) {
        this.text += it
      }
    }

    val status = extra.copy(
      status = VideoGenerationExtra.Status.Success
    )
    updatePluginExtraString(botMessage, Json.encodeToString(status))
  }

}