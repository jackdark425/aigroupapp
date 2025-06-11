package com.aigroup.aigroupmobile.data.dao

import com.aigroup.aigroupmobile.data.AppDatabase
import com.aigroup.aigroupmobile.data.models.MessageSenderUser
import com.aigroup.aigroupmobile.data.models.UserProfile
import com.aigroup.aigroupmobile.data.models.specific
import io.realm.kotlin.ext.query
import io.realm.kotlin.notifications.SingleQueryChange
import io.realm.kotlin.query.RealmSingleQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class UserDao @Inject constructor(private val appDatabase: AppDatabase) {

  private fun queryLocalUser(): RealmSingleQuery<UserProfile> {
    return appDatabase.realm.query<UserProfile>().first();
  }

  suspend fun getOrCreateLocalUserSender(): MessageSenderUser {
    return appDatabase.realm.write {
      val localUser = query<UserProfile>().first().find();
      require(localUser != null) { "Local user not found" }

      val sender = query<MessageSenderUser>("userProfile.id = $0", localUser.id).first().find()
      if (sender == null) {
        val newSender = MessageSenderUser().apply {
          userProfile = localUser
        }
        val inclusive = newSender.createInclusive()
        return@write copyToRealm(inclusive).userSender!!
      }
      return@write sender
    }
  }

  fun getLocalUser(): Flow<SingleQueryChange<UserProfile>> {
    return queryLocalUser().asFlow()
  }

  fun hasLocalUser(): Boolean {
    return appDatabase.realm.query<UserProfile>().count().find() > 0
  }

  suspend fun createInitialLocalUser(username: String): UserProfile {
    return appDatabase.realm.write {
      val user = UserProfile().apply {
        this.username = username
      }
      return@write copyToRealm(user)
    }
  }

  /**
   * If local user not found, create a new one.
   */
  suspend fun updateLocalUser(updater: UserProfile.() -> Unit) {
    val userProfile = getOrCreateLocalUserSender().userProfile!!

    appDatabase.realm.write {
      val localUser = findLatest(userProfile)
      require(localUser != null) { "Local user not found" }
      updater.invoke(localUser)
    }
  }

}