package com.aigroup.aigroupmobile.data

import android.content.Context
import android.util.Log
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.*
import com.aigroup.aigroupmobile.data.models.knowledge.DocumentItem
import com.aigroup.aigroupmobile.data.models.knowledge.KnowledgeBase
import com.aigroup.aigroupmobile.data.models.knowledge.MyObjectBox
import com.aigroup.aigroupmobile.dataStore
import io.objectbox.BoxStore
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.dynamic.DynamicMutableRealmObject
import io.realm.kotlin.dynamic.DynamicRealmObject
import io.realm.kotlin.migration.AutomaticSchemaMigration
import kotlinx.coroutines.runBlocking
import java.io.File

const val SCHEME_VERSION = 34L

class AppDatabase(
  val realm: Realm,
  val objectboxStore: BoxStore,
) {
  companion object {
    private const val TAG = "AppDatabase"

    fun create(context: Context, path: String? = null): AppDatabase {
      return AppDatabase(createNewRealm(path), createNewObjectBox(path, context))
    }

    private fun createNewRealm(path: String? = null): Realm {
      val config = RealmConfiguration
        .Builder(
          schema = setOf(
            ChatSession::class,
            MessageChat::class,
            MessageChatData::class,
            MessageTextItem::class,
            MessageTextItemReference::class,
            MessageImageItem::class,
            MessageVideoItem::class,
            MessageChatError::class,
            MessageSenderInclusive::class,
            MessageSenderUser::class,
            MessageSenderBot::class,
            UserProfile::class,
            LargeLangBot::class,
            ImageMediaItem::class,
            VideoMediaItem::class,
            GenericMediaItem::class,
            BotAssistant::class,

            // rag added
            MessageDocItem::class,
            DocumentMediaItem::class,
            KnowledgeBase::class,
            DocumentItem::class,
            
            // Custom LLM provider
            CustomLLMProvider::class,
          )
        )
        .schemaVersion(SCHEME_VERSION)
        .migration(AutomaticSchemaMigration {
          // 迁移函数 see MIGRATION.md

          // TODO: move to migrations/ (REFACTOR)
          if (it.oldRealm.schemaVersion() <= 12) {
            Log.i(TAG, "Migrating to schema version 12-13")
            it.enumerate(className = "UserProfile") { oldObject: DynamicRealmObject, newObject: DynamicMutableRealmObject? ->
              newObject?.run {
                val list = getValueList("favoriteModels", String::class)
                list.addAll(AppPreferencesDefaults.defaultFavoriteModels)
              }
            }
            Log.i(TAG, "Migration to schema version 12-13 completed")
          }

          if (it.oldRealm.schemaVersion() <= 15) {
            Log.i(TAG, "Migrating to schema version 15-16")
            it.enumerate(className = "ChatSession") { oldObject: DynamicRealmObject, newObject: DynamicMutableRealmObject? ->
              newObject?.run {
                set("voiceCodeString", AppPreferencesDefaults.defaultVoiceCode.fullCode())
              }
            }
            Log.i(TAG, "Migration to schema version 15-16 completed")
          }

          if (it.oldRealm.schemaVersion() <= 29) {
            Log.i(TAG, "Migrating to schema version 29-30")
            it.enumerate(className = "LargeLangBot") { oldObject: DynamicRealmObject, newObject: DynamicMutableRealmObject? ->
              newObject?.run {
                val modelCode = oldObject.getValue("largeLangModelCode", String::class)
                val newModelCode = ModelCode.tryFromOldFullCode(modelCode).fullCode()
                set("largeLangModelCode", newModelCode)
              }
            }
            Log.i(TAG, "Migration to schema version 29-30 completed")
          }

          if (it.oldRealm.schemaVersion() <= 32) {
            Log.i(TAG, "Migrating to schema version 32-33")
            it.enumerate(className = "UserProfile") { oldObject: DynamicRealmObject, newObject: DynamicMutableRealmObject? ->
              newObject?.run {
                val oldList = oldObject.getValueList("favoriteModels", String::class)
                val list = getValueList("favoriteModels", String::class)
                list.clear()

                for (oldModelCode in oldList.toList()) {
                  val newModelCode = ModelCode.tryFromOldFullCode(oldModelCode).fullCode()
                  list.add(newModelCode)
                }
              }
            }
            Log.i(TAG, "Migration to schema version 32-33 completed")
          }
        })
        .let {
          if (path != null) it.directory(path) else it
        }
        .build()
      val realm = Realm.open(config)
      Log.d(TAG, "Realm initialized at: ${config.path}")
      return realm
    }

    private fun createNewObjectBox(path: String? = null, context: Context): BoxStore {
      val store = MyObjectBox.builder()
        .androidContext(context)
        .let {
          if (path != null) {
            Log.d(TAG, "ObjectBox initialized at: $path")
            it.baseDirectory(File(path))
          } else {
            Log.d(TAG, "ObjectBox initialized at default path")
            it
          }
        }
        .build()
      return store
    }
  }
}