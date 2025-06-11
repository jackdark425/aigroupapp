package com.aigroup.aigroupmobile.repositories

import android.util.Log
import androidx.datastore.core.DataStore
import com.aallam.openai.client.Chat
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appStringResource
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.models.LoadingStatus
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.connect.chat.ModelGroup
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.connect.chat.CustomChatServiceProvider
import com.aigroup.aigroupmobile.connect.chat.ServiceProvider
import com.aigroup.aigroupmobile.data.models.data
import com.aigroup.aigroupmobile.data.utils.hasSetToken
import com.aigroup.aigroupmobile.connect.chat.ai
import com.aigroup.aigroupmobile.data.utils.getToken
import com.aigroup.aigroupmobile.repositories.CustomLLMProviderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.SortedMap
import java.util.SortedSet
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

data class ChatModelRepoProviderData(
  val data: SortedMap<ModelGroup, SortedSet<ModelCode>> = sortedMapOf(),
  val loadedAt: Instant = Clock.System.now(),
)

typealias ChatModelRepoData = Map<ChatServiceProvider, LoadingStatus<ChatModelRepoProviderData>>

data class ChatModelRepo(
  val data: ChatModelRepoData = emptyMap(),
)

/**
 * A repository for fetching and storing models cache inside memory.
 */
