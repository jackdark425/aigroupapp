package com.aigroup.aigroupmobile.data.dao

import android.content.Context
import com.aigroup.aigroupmobile.data.AppDatabase
import com.aigroup.aigroupmobile.data.models.BotAssistant
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.RemoteAssistant
import com.aigroup.aigroupmobile.repositories.AssistantRepository
import com.aigroup.aigroupmobile.ui.theme.errorDark
import com.aigroup.aigroupmobile.utils.system.PathManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.Url
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.notifications.SingleQueryChange
import io.realm.kotlin.query.RealmSingleQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mongodb.kbson.ObjectId
import javax.inject.Inject

class AssistantDao @Inject constructor(
  private val appDatabase: AppDatabase,
  private val pathManager: PathManager,
) {

  private val json = AssistantRepository.Companion.Json

  fun countAssistants(): Flow<Int> {
    return appDatabase.realm.query<BotAssistant>().asFlow().map { it.list.count() }
  }

  // TODO: need suspend here?
  fun getAssistantByStoreIdentifier(identifier: String): RealmSingleQuery<BotAssistant> {
    return appDatabase.realm.query<BotAssistant>("storeIdentifier = $0", identifier).first()
  }

  fun getAssistantById(id: String): Flow<SingleQueryChange<BotAssistant>> {
    val objId = ObjectId(id)
    return appDatabase.realm.query<BotAssistant>("id = $0", objId).first().asFlow()
  }

  fun getLocalAssistants(search: String? = null): Flow<ResultsChange<BotAssistant>> {
    return appDatabase.realm.query<BotAssistant>().let {
      if (search != null) {
        it.query("assistantSchemeStr CONTAINS[c] \"$0\"")
      } else {
        it
      }
    }.asFlow()
  }

  suspend fun removeAssistantByStoreIdentifier(identifier: String) {
    val assistant = getAssistantByStoreIdentifier(identifier).find() ?: return
    appDatabase.realm.write {
      findLatest(assistant)?.let {
        delete(it)
      }
    }
  }

  suspend fun deleteAssistant(assistant: BotAssistant) {
    appDatabase.realm.write {
      findLatest(assistant)?.let {
        delete(it)
      }
    }
  }

  suspend fun cloneRemoteAssistant(remote: RemoteAssistant): BotAssistant {
    val avatarUri = when {
      remote.metadata.avatar != null -> {
        pathManager.saveResDrawableToStorage(remote.metadata.avatar, remote.identifier).toString()
      }
      remote.metadata.avatarLink != null -> {
        // check is local or remote
        if (remote.metadata.avatarLink.startsWith("http")) {
          pathManager.downloadMediaToStorage(Url(remote.metadata.avatarLink)).first.toString()
        } else {
          remote.metadata.avatarLink
        }
      }
      remote.metadata.avatarEmoji != null -> {
        null
      }
      else -> error("No avatar found")
    }

    return appDatabase.realm.write {
      val local = BotAssistant().apply {
        storeIdentifier = remote.identifier
        avatar = avatarUri?.let { ImageMediaItem(it) }
        avatarEmoji = remote.metadata.avatarEmoji
        tags = realmListOf(*remote.metadata.tags.toTypedArray())
        presetsPrompt = remote.configuration.role
        assistantSchemeStr = json.encodeToString(remote)
      }
      return@write copyToRealm(local)
    }
  }

}