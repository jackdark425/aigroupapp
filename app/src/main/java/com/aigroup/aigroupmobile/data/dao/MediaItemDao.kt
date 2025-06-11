package com.aigroup.aigroupmobile.data.dao

import com.aigroup.aigroupmobile.data.AppDatabase
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.data.models.MessageChatData
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MediaItemDao @Inject constructor(private val appDatabase: AppDatabase) {

  fun getAllMediaItem(): Flow<List<Pair<MediaItem, String?>>> {
    return appDatabase.realm.query<MessageChatData>()
      .query("imageItem != null || videoItem != null")
      .asFlow()
      .map {
        it.list.mapNotNull {
          when {
            it.imageItem != null -> it.imageItem!!.image!! to it.imageItem!!.helpText
            it.videoItem != null -> it.videoItem!!.video!! to it.videoItem!!.helpText
            else -> null
          }
        }
      }
  }

}