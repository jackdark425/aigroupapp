package com.aigroup.aigroupmobile.data.models

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.RealmUUID
import io.realm.kotlin.types.annotations.PrimaryKey

/**
 * Represents a custom LLM provider configuration stored in Realm
 */
class CustomLLMProvider : RealmObject {
    @PrimaryKey
    var id: String = RealmUUID.random().toString()
    
    // Display name for the provider
    var name: String = ""
    
    // API base URL
    var apiBaseUrl: String = ""
    
    // API key
    var apiKey: String = ""
    
    // Provider ID used in the app
    var providerId: String = ""
    
    // Whether the provider is enabled
    var isEnabled: Boolean = true
    
    // Whether the provider requires API key
    var requiresApiKey: Boolean = true
    
    // Models supported by this provider
    var models: String = "" // Stored as JSON string
    
    // Creation timestamp
    var createdAt: Long = System.currentTimeMillis()
    
    // Last updated timestamp
    var updatedAt: Long = System.currentTimeMillis()
} 