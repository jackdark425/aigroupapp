package com.aigroup.aigroupmobile.utils.network

import android.util.Log
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

val KtorCommonRespInInterceptor = createClientPlugin("KtorCommonRespInInterceptor") {
  // create a interceptor that will be used to intercept the response, detect response like cloudflare

  transformResponseBody { response, content, requestedType ->
    if (response.status.value > 299 && response.contentType()?.match(ContentType.Text.Html) == true) {
      val isCloudflare = response.headers[HttpHeaders.Server]?.contains("cloudflare") == true
      if (isCloudflare) {
        // TODO: try using content param?
        throw CloudflareResponseError(response, response.bodyAsText())
      }
    }
    return@transformResponseBody null
  }

}
