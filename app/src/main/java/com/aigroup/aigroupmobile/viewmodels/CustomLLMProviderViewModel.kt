package com.aigroup.aigroupmobile.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigroup.aigroupmobile.connect.chat.CustomChatServiceProvider
import com.aigroup.aigroupmobile.data.models.CustomLLMModel
import com.aigroup.aigroupmobile.data.models.LoadingStatus
import com.aigroup.aigroupmobile.repositories.CustomLLMProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomLLMProviderViewModel @Inject constructor(
    private val customLLMProviderRepository: CustomLLMProviderRepository
) : ViewModel() {
    
    // State for providers list
    private val _providers = MutableStateFlow<LoadingStatus<List<CustomChatServiceProvider>>>(LoadingStatus.Loading)
    val providers: StateFlow<LoadingStatus<List<CustomChatServiceProvider>>> = _providers.asStateFlow()
    
    // State for current provider being edited
    private val _currentProvider = MutableLiveData<CustomChatServiceProvider?>()
    val currentProvider: LiveData<CustomChatServiceProvider?> = _currentProvider
    
    // State for models of current provider
    private val _currentProviderModels = MutableStateFlow<LoadingStatus<List<CustomLLMModel>>>(LoadingStatus.Loading)
    val currentProviderModels: StateFlow<LoadingStatus<List<CustomLLMModel>>> = _currentProviderModels.asStateFlow()
    
    // State for operation result
    private val _operationResult = MutableStateFlow<OperationResult?>(null)
    val operationResult: StateFlow<OperationResult?> = _operationResult.asStateFlow()
    
    init {
        loadProviders()
    }
    
    /**
     * Load all providers
     */
    fun loadProviders() {
        viewModelScope.launch {
            _providers.value = LoadingStatus.Loading
            try {
                customLLMProviderRepository.getAllProvidersAsFlow().collect { providers ->
                    _providers.value = LoadingStatus.Success(providers)
                }
            } catch (e: Exception) {
                _providers.value = LoadingStatus.Error("Failed to load providers: ${e.message}")
            }
        }
    }
    
    /**
     * Get a provider by ID
     */
    suspend fun getProviderById(id: String): CustomChatServiceProvider? {
        return customLLMProviderRepository.getProviderById(id)
    }
    
    /**
     * Get a provider by provider ID
     */
    suspend fun getProviderByProviderId(providerId: String): CustomChatServiceProvider? {
        return customLLMProviderRepository.getProviderByProviderId(providerId)
    }
    
    /**
     * Get models for a provider by ID
     */
    suspend fun getModelsForProvider(providerId: String): List<CustomLLMModel> {
        return customLLMProviderRepository.getModelsForProvider(providerId)
    }
    
    /**
     * Set current provider for editing
     */
    fun setCurrentProvider(provider: CustomChatServiceProvider?) {
        _currentProvider.value = provider
        if (provider != null) {
            loadModelsForCurrentProvider()
        } else {
            _currentProviderModels.value = LoadingStatus.Success(emptyList())
        }
    }
    
    /**
     * Load models for current provider
     */
    private fun loadModelsForCurrentProvider() {
        val provider = _currentProvider.value ?: return
        
        viewModelScope.launch {
            _currentProviderModels.value = LoadingStatus.Loading
            try {
                val models = customLLMProviderRepository.getModelsForProvider(provider.provider.providerId)
                _currentProviderModels.value = LoadingStatus.Success(models)
            } catch (e: Exception) {
                _currentProviderModels.value = LoadingStatus.Error("Failed to load models: ${e.message}")
            }
        }
    }
    
    /**
     * Create a new provider
     */
    fun createProvider(
        name: String,
        apiBaseUrl: String,
        apiKey: String,
        requiresApiKey: Boolean = true,
        isEnabled: Boolean = true
    ) {
        if (name.isBlank()) {
            _operationResult.value = OperationResult.Error("Name cannot be empty")
            return
        }
        
        if (apiBaseUrl.isBlank()) {
            _operationResult.value = OperationResult.Error("API base URL cannot be empty")
            return
        }
        
        if (requiresApiKey && apiKey.isBlank()) {
            _operationResult.value = OperationResult.Error("API key cannot be empty")
            return
        }
        
        viewModelScope.launch {
            try {
                val provider = customLLMProviderRepository.createProvider(
                    name = name,
                    apiBaseUrl = apiBaseUrl,
                    apiKey = apiKey,
                    requiresApiKey = requiresApiKey,
                    isEnabled = isEnabled
                )
                _operationResult.value = OperationResult.Success("Provider created successfully")
                loadProviders()
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("Failed to create provider: ${e.message}")
            }
        }
    }
    
    /**
     * Update an existing provider
     */
    fun updateProvider(
        id: String,
        name: String? = null,
        apiBaseUrl: String? = null,
        apiKey: String? = null,
        isEnabled: Boolean? = null,
        requiresApiKey: Boolean? = null
    ) {
        viewModelScope.launch {
            try {
                val success = customLLMProviderRepository.updateProvider(
                    id = id,
                    name = name,
                    apiBaseUrl = apiBaseUrl,
                    apiKey = apiKey,
                    isEnabled = isEnabled,
                    requiresApiKey = requiresApiKey
                )
                
                if (success) {
                    _operationResult.value = OperationResult.Success("Provider updated successfully")
                    loadProviders()
                } else {
                    _operationResult.value = OperationResult.Error("Failed to update provider")
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("Failed to update provider: ${e.message}")
            }
        }
    }
    
    /**
     * Delete a provider
     */
    fun deleteProvider(id: String) {
        viewModelScope.launch {
            try {
                val success = customLLMProviderRepository.deleteProvider(id)
                
                if (success) {
                    _operationResult.value = OperationResult.Success("Provider deleted successfully")
                    loadProviders()
                    if (_currentProvider.value?.provider?.id == id) {
                        _currentProvider.value = null
                    }
                } else {
                    _operationResult.value = OperationResult.Error("Failed to delete provider")
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("Failed to delete provider: ${e.message}")
            }
        }
    }
    
    /**
     * Add a model to the current provider
     */
    fun addModelToCurrentProvider(
        modelId: String,
        modelName: String = modelId,
        contextSize: Int = 4096,
        supportsImage: Boolean = false,
        supportsVideo: Boolean = false,
        supportsStreaming: Boolean = true
    ) {
        val provider = _currentProvider.value ?: return
        
        if (modelId.isBlank()) {
            _operationResult.value = OperationResult.Error("Model ID cannot be empty")
            return
        }
        
        val model = CustomLLMModel(
            id = modelId,
            name = modelName.ifBlank { modelId },
            contextSize = contextSize,
            supportsImage = supportsImage,
            supportsVideo = supportsVideo,
            supportsStreaming = supportsStreaming
        )
        
        viewModelScope.launch {
            try {
                val success = customLLMProviderRepository.addModelToProvider(provider.provider.providerId, model)
                
                if (success) {
                    _operationResult.value = OperationResult.Success("Model added successfully")
                    loadModelsForCurrentProvider()
                } else {
                    _operationResult.value = OperationResult.Error("Failed to add model")
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("Failed to add model: ${e.message}")
            }
        }
    }
    
    /**
     * Remove a model from the current provider
     */
    fun removeModelFromCurrentProvider(modelId: String) {
        val provider = _currentProvider.value ?: return
        
        viewModelScope.launch {
            try {
                val success = customLLMProviderRepository.removeModelFromProvider(provider.provider.providerId, modelId)
                
                if (success) {
                    _operationResult.value = OperationResult.Success("Model removed successfully")
                    loadModelsForCurrentProvider()
                } else {
                    _operationResult.value = OperationResult.Error("Failed to remove model")
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("Failed to remove model: ${e.message}")
            }
        }
    }
    
    /**
     * Clear operation result
     */
    fun clearOperationResult() {
        _operationResult.value = null
    }
    
    /**
     * Operation result sealed class
     */
    sealed class OperationResult {
        data class Success(val message: String) : OperationResult()
        data class Error(val message: String) : OperationResult()
    }
} 