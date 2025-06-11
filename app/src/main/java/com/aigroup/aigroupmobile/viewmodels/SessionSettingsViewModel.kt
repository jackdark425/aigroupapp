package com.aigroup.aigroupmobile.viewmodels

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.aigroup.aigroupmobile.data.dao.ChatDao
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.connect.voice.VoiceCode
import com.aigroup.aigroupmobile.data.dao.ChatConversationDao
import com.aigroup.aigroupmobile.data.dao.KnowledgeBaseDao
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.data.models.botSenders
import com.aigroup.aigroupmobile.data.models.knowledge.DocumentItem
import com.aigroup.aigroupmobile.data.models.primaryBotSender
import com.aigroup.aigroupmobile.data.models.specific
import com.aigroup.aigroupmobile.services.chat.RetrievalAugmentedGeneration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SessionSettingsViewModel @Inject constructor(
  stateHandle: SavedStateHandle,
  private val chatDao: ChatDao,
  private val conversationDao: ChatConversationDao,
  private val knowledgeBaseDao: KnowledgeBaseDao,
  private val dataStore: DataStore<AppPreferences>,
  private val rag: RetrievalAugmentedGeneration
) : ViewModel() {

  companion object {
    private const val TAG = "SessionSettingsViewModel"
  }

  // basic state
  private val chatId = stateHandle.get<String>("chatId")!!
  private val session = chatDao.getConversationById(chatId).asLiveData().map { it.obj }
  private val knowledgeDocsData =  knowledgeBaseDao.getDocsFromChat(chatId).asLiveData().map { it.list.toList() }

  val primaryBotSender = session.map { it?.primaryBotSender?.botSender }
  val botSenders = session.map { it?.botSenders }
  val senders = session.map { it?.senders?.map { it.specific }?.toList() ?: emptyList() }
  val voiceCode = session.map { it?.voiceCode }
  val knowledgeDocs = knowledgeDocsData

  fun addKnowledgeDoc(document: DocumentMediaItem) {
    require(session.value != null) {
      "Session must be loaded before adding knowledge doc"
    }

    viewModelScope.launch(Dispatchers.IO) {
      val base = knowledgeBaseDao.ensureKnowledgeBaseOnSession(session.value!!)
      val baseDoc = knowledgeBaseDao.addKnowledgeDocToBase(base, document)
      Log.d("SessionSettingsViewModel", "Added knowledge doc: $baseDoc")
    }
  }

  fun removeKnowledgeDoc(document: DocumentItem) {
    require(session.value != null) {
      "Session must be loaded before removing knowledge doc"
    }

    viewModelScope.launch(Dispatchers.IO) {
      val knowledgeBase = knowledgeBaseDao.getChatSessionBase(chatId).first().obj
      assert(knowledgeBase != null) { "Knowledge base must be loaded" }

      knowledgeBaseDao.deleteKnowledgeDocFromBase(knowledgeBase!!, document)
    }
  }

  fun switchPrimaryBot(botSender: MessageSenderBot) {
    require(session.value != null) {
      "Session must be loaded before switching primary bot"
    }

    viewModelScope.launch(Dispatchers.IO) {
      conversationDao.switchSessionPrimaryBot(session.value!!, botSender)
    }
  }

  fun updateVoiceCode(code: VoiceCode) {
    require(session.value != null) {
      "Session must be loaded before updating voice code"
    }

    viewModelScope.launch(Dispatchers.IO) {
      chatDao.updateSession(session.value!!) {
        this.voiceCode = code
      }
    }
  }

  suspend fun ragIndexDocument(doc: DocumentItem) {
    require(doc.document != null) { "Document is null" }

    withContext(Dispatchers.IO) {
      val knowledgeDoc = rag.indexDocument(doc.document!!)
      Log.i(TAG, "Indexed rag document: ${knowledgeDoc.id}")

      knowledgeBaseDao.updateDocItem(doc) {
        this.knowledgeDocId = knowledgeDoc.id
      }
      Log.i(TAG, "Updated rag doc: ${doc.id} for $doc")
    }
  }

}