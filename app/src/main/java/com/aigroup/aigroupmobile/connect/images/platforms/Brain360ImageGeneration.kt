package com.aigroup.aigroupmobile.connect.images.platforms

import android.util.Log
import com.aallam.openai.client.OpenAIConfig
import com.aigroup.aigroupmobile.connect.images.ImageGenerator
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class Brain360ImageGeneration(private val config: OpenAIConfig): ImageGenerator {

  companion object {
    const val TAG = "Brain360ImageGeneration"
  }

  @Serializable
  private data class ImageResponse(
    val status: String,
    val generationTime: Int,
    val output: List<String>,

    // we ignore meta
    // https://ai.360.com/platform/docs/overview
  )

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
        path("images/text2img")
      }
      setBody(buildJsonObject {
        put("model", modelCode)
        put("style", "realistic")
        put("prompt", prompt)
        put("negative_prompt", "")
        put("guidance_scale", 7.5)
        put("height", height)
        put("width", width)
        put("num_inference_steps", 25)
        put("samples", count)
//        put("seed", 49022)
//        put("enhance_prompt", false) // prompt 润色
      })
    }
    val body = response.body<ImageResponse>()

    Log.d(TAG, "createImages done with: $body")

    if (body.status != "success") {
      throw IllegalStateException("Failed to generate image: ${body.output}")
    }

    return body.output.map {
      it.replace("http://", "https://")
    }
  }
}