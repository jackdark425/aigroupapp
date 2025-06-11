package com.aigroup.aigroupmobile.repositories

import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.connect.chat.CustomChatServiceProvider
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.connect.chat.ModelGroup
import com.aigroup.aigroupmobile.data.dao.CustomLLMProviderDao
import com.aigroup.aigroupmobile.data.models.CustomLLMModel
import com.aigroup.aigroupmobile.data.models.CustomLLMProvider
import com.aigroup.aigroupmobile.data.models.LoadingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomLLMProviderRepository @Inject constructor(
    private val customLLMProviderDao: CustomLLMProviderDao
) {
    /**
     * Get all custom providers as a Flow
     */
    fun getAllProvidersAsFlow(): Flow<List<CustomChatServiceProvider>> {
        return customLLMProviderDao.getAllProvidersAsFlow()
            .map { providers ->
                providers.map { CustomChatServiceProvider(it) }
            }
    }
    
    /**
     * Get all custom providers
     */
    suspend fun getAllProviders(): List<CustomChatServiceProvider> {
        return customLLMProviderDao.getAllProvidersAsFlow()
            .map { providers ->
                providers.map { CustomChatServiceProvider(it) }
            }.first()
    }
    
    /**
     * Get a custom provider by ID
     */
    suspend fun getProviderById(id: String): CustomChatServiceProvider? {
        val provider = customLLMProviderDao.getProviderById(id) ?: return null
        return CustomChatServiceProvider(provider)
    }
    
    /**
     * Get a custom provider by provider ID
     */
    suspend fun getProviderByProviderId(providerId: String): CustomChatServiceProvider? {
        val provider = customLLMProviderDao.getProviderByProviderId(providerId) ?: return null
        return CustomChatServiceProvider(provider)
    }
    
    /**
     * Create a new custom provider
     */
    suspend fun createProvider(
        name: String,
        apiBaseUrl: String,
        apiKey: String,
        requiresApiKey: Boolean = true,
        isEnabled: Boolean = true,
        models: List<CustomLLMModel> = emptyList()
    ): CustomChatServiceProvider {
        val providerId = CustomChatServiceProvider.generateProviderId(name)
        
        val provider = customLLMProviderDao.createProvider(
            name = name,
            apiBaseUrl = apiBaseUrl,
            apiKey = apiKey,
            providerId = providerId,
            requiresApiKey = requiresApiKey,
            isEnabled = isEnabled,
            models = models
        )
        
        return CustomChatServiceProvider(provider)
    }
    
    /**
     * Update an existing custom provider
     */
    suspend fun updateProvider(
        id: String,
        name: String? = null,
        apiBaseUrl: String? = null,
        apiKey: String? = null,
        isEnabled: Boolean? = null,
        requiresApiKey: Boolean? = null,
        models: List<CustomLLMModel>? = null
    ): Boolean {
        return customLLMProviderDao.updateProvider(
            id = id,
            name = name,
            apiBaseUrl = apiBaseUrl,
            apiKey = apiKey,
            isEnabled = isEnabled,
            requiresApiKey = requiresApiKey,
            models = models
        )
    }
    
    /**
     * Delete a custom provider
     */
    suspend fun deleteProvider(id: String): Boolean {
        return customLLMProviderDao.deleteProvider(id)
    }
    
    /**
     * Get models for a provider
     */
    suspend fun getModelsForProvider(providerId: String): List<CustomLLMModel> {
        return customLLMProviderDao.getModelsForProvider(providerId)
    }
    
    /**
     * Update models for a provider
     */
    suspend fun updateModelsForProvider(providerId: String, models: List<CustomLLMModel>): Boolean {
        return customLLMProviderDao.updateModelsForProvider(providerId, models)
    }
    
    /**
     * Add a model to a provider
     */
    suspend fun addModelToProvider(providerId: String, model: CustomLLMModel): Boolean {
        return customLLMProviderDao.addModelToProvider(providerId, model)
    }
    
    /**
     * Remove a model from a provider
     */
    suspend fun removeModelFromProvider(providerId: String, modelId: String): Boolean {
        return customLLMProviderDao.removeModelFromProvider(providerId, modelId)
    }
    
    /**
     * Get all models for all custom providers
     */
    suspend fun getAllModels(): Map<CustomChatServiceProvider, List<ModelCode>> {
        val result = mutableMapOf<CustomChatServiceProvider, List<ModelCode>>()
        
        customLLMProviderDao.getAllProvidersAsFlow().collect { providers ->
            for (provider in providers) {
                if (!provider.isEnabled) continue
                
                val customProvider = CustomChatServiceProvider(provider)
                val models = customLLMProviderDao.getModelsForProvider(provider.providerId)
                
                val modelCodes = models.map { model ->
                    ModelCode(model.id, ChatServiceProvider.valueOf(customProvider.id))
                }
                
                result[customProvider] = modelCodes
            }
        }
        
        return result
    }
    
    /**
     * Get models for a specific provider as a LoadingStatus
     */
    suspend fun getModelsAsLoadingStatus(providerId: String): LoadingStatus<Map<ModelGroup, List<ModelCode>>> {
        try {
            val provider = customLLMProviderDao.getProviderByProviderId(providerId) ?: 
                return LoadingStatus.Error("Provider not found")
            
            if (!provider.isEnabled) {
                return LoadingStatus.Error("Provider is disabled")
            }
            
            val customProvider = CustomChatServiceProvider(provider)
            val models = customLLMProviderDao.getModelsForProvider(provider.providerId)
            
            if (models.isEmpty()) {
                return LoadingStatus.Success(emptyMap())
            }

            val modelCodes = models.map { model ->
                ModelCode(model.id, customProvider)
            }
            
            // Group models by their model group
            val groupedModels = modelCodes.groupBy { it.modelGroup }
            
            return LoadingStatus.Success(groupedModels)
        } catch (e: Exception) {
            return LoadingStatus.Error("Failed to load models: ${e.message}")
        }
    }
} 