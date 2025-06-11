package com.aigroup.aigroupmobile.connect.images

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

data class ImageModelCode(
  val serviceProvider: ImageGenerateServiceProvider,
  val model: String
) {
  companion object {
    val Unknown = ImageModelCode(ImageGenerateServiceProvider.Official, "unknown")

    fun fromFullCode(fullCode: String): ImageModelCode {
      val (providerId, code) = fullCode.split('/')

      val provider = ImageGenerateServiceProvider.entries.find { it.id == providerId }
      require(provider != null) { "Invalid provider id: $providerId" }

      return ImageModelCode(provider, code)
    }
  }

  fun fullCode(): String {
    return "${serviceProvider.id}/$model"
  }

  fun fullDisplayName(): String {
    return "${serviceProvider.displayName}/$model"
  }

  val modelInfo
    get() = serviceProvider.models[this.model]

  val supportBatch: Boolean
    get() = modelInfo?.supportBatch ?: false
}