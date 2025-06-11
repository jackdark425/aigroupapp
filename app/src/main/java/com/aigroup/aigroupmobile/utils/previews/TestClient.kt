package com.aigroup.aigroupmobile.utils.previews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.aigroup.aigroupmobile.BuildConfig
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider

@Composable
fun rememberTestAI(): OpenAI {
  return remember {
    val conf = OpenAIConfig(
      token = BuildConfig.testAiHubMixToken,
      host = OpenAIHost(ChatServiceProvider.OFFICIAL.apiBase)
    )
    OpenAI(conf)
  }
}

val previewModelCode = ModelCode("gpt-4o", ChatServiceProvider.OFFICIAL)