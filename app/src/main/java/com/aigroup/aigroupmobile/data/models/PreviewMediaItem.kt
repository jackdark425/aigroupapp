package com.aigroup.aigroupmobile.data.models

data class PreviewMediaItem(
  val mediaItem: MediaItem,
  val description: String? = null,
  val plugin: PluginInfo? = null,
) {
  data class PluginInfo(
    val id: String,
    val extra: String?
  )
}