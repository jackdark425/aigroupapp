package com.aigroup.aigroupmobile.utils.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

private const val STREAM_PREFIX = "data:"
private const val EVENT_PREFIX = "event:"
private const val STREAM_END_TOKEN = "$STREAM_PREFIX [DONE]"

private val JsonLenient = Json {
  isLenient = true
  ignoreUnknownKeys = true
}

/**
 * Utils for Server-Sent Events client. Can receive event and emit to a flow.
 *
 * @deprecated Use [streamEventsFrom] instead.
 */
internal suspend inline fun <reified T> FlowCollector<T>.legacy_streamEventsFrom(response: HttpResponse) {
  val channel: ByteReadChannel = response.body()
  try {
    while (currentCoroutineContext().isActive && !channel.isClosedForRead) {
      val line = channel.readUTF8Line() ?: continue
      val value: T = when {
        line.startsWith(STREAM_END_TOKEN) -> break
        line.startsWith(STREAM_PREFIX) -> JsonLenient.decodeFromString(line.removePrefix(STREAM_PREFIX))
        else -> continue
      }
      emit(value)
    }
  } finally {
    channel.cancel()
  }
}

sealed class StreamEventStatus<out T> {
  data class Continue<T>(val data: T) : StreamEventStatus<T>()
  data object Stop : StreamEventStatus<Nothing>()
}

internal suspend inline fun <reified T> FlowCollector<T>.streamEventsFrom(
  response: HttpResponse,
  eventBlock: (event: String, data: String) -> StreamEventStatus<T>
) {
  val channel: ByteReadChannel = response.body()
  try {
    var event: String? = null

    while (currentCoroutineContext().isActive && !channel.isClosedForRead) {
      val line = channel.readUTF8Line() ?: continue
      when {
        line.startsWith(EVENT_PREFIX) -> {
          event = line.removePrefix(EVENT_PREFIX)
        }
        line.startsWith(STREAM_PREFIX) -> {
          val data = line.removePrefix(STREAM_PREFIX)
          val status = eventBlock(event ?: "", data)
          when (status) {
            is StreamEventStatus.Continue -> emit(status.data)
            is StreamEventStatus.Stop -> break
          }
        }
        else -> continue
      }
    }
  } finally {
    channel.cancel()
  }
}
