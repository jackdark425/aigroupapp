package com.aigroup.aigroupmobile.data.dao

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.aigroup.aigroupmobile.data.RealmLiveData
import com.aigroup.aigroupmobile.data.models.CustomLLMModel
import com.aigroup.aigroupmobile.data.models.CustomLLMProvider
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomLLMProviderDao @Inject constructor(
    private val realm: Realm
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Get all custom LLM providers as a Flow
     */
    fun getAllProvidersAsFlow(): Flow<List<CustomLLMProvider>> {
        return realm.query<CustomLLMProvider>()
            .sort("name", Sort.ASCENDING)
            .asFlow()
            .map { (it as ResultsChange<CustomLLMProvider>).list }
    }

    /**
     * Get all custom LLM providers as a LiveData
     */
    fun getAllProvidersAsLiveData(): LiveData<List<CustomLLMProvider>> {
        return getAllProvidersAsFlow().asLiveData()
    }

    /**
     * Get a custom LLM provider by ID
     */
    suspend fun getProviderById(id: String): CustomLLMProvider? {
        return realm.query<CustomLLMProvider>("id == $0", id)
            .first()
            .find()
    }

    /**
     * Get a custom LLM provider by provider ID
     */
    suspend fun getProviderByProviderId(providerId: String): CustomLLMProvider? {
        Log.d("CustomLLMProviderDao", "Getting provider by providerId: $providerId")
        val provider = realm.query<CustomLLMProvider>("providerId == $0", providerId)
            .first()
            .find()
        Log.d("CustomLLMProviderDao", "Provider found: ${provider != null}")
        return provider
    }

    /**
     * Create a new custom LLM provider
     */
    suspend fun createProvider(
        name: String,
        apiBaseUrl: String,
        apiKey: String,
        providerId: String,
        requiresApiKey: Boolean = true,
        isEnabled: Boolean = true,
        models: List<CustomLLMModel> = emptyList()
    ): CustomLLMProvider {
        val modelsJson = json.encodeToString(models)

        val provider = CustomLLMProvider().apply {
            this.name = name
            this.apiBaseUrl = apiBaseUrl
            this.apiKey = apiKey
            this.providerId = providerId
            this.requiresApiKey = requiresApiKey
            this.isEnabled = isEnabled
            this.models = modelsJson
            this.createdAt = System.currentTimeMillis()
            this.updatedAt = System.currentTimeMillis()
        }

        realm.write {
            copyToRealm(provider)
        }

        return provider
    }

    /**
     * Update an existing custom LLM provider
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
        val provider = realm.query<CustomLLMProvider>("id == $0", id)
            .first()
            .find() ?: return false

        realm.write {
            findLatest(provider)?.let { latestProvider ->
                name?.let { latestProvider.name = it }
                apiBaseUrl?.let { latestProvider.apiBaseUrl = it }
                apiKey?.let { latestProvider.apiKey = it }
                isEnabled?.let { latestProvider.isEnabled = it }
                requiresApiKey?.let { latestProvider.requiresApiKey = it }
                models?.let { latestProvider.models = json.encodeToString(it) }
                latestProvider.updatedAt = System.currentTimeMillis()
            }
        }

        return true
    }

    /**
     * Delete a custom LLM provider
     */
    suspend fun deleteProvider(id: String): Boolean {
        val provider = realm.query<CustomLLMProvider>("id == $0", id)
            .first()
            .find() ?: return false

        realm.write {
            findLatest(provider)?.let { delete(it) }
        }

        return true
    }

    /**
     * Get models for a provider
     */
    suspend fun getModelsForProvider(providerId: String): List<CustomLLMModel> {
        Log.d("CustomLLMProviderDao", "Getting models for providerId: $providerId")
        val provider = getProviderByProviderId(providerId) ?: return emptyList()
        Log.d("CustomLLMProviderDao", "Provider found, models JSON length: ${provider.models.length}")

        return if (provider.models.isNotEmpty()) {
            try {
                val models = json.decodeFromString<List<CustomLLMModel>>(provider.models)
                Log.d("CustomLLMProviderDao", "Models decoded successfully, count: ${models.size}")
                models
            } catch (e: Exception) {
                Log.e("CustomLLMProviderDao", "Error decoding models: ${e.message}")
                emptyList()
            }
        } else {
            Log.d("CustomLLMProviderDao", "Provider has no models")
            emptyList()
        }
    }

    /**
     * Update models for a provider
     */
    suspend fun updateModelsForProvider(providerId: String, models: List<CustomLLMModel>): Boolean {
        val provider = getProviderByProviderId(providerId) ?: return false

        return updateProvider(
            id = provider.id,
            models = models
        )
    }

    /**
     * Add a model to a provider
     */
    suspend fun addModelToProvider(providerId: String, model: CustomLLMModel): Boolean {
        val provider = getProviderByProviderId(providerId) ?: return false
        val currentModels = if (provider.models.isNotEmpty()) {
            try {
                json.decodeFromString<List<CustomLLMModel>>(provider.models)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val updatedModels = currentModels.toMutableList()
        // Replace if model with same ID exists
        val existingIndex = updatedModels.indexOfFirst { it.id == model.id }
        if (existingIndex >= 0) {
            updatedModels[existingIndex] = model
        } else {
            updatedModels.add(model)
        }

        return updateProvider(
            id = provider.id,
            models = updatedModels
        )
    }

    /**
     * Remove a model from a provider
     */
    suspend fun removeModelFromProvider(providerId: String, modelId: String): Boolean {
        val provider = getProviderByProviderId(providerId) ?: return false
        val currentModels = if (provider.models.isNotEmpty()) {
            try {
                json.decodeFromString<List<CustomLLMModel>>(provider.models)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val updatedModels = currentModels.filter { it.id != modelId }

        return updateProvider(
            id = provider.id,
            models = updatedModels
        )
    }
} 