package com.aigroup.aigroupmobile.data.models

import android.util.Log
import com.aigroup.aigroupmobile.data.utils.setToken
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.yamlMap
import com.charleskorn.kaml.yamlScalar

data class RemoteTokenConfig(
  val tokens: AppPreferences.Token,
  val serviceTokens: AppPreferences.ServiceToken,
  val tokenConfigs: Map<String, AppPreferences.TokenConfig> = emptyMap()
) {

  companion object {
    const val TAG = "RemoteTokenConfig"

    fun fromPreferences(preferences: AppPreferences): RemoteTokenConfig {
      return RemoteTokenConfig(
        tokens = preferences.token,
        serviceTokens = preferences.serviceToken
      )
    }

    fun fromYaml(yaml: String): RemoteTokenConfig {
      val tokens = mutableMapOf<String, String>()
      val serviceTokens = mutableMapOf<String, String>()
      val tokenConfigs = mutableMapOf<String, AppPreferences.TokenConfig>()

      val result = Yaml.default.parseToYamlNode(yaml)

      result.yamlMap.get<YamlList>("tokens")?.let {
        var currentTokenKey = ""

        it.items.forEach {
          it.yamlMap.entries.forEach {
            val config = tokenConfigs.getOrPut(it.key.content) {
              AppPreferences.TokenConfig.getDefaultInstance()
            }
            when (it.key.content) {
              "api_base" -> {
                val cfg = config.toBuilder().setApiBase(it.value.yamlScalar.content).build()
                tokenConfigs[currentTokenKey] = cfg
              }
              else -> {
                currentTokenKey = it.key.content
                tokens[it.key.content] = it.value.yamlScalar.content
              }
            }
          }
        }
      }

      result.yamlMap.get<YamlList>("service_tokens")?.let {
        it.items.forEach {
          it.yamlMap.entries.forEach {
            serviceTokens[it.key.content] = it.value.yamlScalar.content
          }
        }
      }

      Log.d(TAG, "got tokens from yaml: $tokens")
      Log.d(TAG, "got serviceTokens from yaml: $serviceTokens")
      Log.d(TAG, "got tokenConfigs from yaml: $tokenConfigs")

      return RemoteTokenConfig(
        tokens = AppPreferences.Token.getDefaultInstance().toBuilder().apply {
          tokens.forEach { (key, value) ->
            this.setToken(key, value)
          }
        }.build(),
        serviceTokens = AppPreferences.ServiceToken.getDefaultInstance().toBuilder().apply {
          serviceTokens.forEach { (key, value) ->
            this.setToken(key, value)
          }
        }.build(),
        tokenConfigs = tokenConfigs
      )
    }
  }

}