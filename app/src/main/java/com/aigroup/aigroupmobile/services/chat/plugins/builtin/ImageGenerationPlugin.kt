package com.aigroup.aigroupmobile.services.chat.plugins.builtin

import android.content.res.Resources
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.datastore.dataStore
import com.aallam.openai.api.chat.ToolBuilder
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.model.ModelId
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appStringResource
import com.aigroup.aigroupmobile.connect.images.ImageGenerateServiceProvider
import com.aigroup.aigroupmobile.connect.images.ImageGenerator
import com.aigroup.aigroupmobile.connect.images.ImageModelCode
import com.aigroup.aigroupmobile.connect.images.imageAI
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.data.models.MessageChatData
import com.aigroup.aigroupmobile.data.models.MessageImageItem
import com.aigroup.aigroupmobile.data.models.MessageTextItem
import com.aigroup.aigroupmobile.data.models.PreviewMediaItem
import com.aigroup.aigroupmobile.data.models.specific
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPlugin
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginDescription
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginRunScope
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginUpdateScope
import com.aigroup.aigroupmobile.services.chat.plugins.builtin.ImageGenerationPlugin.Companion
import com.aigroup.aigroupmobile.ui.components.AppDropdownMenuItem
import com.aigroup.aigroupmobile.ui.components.MediaItemViewInteractionScope
import com.aigroup.aigroupmobile.ui.components.messages.BatchMediaMsgPresent
import com.aigroup.aigroupmobile.ui.components.messages.ChatMessageContentScope
import com.aigroup.aigroupmobile.ui.components.messages.ChatMessagePresent
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import io.ktor.http.Url
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject


object ImageGenerationPresent : BatchMediaMsgPresent() {
  @Composable
  override fun ColumnScope.ItemMenuContent(
    message: MessageChat,
    part: MessageChatData,
    partIdx: Int,
    scope: ChatMessageContentScope,
    interactionScope: MediaItemViewInteractionScope
  ) {
    val context = LocalContext.current

    AppDropdownMenuItem(
      text = { Text(stringResource(R.string.label_menu_regenerate_image)) },
      leadingIcon = {
        Icon(
          ImageVector.vectorResource(R.drawable.ic_retry_icon),
          "",
          modifier = Modifier.size(20.dp)
        )
      },
      onClick = {
        scope.baseClickBehavior()
        interactionScope.toggleMenu()

        scope.withPluginExecutor { plugin, updater ->
          if (plugin !is ImageGenerationPlugin) {
            return@withPluginExecutor
          }

          // read extra
          // 重新生成的逻辑: 如果找到 extra 中的 model 信息，就用这个 model 重新生成，否则采用默认值，
          // 即 user preferences 中的或者 chat session 中的设置
          val modelCode = message.pluginExtra?.let {
            try {
              Json.decodeFromString(JsonObject.serializer(), it)["model"]?.jsonPrimitive?.content?.let {
                ImageModelCode.fromFullCode(it)
              }
            } catch (e: Exception) {
              null
            }
          }

          with(plugin) {
            val prompt = part.imageItem?.helpText
            if (prompt.isNullOrEmpty()) {
              return@withPluginExecutor
            }

            updater { message ->
              scope.chatViewModel.startMessageLoading(message) {
                // replace item as placeholder
                // TODO: 不要用空 url 表达 placeHolder
                chatDao.updateMessage(message) {
                  val item = this.parts[partIdx].specific
                  if (item is MessageImageItem) {
                    item.image = ImageMediaItem()
                  }
                }

                try {
                  // TODO: 删除老旧图像
                  Log.i(
                    ImageGenerationPlugin.TAG,
                    "Re-generate single image by ${modelCode?.fullDisplayName()}: $prompt"
                  )
                  val link = createSingleImage(prompt, modelCode)
                  onImageGenerationDone(message, partIdx, link)
                } catch (e: Exception) {
                  Log.e(ImageGenerationPlugin.TAG, "Failed to re-generate image", e)
                  Toast.makeText(context, context.getString(R.string.toast_regenerate_image_fail), Toast.LENGTH_SHORT).show()
                }
              }
            }
          }
        }
      }
    )
  }
}

class ImageGenerationPlugin : ChatPlugin() {
  companion object : ChatPluginDescription<ImageGenerationPlugin> {
    const val TAG = "ImageGenerationPlugin"

    override val name = "text-to-image"

    override val displayName: String
      @Composable
      get() = stringResource(R.string.label_plugin_name_text_to_image)

    override val icon = @Composable { ImageVector.vectorResource(R.drawable.ic_image_icon) }
    override val tintColor: Color = MaterialColors.Orange[100]
    override val iconColor: Color = MaterialColors.Orange[900]

    override val present: ChatMessagePresent
      get() = ImageGenerationPresent

    override val builder: ToolBuilder.() -> Unit = {
      function(
        name = name,
        description = "Generate an image based on the entered prompt"
      ) {
        put("type", "object")
        putJsonObject("properties") {
          putJsonObject("prompt") {
            put("type", "string")
            put("description", "The prompt for image generation")
          }
        }
        putJsonArray("required") {
          add("prompt")
        }
      }
    }

    override fun create(): ImageGenerationPlugin {
      return ImageGenerationPlugin()
    }
  }

