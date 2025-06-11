package com.aigroup.aigroupmobile.services.chat

import android.util.Log
import androidx.datastore.core.DataStore
import com.aallam.openai.api.chat.ChatCompletionRequestBuilder
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.ContentPart
import com.aallam.openai.api.chat.ListContent
import com.aallam.openai.api.chat.TextContent
import com.aallam.openai.api.chat.TextPart
import com.aallam.openai.api.chat.ToolCall
import com.aallam.openai.api.chat.VideoPart
import com.aallam.openai.api.chat.chatCompletionRequest
import com.aallam.openai.api.chat.chatMessage
import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.model.ModelId
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appStringResource
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.connect.chat.CustomChatServiceProvider
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.connect.chat.ModelGroup
import com.aigroup.aigroupmobile.connect.chat.ServiceProvider
import com.aigroup.aigroupmobile.connect.chat.ai
import com.aigroup.aigroupmobile.data.dao.ChatDao
import com.aigroup.aigroupmobile.data.extensions.preferencesProperties
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.LargeLangBot
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.data.models.MessageChatData
import com.aigroup.aigroupmobile.data.models.MessageImageItem
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.data.models.MessageSenderUser
import com.aigroup.aigroupmobile.data.models.MessageTextItem
import com.aigroup.aigroupmobile.data.models.MessageVideoItem
import com.aigroup.aigroupmobile.data.models.VideoMediaItem
import com.aigroup.aigroupmobile.data.models.knowledge.KnowledgeChunk
import com.aigroup.aigroupmobile.data.models.mediaItem
import com.aigroup.aigroupmobile.data.models.model
import com.aigroup.aigroupmobile.data.models.primaryBot
import com.aigroup.aigroupmobile.data.models.primaryBotSender
import com.aigroup.aigroupmobile.data.models.readableText
import com.aigroup.aigroupmobile.data.models.specific
import com.aigroup.aigroupmobile.data.models.summary
import com.aigroup.aigroupmobile.services.FileUploader
import com.aigroup.aigroupmobile.services.chat.plugins.BuiltInPlugins
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginExecutor
import com.aigroup.aigroupmobile.viewmodels.ChatViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.redundent.kotlin.xml.xml
import java.util.concurrent.ConcurrentHashMap

interface ChatPluginCoordinator {
  // TODO: 优化返回类型？返回 ChatPluginDescription？
  suspend fun requestToolUsage(primaryMessage: MessageChat, botMessage: MessageChat): ToolCall.Function?
}

@AssistedFactory
interface BaseChatCoordinatorFactory {
  fun create(
    session: @JvmSuppressWildcards Flow<ChatSession>,
    messages: @JvmSuppressWildcards Flow<List<MessageChat>>
  ): BaseChatCoordinator
}

sealed interface ChatIntent {
  data object Normal : ChatIntent
  data object HelpSpecific : ChatIntent
  data class SummarySemantics(val reset: Boolean) : ChatIntent
}

