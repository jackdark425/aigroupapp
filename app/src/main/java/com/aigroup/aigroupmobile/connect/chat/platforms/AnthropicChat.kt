package com.aigroup.aigroupmobile.connect.chat.platforms

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ImagePart
import com.aallam.openai.api.chat.ListContent
import com.aallam.openai.api.chat.TextContent
import com.aallam.openai.api.chat.TextPart
import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.core.Usage
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAIConfig
import com.aigroup.aigroupmobile.connect.chat.ChatEndpoint
import com.aigroup.aigroupmobile.utils.network.StreamEventStatus
import com.aigroup.aigroupmobile.utils.network.streamEventsFrom
import io.ktor.client.call.body
import io.ktor.client.plugins.defaultRequest
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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addAll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.math.min

// TODO: json 模式支持，json scheme 支持，填空模式支持

class AnthropicChat(private val config: OpenAIConfig) : ChatEndpoint {

  @Serializable
  data class ChatResponse(
    val id: String,

    /**
     * For Messages, this is always "message".
     */
    val type: String,

    val role: String,
    val content: List<Content>,

    val model: String,

    /**
     * map to openai value
     *
     * end_turn -> stop
     * max_tokens -> length
     * stop_sequence -> stop
     * tool_use -> tool_calls / function_call (deprecated)
     */
    @SerialName("stop_reason")
    val stopReason: StopReason? = null,

    /**
     * Which custom stop sequence was generated, if any.
     * This value will be a non-null string if one of your custom stop sequences was generated.
     */
    @SerialName("stop_sequence")
    val stopSequence: String? = null,

    val usage: Usage
  ) {
    @Serializable
    enum class StopReason {
      @SerialName("end_turn")
      EndTurn,

      @SerialName("max_tokens")
      MaxTokens,

      @SerialName("stop_sequence")
      StopSequence,

      @SerialName("tool_use")
      ToolUse;

      val openaiStopReason: com.aallam.openai.api.core.FinishReason
        get() = when (this) {
          EndTurn -> com.aallam.openai.api.core.FinishReason.Stop
          MaxTokens -> com.aallam.openai.api.core.FinishReason.Length
          StopSequence -> com.aallam.openai.api.core.FinishReason.Stop
          ToolUse -> com.aallam.openai.api.core.FinishReason.ToolCalls
        }
    }

    @Serializable
    data class Content(
      val type: String,
      val text: String
    )

    @Serializable
    data class Usage(
      @SerialName("input_tokens")
      val inputTokens: Int,
      @SerialName("output_tokens")
      val outputTokens: Int
    )
  }

  sealed class ChatResponseChunk {
    @Serializable
    data class MessageStart(
      val message: ChatResponse,
      val type: String
    ) : ChatResponseChunk()

    @Serializable
    data class ContentBlockDelta(
      val type: String,
      val index: Int,
      val delta: Delta
    ) : ChatResponseChunk() {
      @Serializable
      data class Delta(
        val type: String,
        val text: String
      )
    }

    @Serializable
    data class MessageDelta(
      val type: String,
      val delta: Delta,
      val usage: PartialUsage
    ) : ChatResponseChunk() {
      @Serializable
      data class Delta(
        @SerialName("stop_reason")
        val stopReason: ChatResponse.StopReason? = null,
        @SerialName("stop_sequence")
        val stopSequence: String? = null,
      )

      @Serializable
      data class PartialUsage(
        @SerialName("output_tokens")
        val outputTokens: Int
      )
    }

    // TODO: support content_block_start to support tools
  }


  companion object {
    const val TAG = "AnthropicChat"
    const val PathChat = "messages"

    // TODO: move this to ModelCode
    /**
     * The maximum number of tokens to generate before stopping.
     * https://docs.anthropic.com/en/docs/about-claude/models
     */
    val ModelToMaxTokens = mapOf(
      "claude-3-5-sonnet" to 8192,
      "claude-3-opus" to 4096,
      "claude-3-sonnet" to 4096,
      "claude-3-haiku" to 4096,
      "claude-2.1" to 4096,
      "claude-2.0" to 4096,
      "claude-instant-1.2" to 4096,
    )
  }

