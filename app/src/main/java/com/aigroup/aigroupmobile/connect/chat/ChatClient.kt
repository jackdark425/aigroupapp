package com.aigroup.aigroupmobile.connect.chat

import com.aallam.openai.api.logging.Logger
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.aigroup.aigroupmobile.connect.chat.platforms.OpenAIChatEndpoint
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.utils.TokenNotSetException
import com.aigroup.aigroupmobile.data.utils.getToken
import com.aigroup.aigroupmobile.utils.network.KtorCommonRespInInterceptor
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpSend
import kotlin.time.Duration.Companion.seconds

/**
 * officialAI always using predefined api base instead of user specified api base
 */
fun AppPreferences.officialAI() = let {
  OpenAI(
    OpenAIConfig(
      host = OpenAIHost(ChatServiceProvider.OFFICIAL.apiBase),
      // TODO: 有可能导致 apikey 错误的情况
      token = it.token.getToken(ChatServiceProvider.OFFICIAL) ?: "",
      logging = LoggingConfig(
        logger = Logger.Default
      )
    ),
  )
}

/**
 * @throws TokenNotSetException if token is not set
 */
fun AppPreferences.ai(provider: ServiceProvider, requiresToken: Boolean = false): ChatEndpoint = let {
  when (provider) {
    is ChatServiceProvider -> aiForChatServiceProvider(provider, requiresToken)
    is CustomChatServiceProvider -> aiForCustomChatServiceProvider(provider, requiresToken)
    else -> throw IllegalArgumentException("Unsupported service provider type: ${provider::class.java.name}")
  }
}

/**
 * @throws TokenNotSetException if token is not set
 */
private fun AppPreferences.aiForChatServiceProvider(provider: ChatServiceProvider, requiresToken: Boolean = false): ChatEndpoint = let {
  // TODO: this not apply customModelsEndpoint, and remove directly usage of customModelsEndpoint

  val endpointExtraConfig = tokenConfigMap.get(provider.id)

  val apiBase = endpointExtraConfig?.let { cfg ->
    if (cfg.hasApiBase())
      cfg.apiBase.let {
        if (!it.endsWith("/")) "$it/" else it
      }
    else
      null
  }

  val token = it.token.getToken(provider)
  if (requiresToken && token.isEmpty()) {
    throw TokenNotSetException(provider)
  }

  val config = OpenAIConfig(
    host = OpenAIHost(apiBase ?: provider.apiBase),
    token = token,
    logging = LoggingConfig(
      logger = Logger.Default
    ),
    httpClientConfig = {
      expectSuccess = true

      install(KtorCommonRespInInterceptor)
      
      // 配置默认超时
      install(io.ktor.client.plugins.HttpTimeout) {
        connectTimeoutMillis = (AppPreferencesDefaults.DEFAULT_CONNECT_TIMEOUT_SECONDS * 1000).toLong()
        requestTimeoutMillis = (AppPreferencesDefaults.DEFAULT_REQUEST_TIMEOUT_SECONDS * 1000).toLong()
        socketTimeoutMillis = (AppPreferencesDefaults.DEFAULT_SOCKET_TIMEOUT_SECONDS * 1000).toLong()
      }

//      client.plugin(HttpSend).intercept { request ->
//        val originalCall = execute(request)
//        if (originalCall.response.status.value !in 100..399) {
//          execute(request)
//        } else {
//          originalCall
//        }
//      }

    },
  )
  provider.createChatClient(config) ?: OpenAIChatEndpoint(OpenAI(config))
}

/**
 * @throws TokenNotSetException if token is not set
 */
private fun AppPreferences.aiForCustomChatServiceProvider(provider: CustomChatServiceProvider, requiresToken: Boolean = false): ChatEndpoint = let {
  // For custom providers, we use the API key stored in the provider itself
  val token = provider.apiKey
  if (requiresToken && token.isEmpty()) {
    throw TokenNotSetException(ChatServiceProvider.OFFICIAL) // Using OFFICIAL as a placeholder
  }

  val config = OpenAIConfig(
    host = OpenAIHost(provider.apiBase),
    token = token,
    logging = LoggingConfig(
      logger = Logger.Default
    ),
    httpClientConfig = {
      expectSuccess = true
      install(KtorCommonRespInInterceptor)
      
      // 配置默认超时 - 对自定义LLM提供商特别重要
      install(io.ktor.client.plugins.HttpTimeout) {
        connectTimeoutMillis = (AppPreferencesDefaults.DEFAULT_CONNECT_TIMEOUT_SECONDS * 1000).toLong()
        requestTimeoutMillis = (AppPreferencesDefaults.DEFAULT_REQUEST_TIMEOUT_SECONDS * 1000).toLong()
        socketTimeoutMillis = (AppPreferencesDefaults.DEFAULT_SOCKET_TIMEOUT_SECONDS * 1000).toLong()
      }
    },
  )
  
  // Custom providers use the standard OpenAI API format
  provider.createChatClient(config) ?: OpenAIChatEndpoint(OpenAI(config))
}