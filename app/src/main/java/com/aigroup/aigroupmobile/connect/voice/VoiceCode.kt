package com.aigroup.aigroupmobile.connect.voice

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appStringResource

enum class VoiceServiceProvider(val id: String) {
  ANDROID("android"),
  AZURE("azure"),
  OPENAI_TTS("openai-tts"),
  OPENAI_TTS_HD("openai-tts-hd");

  @get:StringRes
  val displayNameId: Int
    get() = when (this) {
      ANDROID -> (R.string.tts_service_provider_system)
      AZURE -> (R.string.tts_service_provider_azure)
      OPENAI_TTS -> (R.string.tts_service_provider_tts_1)
      OPENAI_TTS_HD -> (R.string.tts_service_provider_tts_1_hd)
    }

  val displayName: String
    get() = appStringResource(displayNameId)

  @get:DrawableRes
  val logoIconId: Int
    get() = when (this) {
      ANDROID -> R.drawable.ic_android_icon
      AZURE -> R.drawable.ic_azure_logo
      OPENAI_TTS -> R.drawable.ic_openai_icon
      OPENAI_TTS_HD -> R.drawable.ic_openai_icon
    }

  val defaultVoiceCode: VoiceCode
    get() = when (this) {
      // TODO: dont hard code here
      ANDROID -> VoiceCode(this, "default")
      AZURE -> VoiceCode(this, "zh-CN-XiaoxiaoNeural")
      OPENAI_TTS -> VoiceCode(this, "fable")
      OPENAI_TTS_HD -> VoiceCode(this, "fable")
    }

  val variantList: List<VoiceCode>
    get() = when (this) {
      ANDROID -> AndroidSystemVoice.entries.map { it.code }
      AZURE -> AzureVoice.entries.map { it.code }
      OPENAI_TTS -> OpenAIVoice.entries.map { it.code }
      OPENAI_TTS_HD -> OpenAIVoice.entries.map { it.code }
    }.map { VoiceCode(this, it) }
}

data class VoiceCode(
  val serviceProvider: VoiceServiceProvider,
  val variant: String,
) {
  companion object {
    val Unknown = VoiceCode(VoiceServiceProvider.OPENAI_TTS, "unknown")

    fun fromFullCode(fullCode: String): VoiceCode {
      val (providerId, code) = fullCode.split('/')

      val provider = VoiceServiceProvider.entries.find { it.id == providerId }
      require(provider != null) { "Invalid provider id: $providerId" }

      return VoiceCode(provider, code)
    }
  }

  fun fullCode(): String {
    return "${serviceProvider.id}/$variant"
  }

  val displayVariant: String
    get() = when (serviceProvider) {
      VoiceServiceProvider.ANDROID -> AndroidSystemVoice.entries.find { it.code == variant }?.displayName ?: variant
      VoiceServiceProvider.AZURE -> AzureVoice.entries.find { it.code == variant }?.displayName ?: variant
      VoiceServiceProvider.OPENAI_TTS -> OpenAIVoice.entries.find { it.code == variant }?.displayName ?: variant
      VoiceServiceProvider.OPENAI_TTS_HD -> OpenAIVoice.entries.find { it.code == variant }?.displayName ?: variant
    }

}