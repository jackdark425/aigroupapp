package com.aigroup.aigroupmobile.connect.images.platforms

import android.util.Log
import com.aallam.openai.client.OpenAIConfig
import com.aigroup.aigroupmobile.connect.images.ImageGenerator
import com.aigroup.aigroupmobile.utils.common.exponentialBackoff
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.retry
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class QwenImageGeneration(private val config: OpenAIConfig) : ImageGenerator {

  companion object {
    const val TAG = "QwenImageGeneration"
  }

  @Serializable
  private data class ImageResponse(
    @SerialName("request_id")
    val requestId: String,
    val output: Output,
    val usage: Usage? = null,
  ) {
    @Serializable
    data class Output(
      @SerialName("task_id")
      val taskId: String,
      @SerialName("task_status")
      val taskStatus: String,
      @SerialName("submit_time")
      val submitTime: String,
      @SerialName("scheduled_time")
      val scheduledTime: String? = null,
      @SerialName("end_time")
      val endTime: String? = null,
      val results: List<Result>? = null,
      @SerialName("task_metrics")
      val taskMetrics: TaskMetrics,

      /**
       * 错误码
       */
      val code: String? = null,

      /**
       * 错误信息
       */
      val message: String? = null,
    ) {

    }

    @Serializable
    data class Result(
      val url: String,
    )

    @Serializable
    data class TaskMetrics(
      @SerialName("TOTAL")
      val total: Int,
      @SerialName("SUCCEEDED")
      val succeeded: Int,
      @SerialName("FAILED")
      val failed: Int,
    )

    @Serializable
    data class Usage(
      @SerialName("image_count")
      val imageCount: Int,
    )
  }

  @Serializable
  private data class ImageCreateTaskResponse(
    val output: Output,
    @SerialName("request_id")
    val requestId: String,
  ) {
    @Serializable
    data class Output(
      @SerialName("task_id")
      val taskId: String,
      @SerialName("task_status")
      val taskStatus: String,
    )
  }

  private val client = createCommonImageClient(config) {
    install(Auth) {
      bearer {
        loadTokens {
          BearerTokens(accessToken = config.token, refreshToken = "")
        }
      }
    }
  }

  override suspend fun createImages(modelCode: String, resolution: String, prompt: String, count: Int): List<String> {
    val (width, height) = resolution.split("x").map { it.toInt() }

    val response = client.post {
      contentType(ContentType.Application.Json)
      url {
        path("services/aigc/text2image/image-synthesis")
      }
      headers {
        append("X-DashScope-Async", "enable")
      }
      setBody(buildJsonObject {
        put("model", modelCode)
        put("input", buildJsonObject {
          put("prompt", prompt)
        })
        put("parameters", buildJsonObject {
          put("style", "<auto>")
          put("size", "$width*$height")
          put("n", count)
//          put("seed", 42)
        })
      })
    }
    val body = response.body<ImageCreateTaskResponse>()

    Log.d(TAG, "createImages done with: $body")

    if (body.output.taskStatus != "PENDING") {
      throw IllegalStateException("Failed to create task to generate image: ${body.output.taskStatus}")
    }

    return getTaskStatus(body.output.taskId).map { it.url }
  }

  private suspend fun getTaskStatus(taskId: String): List<ImageResponse.Result> {
    val response = exponentialBackoff(
      maxRetries = 15,
      shouldRetry = {
        if (it.status == HttpStatusCode.OK) {
          val body = it.body<ImageResponse>()
          if (listOf("FAILED", "UNKNOWN").contains(body.output.taskStatus)) {
            return@exponentialBackoff false
          }

          !listOf("SUCCEEDED").contains(body.output.taskStatus)
        } else {
          true
        }
      }
    ) {
      client.post {
        contentType(ContentType.Application.Json)
        url {
          path("tasks/$taskId")
        }
        retry {
          exponentialDelay()
        }
      }
    }

    if (response?.status != HttpStatusCode.OK) {
      throw IllegalStateException("Failed to get task status: ${response?.status}")
    }

    val body = response.body<ImageResponse>()
    Log.d(TAG, "getTaskStatus done with: $body")

    if (body.output.taskStatus != "SUCCEEDED") {
      val code = body.output.code
      val message = body.output.message
      throw IllegalStateException("Failed to generate image (code: $code / message: $message): ${body.output.taskStatus}")
    }

    return body.output.results!!
  }
}