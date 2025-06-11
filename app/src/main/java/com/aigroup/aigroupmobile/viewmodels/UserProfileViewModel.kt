package com.aigroup.aigroupmobile.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.aigroup.aigroupmobile.data.dao.AssistantDao
import com.aigroup.aigroupmobile.data.dao.ChatConversationDao
import com.aigroup.aigroupmobile.data.dao.MediaItemDao
import com.aigroup.aigroupmobile.data.dao.UserDao
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
  private val userDao: UserDao,
  private val conversationDao: ChatConversationDao,
  private val mediaDao: MediaItemDao,
  private val assistantDao: AssistantDao
) : ViewModel() {

  val profile = userDao.getLocalUser().asLiveData().map {
    // TODO: it's correct? as we already create initial userProfile when started.
    it.obj!!
  }

  val conversationsCount = conversationDao.countChatSession().asLiveData()
  val assistantsCount = assistantDao.countAssistants().asLiveData()
  val commonlyModelCode = conversationDao.getCommonlyModelCode().asLiveData()

  val mediaItems: LiveData<List<Pair<MediaItem, String?>>> = mediaDao.getAllMediaItem().map { it.reversed() }.asLiveData()

  fun updateUsername(username: String) {
    viewModelScope.launch(Dispatchers.IO) {
      userDao.updateLocalUser {
        this.username = username
      }
    }
  }

  fun updateAvatar(mediaItem: ImageMediaItem?) {
    viewModelScope.launch(Dispatchers.IO) {
      userDao.updateLocalUser {
        this.avatar = mediaItem
      }
    }
  }

}