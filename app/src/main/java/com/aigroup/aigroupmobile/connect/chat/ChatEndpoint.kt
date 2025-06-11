package com.aigroup.aigroupmobile.connect.chat

import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.logging.Logger
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.ProxyConfig
import com.aigroup.aigroupmobile.utils.network.KtorCommonRespInInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.android.Android
import io.ktor.client.engine.android.AndroidEngineConfig
import io.ktor.client.engine.http
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendIfNameAbsent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.time.DurationUnit

@OptIn(ExperimentalSerializationApi::class)
interface ChatEndpoint : com.aallam.openai.client.Chat, com.aallam.openai.client.Models {
  fun createCommonChatClient(
    config: OpenAIConfig,
    block: HttpClientConfig<AndroidEngineConfig>.() -> Unit = {}
  ): HttpClient {
    val jsonLenient = Json {
      isLenient = true
      ignoreUnknownKeys = true
    }

    return HttpClient(Android) {
      engine {
        config.proxy?.let { proxyConfig ->
          proxy = when (proxyConfig) {
            is ProxyConfig.Http -> ProxyBuilder.http(proxyConfig.url)
            is ProxyConfig.Socks -> ProxyBuilder.socks(proxyConfig.host, proxyConfig.port)
          }
        }
      }

      install(ContentNegotiation) {
        register(ContentType.Application.Json, KotlinxSerializationConverter(jsonLenient))
      }

      install(Logging) {
        val logging = config.logging
        logger = logging.logger.toKtorLogger()
        level = logging.logLevel.toKtorLogLevel()
        if (logging.sanitize) {
          sanitizeHeader { header -> header == HttpHeaders.Authorization }
          // TODO: hide baidu param query (has token and secret)
        }
      }

      install(HttpTimeout) {
        config.timeout.socket?.let { socketTimeout ->
          socketTimeoutMillis = socketTimeout.toLong(DurationUnit.MILLISECONDS)
        }
        config.timeout.connect?.let { connectTimeout ->
          connectTimeoutMillis = connectTimeout.toLong(DurationUnit.MILLISECONDS)
        }
        config.timeout.request?.let { requestTimeout ->
          requestTimeoutMillis = requestTimeout.toLong(DurationUnit.MILLISECONDS)
        }
      }

      install(HttpRequestRetry) {
        maxRetries = config.retry.maxRetries
        // retry on rate limit error.
        retryIf { _, response -> response.status.value.let { it == 429 } }
        exponentialDelay(config.retry.base, config.retry.maxDelay.inWholeMilliseconds)
      }

      defaultRequest {
        url(config.host.baseUrl)
        config.host.queryParams.onEach { (key, value) -> url.parameters.appendIfNameAbsent(key, value) }
        config.headers.onEach { (key, value) -> headers.appendIfNameAbsent(key, value) }
      }

      expectSuccess = true
      install(KtorCommonRespInInterceptor)

      block.invoke(this)

      config.httpClientConfig(this)
    }
  }


  /**
   * Convert Logger to a Ktor's Logger.
   */
  private fun Logger.toKtorLogger() = when (this) {
    Logger.Default -> io.ktor.client.plugins.logging.Logger.DEFAULT
    Logger.Simple -> io.ktor.client.plugins.logging.Logger.SIMPLE
    Logger.Empty -> io.ktor.client.plugins.logging.Logger.EMPTY
  }

  /**
   * Convert LogLevel to a Ktor's LogLevel.
   */
  private fun LogLevel.toKtorLogLevel() = when (this) {
    LogLevel.All -> io.ktor.client.plugins.logging.LogLevel.ALL
    LogLevel.Headers -> io.ktor.client.plugins.logging.LogLevel.HEADERS
    LogLevel.Body -> io.ktor.client.plugins.logging.LogLevel.BODY
    LogLevel.Info -> io.ktor.client.plugins.logging.LogLevel.INFO
    LogLevel.None -> io.ktor.client.plugins.logging.LogLevel.NONE
  }

  /**
   * Apply request options to the request builder.
   */
  fun HttpRequestBuilder.requestOptions(requestOptions: RequestOptions? = null) {
    if (requestOptions == null) return
    requestOptions.headers.forEach { (key, value) ->
      if (headers.contains(key)) headers.remove(key)
      headers[key] = value
    }
    requestOptions.urlParameters.forEach { (key, value) -> url.parameters.append(key, value) }
    requestOptions.timeout?.let { timeout ->
      timeout {
        timeout.connect?.let { connectTimeout -> connectTimeoutMillis = connectTimeout.inWholeMilliseconds }
        timeout.request?.let { requestTimeout -> requestTimeoutMillis = requestTimeout.inWholeMilliseconds }
        timeout.socket?.let { socketTimeout -> socketTimeoutMillis = socketTimeout.inWholeMilliseconds }
      }
    }
  }
}