class ModelRepository @Inject constructor(
  private val dataStore: DataStore<AppPreferences>,
  private val customLLMProviderRepository: CustomLLMProviderRepository
) {
  companion object {
    private const val TAG = "ModelRepository"

    /**
     * The duration after which the models should be reloaded.
     */
    private val ReloadDuration = 3.hours
  }

  private val repo = MutableStateFlow(ChatModelRepo())
  private val customProviderRepo = MutableStateFlow<Map<String, LoadingStatus<ChatModelRepoProviderData>>>(emptyMap())

  fun models(provider: ChatServiceProvider) = repo.asStateFlow().map { it.data[provider] }
  
  /**
   * Get models for a custom provider
   */
  fun customProviderModels(providerId: String) = customProviderRepo.asStateFlow().map { it[providerId] }

  private fun checkShouldLoadByTime(provider: ChatServiceProvider): Boolean {
    return repo.value.data[provider]?.data?.loadedAt?.let {
      it + ReloadDuration <= Clock.System.now()
    } ?: true
  }
  
  private fun checkCustomProviderShouldLoadByTime(providerId: String): Boolean {
    return customProviderRepo.value[providerId]?.data?.loadedAt?.let {
      it + ReloadDuration <= Clock.System.now()
    } ?: true
  }

  private fun checkRepoEmpty(provider: ChatServiceProvider): Boolean {
    return repo.value.data[provider]?.data?.data?.isEmpty() ?: true
  }
  
  private fun checkCustomProviderRepoEmpty(providerId: String): Boolean {
    return customProviderRepo.value[providerId]?.data?.data?.isEmpty() ?: true
  }

  private fun isLoading(provider: ChatServiceProvider): Boolean {
    return repo.value.data[provider]?.let {
      it is LoadingStatus.Loading
    } ?: false
  }
  
  private fun isCustomProviderLoading(providerId: String): Boolean {
    return customProviderRepo.value[providerId]?.let {
      it is LoadingStatus.Loading
    } ?: false
  }

  val availableProviders = dataStore.data.map { appPreferences ->
    val official = ChatServiceProvider.OFFICIAL
    val providers = ChatServiceProvider.entries.filter { it != official }
    listOf(official) + providers.filter {
      !appPreferences.token.getToken(it).isNullOrEmpty()
    }
  }
  
  /**
   * Get all available providers including custom providers
   * This returns a Flow that combines standard providers and custom providers
   */
  val allAvailableProviders = combine(
    dataStore.data.map { appPreferences ->
      val official = ChatServiceProvider.OFFICIAL
      val providers = ChatServiceProvider.entries.filter { it != official }
      listOf<ServiceProvider>(official) + providers.filter {
        !appPreferences.token.getToken(it).isNullOrEmpty()
      }
    },
    customLLMProviderRepository.getAllProvidersAsFlow()
  ) { standardProviders, customProviders ->
    val enabledCustomProviders = customProviders.filter { it.isEnabled }
    standardProviders + enabledCustomProviders
  }

  /**
   * 刷新指定服务提供商的模型（如果需要）
   * 这个方法接受 ServiceProvider 类型的参数，并根据类型调用相应的方法
   */
  suspend fun refreshModelsIfNeeded(provider: ServiceProvider) = withContext(Dispatchers.IO) {
    when (provider) {
      is CustomChatServiceProvider -> refreshCustomProviderModelsIfNeeded(provider.id)
      is ChatServiceProvider -> refreshModelsIfNeeded(provider)
    }
  }

  suspend fun refreshModelsIfNeeded(provider: ChatServiceProvider) = withContext(Dispatchers.IO) {
    // Check if this is a custom provider
    if (CustomChatServiceProvider.isCustomProviderId(provider.id)) {
      refreshCustomProviderModelsIfNeeded(provider.id)
      return@withContext
    }
    
    // Standard provider logic
    val modelEmpty = checkRepoEmpty(provider)
    val shouldLoad = checkShouldLoadByTime(provider) && !isLoading(provider)
    if (!shouldLoad && !modelEmpty) {
      Log.i(TAG, "Models for ${provider.id} are still fresh")
      return@withContext
    }

    Log.i(TAG, "Refreshing models for ${provider.id}")

    // mark loading
    withContext(Dispatchers.Main) {
      val currentData = repo.value.data.toMutableMap()
      currentData[provider] = LoadingStatus.Loading
      repo.value = ChatModelRepo(currentData)
    }

    // fetch models
    val appPreferences = dataStore.data.first()
    val token = appPreferences.token

    val models = try {
      if (provider == ChatServiceProvider.OFFICIAL) {
        fetchOfficialModels()
      } else {
        if (!token.hasSetToken(provider)) {
          Log.e(TAG, "No token set for ${provider.id} (${provider.displayName})")

          // mark error
          withContext(Dispatchers.Main) {
            val currentData = repo.value.data.toMutableMap()
            currentData[provider] = LoadingStatus.Error(
              appStringResource(
                R.string.label_error_no_token_set_for_provider,
                provider.displayName
              )
            )
            repo.value = ChatModelRepo(currentData)
          }
          return@withContext
        }
        fetchForModelProvider(provider)
      }
    }
    // TODO: REFACTOR, 抽象通用的错误处理
    // TODO: 针对不同错误特殊显示，比如 CloudFlare 错误显示检查代理信息
    // https://ktor.io/docs/client-response-validation.html#default
    catch (e: io.ktor.client.plugins.ResponseException) {
      val errorMessage = when (e.response.status) {
        io.ktor.http.HttpStatusCode.Unauthorized -> "Unauthorized, please check your token"
        io.ktor.http.HttpStatusCode.Forbidden -> "Forbidden access, you do not have permission"
        io.ktor.http.HttpStatusCode.NotFound -> "Not Found, the requested resource could not be found"
        io.ktor.http.HttpStatusCode.BadRequest -> "Bad Request, please check your input"
        io.ktor.http.HttpStatusCode.InternalServerError -> "Internal Server Error, please try again later"
        io.ktor.http.HttpStatusCode.BadGateway -> "Bad Gateway, there might be an issue with the server"
        io.ktor.http.HttpStatusCode.ServiceUnavailable -> "Service Unavailable, the server is currently unable to handle the request"
        io.ktor.http.HttpStatusCode.GatewayTimeout -> "Gateway Timeout, the server did not receive a timely response"
        else -> "Unknown error"
      }
      Log.e(TAG, "Failed to fetch models for ${provider.id}: $errorMessage", e)
      withContext(Dispatchers.Main) {
        val currentData = repo.value.data.toMutableMap()
        currentData[provider] = LoadingStatus.Error(errorMessage)
        repo.value = ChatModelRepo(currentData)
      }
      return@withContext
    }
    catch (e: Exception) {
      Log.e(TAG, "Failed to fetch models for ${provider.id}", e)

      // mark error
      withContext(Dispatchers.Main) {
        val currentData = repo.value.data.toMutableMap()
        currentData[provider] = LoadingStatus.Error(e.message ?: "Unknown error")
        repo.value = ChatModelRepo(currentData)
      }
      return@withContext
    }

    // write to memory cache
    withContext(Dispatchers.Main) {
      val currentData = repo.value.data.toMutableMap()
      currentData[provider] = LoadingStatus.Success(
        ChatModelRepoProviderData(
          models.groupBy { it.modelGroup }.mapValues { it.value.toSortedSet() }.toSortedMap(),
          Clock.System.now()
        )
      )
      repo.value = ChatModelRepo(currentData)
    }
  }
  
  /**
   * Refresh models for a custom provider if needed
   */
  suspend fun refreshCustomProviderModelsIfNeeded(providerId: String) = withContext(Dispatchers.IO) {
    // 对于自定义模型，每次都直接获取，不检查缓存
    Log.i(TAG, "Fetching models for custom provider $providerId")

    // mark loading
    withContext(Dispatchers.Main) {
      val currentData = customProviderRepo.value.toMutableMap()
      currentData[providerId] = LoadingStatus.Loading
      customProviderRepo.value = currentData
    }

    // fetch models
    val modelsResult = customLLMProviderRepository.getModelsAsLoadingStatus(providerId)
    
    // write to memory cache
    withContext(Dispatchers.Main) {
      val currentData = customProviderRepo.value.toMutableMap()
      when (modelsResult) {
        is LoadingStatus.Success -> {
          currentData[providerId] = LoadingStatus.Success(
            ChatModelRepoProviderData(
              modelsResult.data.mapValues { it.value.toSortedSet() }.toSortedMap(),
              Clock.System.now()
            )
          )
        }
        is LoadingStatus.Error -> {
          currentData[providerId] = LoadingStatus.Error(modelsResult.message)
        }
        is LoadingStatus.Loading -> {
          // Should not happen
          currentData[providerId] = LoadingStatus.Error("Unexpected loading state")
        }
      }
      customProviderRepo.value = currentData
    }
  }

  /**
   * 使指定提供商的模型缓存失效
   */
  fun invalidateModels(provider: ServiceProvider) {
    when (provider) {
      is CustomChatServiceProvider -> {
        val currentData = customProviderRepo.value.toMutableMap()
        currentData.remove(provider.id)
        customProviderRepo.value = currentData
      }
      is ChatServiceProvider -> {
        if (CustomChatServiceProvider.isCustomProviderId(provider.id)) {
          val currentData = customProviderRepo.value.toMutableMap()
          currentData.remove(provider.id)
          customProviderRepo.value = currentData
        } else {
          val currentData = repo.value.data.toMutableMap()
          currentData.remove(provider)
          repo.value = ChatModelRepo(currentData)
        }
      }
      else -> {
        // 未知类型，不做处理
      }
    }
  }

  fun invalidateAllModels() {
    repo.value = ChatModelRepo()
    customProviderRepo.value = emptyMap()
  }

  private suspend fun fetchOfficialModels(): List<ModelCode> {
    val client = dataStore.data.first().ai(ChatServiceProvider.OFFICIAL)
    return client.models()
      .asSequence()
      .filter { it.ownedBy?.startsWith("vertex") != true }
      .filter {
        val isQwenSubStyle = it.id.id.lowercase().startsWith("qwen/")
        !isQwenSubStyle
      }
      .filter {
        val isClaude = it.id.id.lowercase().startsWith("claude")
        if (isClaude) {
          it.ownedBy != "aws"
        } else {
          true
        }
      }
      .filter {
        val isLlama = it.id.id.lowercase().startsWith("llama")
        if (isLlama) {
          it.ownedBy != "aws"
        } else {
          true
        }
      }
      .filter {
        it.id.id != "qwen-long"
      }
      .distinctBy { it.id.id }
      .map { ModelCode(it.id.id, ChatServiceProvider.OFFICIAL) }
      .filter { it.modelGroup !is ModelGroup.Custom }
      .distinctBy { it.code }
      .toList()
  }

  private suspend fun fetchForModelProvider(provider: ChatServiceProvider): List<ModelCode> {
    Log.i(TAG, "Fetching ${provider.id} models")
    val ai = provider.customModelsEndpoint ?: dataStore.data.first().ai(provider)
    val models = ai.models()
      .map { ModelCode(it.id.id, provider) }
      .filter { it.modelGroup !is ModelGroup.Custom }
      .filter {
        // special case handling for OpenAI official (TODO: remove special case)
        if (provider == ChatServiceProvider.OPEN_AI) {
          it.modelGroup == ModelGroup.Predefined.OpenAI(provider)
        } else {
          true
        }
      }
      .distinctBy { it.code }

    Log.i(TAG, "Fetched ${provider.id} models: ${models.size}")

    return models
  }
}