  @OptIn(ExperimentalSerializationApi::class)
  private val JsonLenient = Json {
    isLenient = true
    ignoreUnknownKeys = true
    explicitNulls = false
  }

  private val client = createCommonChatClient(config) {
    defaultRequest {
      headers {
        append("anthropic-version", "2023-06-01")
        append("x-api-key", config.token)
      }
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
  private fun ChatCompletionRequest.toAnthropicBody(stream: Boolean): JsonObject {
    val systemMessages = this.messages.takeWhile { it.role == com.aallam.openai.api.chat.ChatRole.System }
    val system = systemMessages.map { it.content }.joinToString("\n")
    val restMessages = this.messages.drop(systemMessages.size)

    val modelCode = model.id
    val maxOutputTokens = ModelToMaxTokens.entries
      .find { modelCode.startsWith(it.key) }
      ?.value ?: 4096

    val chatMessages = restMessages.map {
      // map if is a image
      when (val content = it.messageContent!!) {
        is ListContent -> {
          buildJsonObject {
            put("role", it.role.role)
            putJsonArray("content") {
              val contentList = content.content.map { part ->
                when (part) {
                  is ImagePart -> {
                    val base64Full = part.imageUrl.url
                    val split = ";base64,"
                    val (header, data) = base64Full.split(split)
                    val mime = header.removePrefix("data:")

                    buildJsonObject {
                      put("type", "image")
                      putJsonObject("source") {
                        put("type", "base64")
                        put("media_type", mime)
                        put("data", data)
                      }
                    }
                  }

                  is TextPart -> {
                    buildJsonObject {
                      put("type", "text")
                      put("text", part.text)
                    }
                  }

                  else -> {
                    error("Unsupported content type")
                  }
                }
              }
              addAll(contentList)
            }
          }
        }

        is TextContent -> {
          buildJsonObject {
            put("role", it.role.role)
            put("content", it.content)
          }
        }
      }
    }

    return buildJsonObject {
      // https://docs.anthropic.com/en/api/messages
      put("stream", stream)
      put("model", model.id)
      put("max_tokens", min(maxTokens ?: maxOutputTokens, maxOutputTokens))

      stop?.let { stop ->
        putJsonArray("stop_sequences") {
          addAll(stop)
        }
      }

      if (system.isNotEmpty()) {
        put("system", system)
      }
      temperature?.let { put("temperature", it) }
      topP?.let { put("top_p", it) }

      putJsonArray("messages") {
        addAll(chatMessages)
      }

      // TODO: mark metadata
      // TODO: mark top_k
      // TODO: support tool_choice and tools (support parallel_tool_use)
    }
  }

  override suspend fun chatCompletion(request: ChatCompletionRequest, requestOptions: RequestOptions?): ChatCompletion {
    val body = request.toAnthropicBody(stream = false)

    val response = client.post {
      contentType(ContentType.Application.Json)
      url {
        path(PathChat)
      }
      setBody(body)
      requestOptions(requestOptions)
    }
    val chatResponse = response.body<ChatResponse>()

    return ChatCompletion(
      id = chatResponse.id,
      created = Clock.System.now().toEpochMilliseconds(),
      model = request.model,
      choices = chatResponse.content.map {
        com.aallam.openai.api.chat.ChatChoice(
          index = 0,
          message = com.aallam.openai.api.chat.ChatMessage(
            role = com.aallam.openai.api.chat.ChatRole.Assistant,
            content = it.text
          ),
          finishReason = chatResponse.stopReason?.openaiStopReason
        )
      },
      usage = Usage(
        promptTokens = chatResponse.usage.inputTokens,
        completionTokens = chatResponse.usage.outputTokens,
        totalTokens = chatResponse.usage.inputTokens + chatResponse.usage.outputTokens
      ),
    )
  }

  override fun chatCompletions(
    request: ChatCompletionRequest,
    requestOptions: RequestOptions?
  ): Flow<ChatCompletionChunk> {
    var messageId = ""
    var startUsage: ChatResponse.Usage? = null

    return flow<ChatResponseChunk?> {
      val body = request.toAnthropicBody(stream = true)

      client.preparePost {
        contentType(ContentType.Application.Json)
        url {
          path(PathChat)
        }
        setBody(body)
        accept(ContentType.Text.EventStream)
        headers {
          append(HttpHeaders.CacheControl, "no-cache")
          append(HttpHeaders.Connection, "keep-alive")
        }
        requestOptions(requestOptions)
      }.execute { response ->
        streamEventsFrom(response) { event, data ->
          when (event.trim()) {
            "message_start" -> {
              val message = JsonLenient.decodeFromString<ChatResponseChunk.MessageStart>(data)
              messageId = message.message.id
              startUsage = message.message.usage
              StreamEventStatus.Continue(message)
            }

            "content_block_delta" -> {
              val delta = JsonLenient.decodeFromString<ChatResponseChunk.ContentBlockDelta>(data)
              StreamEventStatus.Continue(delta)
            }

            "message_delta" -> {
              val delta = JsonLenient.decodeFromString<ChatResponseChunk.MessageDelta>(data)
              StreamEventStatus.Continue(delta)
            }

            "message_stop" -> {
              StreamEventStatus.Stop
            }

            else -> {
              StreamEventStatus.Continue(null)
            }
          }
        }
      }
    }.mapNotNull {
      it?.let {
        ChatCompletionChunk(
          id = messageId,
          created = Clock.System.now().toEpochMilliseconds(),
          model = request.model,
          choices = listOf(
            com.aallam.openai.api.chat.ChatChunk(
              index = 0,
              delta = com.aallam.openai.api.chat.ChatDelta(
                role = com.aallam.openai.api.chat.ChatRole.Assistant,
                content = if (it is ChatResponseChunk.ContentBlockDelta) {
                  it.delta.text
                } else {
                  null
                }
              ),
              finishReason = if (it is ChatResponseChunk.MessageDelta) {
                it.delta.stopReason?.openaiStopReason
              } else {
                null
              },
            )
          ),
          usage = if (it is ChatResponseChunk.MessageDelta) {
            Usage(
              promptTokens = startUsage?.inputTokens ?: 0,
              completionTokens = it.usage.outputTokens, // TODO: what is output token of startUsage? (event message_start)
              totalTokens = (startUsage?.inputTokens ?: 0) + it.usage.outputTokens
            )
          } else {
            null
          }
        )
      }
    }
  }

  override suspend fun model(modelId: ModelId, requestOptions: RequestOptions?): Model {
    val models = models(requestOptions)
    return models.find { it.id == modelId } ?: error("Model not found")
  }

  override suspend fun models(requestOptions: RequestOptions?): List<Model> {
    // https://docs.anthropic.com/en/docs/about-claude/models
    return listOf(
      Model(ModelId("claude-3-7-sonnet-20250219"), null, "anthropic"),
      Model(ModelId("claude-3-5-sonnet-20240620"), null, "anthropic"),
      Model(ModelId("claude-3-opus-20240229"), null, "anthropic"),
      Model(ModelId("claude-3-sonnet-20240229"), null, "anthropic"),
      Model(ModelId("claude-3-haiku-20240307"), null, "anthropic"),
      Model(ModelId("claude-2.1"), null, "anthropic"),
      Model(ModelId("claude-2.0"), null, "anthropic"),
      Model(ModelId("claude-instant-1.2"), null, "anthropic"),
    )
  }

}


