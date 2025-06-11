package com.aigroup.aigroupmobile.connect.video

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appStringResource
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.connect.images.ImageGenerateServiceProvider.CogView
import com.aigroup.aigroupmobile.connect.images.ImageGenerateServiceProvider.Official
import com.aigroup.aigroupmobile.connect.images.ImageGenerateServiceProvider.StepFun

enum class VideoModelCode(val code: String) {
  CogVideoX("cogvideox"),
  CogVideoXFlash("cogvideox-flash"); // free

  companion object {
    fun fromFullCode(fullCode: String): VideoModelCode {
      val (providerId, code) = fullCode.split('/')

      val provider = listOf("cogView").find { it == providerId }
      require(provider != null) { "Invalid provider id: $providerId" }

      val modelCode = entries.find { it.code == code }
      require(modelCode != null) { "Invalid model code: $code" }

      return modelCode
    }
  }

  @get:DrawableRes
  val logoIconId: Int
    get() = when (this) {
      CogVideoX -> R.drawable.ic_glm_icon
      CogVideoXFlash -> R.drawable.ic_glm_icon
    }

  @get:StringRes
  private val fullDisplayNameRes: Int
    get() {
    return when (this) {
      CogVideoX -> R.string.video_generation_service_provider_cogviewx
      CogVideoXFlash -> R.string.video_generation_service_provider_cogviewxflash
    }
  }

  fun fullDisplayName(): String {
    return appStringResource(fullDisplayNameRes)
  }

  fun fullCode(): String {
    when (this) {
      // 以 cogView/ 开头跟 ImageGenerateServiceProvider 对齐，方便以后做 video provider 升级
      CogVideoX -> return "cogView/$code"
      CogVideoXFlash -> return "cogView/$code"
    }
  }
}