package com.aigroup.aigroupmobile.viewmodels

import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.buildAnnotatedString
import androidx.datastore.core.DataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.chatCompletionRequest
import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.model.ModelId
import com.aigroup.aigroupmobile.Constants
import com.aigroup.aigroupmobile.GuideQuestion
import com.aigroup.aigroupmobile.Prompts
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.dao.ChatDao
import com.aigroup.aigroupmobile.data.dao.UserDao
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.data.models.MessageImageItem
import com.aigroup.aigroupmobile.data.models.MessageItem
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.data.models.MessageTextItem
import com.aigroup.aigroupmobile.data.models.MessageVideoItem
import com.aigroup.aigroupmobile.data.models.VideoMediaItem
import com.aigroup.aigroupmobile.data.models.primaryBot
import com.aigroup.aigroupmobile.data.models.readableText
import com.aigroup.aigroupmobile.data.models.specific
import com.aigroup.aigroupmobile.data.models.summary
import com.aigroup.aigroupmobile.ui.components.messages.BubbleChatMessagePresent
import com.aigroup.aigroupmobile.ui.components.messages.ChatMessagePresentUnit
import com.aigroup.aigroupmobile.services.SpeechRecognitionState
import com.aigroup.aigroupmobile.connect.chat.officialAI
import com.aigroup.aigroupmobile.data.dao.AssistantDao
import com.aigroup.aigroupmobile.data.dao.ChatConversationDao
import com.aigroup.aigroupmobile.data.models.BotAssistant
import com.aigroup.aigroupmobile.data.models.PreviewMediaItem
import com.aigroup.aigroupmobile.data.models.botSenders
import com.aigroup.aigroupmobile.data.models.model
import com.aigroup.aigroupmobile.data.models.primaryBotSender
import com.aigroup.aigroupmobile.services.chat.BaseChatCoordinator
import com.aigroup.aigroupmobile.services.chat.BaseChatCoordinatorFactory
import com.aigroup.aigroupmobile.services.chat.ChatIntent
import com.aigroup.aigroupmobile.services.chat.ChatModelCode
import com.aigroup.aigroupmobile.services.chat.plugins.BuiltInPlugins
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginExecutor
import com.aigroup.aigroupmobile.utils.common.simulateTextAnimate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mongodb.kbson.BsonObjectId
import java.util.Locale
import javax.inject.Inject

data class ChatBottomBarState(
  val inputText: AnnotatedString = buildAnnotatedString { },
  val isRecognizing: Boolean = false,
  val recognizingText: String? = null,
  val recognizedText: String? = null,
  val mediaItem: MediaItem? = null,
  val atAssistant: BotAssistant? = null,
) {

  val fullInputText: AnnotatedString
    get() = inputText + recognizedText.orEmpty() + recognizingText?.let { "$it..." }.orEmpty()

  fun copyFromSpeechRecognition(recognition: SpeechRecognitionState): ChatBottomBarState {
    return when (recognition.isRecognizing) {
      true -> copy(
        isRecognizing = true,
        recognizingText = recognition.recognizingText,
        recognizedText = recognition.recognizedText
      )

      false -> copy(
        isRecognizing = false,
        recognizingText = null,
        recognizedText = null,
        inputText = inputText + recognizedText.orEmpty()
      )
    }
  }

  fun clearInput() = copy(inputText = buildAnnotatedString {}, mediaItem = null)
}

private operator fun AnnotatedString.plus(other: String): AnnotatedString {
  return with(Builder(this)) {
    append(other)
    toAnnotatedString()
  }
}

/**
 * ## 媒体内容的后备采用策略
 *
 * 当主 LLM 不支持媒体内容时采用的策略
 */
enum class MediaCompatibilityHistory {
  /**
   * 使用 help text 或者 snapshot 辅助
   */
  HELP_PROMPT,

  /**
   * 跳过上下文中的媒体内容
   */
  SKIP;
}

