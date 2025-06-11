package com.aigroup.aigroupmobile.connect.images

import com.aallam.openai.api.logging.Logger
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.connect.images.platforms.Brain360ImageGeneration
import com.aigroup.aigroupmobile.connect.images.platforms.OpenAIImageGenerator
import com.aigroup.aigroupmobile.connect.images.platforms.QwenImageGeneration
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.utils.getToken

fun AppPreferences.imageAI(provider: ImageGenerateServiceProvider): ImageGenerator = let {
  val chatProvider = provider.chatClientCompatible
  if (chatProvider != null) {
    return@let OpenAIImageGenerator(
      ai = OpenAI(
        OpenAIConfig(
          host = OpenAIHost(chatProvider.apiBase),
          // TODO: 有可能导致 apikey 错误的情况
          token = it.token.getToken(chatProvider) ?: "",
          logging = LoggingConfig(
            logger = Logger.Default
          )
        )
      )
    )
  }

  // 以下的 OpenAIConfig 只是借用其定义用于初始化 http client
  // see [ImageGenerator.createCommonImageClient]
  when (provider) {
    ImageGenerateServiceProvider.Brain360 -> {
      Brain360ImageGeneration(
        config = OpenAIConfig(
          host = OpenAIHost(ChatServiceProvider.BRAIN_360.apiBase),
          token = it.token.getToken(ChatServiceProvider.BRAIN_360) ?: "",
          logging = LoggingConfig(
            logger = Logger.Default
          )
        )
      )
    }
    ImageGenerateServiceProvider.Qwen -> {
      QwenImageGeneration(
        config = OpenAIConfig(
          host = OpenAIHost("https://dashscope.aliyuncs.com/api/v1/"),
          token = it.token.getToken(ChatServiceProvider.DASH_SCOPE) ?: "",
          logging = LoggingConfig(
            logger = Logger.Default
          )
        )
      )
    }
//    ImageGenerateServiceProvider.Spark -> {
//      SparkImageGeneration(
//        config = OpenAIConfig(
//          host = OpenAIHost("https://spark-api.cn-huabei-1.xf-yun.com/v2.1/"),
//          token = it.token.getToken(ChatServiceProvider.XUN_FEI) ?: "",
//          logging = LoggingConfig(
//            logger = Logger.Default
//          )
//        )
//      )
//    }
    else -> throw IllegalArgumentException("Unsupported provider: $provider")
  }
}