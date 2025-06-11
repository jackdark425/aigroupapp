package com.aigroup.aigroupmobile.connect.images

import android.content.Context
import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appStringResource
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider

enum class ImageGenerateServiceProvider(val id: String) {
  Official("official"),
  CogView("cogView"),
  StepFun("stepfun"),
  Brain360("brain360"),
  Qwen("qwen");

  // TODO: 重新支持 Spark
//  Spark("spark");

  @get:StringRes
  val displayNameId: Int
    get() = when (this) {
      Official -> (R.string.label_image_provider_name_official)
      CogView -> (R.string.label_image_provider_name_cogview)
      StepFun -> (R.string.label_image_provider_name_stepfun)
      Brain360 -> (R.string.label_image_provider_name_brain360)
      Qwen -> (R.string.label_image_provider_name_qwen)
//      Spark -> "讯飞星火"
    }

  val displayName: String
    get() = appStringResource(displayNameId)

  @get:DrawableRes
  val logoIconId: Int
    get() = when (this) {
      Official -> R.drawable.ic_openai_icon
      CogView -> R.drawable.ic_glm_icon
      StepFun -> R.drawable.ic_stepfun_icon
      Brain360 -> R.drawable.ic_brain360_icon
      Qwen -> R.drawable.ic_qwen_icon
//      Spark -> R.drawable.ic_xunfei_icon
    }

  data class ModelInfo(
    /**
     * 模型支持的分辨率列表
     */
    val supportsResolutions: List<Pair<Int, Int>>,

    /**
     * 是否支持批量生成, 默认支持
     */
    val supportBatch: Boolean = false
  )

  val models: Map<String, ModelInfo>
    get() = when (this) {
      Official -> mapOf(
        "dall-e-2" to ModelInfo(listOf(256 to 256, 512 to 512, 1024 to 1024), supportBatch = true),
        "dall-e-3" to ModelInfo(listOf(1024 to 1024, 1792 to 1024, 1024 to 1792)),
      )

      // https://open.bigmodel.cn/dev/api/image-model/cogview
      CogView -> mapOf(
        "cogview-3" to ModelInfo(listOf(1024 to 1024)), // TODO: 该模型不支持该参数！
        "cogview-3-plus" to ModelInfo(
          listOf(
            1024 to 1024,
            768 to 1344,
            864 to 1152,
            1344 to 768,
            1152 to 864,
            1440 to 720,
            720 to 1440
          )
        ),
        "cogview-3-flash" to ModelInfo(
          listOf(
            1024 to 1024,
            768 to 1344,
            864 to 1152,
            1344 to 768,
            1152 to 864,
            1440 to 720,
            720 to 1440
          )
        ) // free
      )

      // https://platform.stepfun.com/docs/api-reference/image
      StepFun -> mapOf(
        "step-1x-medium" to ModelInfo(
          listOf(
            256 to 256, 512 to 512, 768 to 768, 1024 to 1024, 1280 to 800, 800 to 1280
          )
        )
      )

      // https://ai.360.com/platform/docs/overview
      Brain360 -> mapOf(
        "360CV_S0_V5" to ModelInfo(listOf(512 to 512, 1024 to 1024, 2048 to 2048), supportBatch = true)
      )

      Qwen -> mapOf(
        "wanx-v1" to ModelInfo(listOf(720 to 1280, 1280 to 720, 1024 to 1024), supportBatch = true)
      )

//      Spark -> mapOf(
//        "general" to ModelInfo(
//          listOf(
//            512 to 512,
//            640 to 360,
//            640 to 480,
//            640 to 640,
//            680 to 512,
//            512 to 680,
//            768 to 768,
//            720 to 1280,
//            1280 to 720,
//            1024 to 1024
//          ),
//        )
//      )
    }

  val chatClientCompatible: ChatServiceProvider?
    get() = when (this) {
      Official -> ChatServiceProvider.OFFICIAL
      CogView -> ChatServiceProvider.ZHI_PU
      StepFun -> ChatServiceProvider.STEP_FUN
      else -> null
    }

}

val Pair<Int, Int>.resolutionString: String
  get() = "${first}x$second"
