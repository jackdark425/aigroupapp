package com.aigroup.aigroupmobile.utils.network

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse

class CloudflareResponseError(
  response: HttpResponse,
  cachedResponseText: String
) : ResponseException(response, cachedResponseText) {
  override val message: String =
    "Cloudflare Response Error(${response.call.request.method.value} ${response.call.request.url}) " +
        "invalid: ${response.status}. Text: \"$cachedResponseText\""
}