@HiltViewModel
class ChatViewModel @Inject constructor(
  stateHandle: SavedStateHandle,
  private val chatDao: ChatDao,
  private val userDao: UserDao,
  private val conversationDao: ChatConversationDao,
  private val dataStore: DataStore<AppPreferences>,
  internal val chatPluginExecutor: ChatPluginExecutor, // TODO: shoud allow access chatPluginExecutor out ChatViewModel?
  private val chatCoordinatorFactory: BaseChatCoordinatorFactory,
) : ViewModel() {

  companion object {
    const val TAG = "ChatViewModel"

    const val VIDEO_MODEL = "qwen-vl-max-0809"
    const val DORAEMON_MODEL = "gpt-4o"
    const val FAST_MODEL = "gpt-4o-mini"
  }

  override fun onCleared() {
    super.onCleared()
    Log.i(TAG, "ChatViewModel is cleared")

    chatPluginExecutor.close()
  }

  private val preferences = dataStore.data
  val chatViewMode = preferences.map { it.chatViewMode }

  private val chatCoordinator: BaseChatCoordinator by lazy {
    chatCoordinatorFactory.create(
      session = sessionFlow.filterNotNull(),
      messages = allMessagesFlow
    )
  }

  // basic state
  private val chatId = stateHandle.get<String>("chatId")!!
  private val localUser = userDao.getLocalUser().asLiveData().map { it.obj }

  private val sessionFlow = chatDao.getConversationById(chatId).map { it.obj }
  val session = sessionFlow.asLiveData()

  private val _loadingId = MutableStateFlow<String?>(null)
  private val coordinatorLoadingId by lazy {
    chatCoordinator.liveMessages.map { it.firstOrNull()?.id?.toHexString() }
  }
  val loadingId by lazy {
    merge(_loadingId.asStateFlow(), coordinatorLoadingId)
  }

  // TODO: doc mode 和 bubbleMessages 逻辑合并, 使用 ChatMessagePresentUnit
  private val allMessagesFlow = chatDao.getMessages(chatId).map { it.list.toList() }
  val allMessages = allMessagesFlow.asLiveData()

  // TODO: 考虑类型安全的媒体 message part，BatchMediaMsgItem 同理
  val mediaItems: LiveData<List<PreviewMediaItem>> = allMessagesFlow
    .map {
      it.flatMap { msg ->
        msg.parts.mapNotNull {
          val image = it.imageItem?.image?.let { media ->
            PreviewMediaItem(
              media,
              it.imageItem?.helpText,
              msg.pluginId?.let { PreviewMediaItem.PluginInfo(it, msg.pluginExtra) }
            )
          }
          val video = it.videoItem?.video?.let { media ->
            PreviewMediaItem(
              media,
              it.videoItem?.helpText,
              msg.pluginId?.let { PreviewMediaItem.PluginInfo(it, msg.pluginExtra) }
            )
          }

          image ?: video
        }.reversed()
      }.reversed()
    }
    .asLiveData()

  val bubbleMessages: LiveData<List<ChatMessagePresentUnit>> = allMessagesFlow
    .combine(loadingId) { a, b -> Pair(a, b) }
    .map { (messageList, loadingId) ->
      messageList.flatMap { msg ->
        val isLoading = loadingId == msg.id.toHexString()

        val defaultPresent = BubbleChatMessagePresent
        val present = BuiltInPlugins.plugins.firstOrNull {
          it.name == msg.pluginId
        }?.present ?: defaultPresent

        present.processMessage(msg, isLoading).reversed()
      }
    }
    .asLiveData()

  // bottom bar state
  private var _bottombarState = MutableStateFlow(ChatBottomBarState())
  val bottomBarState = _bottombarState.asStateFlow()

  fun updateBottomBarState(newState: ChatBottomBarState) {
    _bottombarState.value = newState
  }

  fun updateInputText(text: AnnotatedString) {
    _bottombarState.value = bottomBarState.value.copy(inputText = text)
  }

  fun updateInputText(text: String) {
    _bottombarState.value = bottomBarState.value.copy(inputText = buildAnnotatedString { append(text) })
  }

  // properties

  fun updateChatProperties(properties: AppPreferences.LongBotProperties) {
    viewModelScope.launch(Dispatchers.IO) {
      val session = session.value
      require(session != null) { "ChatSession is not loaded" }
      chatDao.updatePrimaryBot(session) {
        temperature = properties.temperature
        topP = properties.topP
        frequencyPenalty = properties.frequencyPenalty
        presencePenalty = properties.presencePenalty
        // TODO: not do maxTokens on UI
      }
    }
  }

  fun resetChatProperties() {
    updateChatProperties(AppPreferencesDefaults.defaultLongBotProperties)
  }

  fun updateChatSessionHistoryInclude(session: ChatSession, history: Int?) {
    viewModelScope.launch(Dispatchers.IO) {
      chatDao.updateSession(session) {
        this.historyInclude = history
      }
    }
  }

  fun enablePlugin(session: ChatSession, code: String) {
    viewModelScope.launch(Dispatchers.IO) {
      chatDao.updateSession(session) {
        this.plugins.add(code)
      }
    }
  }

  fun disablePlugin(session: ChatSession, code: String) {
    viewModelScope.launch(Dispatchers.IO) {
      chatDao.updateSession(session) {
        if (this.plugins.contains(code)) {
          this.plugins.remove(code)
        }
      }
    }
  }

  // actions

  // TODO: combine retryMessage and sendContent

  suspend fun startMessageLoading(message: MessageChat, action: suspend () -> Unit) {
    if (_loadingId.value != null) {
      Log.w(TAG, "Loading id is not null, skip loading")
      return
    }

    withContext(Dispatchers.Main) {
      _loadingId.value = message.id.toHexString()
      action()
      _loadingId.value = null
    }
  }

  fun editMessage(message: MessageChat, partIdx: Int, text: String) {
    viewModelScope.launch(Dispatchers.IO) {
      chatDao.updateMessage(message) {
        val item = this.parts[partIdx].specific
        if (item is MessageTextItem) {
          item.text = text
        }
      }
    }
  }

  fun respondPreference(guide: GuideQuestion) {
    val session = session.value
    val textContent = bottomBarState.value.fullInputText

    require(!bottomBarState.value.isRecognizing) { "Speech recognition is in progress on send content" }
    require(session != null) { "ChatSession is not loaded" }

    viewModelScope.launch(Dispatchers.IO) {
      val localUserSender = userDao.getOrCreateLocalUserSender()
      val item = MessageTextItem(textContent.text)
      val messages = mutableListOf<MessageItem>(item)

      val userMessage = chatDao.createMessage(session, localUserSender, messages)
      withContext(Dispatchers.Main) {
        updateBottomBarState(bottomBarState.value.clearInput())
      }

      val botMessage = chatDao.createEmptyBotMessage(session)
      withContext(Dispatchers.Main) {
        _loadingId.value = botMessage.id.toHexString()
      }

      delay(1000L)

      val answer = Constants.getPreferenceAnswer(guide.question)
      answer.simulateTextAnimate().collect {
        chatDao.updateMessageLastTextOrCreate(botMessage) {
          this.text += it
        }
      }

      withContext(Dispatchers.Main) {
        _loadingId.value = null
      }
    }
  }

  fun translateMessage(message: MessageChat, partIdx: Int, locale: Locale) { // 优化 partIdx api 设计
    val session = session.value
    require(session != null) { "ChatSession is not loaded" }

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val result = chatCoordinator.chat(message, intent = ChatIntent.HelpSpecific) { context ->
          val part = message.parts[partIdx].specific
          if (part !is MessageTextItem) {
            Log.w(TAG, "Translate Message: Not a text message")
            return@chat BaseChatCoordinator.ChatCommand.Stop
          }

          val prompt = Prompts.translateMessage(part, locale)
          Log.i(TAG, "Translate Message: $prompt")

          BaseChatCoordinator.ChatCommand.SingleMessageChat(prompt)
        }.await()

        if (result.isNotEmpty()) {
          Log.i(TAG, "Translate Message: $result")

          // update translated text
          chatDao.updateMessage(message) {
            val part = this.parts[partIdx].specific
            if (part is MessageTextItem) {
              part.translatedText = result
            }
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Translate Message Error: ${e.message}")
      }
    }
  }

  fun summarySession(reset: Boolean = false) {
    val session = session.value
    require(session != null) { "ChatSession is not loaded" }

    Log.i(TAG, "Summary Session (rest: $reset)")

    viewModelScope.launch(Dispatchers.IO) {
      val summaryInfo = session.summary
      val history = allMessages.value?.reversed() ?: emptyList()

      if (history.isEmpty()) {
        Log.w(TAG, "Summary Session Cancel: No history")
        return@launch
      }

      val lastMessage = history.last()
      var lastSummaryMessageId: BsonObjectId? = null

      try {
        val result = chatCoordinator.chat(lastMessage, intent = ChatIntent.SummarySemantics(reset)) { context ->
          if (context.count() < 2) {
            Log.w(TAG, "Summary Session: Not enough context")
            BaseChatCoordinator.ChatCommand.Stop
          } else {
            val oldSummary = if (reset) null else summaryInfo?.content
            val prompt = Prompts.summarySession(oldSummary, context)
            lastSummaryMessageId = context.last().id // TODO: 优化写法，从结果返回是否 result 判断

            Log.i(TAG, "Summary Session: $prompt")

            BaseChatCoordinator.ChatCommand.SingleMessageChat(prompt)
          }
        }.await()

        if (result.isNotEmpty()) {
          Log.i(TAG, "Summary Session: $result")
          chatDao.updateSession(session) {
            this.summaryContent = result
            this.summaryLastMsgId = lastSummaryMessageId!!
          }
        }
      } catch (e: Exception) {
        // TODO: show in ui
        Log.e(TAG, "Summary Session Error: ${e.message}")
      }
    }
  }

  /**
   * > 注意: 当用户选择的模型不支持媒体时，一定要对应的设置 [imageSingleContext] 参数或者 [videoSingleContext] 参数
   *
   * TODO: 与 sendContent 逻辑合并
   *
   * @param retryMessage 重试的消息, 该消息应该为 bot 所发送
   * @param imageSingleContext 是否在单独上下文中发送图片, 默认为 false
   * @param videoSingleContext 是否在单独上下文中发送视频, 默认为 true
   * @param mediaCompatibilityHistory 媒体内容的后备采用策略, 默认为 MediaCompatibilityHistory.HELP_PROMPT
   */
  fun retryMessage(
    retryMessage: MessageChat,
    imageSingleContext: Boolean = false,
    videoSingleContext: Boolean = false,
    mediaCompatibilityHistory: MediaCompatibilityHistory = MediaCompatibilityHistory.HELP_PROMPT,
  ) {
    require(retryMessage.sender?.specific is MessageSenderBot) { "Retry message is not a bot message" }
    val sender = retryMessage.sender?.specific as MessageSenderBot

    val session = session.value
    require(session != null) { "ChatSession is not loaded" }

    // 使用消息时使用 messages 而不是 session.messages，可能涉及 realm 数据库监听层级过深
    val historyMessagesRaw = allMessages.value?.reversed()
      ?.takeWhile { it.id.toHexString() != retryMessage.id.toHexString() } ?: emptyList()
    val currentUserMessage = historyMessagesRaw.lastOrNull()
    require(currentUserMessage != null) { "Invalid last user message" }

    // TODO: better error handling amd add extension scope on all viewmodel with exception handler
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
      throwable.printStackTrace()
    }

    viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
      chatCoordinator.executeMessage(currentUserMessage) {
        this.copy(
          fixedModel = sender.langBot!!.model,
          maxHistoryCount = session.historyInclude,
          mediaCompatibilityHistory = when (mediaCompatibilityHistory) {
            MediaCompatibilityHistory.HELP_PROMPT -> {
              com.aigroup.aigroupmobile.services.chat.MediaCompatibilityHistory.HELP_PROMPT
            }

            MediaCompatibilityHistory.SKIP -> {
              com.aigroup.aigroupmobile.services.chat.MediaCompatibilityHistory.SKIP
            }
          },
          preferredStreamResponse = true,
          mediaRecognizeStrategy = mapOf(
            ImageMediaItem::class to if (imageSingleContext) {
              ChatModelCode.SingleContextFixed(ModelCode(DORAEMON_MODEL, ChatServiceProvider.OFFICIAL))
            } else {
              ChatModelCode.Unspecified
            },
            VideoMediaItem::class to if (videoSingleContext) {
              ChatModelCode.SingleContextFixed(ModelCode(VIDEO_MODEL, ChatServiceProvider.DASH_SCOPE))
            } else {
              ChatModelCode.Unspecified
            }
          ),
          knowledgeBase = session.knowledgeBases.firstOrNull() // TODO: more knowledge base?
        )
      }
    }
  }

  /**
   * > 注意: 当用户选择的模型不支持媒体时，一定要对应的设置 [imageSingleContext] 参数或者 [videoSingleContext] 参数
   *
   * @param imageSingleContext 是否在单独上下文中发送图片, 默认为 false
   * @param videoSingleContext 是否在单独上下文中发送视频, 默认为 false
   * @param mediaCompatibilityHistory 媒体内容的后备采用策略, 默认为 MediaCompatibilityHistory.HELP_PROMPT
   */
  @OptIn(InternalCoroutinesApi::class)
  fun sendContent(
    explicitContent: String? = null,
    imageSingleContext: Boolean = false,
    videoSingleContext: Boolean = false,
    mediaCompatibilityHistory: MediaCompatibilityHistory = MediaCompatibilityHistory.HELP_PROMPT,
  ) {
    require(!bottomBarState.value.isRecognizing) { "Speech recognition is in progress on send content" }
    val textContent = explicitContent ?: bottomBarState.value.fullInputText.text
    val atAssistant = bottomBarState.value.atAssistant

    val session = session.value
    require(session != null) { "ChatSession is not loaded" }

    // TODO: better error handling amd add extension scope on all viewmodel with exception handler
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
      throwable.printStackTrace()
    }
    viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
      val bot = session.primaryBotSender!!.botSender!!

      val assistantBotSender = atAssistant?.let { atAssistant ->
        if (bot.assistant?.id == atAssistant.id) {
          Log.i(TAG, "Send Content: at assistant already ready")
          bot
        } else {
          val atBotSender = session.botSenders.firstOrNull { it.assistant?.id == atAssistant.id }

          // 如果没有该助手，则添加
          if (atBotSender == null) {
            Log.i(TAG, "Send Content: add assistant to chat session")
            val modelCode = dataStore.data.map { it.defaultModelCode }.first()
            conversationDao.addAssistantToChatSession(
              session,
              atAssistant,
              backupModel = ModelCode.fromFullCode(modelCode)
            ).botSender
          } else {
            Log.i(TAG, "Send Content: assistant already in chat session")
            atBotSender
          }
        }
      }

      val localUserSender = userDao.getOrCreateLocalUserSender()
      
      // 对文本内容进行安全处理，确保包含URL的消息能够正常处理
      val safeTextContent = try {
        if (textContent.contains("http://") || textContent.contains("https://")) {
          // 对包含URL的文本进行预处理，确保特殊字符不会导致问题
          textContent.trim()
        } else {
          textContent
        }
      } catch (e: Exception) {
        Log.w(TAG, "Error processing text content: $textContent", e)
        textContent
      }
      
      val item = MessageTextItem(safeTextContent)

      // 检索用户输入框，判断媒体内容，添加 进入消息列表 (Realm)
      val messages = mutableListOf<MessageItem>(item) // 用于插入 realm 数据库的
      val inputMediaItem = bottomBarState.value.mediaItem
      inputMediaItem?.let {
        when (it) {
          is ImageMediaItem -> messages += MessageImageItem().apply {
            image = it;
          }

          is VideoMediaItem -> messages += MessageVideoItem().apply {
            video = it;
          }

          else -> {
            Log.w(TAG, "Unsupported MediaItem type: $it, skipping")
            // 不抛出错误，而是记录警告并跳过
          }
        }
      }

      // 清除用户输入框状态
      withContext(Dispatchers.Main) {
        updateBottomBarState(bottomBarState.value.clearInput())
      }

      // 创建/发送 用户消息
      val message = chatDao.createMessage(session, localUserSender, messages)

      // execute message

      val executeJob = chatCoordinator.executeMessage(message, botSender = assistantBotSender) {
        this.copy(
          maxHistoryCount = session.historyInclude,
          mediaCompatibilityHistory = when (mediaCompatibilityHistory) {
            MediaCompatibilityHistory.HELP_PROMPT -> {
              com.aigroup.aigroupmobile.services.chat.MediaCompatibilityHistory.HELP_PROMPT
            }

            MediaCompatibilityHistory.SKIP -> {
              com.aigroup.aigroupmobile.services.chat.MediaCompatibilityHistory.SKIP
            }
          },
          preferredStreamResponse = true,
          mediaRecognizeStrategy = mapOf(
            ImageMediaItem::class to if (imageSingleContext) {
              ChatModelCode.SingleContextFixed(ModelCode(DORAEMON_MODEL, ChatServiceProvider.OFFICIAL))
            } else {
              ChatModelCode.Unspecified
            },
            VideoMediaItem::class to if (videoSingleContext) {
              ChatModelCode.SingleContextFixed(ModelCode(VIDEO_MODEL, ChatServiceProvider.DASH_SCOPE))
            } else {
              ChatModelCode.Unspecified
            }
          ),
          knowledgeBase = session.knowledgeBases.firstOrNull() // TODO: more knowledge base?
        )
      }

      // TODO: rethrow exception?
//      executeJob.invokeOnCompletion(onCancelling = true) { throwable ->
//        println("Execute message job is cancelled here")
//        this.cancel("Execute message job is cancelled", throwable)
//      }

      // do summary session automatically
      summarySession()
    }
  }
}
