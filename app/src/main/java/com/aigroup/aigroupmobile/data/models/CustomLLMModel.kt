package com.aigroup.aigroupmobile.data.models

import androidx.annotation.DrawableRes
import kotlinx.serialization.Serializable

/**
 * Represents a custom LLM model configuration
 * This is not a Realm object but is serialized to JSON and stored in CustomLLMProvider
 */
@Serializable
data class CustomLLMModel(
    // Model ID/code used in API calls
    val id: String,
    
    // Display name for the model
    val name: String = id,
    
    // Context window size in tokens
    val contextSize: Int = 4096,
    
    // Whether the model supports image input
    val supportsImage: Boolean = false,
    
    // Whether the model supports video input
    val supportsVideo: Boolean = false,
    
    // Whether the model supports streaming responses
    val supportsStreaming: Boolean = true,
    
    // Icon resource ID for the model (optional)
    @DrawableRes
    val iconResId: Int? = null
)