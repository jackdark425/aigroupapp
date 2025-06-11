package com.aigroup.aigroupmobile.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.aigroup.aigroupmobile.data.dao.AssistantDao
import com.aigroup.aigroupmobile.data.models.BotAssistant
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.data.models.RemoteAssistant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
  val assistantDao: AssistantDao
): ViewModel() {

  companion object {
    private const val TAG = "AssistantViewModel"
  }

  private val _search = MutableStateFlow("")
  val search = _search.asStateFlow()

  fun setSearch(search: String) {
    _search.value = search
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private val localAssistantsFlow = search.flatMapLatest { search ->
    val query = search.ifEmpty { null }
    assistantDao.getLocalAssistants(query)
  }
  val localAssistants = localAssistantsFlow.asLiveData().map { it.list.toList() }

  fun checkAssistantExists(identifier: String): Flow<Boolean> {
    return assistantDao.getAssistantByStoreIdentifier(identifier).asFlow().map { it.obj != null }
  }

  suspend fun addRemoteAssistantToLocal(remote: RemoteAssistant): BotAssistant {
    val local = assistantDao.getAssistantByStoreIdentifier(remote.identifier).find()
    if (local != null) {
      Log.i(TAG, "Assistant already exists in local database")
      return local
    }

    return withContext(Dispatchers.IO) {
      assistantDao.cloneRemoteAssistant(remote)
    }
  }

  fun removeLocalAssistant(identifier: String) {
    viewModelScope.launch(Dispatchers.IO) {
      assistantDao.removeAssistantByStoreIdentifier(identifier)
    }
  }

  fun deleteLocalAssistant(assistant: BotAssistant) {
    viewModelScope.launch(Dispatchers.IO) {
      assistantDao.deleteAssistant(assistant)
    }
  }

}