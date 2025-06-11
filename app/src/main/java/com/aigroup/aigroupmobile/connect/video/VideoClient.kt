package com.aigroup.aigroupmobile.connect.video

import com.aallam.openai.api.logging.Logger
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.connect.video.platforms.CogVideoGenerator
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.utils.getToken

fun AppPreferences.videoAI(model: VideoModelCode): VideoGenerator {
  return when (model) {
    VideoModelCode.CogVideoX, VideoModelCode.CogVideoXFlash -> {
      CogVideoGenerator(
        config = OpenAIConfig(
          host = OpenAIHost(ChatServiceProvider.ZHI_PU.apiBase),
          token = this.token.getToken(ChatServiceProvider.ZHI_PU) ?: "",
          logging = LoggingConfig(
            logger = Logger.Default
          )
        )
      )
    }
  }
}