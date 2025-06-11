package com.aigroup.aigroupmobile.viewmodels

import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.aigroup.aigroupmobile.connect.voice.VoiceCode
import com.aigroup.aigroupmobile.data.dao.AssistantDao
import com.aigroup.aigroupmobile.data.dao.KnowledgeBaseDao
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.data.models.knowledge.DocumentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import org.w3c.dom.Document
import javax.inject.Inject

@HiltViewModel
class AssistantSettingViewModel @Inject constructor(
  stateHandle: SavedStateHandle,
  private val assistantDao: AssistantDao,
  private val knowledgeBaseDao: KnowledgeBaseDao,
) : ViewModel() {

  // basic state
  private val assistantId = stateHandle.get<String>("assistantId")!!
  val assistant = assistantDao.getAssistantById(assistantId).asLiveData().map { it.obj }

  // detail state

  // knowledge
  val knowledgeDocs = knowledgeBaseDao.getDocsFromAssistant(assistantId).map { it.list.toList() }

}