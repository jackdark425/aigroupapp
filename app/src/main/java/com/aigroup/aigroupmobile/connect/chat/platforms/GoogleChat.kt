package com.aigroup.aigroupmobile.connect.chat.platforms

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ListContent
import com.aallam.openai.api.chat.TextContent
import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.core.Usage
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAIConfig
import com.aigroup.aigroupmobile.connect.chat.ChatEndpoint
import com.aigroup.aigroupmobile.connect.chat.platforms.AnthropicChat.ChatResponseChunk
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.CodeExecutionResultPart
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.ExecutableCodePart
import com.google.ai.client.generativeai.type.FinishReason
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.ImagePart
import com.google.ai.client.generativeai.type.TextPart
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import java.util.UUID

class GoogleChat(private val config: OpenAIConfig) : ChatEndpoint {
  private val ChatMessage.googleContent: Content
    get() {
      val roleString = when (role) {
        com.aallam.openai.api.chat.ChatRole.User -> "user"
        com.aallam.openai.api.chat.ChatRole.Assistant -> "model"
        else -> { error("Unsupported role ${role.role}") }
      }
      return content {
        role = roleString
        when (val input = this@googleContent.messageContent!!) {
          is ListContent -> {
            input.content.forEach { contentPart ->
              when (contentPart) {
                is com.aallam.openai.api.chat.TextPart -> { text(contentPart.text) }
                is com.aallam.openai.api.chat.ImagePart -> {
                  val base64Full = contentPart.imageUrl.url
                  val split = ";base64,"
                  val (header, data) = base64Full.split(split)
                  val mime = header.removePrefix("data:")

                  blob(mime, data.encodeToByteArray())
                }
                is com.aallam.openai.api.chat.VideoPart -> {
                  // TODO: support this
                  error("Unsupported video part")
                }
              }
            }
          }
          is TextContent -> {
            text(input.content)
          }
        }
      }
    }

  private val FinishReason.openaiFinishReason: com.aallam.openai.api.core.FinishReason?
    get() {
      return when (this) {
        FinishReason.UNKNOWN -> com.aallam.openai.api.core.FinishReason.Stop
        FinishReason.UNSPECIFIED -> null
        FinishReason.STOP -> com.aallam.openai.api.core.FinishReason.Stop
        FinishReason.MAX_TOKENS -> com.aallam.openai.api.core.FinishReason.Length
        FinishReason.SAFETY -> com.aallam.openai.api.core.FinishReason.ContentFilter
        // TODO: dive into this
        FinishReason.RECITATION -> com.aallam.openai.api.core.FinishReason.Stop
        FinishReason.OTHER -> com.aallam.openai.api.core.FinishReason.Stop
      }
    }

  override suspend fun chatCompletion(request: ChatCompletionRequest, requestOptions: RequestOptions?): ChatCompletion {
    val model = request.model.id
    val token = config.token

    val systemMessages = request.messages.takeWhile { it.role == com.aallam.openai.api.chat.ChatRole.System }
    val system = systemMessages.map { it.content }.joinToString("\n")
    val restMessages = request.messages.drop(systemMessages.size)

    val history = restMessages.dropLast(1)
    val lastMessage = restMessages.last()

    val generativeModel = GenerativeModel(
      modelName = model,
      apiKey = token,
      systemInstruction = content { text(system) },
      generationConfig = generationConfig {
        temperature = request.temperature?.toFloat()
        topP = request.topP?.toFloat()
        maxOutputTokens = request.maxTokens
        stopSequences = request.stop
        // TODO: topK and SafetySetting
      },
      requestOptions = com.google.ai.client.generativeai.type.RequestOptions(
        timeout = (requestOptions?.timeout?.request ?: requestOptions?.timeout?.socket)?.inWholeMilliseconds
      )
    )

    val response = generativeModel
      .startChat(history.map { it.googleContent })
      .sendMessage(lastMessage.googleContent)

    val id = "chatcmpl-${UUID.randomUUID()}"
    return ChatCompletion(
      id = id,
      created = Clock.System.now().toEpochMilliseconds(),
      model = request.model,
      choices = response.candidates.map {
        com.aallam.openai.api.chat.ChatChoice(
          index = 0,
          message = com.aallam.openai.api.chat.ChatMessage(
            role = com.aallam.openai.api.chat.ChatRole.Assistant,
            content = it.content.parts.filter { it is TextPart || it is ExecutableCodePart || it is CodeExecutionResultPart }
              .joinToString(" ") {
                when (it) {
                  is TextPart -> it.text
                  is ExecutableCodePart -> "\n```${it.language.lowercase()}\n${it.code}\n```"
                  is CodeExecutionResultPart -> "\n```\n${it.output}\n```"
                  else -> throw RuntimeException("unreachable")
                }
              }
          ),
          finishReason = it.finishReason?.openaiFinishReason
        )
      },
      usage = Usage(
        promptTokens = response.usageMetadata?.promptTokenCount,
        completionTokens = response.usageMetadata?.candidatesTokenCount,
        totalTokens = response.usageMetadata?.totalTokenCount,
      ),
    )
  }

