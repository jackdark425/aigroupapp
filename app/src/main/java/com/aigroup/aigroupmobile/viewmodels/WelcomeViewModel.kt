package com.aigroup.aigroupmobile.viewmodels

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigroup.aigroupmobile.BuildConfig
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.dao.ChatConversationDao
import com.aigroup.aigroupmobile.data.dao.UserDao
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.ui.pages.welcome.WelcomeInitial
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
  private val dataStore: DataStore<AppPreferences>,
  private val userDao: UserDao,
  private val conversationDao: ChatConversationDao
) : ViewModel() {

  companion object {
    const val TAG = "WelcomeViewModel"
  }

  val userProfile = userDao.getLocalUser().map { it.obj }

  suspend fun flushWelcomeInitial(initial: WelcomeInitial) {
    Log.i(TAG, "flushWelcomeInitial: $initial")
    if (initial.latestInitializedVersionCode != null) {
      dataStore.updateData { preferences ->
        preferences.toBuilder()
          .setInitializedVersion(initial.latestInitializedVersionCode)
          .build()
      }
    }
  }

  suspend fun initializeUsername(username: String) {
    userDao.createInitialLocalUser(username)
  }

  suspend fun initializeDefaultModel(modelCode: ModelCode): ChatSession {
    dataStore.updateData { preferences ->
      preferences.toBuilder()
        .setDefaultModelCode(modelCode.fullCode())
        .build()
    }
    return initializeEmptySession(modelCode)
  }

  private suspend fun initializeEmptySession(modelCode: ModelCode): ChatSession {
    return conversationDao.ensureEmptySession(modelCode)
  }

  fun updateAvatar(avatar: ImageMediaItem?) {
    viewModelScope.launch(Dispatchers.IO) {
      userDao.updateLocalUser {
        this.avatar = avatar
      }
    }
  }

}