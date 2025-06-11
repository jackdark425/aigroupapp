package com.aigroup.aigroupmobile.data

import com.aigroup.aigroupmobile.BuildConfig
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.CustomLLMModel
import com.aigroup.aigroupmobile.data.models.CustomLLMProvider
import com.aigroup.aigroupmobile.repositories.CustomLLMProviderRepository

/**
 * Default custom providers that should be available in the app
 */
object DefaultCustomProviders {
    
    /**
     * Initialize default custom providers if they don't exist
     */
    suspend fun initializeDefaultProviders(repository: CustomLLMProviderRepository) {
        // Create three separate providers, each with one model
        val providers = listOf(
            Triple(
                "custom_enterprise_insight_pro",
                "Enterprise Insight Pro",
                CustomLLMModel(
                    id = "Enterprise Insight Pro",
                    name = "Enterprise Insight Pro",
                    contextSize = 8192,
                    supportsImage = true,
                    supportsVideo = false,
                    supportsStreaming = true,
                    iconResId = R.drawable.ic_enterprise_insight_pro_icon
                )
            ),
            Triple(
                "custom_finwise_sprite",
                "FinWise Sprite",
                CustomLLMModel(
                    id = "FinWise Sprite",
                    name = "FinWise Sprite",
                    contextSize = 8192,
                    supportsImage = true,
                    supportsVideo = false,
                    supportsStreaming = true,
                    iconResId = R.drawable.ic_finwise_sprite_icon
                )
            ),
            Triple(
                "custom_profitpulse",
                "ProfitPulse",
                CustomLLMModel(
                    id = "ProfitPulse",
                    name = "ProfitPulse",
                    contextSize = 8192,
                    supportsImage = true,
                    supportsVideo = false,
                    supportsStreaming = true,
                    iconResId = R.drawable.ic_profitpulse_icon
                )
            )
        )
        
        // Create each provider separately
        providers.forEach { (providerId, providerName, model) ->
            val existingProvider = repository.getProviderByProviderId(providerId)
            
            if (existingProvider == null) {
                repository.createProvider(
                    name = providerName,
                    apiBaseUrl = BuildConfig.CUSTOM_PROVIDER_API_BASE_URL,
                    apiKey = BuildConfig.CUSTOM_PROVIDER_API_KEY,
                    requiresApiKey = true,
                    isEnabled = true,
                    models = listOf(model)
                )
            } else {
                // Update existing provider
                repository.updateProvider(
                    id = existingProvider.provider.id,
                    name = providerName
                )
                repository.updateModelsForProvider(providerId, listOf(model))
            }
        }
    }
}