class BaseChatCoordinator @AssistedInject constructor(
  /**
   * session 的 flow 可以不保证其中 messages 的 实时活动 特性
   */
  @Assisted
  val session: @JvmSuppressWildcards Flow<ChatSession>,
  @Assisted
  val messages: @JvmSuppressWildcards Flow<List<MessageChat>>,

  private val chatDao: ChatDao,
  private val dataStore: DataStore<AppPreferences>,

  private val chatPluginExecutor: ChatPluginExecutor,

  private val fileUploader: FileUploader,
  private var rag: RetrievalAugmentedGeneration,
) : ChatPluginCoordinator {

  private data class ChatProperties(
    val context: List<MessageChat>,
    val primaryModel: ModelCode
  )

  companion object {
    const val TAG = "BaseChatCoordinator"
    private var CoroutineName = "BaseChatCoordinator-Coroutine"

    // TODO: make constants in ModelCode
    private val HelpModel = ModelCode("gpt-4o-mini", ChatServiceProvider.OFFICIAL)
  }

  private val mediaFileManagers = mapOf(
    typedMediaFileManager(MediaFileManagerImage),
    typedMediaFileManager(MediaFileManagerVideo(fileUploader))
  )

  private inline fun <reified M : MediaItem, reified T : ChatMediaFileManager<M>> typedMediaFileManager(value: T) =
    Pair(M::class, value)

  private inline fun <reified M : MediaItem> getMediaFileManager(type: M): ChatMediaFileManager<M> {
    val manager = mediaFileManagers[type::class]
    if (manager == null) {
      Log.w(TAG, "Unsupported media item: $type, this may cause processing issues")
      throw IllegalArgumentException("Unsupported media item: $type")
    }
    return manager as ChatMediaFileManager<M>
  }

  private val coroutineScope by lazy {
    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
      Log.e(TAG, "Error while executing message [exceptionHandler]: ${throwable.message}", throwable)
    }
    CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler + CoroutineName(CoroutineName))
  }

  private var liveBotMessages: MutableStateFlow<List<MessageChat>> = MutableStateFlow(emptyList())

  /**
   * 处于活动状态的机器人消息
   */
  val liveMessages: Flow<List<MessageChat>>
    get() = liveBotMessages.asStateFlow()

  private suspend fun selectPrimaryChatModelAndContext(
    history: List<MessageChat>,
    primaryMessage: MessageChat,
    config: CoordinatorConfig,
    intent: ChatIntent = ChatIntent.Normal,
    botSender: MessageSenderBot? = null
  ): ChatProperties {
    val session = session.first()
    val bot = botSender ?: session.primaryBotSender!!.botSender!! // TODO: remove default value

    val defaultModelCode = when (intent) {
      ChatIntent.HelpSpecific -> HelpModel
      is ChatIntent.SummarySemantics -> config.fixedModel ?: bot.langBot!!.model
      else -> config.fixedModel ?: bot.langBot!!.model
    }

    val hasVideo = primaryMessage.parts.any { it.specific is MessageVideoItem }
    val hasImage = primaryMessage.parts.any { it.specific is MessageImageItem }
    var singleContext = false

    val primaryModel = when {
      hasVideo -> {
        config.mediaRecognizeStrategy[VideoMediaItem::class]?.let {
          when (it) {
            is ChatModelCode.Fixed -> it.model
            is ChatModelCode.SingleContextFixed -> {
              singleContext = true
              it.model
            }

            else -> defaultModelCode
          }
        } ?: defaultModelCode
      }

      hasImage -> {
        config.mediaRecognizeStrategy[ImageMediaItem::class]?.let {
          when (it) {
            is ChatModelCode.Fixed -> it.model
            is ChatModelCode.SingleContextFixed -> {
              singleContext = true
              it.model
            }

            else -> defaultModelCode
          }
        } ?: defaultModelCode
      }

      else -> defaultModelCode
    }

    val context = when (intent) {
      ChatIntent.HelpSpecific -> {
        listOf(primaryMessage)
      }

      is ChatIntent.SummarySemantics -> {
        // TODO: 没有考虑 primaryMessage

        require(!singleContext) { "Single context is not supported in summary semantics." }

        if (intent.reset) {
          history
        } else {
          // TODO: 没有判断 takeLastWhile 的结果是否为空
          history.takeLastWhile {
            it.id != session.summary?.lastMessageId
          }
        }
      }

      else -> {
        if (singleContext) {
          listOf(primaryMessage)
        } else {
          val contextLimit = config.maxHistoryCount
          history.takeWhile { it.id <= primaryMessage.id }.takeLast(contextLimit ?: Int.MAX_VALUE)
        }
      }
    }

    return ChatProperties(context, primaryModel)
  }

  private fun markBotMessageLive(vararg messages: MessageChat) {
    require(messages.isNotEmpty()) { "At least one message should be provided." }
    require(messages.all { it.sender!!.specific is MessageSenderBot }) { "Only bot message can be marked as live." }
    liveBotMessages.update { currentList ->
      currentList + messages
    }
  }

  private fun clearLiveBotMessages() {
    liveBotMessages.update { emptyList() }
  }

  private fun getPluginCoordinator(): ChatPluginCoordinator {
    return this
  }

  sealed class ChatCommand {
    data object Stop : ChatCommand()
    data class SingleMessageChat(val content: String) : ChatCommand()
  }

  /**
   * 独立于会话的聊天并直接从函数调用获取结果，该方法不支持知识库和插件
   */
  fun chat(
    message: MessageChat,
    intent: ChatIntent,
    config: CoordinatorConfig.() -> CoordinatorConfig = { this },
    chatCommand: ((context: List<MessageChat>) -> ChatCommand)? = null
  ): Deferred<String> {
    // initial config
    val coordinatorConfigFlow = session.map {
      config(
        CoordinatorConfig(
          maxHistoryCount = it.historyInclude,
          mediaRecognizeStrategy = emptyMap(),
          preferredStreamResponse = true,
          mediaCompatibilityHistory = MediaCompatibilityHistory.HELP_PROMPT,
        )
      )
    }

    // TODO: this is must catch? see viewModel summary
    return coroutineScope.async {
      val coordinatorConfig = coordinatorConfigFlow.first()
      val preferences = dataStore.data.first()
      val session = session.first()
      val historyMessages = messages.first().reversed() // database store chat message in order of newest first

      // select primary chat model
      val (context, primaryModel) = selectPrimaryChatModelAndContext(
        historyMessages,
        message,
        coordinatorConfig,
        intent = intent
      )
      // Use the ServiceProvider interface instead of ChatServiceProvider
      val primaryAI = preferences.ai(primaryModel.serviceProvider, requiresToken = true)

      // doing job
      try {
        // handle media items
        val requestHeaders = ConcurrentHashMap<String, String>()

        message.parts.mapNotNull { it.mediaItem }.map { media ->
          async {
            val fileManager = getMediaFileManager(media)
            val available = fileManager.checkMediaItemAvailable(media)
            if (!available) {
              Log.i(TAG, "Media item ${media.identifier} is not available, uploading...")
              fileManager.uploadMediaItem(media)?.let { uploadResult ->
                requestHeaders += uploadResult.requestHeaders
                chatDao.updateMessage(message) {
                  val mediaItem = parts.firstOrNull { it.mediaItem?.identifier == media.identifier }
                  mediaItem!!.mediaItem!!.onlineLink = uploadResult.onlineLink
                }
              }
            }
          }
        }.awaitAll()

        // TODO: detect local history messages' media content it's available for llm or not

        val sender = session.primaryBotSender!!.botSender!!
        val bot = sender.langBot!!
        val defaultProperties = bot.preferencesProperties

        val usingStream = primaryModel.supportStream && coordinatorConfig.preferredStreamResponse
        val requestOptions = RequestOptions(
          headers = requestHeaders
        )

        var stop = false
        val request = chatCompletionRequest {
          baseChatCompletionRequest(this, bot, defaultProperties)
          this.model = ModelId(primaryModel.code)
          this.messages = chatCommand?.invoke(context)?.let {
            when (it) {
              is ChatCommand.Stop -> {
                stop = true
                emptyList()
              }

              is ChatCommand.SingleMessageChat -> listOf(
                ChatMessage(
                  role = ChatRole.User,
                  content = it.content
                )
              )
            }
          } ?: context.mapIndexedNotNull { index, it ->
            it.calculateApiModel(
              primaryModel,
              coordinatorConfig.mediaCompatibilityHistory,
              excludeVideo = index != context.size - 1 // TODO: 阿里云补丁 Video file input is not supported. If you want to use this feature
            )
          }
          buildChatParameters(sender, applyAssistant = false)
        }

        if (stop) {
          Log.i(TAG, "Stop command received.")
          // TODO: should return empty string here?
          return@async ""
        }

        var responseText = ""

        if (usingStream) {
          // TODO: release this flow when viewmodel lifecycle is done (管理 flow，类似 flutter riverpod 的 keepAlive，包括 plugin 里的)
          primaryAI.chatCompletions(request, requestOptions).collect {
            val content = it.choices.firstOrNull()?.delta?.content
            if (content?.isNotEmpty() == true) {
              responseText += content
            }
          }
        } else {
          val completion = primaryAI.chatCompletion(request, requestOptions)
          val content = completion.choices.firstOrNull()?.message?.content
          if (content?.isNotEmpty() == true) {
            responseText = content
          }
        }

        Log.i(ChatViewModel.TAG, "executeChat Response: $responseText")

        responseText
      } catch (e: Exception) {
        Log.e(TAG, "Error while executing message: ${message.id.toHexString()}", e)
        throw e
      }
    }
  }

  /**
   * 对聊天记录重点某一条消息执行 llm 调度流程
   *
   * @param message 要执行的消息，该消息必须是用户消息
   * @param config llm 调度器配置
   */
  fun executeMessage(
    message: MessageChat,
    botSender: MessageSenderBot? = null,
    config: CoordinatorConfig.() -> CoordinatorConfig = { this }
  ): Job {
    // make sure the message is user message
    require(message.sender!!.specific is MessageSenderUser) { "Only user message can be executed." }

    // initial config
    val coordinatorConfigFlow = session.map {
      config(
        CoordinatorConfig(
          maxHistoryCount = it.historyInclude,
          mediaRecognizeStrategy = emptyMap(),
          preferredStreamResponse = true,
          mediaCompatibilityHistory = MediaCompatibilityHistory.HELP_PROMPT,
        )
      )
    }

    return coroutineScope.launch {
      val coordinatorConfig = coordinatorConfigFlow.first()
      val preferences = dataStore.data.first()
      val session = session.first()
      val historyMessages = messages.first().reversed() // database store chat message in order of newest first

      // TODO: check bot sender exists in session [Unmanaged objects don't support backlinks.]
//      if (botSender != null) {
//        require(session.senders.any { it.id == botSender.inclusive.id }) { "Bot sender is not in session." }
//      }
      val sender = botSender ?: session.primaryBotSender!!.botSender!!

      // select primary chat model
      val (context, primaryModel) = selectPrimaryChatModelAndContext(
        historyMessages,
        message,
        coordinatorConfig,
        botSender = botSender
      )
      val primaryAI = preferences.ai(primaryModel.serviceProvider, requiresToken = true)

      // create bot message

      // context is trimmed list of MessageChat so using historyMessages here
      val isLatestMessage = message.id == historyMessages.last().id
      val botMessage = if (isLatestMessage) {
        chatDao.createEmptyBotMessage(session, botSender = botSender)
      } else {
        // TODO
        if (botSender != null) {
          Log.w(TAG, "Currently not support to execute message with bot sender if it's not the latest message.")
        }

        val nextMessageIdx = historyMessages.indexOfLast { it.id == message.id } + 1
        val nextMessage = historyMessages[nextMessageIdx]

        // 清空机器人状态
        chatDao.clearMessageParts(nextMessage)
        chatDao.updateMessageLastTextOrCreate(nextMessage) {}

        // clear tool usage
        chatDao.updateMessage(nextMessage) {
          this.pluginId = null
          this.pluginExtra = null
        }

        nextMessage
      }
      markBotMessageLive(botMessage)

      // doing job
      try {
        // handle media items
        val requestHeaders = ConcurrentHashMap<String, String>()

        message.parts.mapNotNull { it.mediaItem }.map { media ->
          async {
            val fileManager = getMediaFileManager(media)
            val available = fileManager.checkMediaItemAvailable(media)
            if (!available) {
              Log.i(TAG, "Media item ${media.identifier} is not available, uploading...")
              fileManager.uploadMediaItem(media)?.let { uploadResult ->
                requestHeaders += uploadResult.requestHeaders
                chatDao.updateMessage(message) {
                  val mediaItem = parts.firstOrNull { it.mediaItem?.identifier == media.identifier }
                  mediaItem!!.mediaItem!!.onlineLink = uploadResult.onlineLink
                }
              }
            }
          }
        }.awaitAll()

        // TODO: detect local history messages' media content it's available for llm or not

        // detect plugin call
        val tool = getPluginCoordinator().requestToolUsage(message, botMessage)
        if (tool != null) {
          // 使用插件调用来接管消息处理，不再继续后续逻辑
          return@launch
        }

        // doing rag
        val knowledgeBase = coordinatorConfig.knowledgeBase
        val ragChunks = if (knowledgeBase != null) {
          // TODO: 多模态 retrieve
          val ragChunks = rag.retrieveRelatedChunks(knowledgeBase, message.readableText)
          for ((idx, chunk) in ragChunks.entries.withIndex()) {
            Log.i(TAG, "RAG Chunk $idx: ${chunk.key.textContent} -> ${chunk.value}")
          }
          ragChunks.entries.sortedBy { -it.value }.map { it.key }
        } else {
          emptyList()
        }

        val bot = sender.langBot!!
        val defaultProperties = bot.preferencesProperties

        val usingStream = primaryModel.supportStream && coordinatorConfig.preferredStreamResponse
        val requestOptions = RequestOptions(
          headers = requestHeaders
        )

        val request = chatCompletionRequest {
          baseChatCompletionRequest(this, bot, defaultProperties)
          this.model = ModelId(primaryModel.code)
          this.messages = context.mapIndexedNotNull { index, it ->
            it.calculateApiModel(
              primaryModel,
              coordinatorConfig.mediaCompatibilityHistory,
              excludeVideo = index != context.size - 1 // TODO: 阿里云补丁 Video file input is not supported. If you want to use this feature
            )
          }
          buildChatParameters(sender, ragChunks = ragChunks)
        }

        var responseText = ""

        if (usingStream) {
          // TODO: release this flow when viewmodel lifecycle is done (管理 flow，类似 flutter riverpod 的 keepAlive，包括 plugin 里的)
          try {
            primaryAI.chatCompletions(request, requestOptions).collect {
              val content = it.choices.firstOrNull()?.delta?.content
              if (content?.isNotEmpty() == true) {
                responseText += content
                chatDao.updateMessageLastTextOrCreate(botMessage) {
                  this.text += content
                }
              }
            }
          } catch (streamException: Exception) {
            Log.w(TAG, "Stream processing interrupted, but may have partial response: ${streamException.message}")
            // 如果流式处理中断但已经有部分响应，不抛出异常
            if (responseText.isNotEmpty()) {
              Log.i(TAG, "Partial response received: ${responseText.length} characters")
            } else {
              // 只有在完全没有响应时才重新抛出异常
              throw streamException
            }
          }
        } else {
          val completion = primaryAI.chatCompletion(request, requestOptions)
          val content = completion.choices.firstOrNull()?.message?.content
          if (content?.isNotEmpty() == true) {
            responseText = content
            chatDao.updateMessageLastTextOrCreate(botMessage) {
              this.text += content
            }
          }
        }

        Log.i(ChatViewModel.TAG, "Response: $responseText")

        // 存储 help prompt (text)
        chatDao.updateMessage(message) {
          for (part in this.parts) {
            val specific = part.specific
            if (specific is MessageImageItem) {
              specific.helpText = responseText
              Log.i(ChatViewModel.TAG, "Save Image Help text: $responseText")
              break
            } else if (specific is MessageVideoItem) {
              specific.helpText = responseText
              Log.i(ChatViewModel.TAG, "Save Video Help text: $responseText")
              break
            }
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error while executing message: ${message.id.toHexString()}", e)
        Log.d(TAG, "Message content: ${message.readableText}")



        // 对于不同类型的错误，提供更友好的错误处理
        val messageText = message.readableText
        val containsUrl = messageText.contains("http://") || messageText.contains("https://")
        val containsOssUrl = messageText.contains("oss-cn-") || messageText.contains("aliyuncs.com")
        
        val errorMessage = when {
          // 网络连接超时错误
          e is com.aallam.openai.api.exception.OpenAITimeoutException ||
          e.message?.contains("timeout", ignoreCase = true) == true ||
          e.message?.contains("Connect timeout", ignoreCase = true) == true -> {
            Log.i(TAG, "Network timeout error detected")
            "网络连接超时，请检查网络连接后重试"
          }
          // 如果消息包含OSS链接，很可能是正常的文档分享消息被误判
          containsOssUrl -> {
            Log.i(TAG, "Message contains OSS URL, treating as normal message processing issue")
            appStringResource(R.string.label_chat_message_url_processing_info)
          }
          // 如果消息包含其他URL且是不支持的类型错误
          containsUrl && (e.message?.contains("Unsupported") == true || e.message?.contains("error") == true) -> {
            appStringResource(R.string.label_chat_message_url_processing_info)
          }
          // 其他错误正常处理
          else -> {
            normalizedException(e)
          }
        }
        
        chatDao.updateMessageError(botMessage, errorMessage)
      } finally {
        clearLiveBotMessages()
      }
    }
  }

  // now the skip media logic is following speak-gpt project
  private fun MessageChat.calculateApiModel(
    model: ModelCode,
    historyStrategy: MediaCompatibilityHistory,
    excludeVideo: Boolean = false // TODO: fix 参数，暂时修复阿里云 Video file input is not supported. If you want to use this feature
  ): ChatMessage? {
    // 有些不支持 vision 的模型也不支持 parts 模式
    val supportVision = model.supportImage || model.supportVideo

    val isBot = sender?.role == "assistant" // TODO: don't hard code

    val isYi = model.modelGroup is ModelGroup.Predefined.Yi
    val isMistral = model.modelGroup is ModelGroup.Predefined.Mistral
    val isLlama = model.modelGroup is ModelGroup.Predefined.Llama
    val isLlaVA = model.modelGroup is ModelGroup.Predefined.LlaVA

    if (supportVision) {
      // TODO: yi bot and mistral 不支持 parts！
      if (isBot && (isYi || isMistral || isLlama || isLlaVA)) {
        val textListContent = parts
          .flatMap {
            it.calculateApiModel(model, sender!!.role, historyStrategy)
          }
          .filterIsInstance<TextPart>()
        return ChatMessage(
          role = ChatRole(role = sender!!.role),
          content = textListContent.joinToString("\n") { it.text }
        )
      }

      return ChatMessage(
        role = ChatRole(role = sender!!.role),
        messageContent = ListContent(
          parts.flatMap {
            it.calculateApiModel(model, sender!!.role, historyStrategy)
          }.filter {
            if (excludeVideo) {
              it !is VideoPart
            } else {
              true
            }
          }
        )
      )
    } else {
      val textListContent = parts
        .flatMap {
          it.calculateApiModel(model, sender!!.role, historyStrategy)
        }
        .filterIsInstance<TextPart>()

      return if (textListContent.isEmpty()) {
        null
      } else {
        ChatMessage(
          role = ChatRole(role = sender!!.role),
          content = textListContent.joinToString("\n") { it.text }
        )
      }
    }
  }

  private fun MessageChatData.calculateApiModel(
    model: ModelCode,
    role: String,
    historyStrategy: MediaCompatibilityHistory,
  ): List<ContentPart> {
    error?.let {
      return emptyList()
    }

    (specific as? MessageTextItem)?.let {
      if (it.text.isEmpty()) {
        return emptyList()
      }
      
      // 对包含长URL的文本进行特殊处理，确保不会因为URL长度或特殊字符导致处理失败
      val processedText = try {
        // 检查文本是否包含URL，如果包含则进行安全处理
        if (it.text.contains("http://") || it.text.contains("https://")) {
          // 确保URL文本能够安全传递给AI模型
          it.text.trim()
        } else {
          it.text
        }
      } catch (e: Exception) {
        Log.w(TAG, "Error processing text content, using original text", e)
        it.text
      }
      
      return listOf(TextPart(text = processedText))
    }

    mediaItem?.let { media ->
      val mediaManager = getMediaFileManager(media)

      return when {
        // TODO: don't hard code (and role)
        role == "assistant" -> listOf(mediaManager.toBotChatPart(media))

        else -> {
          mediaManager.toChatPart(media, model)
            ?.let { listOf(it) }
            ?: mediaManager.toCompatibleChatPart(
              media,
              this,
              model,
              historyStrategy
            )
        }
      }
    }

    // 如果没有匹配的类型，返回空列表而不是抛出错误
    // 这样可以避免因为未知消息类型导致的崩溃
    Log.w(TAG, "Unsupported MessageChatData type: $specific, returning empty list")
    return emptyList()
  }

  private fun baseChatCompletionRequest(
    builder: ChatCompletionRequestBuilder,
    bot: LargeLangBot,
    default: AppPreferences.LongBotProperties
  ) {
    // TODO: 适配每个厂商的参数
//    builder.temperature = bot.temperature ?: default.temperature
//    builder.topP = bot.topP ?: default.topP
//    builder.frequencyPenalty = bot.frequencyPenalty ?: default.frequencyPenalty
//    builder.presencePenalty = bot.presencePenalty ?: default.presencePenalty
  }

  private fun ChatCompletionRequestBuilder.buildChatParameters(
    sender: MessageSenderBot,
    ragChunks: List<KnowledgeChunk> = emptyList(),
    applyAssistant: Boolean = true
  ) {
    // TODO: 优化该方法的逻辑

    val assistantSystem = if (sender.assistant != null && applyAssistant) {
      sender.assistant!!.presetsPrompt
    } else {
      ""
    }
    val ragSystem = if (ragChunks.isNotEmpty()) {
      // TODO: to prompts
      if (assistantSystem.isBlank()) {
        """
          You are an assistant for question-answering tasks. Use the retrieved context to answer the question.
          If you don't know the answer, just say that you don't know.
          Use three sentences maximum and keep the answer concise.
          Answer in Chinese.
        """.trimIndent()
      } else {
        """
          When answer any questions. Use the retrieved context to answer the question.
          If you don't know the answer, just say that you don't know.
          Use three sentences maximum and keep the answer concise.
          Answer in Chinese.
        """.trimIndent()
      }
    } else {
      ""
    }
    val systemMessageContent = (assistantSystem + "\n\n" + ragSystem).trim()

    val noNeedSystem = assistantSystem.isEmpty() && ragSystem.isEmpty()
    if (noNeedSystem) {
      Log.i(TAG, "No need to add system message.")
      return
    } else {
      Log.i(TAG, "Add system message: $systemMessageContent")
    }

    val promptMessageContent = if (ragChunks.isNotEmpty() && this.messages?.isNotEmpty() == true) {
      // TODO: to prompts
      // TODO: support KonwledgeDocument File meta
      // TODO: move this to build api message functions

      val chunkXml = ragChunks.map {
        xml("chunk") {
          for ((key, value) in it.metadata) {
            attribute(key, value)
          }
          "p" { -it.textContent }
        }
      }

      val contextStr = chunkXml.joinToString("\n")
      val query = when (val lastMessageContent = this.messages!!.last().messageContent) {
        is ListContent -> {
          lastMessageContent.content.filterIsInstance<TextPart>().joinToString("\n") { it.text }
        }
        is TextContent -> {
          lastMessageContent.content
        }
        else -> {
          // 不抛出错误，而是记录警告并使用默认处理
          Log.w(TAG, "Unsupported last message content type: ${lastMessageContent?.javaClass?.simpleName}, using fallback")
          lastMessageContent?.toString() ?: ""
        }
      }

      """
      Context information is below.
      ---------------------
      $contextStr
      ---------------------
      Given the context information and not prior knowledge, answer the query.
      Query: $query
      """.trimIndent()
    } else {
      null
    }

    if (promptMessageContent != null) {
      Log.i(TAG, "Modified last message prompt: $promptMessageContent")
    }

    val systemMessage = ChatMessage(
      role = ChatRole.System,
      content = systemMessageContent
    )
    this.messages = listOf(systemMessage) + (this.messages ?: emptyList())

    if (promptMessageContent != null) {
      // replace last
      val last = this.messages!!.last()
      val noneTextParts = (last.messageContent as ListContent).content.filter {
        it !is TextPart
      }
      // TODO: 支持多模态
      this.messages = this.messages!!.dropLast(1) + chatMessage {
        role = last.role
        content = promptMessageContent
      }
    }
  }

  private fun normalizedException(exception: java.lang.Exception): String {
    val message = exception.message?.let {
      // 只过滤特定的aihubmix.com域名，不影响其他合法的URL
      // remove http[s]://aihubmix.com, if its in "" or () remove it also
      val regex = Regex("""(http[s]?://)?aihubmix\.com""")
      val match = regex.findAll(it)
      for (m in match) {
        // detect if the url or link is surrounded by "" or ()
        val start = m.range.first
        val end = m.range.last

        if (start > 0 && it[start - 1] == '"' && end + 2 < it.length && it[end + 1] == '"') {
          return@let it.replaceRange(start - 1, end + 2, "")
        } else if (start > 0 && it[start - 1] == '(' && end + 2 < it.length && it[end + 1] == ')') {
          return@let it.replaceRange(start - 1, end + 2, "")
        } else {
          return@let it.replaceRange(start, end + 1, "")
        }
      }
      it
    } ?: appStringResource(R.string.label_unknown_error_message)

    return appStringResource(R.string.label_chat_message_error_template, message)
  }

  override suspend fun requestToolUsage(primaryMessage: MessageChat, botMessage: MessageChat): ToolCall.Function? {
    val pluginCodes = session.first().plugins.toList()
    val pluginsAvailable = BuiltInPlugins.plugins.filter { pluginCodes.contains(it.name) }

    if (pluginsAvailable.isNotEmpty()) {
      Log.i(ChatViewModel.TAG, "Request tool usage...")
      val prompt = primaryMessage.readableText
      return chatPluginExecutor.requestToolUsage(prompt, botMessage, pluginsAvailable)
    }
    return null
  }
}

suspend fun BaseChatCoordinator.executeLastUserMessage(): Job {
  val lastUserMessage = messages.map { it.first { it.sender!!.specific is MessageSenderUser } }.first()
  return executeMessage(lastUserMessage)
}