  override fun chatCompletions(
    request: ChatCompletionRequest,
    requestOptions: RequestOptions?
  ): Flow<ChatCompletionChunk> {
    val model = request.model.id
    val token = config.token

    val systemMessages = request.messages.takeWhile { it.role == com.aallam.openai.api.chat.ChatRole.System }
    val system = systemMessages.map { it.content }.joinToString("\n")
    val restMessages = request.messages.drop(systemMessages.size)

    require(restMessages.isNotEmpty()) { "At least one message is required" }

    val history = restMessages.dropLast(1)
    val lastMessage = restMessages.last()

    val generativeModel = GenerativeModel(
      modelName = model,
      apiKey = token,
      systemInstruction = content { text(system) },
      generationConfig = generationConfig {
        temperature = request.temperature?.toFloat()
        topP = request.topP?.toFloat()
        maxOutputTokens = request.maxTokens
        stopSequences = request.stop
        // TODO: topK and SafetySetting
      },
      requestOptions = com.google.ai.client.generativeai.type.RequestOptions(
        timeout = (requestOptions?.timeout?.request ?: requestOptions?.timeout?.socket)?.inWholeMilliseconds
      )
    )

    val response = generativeModel
      .startChat(history.map { it.googleContent })
      .sendMessageStream(lastMessage.googleContent)

    // TODO: its take time?
//    val tokens = generativeModel.countTokens(*request.messages.map { it.googleContent }.toTypedArray())
//    val completionToken = generativeModel.countTokens(*response.candidates.map { it.content }

    val id = "chatcmpl-${UUID.randomUUID()}"

    return response.map {
      ChatCompletionChunk(
        id = id,
        created = Clock.System.now().toEpochMilliseconds(),
        model = request.model,
        choices = it.candidates.map {
          com.aallam.openai.api.chat.ChatChunk(
            index = 0,
            delta = com.aallam.openai.api.chat.ChatDelta(
              role = com.aallam.openai.api.chat.ChatRole.Assistant,
              content = it.content.parts.filter { it is TextPart || it is ExecutableCodePart || it is CodeExecutionResultPart }
                .joinToString(" ") {
                  when (it) {
                    is TextPart -> it.text
                    is ExecutableCodePart -> "\n```${it.language.lowercase()}\n${it.code}\n```"
                    is CodeExecutionResultPart -> "\n```\n${it.output}\n```"
                    else -> throw RuntimeException("unreachable")
                  }
                }
            ),
            finishReason = it.finishReason?.openaiFinishReason,
          )
        },
        usage = Usage(
          promptTokens = it.usageMetadata?.promptTokenCount,
          completionTokens = it.usageMetadata?.candidatesTokenCount,
          totalTokens = it.usageMetadata?.totalTokenCount,
        )
      )
    }
  }

  override suspend fun model(modelId: ModelId, requestOptions: RequestOptions?): Model {
    val models = models(requestOptions)
    return models.find { it.id == modelId } ?: error("Model not found")
  }

  override suspend fun models(requestOptions: RequestOptions?): List<Model> {
    // TODO: using this https://ai.google.dev/api/models#models_get-SHELL
    return listOf(
      Model(ModelId("gemini-1.5-flash"), null, "google"),
      Model(ModelId("gemini-1.5-flash-8b"), null, "google"),
      Model(ModelId("gemini-1.5-pro"), null, "google"),
      Model(ModelId("gemini-1.0-pro"), null, "google"),
      Model(ModelId("gemini-2.0-flash-exp"), null, "google"),
      Model(ModelId("gemini-2.0-flash-thinking-exp-1219"), null, "google"),

      // gemma
      Model(ModelId("gemma-2-2b-it"), null, "google"),
      Model(ModelId("gemma-2-9b-it"), null, "google"),
      Model(ModelId("gemma-2-27b-it"), null, "google"),
    )
  }
}