package com.aigroup.aigroupmobile.connect.chat.platforms

import com.aallam.openai.client.OpenAI
import com.aigroup.aigroupmobile.connect.chat.ChatEndpoint

class OpenAIChatEndpoint(private val openAI: OpenAI) : ChatEndpoint,
  com.aallam.openai.client.Chat by openAI,
  com.aallam.openai.client.Models by openAI