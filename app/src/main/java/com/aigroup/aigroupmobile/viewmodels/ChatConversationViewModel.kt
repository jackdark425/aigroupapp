package com.aigroup.aigroupmobile.viewmodels

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.dao.ChatConversationDao
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.dao.KnowledgeBaseDao
import com.aigroup.aigroupmobile.data.models.BotAssistant
import com.aigroup.aigroupmobile.utils.common.instant
import com.aigroup.aigroupmobile.utils.common.now
import com.aigroup.aigroupmobile.utils.common.startOfDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import javax.inject.Inject

enum class ConversationFilter(@StringRes val displayRes: Int) {
  ALL((R.string.label_session_filter_all)),
  TODAY((R.string.label_session_filter_today)),
  YESTERDAY((R.string.label_session_filter_yesterday)),
  OLDER((R.string.label_session_filter_earlier));

  val display: String
    @Composable
    get() = stringResource(displayRes)

  // TODO: 度过 local date 的12点，该数据筛选可能存在问题
  val timeRange: ClosedRange<LocalDateTime>
    get() {
      val start = when (this) {
        TODAY -> LocalDateTime.now.startOfDay.date
        YESTERDAY -> LocalDateTime.now.startOfDay.date.minus(DatePeriod(days = 1))
        else -> LocalDate.fromEpochDays(0)
      }

      val end = when (this) {
        YESTERDAY -> LocalDateTime.now.startOfDay.date
        OLDER -> LocalDateTime.now.startOfDay.date.minus(DatePeriod(days = 1))
        else -> LocalDateTime.now.startOfDay.date.plus(DatePeriod(days = 1))
      }

      return (LocalDateTime(start, LocalTime.fromSecondOfDay(0)))..(LocalDateTime(
        end,
        LocalTime.fromSecondOfDay(0)
      ))
    }

  val timeRangeLongInSeconds: LongRange
    get() {
      val range = timeRange
      return range.start.instant.epochSeconds..range.endInclusive.instant.epochSeconds
    }
}

@HiltViewModel
class ChatConversationViewModel @Inject constructor(
  private val chatConversationDao: ChatConversationDao,
  private val knowledgeBaseDao: KnowledgeBaseDao,
  private val dataStore: DataStore<AppPreferences>,
) : ViewModel() {

  companion object {
    private const val TAG = "ChatConversationViewModel"
  }

  // filter
  private val _filter = MutableStateFlow(ConversationFilter.ALL)
  val filter = _filter.asStateFlow()

  fun setFilter(filter: ConversationFilter) {
    _filter.value = filter
  }

  private val _search = MutableStateFlow("")
  val search = _search.asStateFlow()

  fun setSearch(search: String) {
    _search.value = search
  }

  // session list
  private val normalSessionsFlow = filter.combine(search) { f, s -> Pair(f, s) }
    .flatMapLatest { (filter, search) ->
      val query = search.ifEmpty { null }
      when (filter) {
        ConversationFilter.ALL -> chatConversationDao.getNormalChatSession(query = query)
        else -> chatConversationDao.getNormalChatSession(filter.timeRangeLongInSeconds, query)
      }
    }
  private val pinnedSessionsFlow = chatConversationDao.getPinnedChatSession()

  // TODO: using flow or livedata (considering the lifecycle-awareness)
  // https://stackoverflow.com/a/73997130
  val normalSessions = normalSessionsFlow.asLiveData().map {
    val raw = it.list.toMutableList()
    val empty = raw.firstOrNull { it.messages.isEmpty() }
    if (empty != null) {
      raw.remove(empty)
      raw.add(0, empty)
    }
    raw.toList()
  }
  val pinnedSessions = pinnedSessionsFlow.asLiveData().map {
    it.list.toList()
  }
  val lastNormalSession = normalSessionsFlow.asLiveData().map {
    it.list.firstOrNull { it.messages.isNotEmpty() }
  }

  // TODO: 优化整理 (REFACTOR)
  fun findSessionWithAssistant(assistant: BotAssistant): Flow<List<ChatSession>> {
    return chatConversationDao.getChatSessionByAssistant(assistant).map { it.list }
  }

  // TODO: don't use suspend function in viewModel in Google Best Practice
  suspend fun createEmptySessionIfNotExists() = withContext(Dispatchers.IO) {
    val modelCode = dataStore.data.map { it.defaultModelCode }.first()
    chatConversationDao.ensureEmptySession(ModelCode.fromFullCode(modelCode))
  }

  suspend fun createEmptySessionWithAssistant(assistant: BotAssistant) = withContext(Dispatchers.IO) {
    val modelCode = dataStore.data.map { it.defaultModelCode }.first()
    val session = chatConversationDao.createChatSession(assistant, backupModel = ModelCode.fromFullCode(modelCode))

    if (assistant.knowledgeBases.isNotEmpty()) {
      Log.d(TAG, "cloning knowledge base")
      // TODO: handle all bases
      val clone = knowledgeBaseDao.cloneKnowledgeBase(assistant.knowledgeBases.first())
      knowledgeBaseDao.attachBaseToSession(clone, session)
    }

    return@withContext session
  }

  fun pinChatSession(session: ChatSession) {
    viewModelScope.launch(Dispatchers.IO) {
      chatConversationDao.updateChatSession(session) {
        this.pinned = true
      }
    }
  }

  fun unpinChatSession(session: ChatSession) {
    viewModelScope.launch(Dispatchers.IO) {
      chatConversationDao.updateChatSession(session) {
        this.pinned = false
      }
    }
  }

  fun updateChatSessionTitle(session: ChatSession, title: String) {
    viewModelScope.launch(Dispatchers.IO) {
      chatConversationDao.updateChatSession(session) {
        this.title = title
      }
    }
  }

  // TODO: don't use suspend function in viewModel in Google Best Practice
  suspend fun deleteChatSession(session: ChatSession) = withContext(Dispatchers.IO) {
    val defaultModelCode = dataStore.data.map { it.defaultModelCode }.first()
    val nextSession = chatConversationDao.ensureEmptySession(
      ModelCode.fromFullCode(defaultModelCode),
      exceptSession = session
    )

    chatConversationDao.deleteChatSession(session)
    nextSession
  }

  fun cloneChatSession(session: ChatSession) {
    viewModelScope.launch(Dispatchers.IO) {
      chatConversationDao.cloneChatSession(session)
    }
  }

  fun updateChatSessionDefaultModel(session: ChatSession, modelCode: ModelCode) {
    viewModelScope.launch(Dispatchers.IO) {
      chatConversationDao.checkAndSwitchSessionPrimaryBot(session, modelCode)
    }
  }

}