package com.aigroup.aigroupmobile.services.chat.plugins

import android.util.Log
import androidx.datastore.core.DataStore
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.ToolCall
import com.aallam.openai.api.chat.ToolChoice
import com.aallam.openai.api.chat.chatCompletionRequest
import com.aallam.openai.api.model.ModelId
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.connect.chat.ai
import com.aigroup.aigroupmobile.connect.chat.officialAI
import com.aigroup.aigroupmobile.data.dao.ChatDao
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.data.models.model
import com.aigroup.aigroupmobile.data.models.specific
import com.aigroup.aigroupmobile.ui.components.messages.ChatMessagePresentUnit
import com.aigroup.aigroupmobile.utils.system.PathManager
import com.aigroup.aigroupmobile.viewmodels.ChatViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * 跟 ChatViewModel 绑定
 */
class ChatPluginExecutor @Inject constructor(
  private val dataStore: DataStore<AppPreferences>,
  private val pathManager: PathManager,
  private val chatDao: ChatDao
) {
  sealed class EffectStatus(pluginExtra: String?) {
    /**
     * 永远不再执行
     */
    data class Never(val pluginExtra: String?) : EffectStatus(pluginExtra)

    /**
     * 等待下一次执行
     */
    data class NextRun(val timestamp: Instant, val intervalMill: Long, val pluginExtra: String?) :
      EffectStatus(pluginExtra)

    /**
     * 正在执行
     */
    data class Running(val pluginExtra: String?) : EffectStatus(pluginExtra)
  }

  companion object {
    private const val TAG = "ChatPluginExecutor"
    private const val EffectMinDelay = 100L
  }

  private val coroutineScope by lazy {
    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
      throwable.printStackTrace()
    }
    CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler + CoroutineName("ChatPluginExecutor-Coroutine"))
  }

  internal fun close() {
    effectsRunnable.forEach { (_, runnable) ->
      runnable.cancel()
    }
    effectsRunnable.clear()
    Log.i(TAG, "ChatPluginExecutor closed")
  }

  /**
   * Plugin effects cache, messageChat id to plugin
   */
  private val effects: MutableMap<String, EffectStatus> = mutableMapOf()
  private val effectsRunnable: MutableMap<String, Job> = mutableMapOf()

  private fun clearEffectStatus(present: ChatMessagePresentUnit) {
    val effectId = "effect@" + present.id
    effects.remove(effectId)
  }

  private fun markEffectRunning(present: ChatMessagePresentUnit) {
    val effectId = "effect@" + present.id
    effects[effectId] = EffectStatus.Running(present.message.pluginExtra)
  }

  private fun markEffectDone(present: ChatMessagePresentUnit) {
    val effectId = "effect@" + present.id
    effects.remove(effectId)
  }

  private fun markEffectNextRun(present: ChatMessagePresentUnit, durationMills: Long) {
    val effectId = "effect@" + present.id
    effects[effectId] = EffectStatus.NextRun(Clock.System.now(), durationMills, present.message.pluginExtra)
  }

  private fun markEffectNeverExecute(present: ChatMessagePresentUnit) {
    val effectId = "effect@" + present.id
    effects[effectId] = EffectStatus.Never(present.message.pluginExtra)
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun coordinateEffectNextRun(present: ChatMessagePresentUnit, userInteraction: Boolean, durationMills: Long = 0) {
    val effectId = "effect@" + present.id

    if (durationMills == 0L && userInteraction) {
      GlobalScope.launch(Dispatchers.Default) {
        requestEffectFor(present, true)
      }
      return
    }

    markEffectNextRun(present, durationMills) // 标记下次执行时间

    val job = coroutineScope.launch {
      delay(durationMills + EffectMinDelay)
      requestEffectFor(present, userInteraction)
    }
    effectsRunnable[effectId] = job
  }

  private fun clearCoordinate(present: ChatMessagePresentUnit) {
    val effectId = "effect@" + present.id
    effectsRunnable[effectId]?.let {
      it.cancel()
      effectsRunnable.remove(effectId)
    }
  }

  /**
   * 请求执行插件副作用事件, 非线程安全!
   *
   * TODO: 使用 actor 实现线程安全
   *
   * @param present 执行副作用的 present 对象
   * @param userInteraction 是否是用户交互触发，默认为 false
   * @return 返回true表示副作用已经被处理，否则返回false
   */
  private suspend fun requestEffectFor(present: ChatMessagePresentUnit, userInteraction: Boolean = false): Boolean =
    withContext(Dispatchers.Default) {
      val messageChat = present.message
      val effectId = "effect@" + present.id

      Log.i(TAG, "requestEffectFor: [${effectId}] $present")

      // plugin 副作用只适用于 bot 消息
      if (messageChat.sender?.specific !is MessageSenderBot) {
        Log.w(TAG, "[requestEffectFor reject] Plugin effect only for bot message: [$effectId]")
        return@withContext false
      }
      val botSender = messageChat.sender!!.specific as MessageSenderBot

      // 获取插件实例
      val pluginId = BuiltInPlugins.plugins.firstOrNull { messageChat.pluginId == it.name }
      if (pluginId == null) {
        Log.w(TAG, "[requestEffectFor reject] Plugin not found for message: $effectId")
        return@withContext false
      }
      val plugin = pluginId.create()

      // 如果有副作用状态，需要判断能否执行后续副作用
      if (!userInteraction && effects.containsKey(messageChat.id.toHexString())) {
        when (val status = effects[effectId]!!) {
          is EffectStatus.NextRun -> {
            // 已经规划下一次执行的任务
            val now = Clock.System.now()
            if (now < status.timestamp + status.intervalMill.milliseconds) {
              // 还没到下一次执行时间。不允许执行
              Log.i(TAG, "[requestEffectFor reject] Plugin effect are waiting for next run for message: $effectId")
              return@withContext false
            }
            // 允许执行!
          }

          is EffectStatus.Running -> {
            // 拒绝执行已经在执行的任务
            Log.i(TAG, "[requestEffectFor reject] Plugin effect are running for message: $effectId")
            return@withContext false
          }

          is EffectStatus.Never -> {
            // 原则上不会再调用的任务
            if (plugin.shouldReRunEffect(status.pluginExtra, messageChat.pluginExtra)) {
              Log.i(TAG, "[requestEffectFor reject] Plugin effect should re-run for message: $effectId")
              // 由 shouldReRunEffect 激活，允许执行!
            } else {
              // 不允许调用
              Log.i(TAG, "[requestEffectFor reject] Plugin effect won't execute for message: $effectId")
              return@withContext false
            }
          }
        }
      }

      // 初步判定成功，由 plugin 的 shouldRunEffect 来决定是否执行副作用
      if (!plugin.shouldRunEffect(present)) {
        Log.i(TAG, "[requestEffectFor reject] rejected by ${pluginId.name}: $effectId")
        // 如果不应该执行 effect，应该清除定时器以及标注 never 状态
        markEffectNeverExecute(present)
        clearCoordinate(present)
        return@withContext false
      }

      // 如果是用户交互触发，移除现有状态
      if (userInteraction) {
        clearEffectStatus(present)
      }
      // 通过执行验证的情况下，清除定时器, 防止重复执行
      clearCoordinate(present)

      // prepare for plugin
      val userPreferences = dataStore.data.first()
      val userModel = botSender.langBot!!.model

      val userAi = userPreferences.ai(userModel.serviceProvider)

      val pm = pathManager
      val dao = chatDao
      val runScope = object : ChatPluginEffectRunScope {
        override val userAI = userAi
        override val userModel = userModel
        override val userPreferences = userPreferences
      }
      val updateScope = object : ChatPluginUpdateScope {
        override val pathManager = pm
        override val chatDao = dao
      }

      // 运行插件副作用
      Log.i(TAG, "[requestEffectFor running] Executing plugin effect: $effectId")
      markEffectRunning(present)
      val dispatch = withContext(Dispatchers.IO) {
        // TODO: 超时限制
        plugin.launchEffect(
          present,
          userInteraction,
          run = { block -> block(runScope) },
          updater = { block -> block(updateScope) },
        )
      }
      when (dispatch) {
        is ChatPlugin.EffectDispatch.Done -> {
          Log.i(TAG, "[requestEffectFor done] Plugin effect won't run next: $effectId")
          markEffectNeverExecute(present)
        }

        is ChatPlugin.EffectDispatch.Next -> {
          Log.i(TAG, "[requestEffectFor next] Plugin effect will run next (${dispatch.mills}ms): $effectId")
          markEffectDone(present)
          coordinateEffectNextRun(present, false, dispatch.mills)
        }
      }

      return@withContext true
    }

  // TODO: 优化返回类型？返回 ChatPluginDescription？
  suspend fun requestToolUsage(
    prompt: String,
    botMessage: MessageChat,
    plugins: List<ChatPluginDescription<ChatPlugin>>
  ): ToolCall.Function? {
    require(botMessage.sender?.botSender != null) {
      "Bot message should have a bot sender"
    }
    val botSender = botMessage.sender!!.botSender!!

    return withContext(Dispatchers.IO) {
      val userPreferences = dataStore.data.first()
      val userModel = botSender.langBot!!.model

      val userAi = userPreferences.ai(userModel.serviceProvider)

      val pm = pathManager
      val dao = chatDao
      val runScope = object : ChatPluginRunScope {
        override val userAI = userAi
        override val userModel = userModel
        override val userPreferences = userPreferences

        // TODO: remove this function?
        override fun requestEffect(userInteraction: Boolean, mills: Long): Boolean {
          Log.w(
            TAG,
            "requestEffect is not supported in tool usage, set the pluginExtra, the executor will handle the effect"
          )
          return false
        }
      }
      val updateScope = object : ChatPluginUpdateScope {
        override val pathManager = pm
        override val chatDao = dao
      }

      val functionRequest = chatCompletionRequest {
        this.model = ModelId(ChatViewModel.DORAEMON_MODEL)
        messages = listOf(
          ChatMessage(
            role = ChatRole.User,
            content = prompt
          )
        )
        tools {
          plugins.forEach { plugin ->
            plugin.builder.invoke(this@tools)
          }
        }
        toolChoice = ToolChoice.Auto
      }
      // TODO: 使用 userAI，类似 lobe，标注模型是否支持 tool api, 不支持可以选择 fallback 到 oepnai 或者直接不支持，图像模型同理，不要总是使用 dalle
      val ai = userPreferences.officialAI()
      val toolResponse = ai.chatCompletion(functionRequest)

      toolResponse.choices.first().message.toolCalls.let {
        val calls = it ?: emptyList()

        if (calls.isEmpty()) return@let null

        // TODO: only execute first tool by now
        val toolToCall = calls.first()
        require(toolToCall is ToolCall.Function) { "Unsupported tool call type: $toolToCall" }

        val pluginDescriptor = plugins.firstOrNull { plugin ->
          plugin.name.lowercase() == toolToCall.function.name.lowercase()
        }
        if (pluginDescriptor == null) {
          return@let null
        }

        // 更新消息相关的插件 ID
        chatDao.updateMessage(botMessage) {
          this.pluginId = pluginDescriptor.name
        }

        Log.i(TAG, "Executing plugin: ${pluginDescriptor.name}")
        val plugin = pluginDescriptor.create()

        plugin.execute(
          toolToCall.function.argumentsAsJson(),
          botMessage,
          { block -> block(runScope) },
          { block -> block(updateScope) }
        )

        toolToCall
      }
    }
  }

  suspend fun runScope(
    present: ChatMessagePresentUnit,
    plugin: ChatPlugin?,
    run: ChatPluginRunScopeRunner,
  ) {
    val botMessage = present.message

    require(botMessage.sender?.botSender != null) {
      "Bot message should have a sender"
    }
    val botSender = botMessage.sender!!.botSender!!

    withContext(Dispatchers.IO) {
      val userPreferences = dataStore.data.first()
      val userModel = botSender.langBot!!.model

      val userAi = userPreferences.ai(userModel.serviceProvider)

      val pm = pathManager
      val dao = chatDao
      val runScope = object : ChatPluginRunScope {
        override val userAI = userAi
        override val userModel: ModelCode = userModel
        override val userPreferences = userPreferences

        override fun requestEffect(userInteraction: Boolean, mills: Long): Boolean {
          coordinateEffectNextRun(present, userInteraction, mills)
          return true
        }
      }
      val updateScope = object : ChatPluginUpdateScope {
        override val pathManager = pm
        override val chatDao = dao
      }

      run.invoke(
        runScope,
        plugin
      ) { block ->
        block.invoke(updateScope, botMessage)
      }
    }
  }
}