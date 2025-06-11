package com.aigroup.aigroupmobile.connect.chat

import android.content.Context
import android.content.res.Resources
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.aallam.openai.client.OpenAIConfig
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appStringResource
import com.aigroup.aigroupmobile.connect.chat.platforms.AiMassModels
import com.aigroup.aigroupmobile.connect.chat.platforms.AnthropicChat
import com.aigroup.aigroupmobile.connect.chat.platforms.BaichuanModels
import com.aigroup.aigroupmobile.connect.chat.platforms.BaiduChat
import com.aigroup.aigroupmobile.connect.chat.platforms.GoogleChat
import com.aigroup.aigroupmobile.connect.chat.platforms.OpenRouterChatEndpoint
import com.aigroup.aigroupmobile.connect.chat.platforms.PerplexityModels
import com.aigroup.aigroupmobile.connect.chat.platforms.XunfeiModels
import com.aigroup.aigroupmobile.connect.chat.platforms.ZhipuModels
import kotlinx.parcelize.Parcelize
import kotlin.reflect.KClass

@Parcelize
enum class ChatServiceProvider(
  override val id: String,
  override val apiBase: String,
  @StringRes val descriptionRes: Int,
) : Parcelable, ServiceProvider {
  

  OFFICIAL(
    "official",
    "https://aihubmix.com/v1/",
    R.string.chat_provider_desc_official,
  ),

  OPEN_ROUTER(
    "openrouter",
    "https://openrouter.ai/api/v1/",
    R.string.chat_provider_desc_openrouter,
  ),

  OPEN_AI(
    "openai",
    "https://api.openai.com/v1/",
    R.string.chat_provider_desc_openai
  ),

  ANTHROPIC(
    "anthropic",
    "https://api.anthropic.com/v1/",
    R.string.chat_provider_desc_anthropic
  ),

  GOOGLE(
    "google",
    "https://generativelanguage.googleapis.com/v1beta/",
    R.string.chat_provider_desc_google,
  ),

  MISTRAL(
    "mistral",
    "https://api.mistral.ai/v1/",
    R.string.chat_provider_desc_mistral,
  ),

  PERPLEXITY(
    "perplexity",
    "https://api.perplexity.ai/",
    R.string.chat_provider_desc_perplexity,
  ),

  GROQ(
    "groq",
    "https://api.groq.com/openai/v1/",
    R.string.chat_provider_desc_groq,
  ),

  MOON_SHOT(
    "moonshot",
    "https://api.moonshot.cn/v1/",
    R.string.chat_provider_desc_moonshot,
  ),

  DASH_SCOPE(
    "dashscope",
    "https://dashscope.aliyuncs.com/compatible-mode/v1/",
    R.string.chat_provider_desc_dashscope,
  ),

  STEP_FUN(
    "stepfun",
    "https://api.stepfun.com/v1/",
    R.string.chat_provider_desc_stepfun,
  ),

  BAI_CHUAN(
    "baichuan",
    "https://api.baichuan-ai.com/v1/",
    R.string.chat_provider_desc_baichuan
  ),

  AI_MASS(
    "aimass",
    "https://ai-maas.wair.ac.cn/maas/v1/",
    R.string.chat_provider_desc_aimass,
  ),

  LING_YI_WAN_WU(
    "lingyiwanwu",
    "https://api.lingyiwanwu.com/v1/",
    R.string.chat_provider_desc_lingyiwanwu,
  ),

  BRAIN_360(
    "brain360",
    "https://api.360.cn/v1/",
    R.string.chat_provider_desc_brain360,
  ),

  XUN_FEI(
    "xunfei",
    "https://spark-api-open.xf-yun.com/v1/",
    R.string.chat_provider_desc_xunfei,
  ),

  ZHI_PU(
    "zhipu",
    "https://open.bigmodel.cn/api/paas/v4/",
    R.string.chat_provider_desc_zhipu,
  ),

  BAI_DU(
    "baidu",
    // NOTE: IT'S DO NOT SUPPORT OPENAI API SCHEMA
    "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/",
    R.string.chat_provider_desc_baidu,
  ),

  DEEP_SEEK(
    "deepseek",
    "https://api.deepseek.com/",
    R.string.chat_provider_desc_deepseek,
  );

  override val isEnabled: Boolean
    get() = true;

  override val description: String
    @Composable
    get() = stringResource(descriptionRes)

  @get:StringRes
  private val displayNameRes: Int
    get() = when (this) {
      OFFICIAL -> (R.string.chat_provider_name_official)
      MOON_SHOT -> (R.string.chat_provider_name_moonshot)
      DASH_SCOPE -> (R.string.chat_provider_name_dashscope)
      STEP_FUN -> (R.string.chat_provider_name_stepfun)
      BAI_CHUAN -> (R.string.chat_provider_name_baichuan)
      AI_MASS -> (R.string.chat_provider_name_aimass)
      LING_YI_WAN_WU -> (R.string.chat_provider_name_lingyiwanwu)
      BRAIN_360 -> (R.string.chat_provider_name_brain360)
      XUN_FEI -> (R.string.chat_provider_name_xunfei)
      ZHI_PU -> (R.string.chat_provider_name_zhipu)
      BAI_DU -> (R.string.chat_provider_name_baidu)
      OPEN_AI -> (R.string.chat_provider_name_openai)
      ANTHROPIC -> (R.string.chat_provider_name_anthropic)
      GOOGLE -> (R.string.chat_provider_name_google)
      MISTRAL -> (R.string.chat_provider_name_mistral)
      PERPLEXITY -> (R.string.chat_provider_name_perplexity)
      GROQ -> (R.string.chat_provider_name_groq)
      DEEP_SEEK -> (R.string.chat_provider_name_deep_seek)
      OPEN_ROUTER -> (R.string.chat_provider_name_openrouter)
    }

  override val displayName: String
    get() = appStringResource(displayNameRes)

  /**
   * @Deprecated("Use [displayName] instead")
   */
  fun displayName(context: Context): String {
    return context.getString(displayNameRes)
  }

  @get:DrawableRes
  override val logoIconId: Int
    get() = when (this) {
      OFFICIAL -> R.drawable.ic_custom_bot_icon
      MOON_SHOT -> R.drawable.ic_moonshot_icon
      DASH_SCOPE -> R.drawable.ic_qwen_icon
      STEP_FUN -> R.drawable.ic_stepfun_icon
      BAI_CHUAN -> R.drawable.ic_baichuan_icon
      AI_MASS -> R.drawable.ic_aimass_icon
      LING_YI_WAN_WU -> R.drawable.ic_lingyiwanwu_icon
      BRAIN_360 -> R.drawable.ic_brain360_icon
      XUN_FEI -> R.drawable.ic_xunfei_icon
      ZHI_PU -> R.drawable.ic_glm_icon
      BAI_DU -> R.drawable.ic_baidu_icon
      OPEN_AI -> R.drawable.ic_openai_icon
      ANTHROPIC -> R.drawable.ic_claude_icon
      GOOGLE -> R.drawable.ic_google_icon
      MISTRAL -> R.drawable.ic_mistral_icon
      PERPLEXITY -> R.drawable.ic_perplexity_icon
      GROQ -> R.drawable.ic_groq_icon
      DEEP_SEEK -> R.drawable.ic_deepseek_icon
      OPEN_ROUTER -> R.drawable.ic_openrouter_icon
    }

  override val backColor: Color?
    get() = when (this) {
      MOON_SHOT -> Color(0xFF1A1D22)
      LING_YI_WAN_WU -> Color(0xFF003425)
      OPEN_AI -> Color.Black
      ANTHROPIC -> Color(0xFFCC9B7A)
      else -> null
    }

  // TODO: Remove this
  val customModelsEndpoint: com.aallam.openai.client.Models?
    get() = when (this) {
      BAI_CHUAN -> BaichuanModels()
      AI_MASS -> AiMassModels()
      XUN_FEI -> XunfeiModels()
      ZHI_PU -> ZhipuModels()
      PERPLEXITY -> PerplexityModels()
      else -> null
    }

  fun createChatClient(config: OpenAIConfig): ChatEndpoint? {
    return when (this) {
      BAI_DU -> BaiduChat(config)
      ANTHROPIC -> AnthropicChat(config)
      GOOGLE -> GoogleChat(config)
      OPEN_ROUTER -> OpenRouterChatEndpoint(config)
      else -> null
    }
  }
}
