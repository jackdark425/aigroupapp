package com.aigroup.aigroupmobile.services.chat.plugins

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.aallam.openai.api.chat.ToolBuilder
import com.aigroup.aigroupmobile.data.dao.ChatDao
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.models.PreviewMediaItem
import com.aigroup.aigroupmobile.ui.components.messages.ChatMessagePresent
import com.aigroup.aigroupmobile.ui.components.messages.ChatMessagePresentUnit
import com.aigroup.aigroupmobile.utils.system.PathManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

typealias ChatPluginRunScopeRunner = suspend ChatPluginRunScope.(
  plugin: ChatPlugin?,
  updater: suspend (suspend ChatPluginUpdateScope.(msg: MessageChat) -> Unit) -> Unit
) -> Unit

interface ChatPluginDescription<P : ChatPlugin> {
  val builder: ToolBuilder.() -> Unit
  val name: String

  @get:Composable
  val displayName: String

  val icon: @Composable () -> ImageVector
  val tintColor: Color
  val iconColor: Color

  /**
   * 该插件特定的消息展示方式
   */
  val present: ChatMessagePresent?
    get() = null

  fun create(): P
}

interface ChatPluginRunScope {
  val userAI: com.aallam.openai.client.Chat
  val userModel: ModelCode
  val userPreferences: AppPreferences

  /**
   * 请求调度一个副作用执行
   */
  fun requestEffect(userInteraction: Boolean = false, mills: Long = 0): Boolean
}

interface ChatPluginEffectRunScope {
  val userAI: com.aallam.openai.client.Chat
  val userModel: ModelCode
  val userPreferences: AppPreferences
}

interface ChatPluginUpdateScope {
  val pathManager: PathManager
  val chatDao: ChatDao

  // TODO: support type-save api for extra
  suspend fun updatePluginExtra(msg: MessageChat, extra: JsonObject) {
    val str = Json.encodeToString(extra)
    Log.i(ChatPlugin.TAG, "updating plugin extra: $str")
    chatDao.updateMessage(msg) {
      this.pluginExtra = str
    }
  }

  suspend fun updatePluginExtraString(msg: MessageChat, extra: String) {
    Log.i(ChatPlugin.TAG, "updating plugin extra: $extra")
    chatDao.updateMessage(msg) {
      this.pluginExtra = extra
    }
  }


}

abstract class ChatPlugin {
  sealed class EffectDispatch {
    data object Done: EffectDispatch()
    data class Next(val mills: Long): EffectDispatch()
  }

  companion object {
    const val TAG = "ChatPlugin"
  }

  abstract suspend fun execute(
    args: JsonObject,
    botMessage: MessageChat,
    run: suspend (suspend ChatPluginRunScope.() -> Unit) -> Unit,
    updater: suspend (suspend ChatPluginUpdateScope.() -> Unit) -> Unit
  )

  /**
   * 运行组件副作用，例如获取任务进度，视频自动播放等，副作用由 [ChatPluginExecutor] 负责调度，但是该方法可以返回一个 [EffectDispatch]
   * 来决定下次调用的时机。
   *
   * 副作用以 [ChatMessagePresentUnit] 为单位运行，插件可以自行判断 [ChatMessagePresentUnit] 的具体类型。
   *
   * @param present 当前的消息展示单元
   * @param userInteraction 是否是用户交互触发的副作用
   * @param run 用来执行副作用的代码块，可以通过 [ChatPluginEffectRunScope] 来获取当前的上下文, 包括用户配置等
   * @param updater 用来更新消息的代码块，可以通过 [ChatPluginUpdateScope] 来更新消息的插件额外信息等
   * @return 返回一个 [EffectDispatch] 来决定下次调用的时机
   */
  open suspend fun launchEffect(
    present: ChatMessagePresentUnit,
    userInteraction: Boolean,
    run: suspend (suspend ChatPluginEffectRunScope.() -> Unit) -> Unit,
    updater: suspend (suspend ChatPluginUpdateScope.() -> Unit) -> Unit
  ): EffectDispatch {
    return EffectDispatch.Done
  }

  /**
   * 用来判断是否重新执行副作用的情况，默认情况下，当 [MessageChat.pluginExtra] 改变的情况下需要重新执行。比如用户重新生成图片，pluginExtra
   * 的任务 ID 改变，应该重新调度副作用来获取任务进度
   */
  open fun shouldReRunEffect(oldExtra: String?, newExtra: String?): Boolean {
    return oldExtra != newExtra
  }

  /**
   * 插件自行决定是否需要执行副作用，通过传入的 [ChatMessagePresentUnit] 可以自行判断其类型和状态，返回 false 的情况下，只能等待下次调用副作用
   * 并在 [shouldReRunEffect] 中返回 true 来激活副作用调度
   *
   * @param present 当前的消息展示单元
   */
  open fun shouldRunEffect(present: ChatMessagePresentUnit): Boolean {
    return false
  }

  @Composable
  open fun PreviewLeadingTopContent(
    previewMedia: PreviewMediaItem,
  ) {
  }
}