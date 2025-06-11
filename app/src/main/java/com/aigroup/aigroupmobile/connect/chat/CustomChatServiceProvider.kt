package com.aigroup.aigroupmobile.connect.chat

import android.content.Context
import android.os.Parcel
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aallam.openai.client.OpenAIConfig
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.CustomLLMProvider
import com.aigroup.aigroupmobile.repositories.CustomLLMProviderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Represents a custom chat service provider
 */
@Parcelize
class CustomChatServiceProvider(
  val provider: @RawValue CustomLLMProvider
) : Comparable<CustomChatServiceProvider>, ServiceProvider {

  override val id: String
    get() = provider.providerId

  override val apiBase: String
    get() = provider.apiBaseUrl

  override val description: String
    @Composable
    get() = "Custom provider for ${provider.name}"

  override val displayName: String
    get() = provider.name

  fun displayName(context: Context): String {
    return provider.name
  }

  @get:DrawableRes
  override val logoIconId: Int
    get() = when (provider.providerId) {
      "custom_enterprise_insight_pro" -> R.drawable.ic_enterprise_insight_pro_icon
      "custom_finwise_sprite" -> R.drawable.ic_finwise_sprite_icon
      "custom_profitpulse" -> R.drawable.ic_profitpulse_icon
      else -> R.drawable.ic_custom_bot_icon
    }

  override val backColor: Color?
    get() = null

  override val isEnabled: Boolean
    get() = provider.isEnabled

  val requiresApiKey: Boolean
    get() = provider.requiresApiKey

  val apiKey: String
    get() = provider.apiKey

  fun createChatClient(config: OpenAIConfig): ChatEndpoint? {
    // Custom providers use the standard OpenAI API format
    return null
  }

  override fun compareTo(other: CustomChatServiceProvider): Int {
    return displayName.compareTo(other.displayName)
  }

  companion object : Parceler<CustomChatServiceProvider> {
    override fun create(parcel: Parcel): CustomChatServiceProvider {
      // Since CustomLLMProvider is not Parcelable, we need to manually recreate it
      // This is a placeholder implementation that returns null
      // In a real application, you would need to implement a proper deserialization
      // from the parcel data
      return CustomChatServiceProvider(CustomLLMProvider().apply {
        id = parcel.readString() ?: ""
        name = parcel.readString() ?: ""
        apiBaseUrl = parcel.readString() ?: ""
        apiKey = parcel.readString() ?: ""
        providerId = parcel.readString() ?: ""
        isEnabled = parcel.readInt() == 1
        requiresApiKey = parcel.readInt() == 1
        models = parcel.readString() ?: ""
        createdAt = parcel.readLong()
        updatedAt = parcel.readLong()
      })
    }

    override fun CustomChatServiceProvider.write(parcel: Parcel, flags: Int) {
      // Write all the necessary fields from the CustomLLMProvider
      parcel.writeString(provider.id)
      parcel.writeString(provider.name)
      parcel.writeString(provider.apiBaseUrl)
      parcel.writeString(provider.apiKey)
      parcel.writeString(provider.providerId)
      parcel.writeInt(if (provider.isEnabled) 1 else 0)
      parcel.writeInt(if (provider.requiresApiKey) 1 else 0)
      parcel.writeString(provider.models)
      parcel.writeLong(provider.createdAt)
      parcel.writeLong(provider.updatedAt)
    }

    // Prefix for custom provider IDs to avoid conflicts with built-in providers
    const val CUSTOM_PROVIDER_PREFIX = "custom_"

    /**
     * Repository provider for accessing the CustomLLMProviderRepository
     */
    private var repositoryProvider: (() -> CustomLLMProviderRepository)? = null

    /**
     * Set the repository provider for accessing the CustomLLMProviderRepository
     * This should be called during application initialization
     */
    fun setRepositoryProvider(provider: () -> CustomLLMProviderRepository) {
      repositoryProvider = provider
    }

    /**
     * Generate a provider ID for a new custom provider
     */
    fun generateProviderId(name: String): String {
      val sanitizedName = name.lowercase().replace(Regex("[^a-z0-9_]"), "_")
      return "$CUSTOM_PROVIDER_PREFIX$sanitizedName"
    }

    /**
     * Check if a provider ID belongs to a custom provider
     */
    fun isCustomProviderId(providerId: String): Boolean {
      return providerId.startsWith(CUSTOM_PROVIDER_PREFIX)
    }

    /**
     * Get a custom provider by ID
     *
     * @param providerId The ID of the provider to get
     * @return The provider, or null if not found
     */
    fun getProviderById(providerId: String): CustomChatServiceProvider? {
      val repository = repositoryProvider?.invoke() ?: return null
      
      // Since this is a static method, we need to use runBlocking to call the suspend function
      return runBlocking(Dispatchers.IO) {
        repository.getProviderByProviderId(providerId)
      }
    }
  }
} 