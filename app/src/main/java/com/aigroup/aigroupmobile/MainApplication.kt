package com.aigroup.aigroupmobile

import android.app.Application
import android.content.Intent
import android.content.res.Resources
import android.util.Log
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import com.aigroup.aigroupmobile.connect.chat.CustomChatServiceProvider
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.AppDatabase
import com.aigroup.aigroupmobile.data.DefaultCustomProviders
import com.aigroup.aigroupmobile.data.dao.ChatConversationDao
import com.aigroup.aigroupmobile.data.dao.UserDao
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.repositories.CustomLLMProviderRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

// TODO: moving to utils
fun appStringResource(@StringRes id: Int): String {
  return MainApplication.resources.getString(id)
}

fun appStringResource(@StringRes id: Int, vararg formatArgs: Any): String {
  return MainApplication.resources.getString(id, *formatArgs)
}

fun appQuantityStringResource(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any): String {
  return MainApplication.resources.getQuantityString(id, quantity, *formatArgs)
}

@HiltAndroidApp
class MainApplication : Application() {
  @Inject
  lateinit var database: AppDatabase

  @Inject
  lateinit var userDao: UserDao

  @Inject
  lateinit var conversationDao: ChatConversationDao

  @Inject
  lateinit var dataStore: DataStore<AppPreferences>
  
  @Inject
  lateinit var customLLMProviderRepository: CustomLLMProviderRepository

  // TODO: 优化这个写法 TODO-I18N
  companion object {
    lateinit var resources: Resources
  }

  override fun onCreate() {
    super.onCreate()
    
//    resources = this.resources

    // Initialize the repository provider for CustomChatServiceProvider
    CustomChatServiceProvider.setRepositoryProvider { customLLMProviderRepository }

    // https://stackoverflow.com/a/7739454/1090523
    // 无需手动断开数据库连接
    Log.i(
      "MainApplication",
      "database initialized with scheme version: ${database.realm.schemaVersion()}"
    )

    runBlocking {
      var appPreferences = dataStore.data.first()

      // set default value
      // TODO: better solution (ref: https://stackoverflow.com/questions/71515906/datastore-chaining-serializers-default-values)

      // 初始化默认 llm 模型
      // migrate to welcome page
//      if (appPreferences.defaultModelCode.isEmpty()) {
//        dataStore.updateData { preferences ->
//          preferences.toBuilder()
//            .setDefaultModelCode(AppPreferencesDefaults.defaultModelCode.fullCode())
//            .build()
//        }
//      }

      // 初始化默认图片模型
      if (appPreferences.defaultImageModel.isEmpty()) {
        appPreferences = dataStore.updateData { preferences ->
          preferences.toBuilder()
            .setDefaultImageModel(AppPreferencesDefaults.defaultImageModel)
            .setDefaultImageResolution(AppPreferencesDefaults.defaultImageResolution)
            .build()
        }
      }
      if (!(1..4).contains(appPreferences.imageN)) {
        appPreferences = dataStore.updateData { preferences ->
          preferences.toBuilder()
            .setImageN(AppPreferencesDefaults.defaultImageN)
            .build()
        }
      }

      // 初始化默认视频模型
      if (appPreferences.defaultVideoModel.isEmpty()) {
        appPreferences = dataStore.updateData { preferences ->
          preferences.toBuilder()
            .setDefaultVideoModel(AppPreferencesDefaults.defaultVideoModel)
            .build()
        }
      }

      // 初始化语音服务
      if (appPreferences.voiceCode.isEmpty()) {
        dataStore.updateData { preferences ->
          preferences.toBuilder()
            .setVoiceCode(AppPreferencesDefaults.defaultVoiceCode.fullCode())
            .build()
        }
      }

      // 初始化默认 llm 参数
      appPreferences = if (appPreferences.hasDefaultModelProperties()) {
        dataStore.updateData { preferences ->
          preferences.toBuilder()
            .setDefaultModelProperties(
              AppPreferencesDefaults.mergeDefaultLongBotProperties(
                preferences.defaultModelProperties
              )
            )
            .build()
        }
      } else {
        dataStore.updateData { preferences ->
          preferences.toBuilder()
            .setDefaultModelProperties(AppPreferencesDefaults.defaultLongBotProperties)
            .build()
        }
      }

      // 初始化默认 deepseek token
      if (appPreferences.token.deepseek.isEmpty()) {
        val deepseekToken = BuildConfig.DEEPSEEK_API_KEY
        if (deepseekToken.isNotEmpty()) {
          dataStore.updateData { preferences ->
            preferences.toBuilder()
              .setToken(
                preferences.token.toBuilder()
                  .setDeepseek(deepseekToken)
                  .build()
              )
              .build()
          }
        }
      }

      // 初始化默认颜色方案
      if (appPreferences.colorScheme == AppPreferences.ColorScheme.UNRECOGNIZED) {
        Log.i("MainApplication", "配色方案未识别，设置为默认配色: ${AppPreferencesDefaults.defaultColorScheme}")
        dataStore.updateData { preferences ->
          preferences.toBuilder()
            .setColorScheme(AppPreferencesDefaults.defaultColorScheme)
            .build()
        }
      } else {
        Log.i("MainApplication", "当前配色方案: ${appPreferences.colorScheme}")
      }

      migrateDataStore(appPreferences)

      // 初始化自定义供应商
      try {
        DefaultCustomProviders.initializeDefaultProviders(customLLMProviderRepository)
        Log.i("MainApplication", "Custom provider with three models initialized successfully")
      } catch (e: Exception) {
        Log.e("MainApplication", "Failed to initialize custom provider: ${e.message}")
      }

      // migrate to welcome page
//      if (!userDao.hasLocalUser()) {
//        Log.i("MainApplication", "Creating initial local user")
//        userDao.createInitialLocalUser("Me")
//      }
    }
  }

  override fun startActivity(intent: Intent?) {
    super.startActivity(intent)
  }

  private suspend fun migrateDataStore(appPreferences: AppPreferences) {
    // migrate defaultImageModel (like: dall-e-2 --> official/dall-e-2)
    if (!appPreferences.defaultImageModel.contains("/")) {
      dataStore.updateData { preferences ->
        preferences.toBuilder()
          .setDefaultImageModel(
            "official/${appPreferences.defaultImageModel}"
          )
          .build()
      }
    }

    // migrate defaultModelCode (MIGRATION.md 29-30)
    dataStore.updateData { preferences ->
      if (preferences.defaultModelCode.isEmpty()) {
        return@updateData preferences
      }
      val newModelCode = ModelCode.tryFromOldFullCode(preferences.defaultModelCode).fullCode()
      preferences.toBuilder()
        .setDefaultModelCode(newModelCode)
        .build()
    }
  }
}