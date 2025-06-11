package com.aigroup.aigroupmobile.utils.common

import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.random.Random

suspend fun <T> exponentialBackoff(
  base: Int = 2,
  maxDelayMs: Long = 60_000L,
  randomizationMs: Long = 1000L,
  maxRetries: Int = 5,
  shouldRetry: suspend (T) -> Boolean = { false },
  block: suspend (retry: Int) -> T,
): T? {
  var retry = 0
  while (retry <= maxRetries) {
    try {
      val result = block(retry)
      if (!shouldRetry(result)) {
        return result
      } else {
        val delay = minOf(base.toDouble().pow(retry).toLong() * 1000L, maxDelayMs)
        delay(delay + randomMs(randomizationMs))
        retry++
      }
    } catch (e: Exception) {
      val delay = minOf(base.toDouble().pow(retry).toLong() * 1000L, maxDelayMs)
      delay(delay + randomMs(randomizationMs))
      retry++
    }
  }
  return null
}

private fun randomMs(randomizationMs: Long): Long =
  if (randomizationMs == 0L) 0L else Random.nextLong(randomizationMs)