package com.aigroup.aigroupmobile.connect.video.platforms

import com.aallam.openai.client.OpenAIConfig
import com.aigroup.aigroupmobile.connect.video.VideoGenerator
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CogVideoGenerator(private val config: OpenAIConfig): VideoGenerator {

  @Serializable
  private data class CreateResponse(
    @SerialName("request_id")
    val requestId: String,
    val id: String,
    val model: String,
    /**
     * PROCESSING, SUCCESS, FAIL
     */
    @SerialName("task_status")
    val taskStatus: String
  )

  @Serializable
  private data class VideoResultResponse(
    @SerialName("request_id")
    val requestId: String,
    val model: String,
    /**
     * PROCESSING, SUCCESS, FAIL
     */
    @SerialName("task_status")
    val taskStatus: String,
    @SerialName("video_result")
    val videoResult: List<Result>? = null
  ) {
    @Serializable
    data class Result(
      val url: String,
      @SerialName("cover_image_url")
      val coverImageUrl: String
    )
  }

  private val client = createCommonVideoClient(config) {
    install(Auth) {
      bearer {
        loadTokens {
          BearerTokens(accessToken = config.token, refreshToken = "")
        }
      }
    }
  }

  override suspend fun createVideo(modelCode: String, prompt: String): String {
    val response = client.post {
      contentType(ContentType.Application.Json)
      url { path("videos/generations") }
      setBody(
        buildJsonObject {
          put("model", modelCode)
          put("prompt", prompt)
          // TODO: 实现 image_url 参考图像
        }
      )
    }.body<CreateResponse>()

    return response.id
  }

  override suspend fun retrieveTaskStatus(id: String): VideoGenerator.VideoGeneration? {
    val response = client.get {
      contentType(ContentType.Application.Json)
      url { path("async-result/$id") }
    }.body<VideoResultResponse>()

    if (response.taskStatus == "PROCESSING") {
      return null
    }
    if (response.taskStatus == "FAIL") {
      throw IllegalStateException("Failed to generate video: ${response.videoResult}")
    }
    if (response.videoResult.isNullOrEmpty()) {
      throw IllegalStateException("Failed to generate video: ${response.videoResult}")
    }

    val video = response.videoResult.first()
    return VideoGenerator.VideoGeneration(
      videoLink = video.url,
      coverLink = video.coverImageUrl
    )
  }
}