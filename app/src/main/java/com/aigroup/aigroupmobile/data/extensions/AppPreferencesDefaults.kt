package com.aigroup.aigroupmobile.data.extensions

import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.connect.images.ImageModelCode
import com.aigroup.aigroupmobile.connect.video.VideoModelCode
import com.aigroup.aigroupmobile.connect.voice.VoiceCode
import com.aigroup.aigroupmobile.connect.voice.VoiceServiceProvider
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.models.AppPreferences.LongBotProperties

// TODO: better way to handle default values

object AppPreferencesDefaults {

  // 默认使用经典配色方案，不再根据 Android 版本判断
  val defaultColorScheme: AppPreferences.ColorScheme = AppPreferences.ColorScheme.CLASSIC

  val defaultLongBotProperties: AppPreferences.LongBotProperties
    get() {
      return AppPreferences.LongBotProperties.newBuilder()
        .setTemperature(1.0)
        .setTopP(1.0)
        .setFrequencyPenalty(0.0)
        .setPresencePenalty(0.0)
        .build()
    }

  // TODO: set to gradle
  @Deprecated("Moved to WelcomeModelPage")
  val defaultModelCode: ModelCode = ModelCode("deepseek-chat", ChatServiceProvider.DEEP_SEEK)
  val defaultVoiceCode: VoiceCode = VoiceCode(VoiceServiceProvider.OPENAI_TTS, "fable")

  // TODO: support config
  val defaultEmbeddingModel: ModelCode = ModelCode("text-embedding-3-large", ChatServiceProvider.OFFICIAL)

  val defaultImageModel: String = "official/dall-e-3"
  val defaultImageResolution: String = "1024x1024"
  val defaultImageN: Int = 4

  val defaultAvatarImageModel = ImageModelCode.fromFullCode("cogView/Cogview-3-Flash")
  val defaultAvatarImageResolution: String = "1024x1024"

  val defaultVideoModel: String = VideoModelCode.CogVideoXFlash.fullCode()

  val defaultFavoriteModels: List<String> = listOf(
    "deepseek:deepseek-chat",
    "deepseek:deepseek-reasoner",
    "custom_enterprise_insight_pro:Enterprise Insight Pro",
    "custom_finwise_sprite:FinWise Sprite",
    "custom_profitpulse:ProfitPulse",
  )
  val defaultFavoriteModelCodes: List<ModelCode> = defaultFavoriteModels.map { ModelCode.fromFullCode(it) }

  fun mergeDefaultLongBotProperties(
    instance: LongBotProperties
  ): AppPreferences.LongBotProperties {
    val d = defaultLongBotProperties
    return instance.toBuilder()
      .let {  if (instance.hasTemperature()) { it } else { it.setTemperature(d.temperature) } }
      .let {  if (instance.hasTopP()) { it } else { it.setTopP(d.topP) } }
      .let {  if (instance.hasFrequencyPenalty()) { it } else { it.setFrequencyPenalty(d.frequencyPenalty) } }
      .let {  if (instance.hasPresencePenalty()) { it } else { it.setPresencePenalty(d.presencePenalty) } }
      .build()
  }

  // TODO: 等待 protobuf 重新生成后启用
  // val defaultTimeoutConfig: AppPreferences.TimeoutConfig
  //   get() {
  //     return AppPreferences.TimeoutConfig.newBuilder()
  //       .setConnectTimeoutSeconds(30)  // 连接超时 30 秒
  //       .setRequestTimeoutSeconds(120) // 请求超时 120 秒 (适合长时间的AI响应)
  //       .setSocketTimeoutSeconds(60)   // Socket超时 60 秒
  //       .build()
  //   }

  // 临时的默认超时值（秒）- 设置为1200秒(20分钟)允许长时间等待
  const val DEFAULT_CONNECT_TIMEOUT_SECONDS = 1200
  const val DEFAULT_REQUEST_TIMEOUT_SECONDS = 1200
  const val DEFAULT_SOCKET_TIMEOUT_SECONDS = 1200

}
