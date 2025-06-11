package com.aigroup.aigroupmobile.connect.chat.platforms

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aigroup.aigroupmobile.connect.chat.ChatEndpoint
import kotlinx.coroutines.flow.Flow

/**
 * OpenRouter API endpoint implementation.
 * OpenRouter requires additional headers for API calls.
 */
class OpenRouterChatEndpoint(private val config: OpenAIConfig) : ChatEndpoint {
    private val openAI = OpenAI(config)
    private val chatEndpoint = OpenAIChatEndpoint(openAI)
    
    /**
     * Add OpenRouter-specific headers to the request options.
     */
    private fun enhanceRequestOptions(requestOptions: RequestOptions?): RequestOptions {
        val additionalHeaders = mapOf(
            "HTTP-Referer" to "com.aigroup.aigroupmobile",
            "X-Title" to "AIGroupApp"
        )
        
        return requestOptions?.let { options ->
            RequestOptions(
                headers = options.headers + additionalHeaders,
                urlParameters = options.urlParameters,
                timeout = options.timeout
            )
        } ?: RequestOptions(
            headers = additionalHeaders
        )
    }
    
    override suspend fun chatCompletion(
        request: ChatCompletionRequest, 
        requestOptions: RequestOptions?
    ): ChatCompletion {
        val enhancedOptions = enhanceRequestOptions(requestOptions)
        return chatEndpoint.chatCompletion(request, enhancedOptions)
    }
    
    override fun chatCompletions(
        request: ChatCompletionRequest, 
        requestOptions: RequestOptions?
    ): Flow<ChatCompletionChunk> {
        val enhancedOptions = enhanceRequestOptions(requestOptions)
        return chatEndpoint.chatCompletions(request, enhancedOptions)
    }
    
    override suspend fun model(modelId: ModelId, requestOptions: RequestOptions?): Model {
        val enhancedOptions = enhanceRequestOptions(requestOptions)
        return chatEndpoint.model(modelId, enhancedOptions)
    }
    
    override suspend fun models(requestOptions: RequestOptions?): List<Model> {
        val enhancedOptions = enhanceRequestOptions(requestOptions)
        return chatEndpoint.models(enhancedOptions)
    }
}
