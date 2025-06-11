package com.aigroup.aigroupmobile.connect.chat.platforms

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAIConfig
import com.aigroup.aigroupmobile.connect.chat.ChatEndpoint
import com.aigroup.aigroupmobile.utils.network.legacy_streamEventsFrom
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: 考虑 presence_penalty 和 frequency_penalty 参数
// TODO: mark tool choice

class BaiduChat(private val config: OpenAIConfig) : ChatEndpoint {

  @Serializable
  data class AccessTokenResponse(
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("session_key")
    val sessionKey: String,
    @SerialName("access_token")
    val accessToken: String,
    val scope: String,
    @SerialName("session_secret")
    val sessionSecret: String
  )

  @Serializable
  data class ChatBody(
    val messages: List<ChatBodyItem>,
    val stream: Boolean = false,
    val temperature: Double? = null,

    @SerialName("top_p")
    val topP: Double? = null,

    @SerialName("penalty_score")
    val penaltyScore: Double? = null,

    @SerialName("disable_search")
    val disableSearch: Boolean? = null, // default to false

    @SerialName("max_output_tokens")
    val maxOutputTokens: Int? = null,

    val system: String? = null
  ) {

    @Serializable
    data class ChatBodyItem(
      val role: ChatRole,
      val content: String
    )

    @Serializable
    enum class ChatRole {
      @SerialName("user")
      USER,

      @SerialName("assistant")
      ASSISTANT
    }
  }

  @Serializable
  data class ChatResponse(
    val id: String,
    val `object`: String,
    val created: Int,

    /**
     * 表示当前子句的序号。只有在流式接口模式下会返回该字段
     */
    @SerialName("sentence_id")
    val sentenceId: Int? = null,

    /**
     * 	表示当前子句是否是最后一句。只有在流式接口模式下会返回该字
     */
    @SerialName("is_end")
    val isEnd: Boolean? = null,

    @SerialName("is_truncated")
    val isTruncated: Boolean,

    @SerialName("finish_reason")
    val finishReason: String? = null,

    val result: String,

    val usage: com.aallam.openai.api.core.Usage
  )


  companion object {
    const val TAG = "BaiduChat"
    const val ACCESS_API_URL = "https://aip.baidubce.com/oauth/2.0/token"
    const val CHAT_API_BASE = "wenxinworkshop/chat/"

    val modelCodeToUrl = mapOf(
      "ernie-4.0-8k" to "completions_pro",
      "ernie-3.5-8k" to "completions",
      "ernie-speed-8k" to "ernie_speed",
    )
  }

  private val client = createCommonChatClient(config) {
    // TODO: Using install(Auth) 参考 BearerAuthProvider
    // TODO: logger 打印了 百度 key！
  }

  private fun getCredentialInfo(): Pair<String, String> {
    val (apiKey, secretKey) = this.config.token.split(":")
    return Pair(apiKey, secretKey)
  }

  private suspend fun createAccessToken(): String {
    val (apiKey, secretKey) = getCredentialInfo()

    val response = client.post(ACCESS_API_URL) {
      contentType(ContentType.Application.Json)
      url {
        parameters.append("grant_type", "client_credentials")
        parameters.append("client_id", apiKey)
        parameters.append("client_secret", secretKey)
      }
    }
    val accessToken = response.body<AccessTokenResponse>()

    return accessToken.accessToken
  }

  private fun ChatCompletionRequest.toErnieBody(stream: Boolean): ChatBody {
    val systemMessages = this.messages.takeWhile { it.role == com.aallam.openai.api.chat.ChatRole.System }
    val system = systemMessages.map { it.content }.joinToString("\n")
    val restMessages = this.messages.drop(systemMessages.size)

    return ChatBody(
      messages = restMessages.map {
        val role = ChatBody.ChatRole.valueOf(it.role.role.uppercase()) // TODO: consider the function and tool role?
        ChatBody.ChatBodyItem(
          role = role,
          content = it.content ?: "" // TODO: check empty fallback it's correct?
        )
      },
      stream = stream,
      temperature = this.temperature,
      topP = this.topP,
      disableSearch = true,
      maxOutputTokens = this.maxTokens,
      system = system.ifEmpty { null }
    )
  }

  private fun getModelPath(modelId: ModelId): String {
    val id = modelId.id.let {
      modelCodeToUrl[it] ?: it
    }
    return CHAT_API_BASE + id
  }

