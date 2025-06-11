package com.aigroup.aigroupmobile.viewmodels

import android.app.Application
import android.net.Uri
import android.os.storage.StorageManager
import android.util.Log
import android.webkit.URLUtil
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aigroup.aigroupmobile.MainApplication
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.data.AppDatabase
import com.aigroup.aigroupmobile.data.dao.UserDao
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.models.AppPreferences.ServiceToken
import com.aigroup.aigroupmobile.data.models.RemoteTokenConfig
import com.aigroup.aigroupmobile.data.models.UserProfile
import com.aigroup.aigroupmobile.data.utils.clearToken
import com.aigroup.aigroupmobile.data.utils.setToken
import com.aigroup.aigroupmobile.repositories.ModelRepository
import com.aigroup.aigroupmobile.ui.pages.settings.components.CacheStatus
import com.aigroup.aigroupmobile.ui.pages.settings.components.SettingItemActionStatus
import com.aigroup.aigroupmobile.utils.system.PathManager
import com.aigroup.aigroupmobile.utils.common.instant
import com.aigroup.aigroupmobile.utils.common.local
import com.aigroup.aigroupmobile.utils.common.now
import com.aigroup.aigroupmobile.utils.common.sanitize
import com.aigroup.aigroupmobile.utils.network.createHttpClient
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.realm.kotlin.ext.query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@HiltViewModel
class SettingsViewModel @Inject constructor(
  application: Application,
  private val appDataStore: AppDatabase,
  private val dataStore: DataStore<AppPreferences>,
  private val pathManager: PathManager,
  internal val modelRepository: ModelRepository,
  private val userDao: UserDao,
) : AndroidViewModel(application) {

  companion object {
    private const val TAG = "SettingsViewModel"
  }

  val preferences = dataStore.data

  // 清理用户缓存状态
  private val _cacheStatus = MutableStateFlow<SettingItemActionStatus<CacheStatus>>(
    SettingItemActionStatus.Loading
  )
  val cacheStatus = _cacheStatus.asStateFlow()

  // 清理用户数据状态
  private val _localDataStatus = MutableStateFlow<SettingItemActionStatus<Unit>>(
    SettingItemActionStatus.Success(Unit)
  )
  val localDataStatus = _localDataStatus.asStateFlow()

  // 数据备份状态
  private val _backupStatus = MutableStateFlow<SettingItemActionStatus<LocalDateTime?>>(
    SettingItemActionStatus.Success(null)
  )
  val backupStatus = _backupStatus.asStateFlow()

  // models from ai
  val favoriteModels = userDao.getLocalUser()
    .map { it.obj!!.favoriteModels }
    .map { it.map { ModelCode.fromFullCode(it) } }
    .asLiveData()

  fun models(provider: ChatServiceProvider) = modelRepository.models(provider)

  /**
   * 更新 APP 统一 Token，可二维码扫描，支持一键导入
   *
   * @throws IllegalArgumentException 无效的 Token
   */
  @OptIn(ExperimentalEncodingApi::class)
  suspend fun retrieveConfigByAppToken(token: String?): RemoteTokenConfig {
    Log.i(TAG, "update app token: ${token?.sanitize()}")
    if (token.isNullOrEmpty()) {
      // TODO: 自定义 error 方案，以及 i18n (getLocalizedMessage)
      throw IllegalArgumentException("Token 不能为空")
    }

    // token 支持 url 链接，base64 编码的 Token
    // check is url
    val url = if (URLUtil.isNetworkUrl(token)) {
      token
    } else {
      try {
        val decoded = Base64.decode(token).toString(Charsets.UTF_8)
        // TODO: move to constants or invoke fc here (without accessKey and secret?)
        "https://retrieve-config-rcejgpyjkc.cn-shanghai.fcapp.run?token=$decoded"
      } catch (e: IllegalArgumentException) {
        Log.e(TAG, "invalid token in base64: $token, trying using token directly", e)
        "https://retrieve-config-rcejgpyjkc.cn-shanghai.fcapp.run?token=$token"
      }
    }

    if (url == null) {
      throw IllegalArgumentException("Token 无效")
    }

    // TODO: using ktor client as global singleton
    val ktor = createHttpClient()
    val response = ktor.post(url).bodyAsText()

    try {
      val yamlConfig = Base64.decode(response).toString(Charsets.UTF_8)
      return RemoteTokenConfig.fromYaml(yamlConfig)
    } catch (e: IllegalArgumentException) {
      Log.e(TAG, "invalid token response", e)
      throw IllegalArgumentException("Token 无效")
    }
  }

  fun updateModelsIfNeeded(provider: ChatServiceProvider) {
    viewModelScope.launch(Dispatchers.IO) {
      modelRepository.refreshModelsIfNeeded(provider)
    }
  }

  fun updateAllTokenPreferences(token: AppPreferences.Token) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStore.updateData { preferences ->
        preferences.toBuilder()
          .setToken(token)
          .build()
      }

      Log.d(TAG, "all token preferences updated. $token")

      modelRepository.invalidateAllModels()
    }
  }

  fun updateTokenPreferences(provider: ChatServiceProvider, token: String) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStore.updateData { preferences ->
        preferences.toBuilder()
          .setToken(preferences.token.toBuilder().setToken(provider, token))
          .build()
      }

      Log.d(TAG, "token preferences updated for $provider. $token")

      modelRepository.invalidateModels(provider)
    }
  }

  fun clearTokenPreferences(provider: ChatServiceProvider) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStore.updateData { preferences ->
        preferences.toBuilder()
          .setToken(preferences.token.toBuilder().clearToken(provider))
          .build()
      }

      Log.d(TAG, "token preferences cleared for $provider")

      modelRepository.invalidateModels(provider)
    }
  }

  fun updateServiceTokenPreferences(serviceToken: ServiceToken) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStore.updateData { preferences ->
        preferences.toBuilder()
          .setServiceToken(serviceToken)
          .build()
      }

      Log.d(TAG, "service token preferences updated. $serviceToken")
    }
  }

  fun updateTokenConfigs(tokenConfigs: Map<String, AppPreferences.TokenConfig>) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStore.updateData { preferences ->
        preferences.toBuilder()
          .clearTokenConfig()
          .putAllTokenConfig(tokenConfigs)
          .build()
      }

      Log.d(TAG, "token configs updated. $tokenConfigs")
    }
  }

  fun updateTokenConfig(key: String, config: AppPreferences.TokenConfig) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStore.updateData { preferences ->
        preferences.toBuilder()
          .putTokenConfig(key, config)
          .build()
      }

      Log.d(TAG, "token config updated for $key. $config")
    }
  }

  fun updateServiceTokenPreferences(updater: AppPreferences.ServiceToken.Builder.() -> AppPreferences.ServiceToken) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStore.updateData { preferences ->
        preferences.toBuilder()
          .setServiceToken(updater.invoke(preferences.serviceToken.toBuilder()))
          .build()
      }

      Log.d(TAG, "service token preferences updated.")
    }
  }

  fun updateDefaultModel(model: ModelCode) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStore.updateData { preferences ->
        preferences.toBuilder()
          .setDefaultModelCode(model.fullCode())
          .build()
      }
    }
  }

  fun updatePreferences(updater: AppPreferences.Builder.() -> AppPreferences) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStore.updateData {
        updater.invoke(it.toBuilder())
      }
    }
  }

  fun updateLongBotProperties(properties: AppPreferences.LongBotProperties) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStore.updateData { preferences ->
        preferences.toBuilder()
          .setDefaultModelProperties(properties)
          .build()
      }
    }
  }

  fun resetLongBotProperties() = updateLongBotProperties(AppPreferencesDefaults.defaultLongBotProperties)

  fun pinModelCode(model: ModelCode) {
    viewModelScope.launch(Dispatchers.IO) {
      userDao.updateLocalUser {
        // TODO: Using set?
        if (!favoriteModels.contains(model.fullCode())) {
          favoriteModels.add(model.fullCode())
        }
      }
    }
  }

  fun cancelPinModelCode(model: ModelCode) {
    viewModelScope.launch(Dispatchers.IO) {
      userDao.updateLocalUser {
        favoriteModels.remove(model.fullCode())
      }
    }
  }

  // ACTIONS

  // TODO: update every 5 seconds with LifecycleObserver and using okio? (all update functions)
  fun updateCacheSize() {
    viewModelScope.launch(Dispatchers.IO) {
      val newSize = getCacheMbSize()
      _cacheStatus.value = SettingItemActionStatus.Success(CacheStatus(newSize))
    }
  }

  fun updateBackupStatus() {
    viewModelScope.launch(Dispatchers.IO) {
      val instant = preferences.map {
        if (it.hasLastDataBackup()) {
          it.lastDataBackup
        } else {
          null
        }
      }.first()?.let {
        Instant.fromEpochSeconds(it.seconds, it.nanos)
      }
      if (instant != null) {
        _backupStatus.value = SettingItemActionStatus.Success(instant.local)
      }
    }
  }

  fun clearCache() {
    viewModelScope.launch(Dispatchers.IO) {
      _cacheStatus.value = SettingItemActionStatus.Loading

      val ctx = getApplication<MainApplication>().applicationContext;

      val cacheDir = ctx.cacheDir;
      Log.i(TAG, "clear Cache dir: $cacheDir")
      delay(2000L) // For UX

      // clear all subdirectories
      cacheDir.listFiles()?.forEach {
        it.deleteRecursively()
      }

      val newSize = getCacheMbSize()
      _cacheStatus.value = SettingItemActionStatus.Success(CacheStatus(newSize))
    }
  }

  suspend fun backupUserData(): Uri {
    return withContext(Dispatchers.IO) {
      _backupStatus.value = SettingItemActionStatus.Loading
      delay(2000L) // For UX

      val targetZip = pathManager.zipUserDataDirectory()

      val now = LocalDateTime.now
      val timestamp = com.google.protobuf.Timestamp.newBuilder()
        .setSeconds(now.instant.epochSeconds)
        .setNanos(now.instant.nanosecondsOfSecond)
        .build()
      dataStore.updateData { preferences ->
        preferences.toBuilder()
          .setLastDataBackup(timestamp)
          .build()
      }
      _backupStatus.value = SettingItemActionStatus.Success(now)

      return@withContext targetZip
    }
  }

  fun clearUserLocalData() {
    viewModelScope.launch(Dispatchers.IO) {
      _localDataStatus.value = SettingItemActionStatus.Loading
      delay(2000L) // For UX

      // DANGER!!!: clear all user data
      // NOTE!!!: this is really dangerous
      appDataStore.realm.write {
        delete(ChatSession::class)

        // TODO: 统一跟 MainApplication 的初始化逻辑, 清楚 medias，放在 PathManager, 做 realm close？
        val profile = query<UserProfile>().first().find()
        if (profile != null) {
          profile.username = "Me"
          profile.avatar = null
          profile.email = null
        }
      }

      _localDataStatus.value = SettingItemActionStatus.Success(Unit)
    }
  }

  // UTILS

  private fun getCacheMbSize(): Double {
    val ctx = getApplication<MainApplication>().applicationContext;
    val storage = ctx.getSystemService<StorageManager>()!!;

    val cacheDir = ctx.cacheDir;
    val uuid = storage.getUuidForPath(cacheDir)

    val size = storage.getCacheSizeBytes(uuid);
    val quota = storage.getCacheQuotaBytes(uuid)
    // TODO: using quota

    return size.toDouble() / 1024 / 1024
  }

}