package com.aigroup.aigroupmobile.data.utils

import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.connect.chat.CustomChatServiceProvider
import com.aigroup.aigroupmobile.connect.chat.ServiceProvider

// custom exception for token not set
class TokenNotSetException(provider: ServiceProvider) : IllegalStateException("Token not set for $provider")

/**
 * @throws TokenNotSetException if token is not set
 */
fun AppPreferences.Token.getToken(provider: ServiceProvider): String {
  return when (provider) {
    is ChatServiceProvider -> getTokenForChatServiceProvider(provider)
    is CustomChatServiceProvider -> provider.apiKey
    else -> throw IllegalArgumentException("Unsupported service provider type: ${provider::class.java.name}")
  }
}

/**
 * @throws TokenNotSetException if token is not set
 */
private fun AppPreferences.Token.getTokenForChatServiceProvider(provider: ChatServiceProvider): String {
  val serviceCode = when (provider) {
    ChatServiceProvider.OFFICIAL -> "generic"
    else -> provider.id
  }
  val getterName = serviceCode.replaceFirstChar(Char::uppercase)
  val getterMap = this::class.members.associateBy { it.name }

  val hasSet = getterMap["has$getterName"]?.call(this) as? Boolean
  if (hasSet == true) {
    val result = getterMap["get$getterName"]?.call(this)
    if (result is String) {
      return result
    }
  }

  return "" // TODO: throw TokenNotSetException(provider)
//  throw TokenNotSetException(provider)
}

fun AppPreferences.Token.hasSetToken(provider: ServiceProvider): Boolean {
  return when (provider) {
    is ChatServiceProvider -> hasSetTokenForChatServiceProvider(provider)
    is CustomChatServiceProvider -> provider.apiKey.isNotEmpty()
    else -> throw IllegalArgumentException("Unsupported service provider type: ${provider::class.java.name}")
  }
}

private fun AppPreferences.Token.hasSetTokenForChatServiceProvider(provider: ChatServiceProvider): Boolean {
  val serviceCode = when (provider) {
    ChatServiceProvider.OFFICIAL -> "generic"
    else -> provider.id
  }
  val getterName = serviceCode.replaceFirstChar(Char::uppercase)
  val getterMap = this::class.members.associateBy { it.name }

  return getterMap["has$getterName"]?.call(this) as? Boolean == true
}

fun AppPreferences.Token.Builder.clearToken(provider: ServiceProvider): AppPreferences.Token.Builder {
  return when (provider) {
    is ChatServiceProvider -> clearTokenForChatServiceProvider(provider)
    is CustomChatServiceProvider -> this // Custom providers store their token in the provider itself, no need to clear from preferences
    else -> throw IllegalArgumentException("Unsupported service provider type: ${provider::class.java.name}")
  }
}

private fun AppPreferences.Token.Builder.clearTokenForChatServiceProvider(provider: ChatServiceProvider): AppPreferences.Token.Builder {
  val serviceCode = when (provider) {
    ChatServiceProvider.OFFICIAL -> "generic"
    else -> provider.id
  }
  val setterName = serviceCode.replaceFirstChar(Char::uppercase)
  val setterMap = this::class.members.associateBy { it.name }

  setterMap["clear$setterName"]?.call(this)

  return this
}

fun AppPreferences.Token.Builder.setToken(provider: ServiceProvider, token: String): AppPreferences.Token.Builder {
  return when (provider) {
    is ChatServiceProvider -> setTokenForChatServiceProvider(provider, token)
    is CustomChatServiceProvider -> this // Custom providers store their token in the provider itself, no need to set in preferences
    else -> throw IllegalArgumentException("Unsupported service provider type: ${provider::class.java.name}")
  }
}

private fun AppPreferences.Token.Builder.setTokenForChatServiceProvider(provider: ChatServiceProvider, token: String): AppPreferences.Token.Builder {
  val serviceCode = when (provider) {
    ChatServiceProvider.OFFICIAL -> "generic"
    else -> provider.id
  }
  val setterName = serviceCode.replaceFirstChar(Char::uppercase)
  val setterMap = this::class.members.associateBy { it.name }

  setterMap["set$setterName"]?.call(this, token)

  return this
}

fun AppPreferences.Token.Builder.setToken(key: String, token: String): AppPreferences.Token.Builder {
  val serviceCode = when (key) {
    ChatServiceProvider.OFFICIAL.id -> "generic"
    else -> key
  }
  val setterName = serviceCode.replaceFirstChar(Char::uppercase)
  val setterMap = this::class.members.associateBy { it.name }

  setterMap["set$setterName"]?.call(this, token)

  return this
}

fun AppPreferences.ServiceToken.Builder.setToken(key: String, token: String): AppPreferences.ServiceToken.Builder {
  val setterName = key.replaceFirstChar(Char::uppercase)
  val setterMap = this::class.members.associateBy { it.name }

  setterMap["set$setterName"]?.call(this, token)

  return this
}