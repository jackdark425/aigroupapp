package com.aigroup.aigroupmobile.utils.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// TODO: using retrofit, 优化写法 (REFACTOR)
// TODO: add logging (脱敏), retry, backoff 等

internal fun createHttpClient(): HttpClient {
  return HttpClient(Android) {
    install(ContentNegotiation) {
      json(
        json = Json { ignoreUnknownKeys = true }
      )
    }
  }
}