  override suspend fun chatCompletion(request: ChatCompletionRequest, requestOptions: RequestOptions?): ChatCompletion {
    val token = createAccessToken()
    val body = request.toErnieBody(stream = false)

    val response = client.post {
      contentType(ContentType.Application.Json)
      url {
        path(getModelPath(request.model))
        parameters.append("access_token", token)
      }
      setBody(body)
      requestOptions(requestOptions)
    }
    val chatResponse = response.body<ChatResponse>()

    return ChatCompletion(
      id = chatResponse.id,
      // seconds epoch time to milliseconds epoch time
      created = chatResponse.created.toLong() * 1000,
      model = request.model,
      choices = listOf(
        com.aallam.openai.api.chat.ChatChoice(
          index = 0,
          message = com.aallam.openai.api.chat.ChatMessage(
            role = com.aallam.openai.api.chat.ChatRole.Assistant,
            content = chatResponse.result
          ),
          finishReason = chatResponse.finishReason?.let {
            if (chatResponse.finishReason == "normal") {
              com.aallam.openai.api.core.FinishReason.Stop
            } else {
              com.aallam.openai.api.core.FinishReason(chatResponse.finishReason)
            }
          }
        )
      ),
      usage = chatResponse.usage,
    )
  }

  override fun chatCompletions(
    request: ChatCompletionRequest,
    requestOptions: RequestOptions?
  ): Flow<ChatCompletionChunk> {
    return flow<ChatResponse> {
      val token = createAccessToken()
      val body = request.toErnieBody(stream = true)

      client.preparePost {
        contentType(ContentType.Application.Json)
        url {
          path(getModelPath(request.model))
          parameters.append("access_token", token)
        }
        setBody(body)
        accept(ContentType.Text.EventStream)
        headers {
          append(HttpHeaders.CacheControl, "no-cache")
          append(HttpHeaders.Connection, "keep-alive")
        }
        requestOptions(requestOptions)
      }.execute { response ->
        legacy_streamEventsFrom(response)
      }
    }.map {
      ChatCompletionChunk(
        id = it.id,
        created = it.created.toLong() * 1000,
        model = request.model,
        choices = listOf(
          com.aallam.openai.api.chat.ChatChunk(
            index = 0,
            delta = com.aallam.openai.api.chat.ChatDelta(
              role = com.aallam.openai.api.chat.ChatRole.Assistant,
              content = it.result
            ),
            finishReason = it.finishReason?.let {
              if (it == "normal") {
                com.aallam.openai.api.core.FinishReason.Stop
              } else {
                com.aallam.openai.api.core.FinishReason(it)
              }
            }
          )
        ),
        usage = it.usage
      )
    }
  }

  override suspend fun model(modelId: ModelId, requestOptions: RequestOptions?): Model {
    val models = models(requestOptions)
    return models.find { it.id == modelId } ?: error("Model not found")
  }

  override suspend fun models(requestOptions: RequestOptions?): List<Model> {
    // https://cloud.baidu.com/doc/WENXINWORKSHOP/s/Nlks5zkzu
    return listOf(
      Model(ModelId("ernie-4.0-8k"), null, "baidu"),
      Model(ModelId("ernie-4.0-8k-preview"), null, "baidu"),
      Model(ModelId("ernie-4.0-8k-latest"), null, "baidu"),
      Model(ModelId("ernie-4.0-8k-0329"), null, "baidu"),
      Model(ModelId("ernie-4.0-8k-0613"), null, "baidu"),
      Model(ModelId("ernie-4.0-turbo-8k"), null, "baidu"),
      Model(ModelId("ernie-4.0-turbo-8k-preview"), null, "baidu"),
      Model(ModelId("ernie-4.0-turbo-8k-0628"), null, "baidu"),
      Model(ModelId("ernie-3.5-8k"), null, "baidu"),
      Model(ModelId("ernie-3.5-8k-preview"), null, "baidu"),
      Model(ModelId("ernie-3.5-8k-0329"), null, "baidu"),
      Model(ModelId("ernie-3.5-128k"), null, "baidu"),
      Model(ModelId("ernie-3.5-8k-0613"), null, "baidu"),
      Model(ModelId("ernie-3.5-8k-0701"), null, "baidu"),
      Model(ModelId("ernie-speed-pro-128k"), null, "baidu"),
      Model(ModelId("ernie-speed-8k"), null, "baidu"),
      Model(ModelId("ernie-speed-128k"), null, "baidu"),
      Model(ModelId("ernie-tiny-8k"), null, "baidu"),
    )
  }

}


