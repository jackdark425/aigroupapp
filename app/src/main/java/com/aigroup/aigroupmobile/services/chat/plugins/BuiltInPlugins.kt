package com.aigroup.aigroupmobile.services.chat.plugins

import com.aigroup.aigroupmobile.services.chat.plugins.builtin.ImageGenerationPlugin
import com.aigroup.aigroupmobile.services.chat.plugins.builtin.MindMapPlugin
import com.aigroup.aigroupmobile.services.chat.plugins.builtin.SerperGooglePlugin
import com.aigroup.aigroupmobile.services.chat.plugins.builtin.VideoGenerationPlugin

object BuiltInPlugins {
  val plugins = listOf<ChatPluginDescription<ChatPlugin>>(
    ImageGenerationPlugin.Companion as ChatPluginDescription<ChatPlugin>,
    SerperGooglePlugin.Companion as ChatPluginDescription<ChatPlugin>,
    VideoGenerationPlugin.Companion as ChatPluginDescription<ChatPlugin>,
    MindMapPlugin.Companion as ChatPluginDescription<ChatPlugin>
  )
}