  private fun ChatPluginRunScope.createGenerator(provider: ImageGenerateServiceProvider): ImageGenerator {
    return userPreferences.imageAI(provider)
  }

  suspend fun ChatPluginRunScope.createSingleImage(
    prompt: String,
    model: ImageModelCode? = null,
  ): String {
    val imageModel = model ?: ImageModelCode.fromFullCode(userPreferences.defaultImageModel)
    val imageResolution = userPreferences.defaultImageResolution

    val generator = createGenerator(imageModel.serviceProvider)
    val link = generator.createImages(
      modelCode = imageModel.model,
      resolution = imageResolution,
      prompt = prompt,
      count = 1
    ).first()

    return link
  }

  suspend fun ChatPluginRunScope.batchCreateImages(
    prompt: String,
    count: Int,
    model: ImageModelCode? = null
  ): List<String> {
    val imageModel = model ?: ImageModelCode.fromFullCode(userPreferences.defaultImageModel)
    val imageResolution = userPreferences.defaultImageResolution

    val generator = createGenerator(imageModel.serviceProvider)
    val links = generator.createImages(
      modelCode = imageModel.model,
      resolution = imageResolution,
      prompt = prompt,
      count = count
    )

    return links
  }

  override suspend fun execute(
    args: JsonObject,
    botMessage: MessageChat,
    run: suspend (suspend ChatPluginRunScope.() -> Unit) -> Unit,
    updater: suspend (suspend ChatPluginUpdateScope.() -> Unit) -> Unit
  ) {
    val prompt = args.getValue("prompt").jsonPrimitive.content
    Log.i(TAG, "Generate image: $prompt")

    run {
      val imageModel = ImageModelCode.fromFullCode(userPreferences.defaultImageModel)
      val imageCount = userPreferences.imageN

      require(imageCount in 1..5) { "Image count should be between 1 and 5" }

      updater {
        chatDao.clearMessageParts(botMessage)

        // create placeholder
        // TODO: 不要用空 url 表达 placeHolder
        // TODO: show error message using toast or show on ui

        chatDao.appendParts(
          botMessage,
          (0 until imageCount).map {
            MessageImageItem().apply {
              this.image = ImageMediaItem()
              this.helpText = prompt
            }
          }
        )

        coroutineScope {
          val generationTasks = if (!imageModel.supportBatch) {
            Log.i(
              TAG,
              "Generate $imageCount images (parallel mode) by ${imageModel.fullCode()}: $prompt"
            )
            (0 until imageCount).map { idx ->
              async {
                try {
                  val link = createSingleImage(prompt)
                  onImageGenerationDone(botMessage, idx, link)
                } catch (e: Exception) {
                  Log.e(TAG, "Failed to generate image $idx", e)
                }
              }
            }
          } else {
            Log.i(
              TAG,
              "Generate $imageCount images (batch mode) by ${imageModel.fullCode()}: $prompt"
            )
            try {
              val links = batchCreateImages(prompt, imageCount)
              links.mapIndexed { idx, link ->
                async { onImageGenerationDone(botMessage, idx, link) }
              }
            } catch (e: Exception) {
              Log.e(TAG, "Failed to generate images", e)
              emptyList()
            }
          }

          generationTasks.awaitAll()
        }

        // 更新图片生成模型信息
        updatePluginExtra(botMessage, buildJsonObject {
          put("model", imageModel.fullCode())
        })

        chatDao.appendParts(
          botMessage,
          listOf(
            MessageTextItem().apply { this.text = appStringResource(R.string.chat_message_text2img_generated, prompt) }
          ),
          simulateTextAnimation = true
        )
      }
    }
  }

  suspend fun ChatPluginUpdateScope.onImageGenerationDone(botMessage: MessageChat, idx: Int, link: String) {
    val (localUri, _media) = pathManager.downloadMediaToStorage(Url(link))
    Log.i(TAG, "Downloaded image $idx to: $localUri")

    chatDao.updateMessage(botMessage) {
      val item = this.parts[idx].specific
      if (item is MessageImageItem) {
        item.image = ImageMediaItem(localUri.toString())
      }
    }
  }

  @Composable
  override fun PreviewLeadingTopContent(
    previewMediaItem: PreviewMediaItem
  ) {
    val modelCode = remember {
      // TODO: decode json in UI thread is not good !!
      val obj = previewMediaItem.plugin?.extra?.let {
        Json.decodeFromString(JsonObject.serializer(), it)
      }
      obj?.get("model")?.jsonPrimitive?.content?.let {
        ImageModelCode.fromFullCode(it)
      }
    }

    modelCode?.let {
      Box(
        Modifier
          .clip(MaterialTheme.shapes.medium)
          .background(Color.White)
          .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Text(
          stringResource(R.string.label_image_generated_by, it.fullDisplayName()),
          style = MaterialTheme.typography.labelSmall,
          color = AppCustomTheme.colorScheme.secondaryLabel
        )
      }
    }
  }

}