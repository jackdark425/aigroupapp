package com.aigroup.aigroupmobile.connect.chat

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appStringResource
import com.aigroup.aigroupmobile.connect.chat.platforms.PerplexityModels
import com.aigroup.aigroupmobile.connect.chat.platforms.XunfeiModels
import com.aigroup.aigroupmobile.connect.chat.platforms.ZhipuModels
import kotlinx.parcelize.Parcelize

private data class RawInfo(
  val context: Int,
  val supportVision: Boolean,
  val supportVideo: Boolean = false,
)

private val modelCodeRawInfo = mapOf(
  // https://platform.openai.com/docs/models/
  "gpt-4o*" to RawInfo(128000, true),
  "gpt-4-turbo*" to RawInfo(128000, true),
  "gpt-4-0125-preview" to RawInfo(128000, true),
  "gpt-4-1106-preview" to RawInfo(128000, true),
  "gpt-4*" to RawInfo(8192, true),
  "o1-mini*" to RawInfo(128000, false),
  "o1-preview*" to RawInfo(128000, false),
  "o1*" to RawInfo(200000, true),
  "o3-mini*" to RawInfo(200000, false),
  "gpt-3.5-turbo*" to RawInfo(16385, true),
  "gpt-3.5-turbo-instruct" to RawInfo(4096, true),
  "gpt-4.5-preview" to RawInfo(128000, true),

  // https://ai.google.dev/gemini-api/docs/models/gemini
  "gemini-1.5-flash*" to RawInfo(1048576, true),
  "gemini-1.5-pro*" to RawInfo(2097152, true),
  "gemini-1.0-pro*" to RawInfo(32000, false),
  "gemini-pro" to RawInfo(32000, false),
  "gemini-2.0-flash-thinking*" to RawInfo(32000, true), // https://cloud.google.com/vertex-ai/generative-ai/docs/thinking-mode
  "gemini-2.0-flash*" to RawInfo(1048576, true),

  // https://docs.anthropic.com/en/docs/about-claude/models
  "claude-3-7-sonnet*" to RawInfo(200000, true),
  "claude-3-7-haiku*" to RawInfo(200000, true),
  "claude-3-5-sonnet*" to RawInfo(200000, true),
  "claude-3-5-haiku*" to RawInfo(200000, true),
  "claude-3-opus*" to RawInfo(200000, true),
  "claude-3-sonnet*" to RawInfo(200000, true),
  "claude-3-haiku*" to RawInfo(200000, true),
  "claude-2.1*" to RawInfo(200000, false),
  "claude-2.0*" to RawInfo(100000, false),
  "claude-instant-1.2" to RawInfo(100000, false),

  // https://help.aliyun.com/zh/dashscope/developer-reference/model-introduction
  "qwen-max-longcontext" to RawInfo(30000, false),
  "qwen-max*" to RawInfo(8192, false),
  "qwen-plus-0806" to RawInfo(131072, false),
  "qwen-plus" to RawInfo(131072, false),
  "qwen-plus-*" to RawInfo(32000, false),
  "qwen-turbo*" to RawInfo(8192, false),
  // https://help.aliyun.com/zh/dashscope/developer-reference/tongyi-qianwen-vl-plus-api
  "qwen-vl-max" to RawInfo(32000, true, supportVideo = true),
  "qwen-vl-max-0809" to RawInfo(32000, true, supportVideo = true),
  "qwen-vl-plus-0809" to RawInfo(32000, true, supportVideo = true),
  "qwen-vl-max-0201" to RawInfo(8192, true),
  "qwen-vl-plus" to RawInfo(8192, true),
  // https://qwenlm.github.io/zh/blog/qwen2.5/
  "qwen2.5-3b*" to RawInfo(32000, false),
  "qwen2.5-7b*" to RawInfo(128000, false),
  "qwen2.5-14b*" to RawInfo(128000, false),
  "qwen2.5-32b*" to RawInfo(128000, false),
  "qwen2.5-72b*" to RawInfo(128000, false),
  "qwen2.5-math-7b*" to RawInfo(4000, false),
  "qwen2.5-math-72b*" to RawInfo(4000, false),
  "qwen2.5-coder-1.5b*" to RawInfo(128000, false),
  "qwen2.5-coder-7b*" to RawInfo(128000, false),
  // https://huggingface.co/Qwen/Qwen2-72B-Instruct
  "qwen/qwen2-72n-instruct" to RawInfo(131072, false),
  // https://console.groq.com/docs/models#meta-llama-3-70b
  "llama3-70b-8192" to RawInfo(8192, false),
  "llama3-8b-8192" to RawInfo(8192, false),
  "llama3-8b-8192" to RawInfo(8192, false),
  "llama-3.1-70b*" to RawInfo(128000, false),
  "llama-3.1-8b-instant" to RawInfo(128000, false),
  "llama-3.1-405b*" to RawInfo(128000, false),

  "llama-3.2-1b*" to RawInfo(128000, false),
  "llama-3.2-3b*" to RawInfo(128000, false),
  "llama-3.2-11b-vision*" to RawInfo(128000, true),
  "llama-3.2-90b-vision*" to RawInfo(128000, true),
  "llama-3.2-11b-text*" to RawInfo(128000, true),
  "llama-3.2-90b-text*" to RawInfo(128000, true),
  "llama-guard-3-8b*" to RawInfo(8192, false),

  // https://bigmodel.cn/dev/howuse/model
  "glm-zero*" to RawInfo(16000, false),
  "glm-4-plus" to RawInfo(128000, false),
  "glm-4-0520" to RawInfo(128000, false),
  "glm-4-long" to RawInfo(1000000, false),
  "glm-4-airx" to RawInfo(8192, false),
  "glm-4-air" to RawInfo(128000, false),
  "glm-4-flashx" to RawInfo(128000, false),
  "glm-4-flash" to RawInfo(128000, false), // free
  "glm-4v" to RawInfo(2048, true),
  "glm-4v-flash" to RawInfo(8000, true), // free
  "glm-4v-plus" to RawInfo(8000, true),
  "glm-4" to RawInfo(128000, false),
  "glm-4-alltools" to RawInfo(128000, false),
  "glm-3-turbo" to RawInfo(8192, false),
  "chatglm-3" to RawInfo(4096, false),
  "emohaa" to RawInfo(8192, false),
  "codegeex-4" to RawInfo(8192, false),

  // https://platform.moonshot.cn/docs/api/chat#字段说明
  "moonshot-v1-8k" to RawInfo(8192, false),
  "moonshot-v1-32k" to RawInfo(32000, false),
  "moonshot-v1-128k" to RawInfo(128000, false),

  // https://platform.stepfun.com/docs/llm/modeloverview
  "step-1-flash" to RawInfo(8192, false),
  "step-1-8k" to RawInfo(8192, false),
  "step-1-32k" to RawInfo(32000, false),
  "step-1-128k" to RawInfo(128000, false),
  "step-1-256k" to RawInfo(256000, false),
  "step-1v-8k" to RawInfo(8192, true),
  "step-1v-32k" to RawInfo(32000, true),
  "step-2-16k" to RawInfo(16384, false),

  // https://platform.baichuan-ai.com/price
  "Baichuan4-Air" to RawInfo(32000, false),
  "Baichuan4-Turbo" to RawInfo(32000, false),
  "Baichuan4" to RawInfo(32000, false),
  "Baichuan3-Turbo" to RawInfo(32000, false),
  "Baichuan3-Turbo-128k" to RawInfo(128000, false),
  "Baichuan2-Turbo" to RawInfo(32000, false),

  // https://ai-maas.wair.ac.cn/#/doc
  "taichu_llm" to RawInfo(32000, false),

  // https://platform.lingyiwanwu.com/docs#%E6%A8%A1%E5%9E%8B%E4%B8%8E%E8%AE%A1%E8%B4%B9
  "yi-large" to RawInfo(32000, false),
  "yi-medium" to RawInfo(16384, false),
  "yi-vision" to RawInfo(16384, true),
  "yi-medium-200k" to RawInfo(200000, false),
  "yi-spark" to RawInfo(16384, false),
  "yi-large-rag" to RawInfo(16384, false),
  "yi-large-fc" to RawInfo(32000, false),
  "yi-large-turbo" to RawInfo(16384, false),
  "yi-large-preview" to RawInfo(16384, false),
  "yi-lightning" to RawInfo(16384, false),
  "yi-lightning-lite" to RawInfo(16384, false),

  // 360 暂无模型信息 https://ai.360.com/platform/limit

  // spark 暂未找到官方信息
  // https://github.com/lobehub/lobe-chat/pull/3098/files#diff-7cff3b24f2ed2939cc447c68dcd9b65ccefb8ed24c132df5d6c618bc94fb984b
  "general" to RawInfo(8192, false),
  "generalv3" to RawInfo(8192, false),
  "pro-128k" to RawInfo(128000, false),
  "generalv3.5" to RawInfo(8192, false),
  "max-32k" to RawInfo(32000, false),
  "4.0ultra" to RawInfo(8192, false), // TODO: 该模型多模态不支持 openai 风格

  // https://cloud.baidu.com/doc/WENXINWORKSHOP/s/Nlks5zkzu
  "ernie-4.0-8k*" to RawInfo(8192, false),
  "ernie-4.0-turbo-8k*" to RawInfo(8192, false),
  "ernie-3.5-8k*" to RawInfo(8192, false),
  "ernie-3.5-128k*" to RawInfo(128000, false),
  "ernie-speed-pro-128k*" to RawInfo(128000, false),
  "ernie-speed-8k*" to RawInfo(8192, false),
  "ernie-speed-128k*" to RawInfo(128000, false),
  "ernie-tiny-8k*" to RawInfo(8192, false),

  // https://docs.mistral.ai/getting-started/models/models_overview/
  "mistral-large*" to RawInfo(128000, false),
  "mistral-small*" to RawInfo(32000, false),
  "codestral*" to RawInfo(32000, false),
  "pixtral-12b*" to RawInfo(128000, true),
  "open-mistral-nemo" to RawInfo(128000, false),
  "open-codestral-mamba" to RawInfo(256000, false),
  "open-mistral-7b" to RawInfo(32000, false),
  "open-mixtral-8x7b" to RawInfo(32000, false),
  "open-mixtral-8x22b" to RawInfo(64000, false),

  // https://console.groq.com/docs/models#mixtral-8x7b
  "mixtral-8x7b*" to RawInfo(32768, false),

  // https://docs.perplexity.ai/guides/model-cards
  "llama-3.1-sonar-small-128k-online" to RawInfo(128000, false),
  "llama-3.1-sonar-large-128k-online" to RawInfo(128000, false),
  "llama-3.1-sonar-huge-128k-online" to RawInfo(128000, false),
  "llama-3.1-sonar-small-128k-chat" to RawInfo(128000, false),
  "llama-3.1-sonar-large-128k-chat" to RawInfo(128000, false),

  // https://console.groq.com/docs/models#llama-3-groq-70b-tool-use-preview
  "llama3-groq-8b-8192-tool-use-preview" to RawInfo(8192, false),
  "llama3-groq-70b-8192-tool-use-preview" to RawInfo(8192, false),

  // https://console.groq.com/docs/models#gemma-2-9b
  "gemma2-9b*" to RawInfo(8192, false),
  "gemma-2-9b*" to RawInfo(8192, false),
  "gemma-2-2b*" to RawInfo(8192, false),
  "gemma-2-27b*" to RawInfo(8192, false),
  "gemma-7b*" to RawInfo(8192, false),

  // https://api-docs.deepseek.com/zh-cn/quick_start/pricing
  "deepseek-chat" to RawInfo(64000, false),
  "deepseek-coder" to RawInfo(128000, false),
  "deepseek-reasoner" to RawInfo(64000, false),

  // https://console.groq.com/docs/models#llava-15-7b
  // TODO: only support single user message?
//  "llava-v1.5-7b*" to RawInfo(4096, true),
)

private fun getRawInfoOfModelCode(code: String): RawInfo? {
  val code = code.lowercase()

  if (modelCodeRawInfo.containsKey(code)) {
    return modelCodeRawInfo[code]!!
  }

  // using regex
  for ((key, value) in modelCodeRawInfo) {
    if (key.endsWith("*") && code.startsWith(key.dropLast(1))) {
      return value
    }
  }

  return null
}

sealed interface ModelGroup : Comparable<ModelGroup> {
  val displayName: String
  val serviceProvider: ServiceProvider

  val fullDisplayName: String
    get() = "${serviceProvider.displayName}/$displayName"

  sealed class Predefined(@StringRes val displayNameRes: Int) : ModelGroup {
    // TODO: openai -> gpt, google -> gemini
    data class OpenAI(override val serviceProvider: ChatServiceProvider) : Predefined((R.string.llm_model_group_openai))
    
    data class DeepSeek(
      override val serviceProvider: ChatServiceProvider = ChatServiceProvider.DEEP_SEEK
    ) : Predefined((R.string.deepseek))

    // TODO: rename to gemini
    data class Google(override val serviceProvider: ChatServiceProvider) : Predefined((R.string.llm_model_group_gemini))
    data class Alibaba(override val serviceProvider: ChatServiceProvider) : Predefined((R.string.llm_model_group_qwen))
    data class Anthropic(override val serviceProvider: ChatServiceProvider) : Predefined((R.string.llm_model_group_claude))
    data class Llama(override val serviceProvider: ChatServiceProvider) : Predefined((R.string.llm_model_group_llama))
    data class Glm(override val serviceProvider: ChatServiceProvider) : Predefined((R.string.llm_model_group_glm))

    data class Moonshot(
      override val serviceProvider: ChatServiceProvider = ChatServiceProvider.MOON_SHOT
    ) : Predefined((R.string.llm_model_group_moonshot))

    data class Stepfun(
      override val serviceProvider: ChatServiceProvider = ChatServiceProvider.STEP_FUN
    ) : Predefined((R.string.llm_model_group_stepfun))

    data class Baichuan(
      override val serviceProvider: ChatServiceProvider = ChatServiceProvider.BAI_CHUAN
    ) : Predefined((R.string.llm_model_group_baichuan))

    data class Taichu(
      override val serviceProvider: ChatServiceProvider = ChatServiceProvider.AI_MASS
    ) : Predefined((R.string.llm_model_group_taichu))

    data class Yi(
      override val serviceProvider: ChatServiceProvider = ChatServiceProvider.LING_YI_WAN_WU
    ) : Predefined((R.string.llm_model_group_lingyiwanw))

    data class Brain360(
      override val serviceProvider: ChatServiceProvider = ChatServiceProvider.BRAIN_360
    ) : Predefined((R.string.llm_model_group_brain360))

    data class Spark(
      override val serviceProvider: ChatServiceProvider = ChatServiceProvider.XUN_FEI
    ) : Predefined((R.string.llm_model_group_spark))

    data class Ernie(
      override val serviceProvider: ChatServiceProvider = ChatServiceProvider.BAI_DU
    ) : Predefined((R.string.llm_model_group_ernie))

    data class Mistral(
      override val serviceProvider: ChatServiceProvider = ChatServiceProvider.MISTRAL
    ) : Predefined((R.string.llm_model_group_mistral))

    data class Perplexity(
      override val serviceProvider: ChatServiceProvider = ChatServiceProvider.PERPLEXITY
    ) : Predefined((R.string.llm_model_group_perplexity))

    data class Groq(
      override val serviceProvider: ChatServiceProvider = ChatServiceProvider.GROQ
    ) : Predefined((R.string.llm_model_group_groq))

    data class Gemma(
      override val serviceProvider: ChatServiceProvider = ChatServiceProvider.GROQ
    ) : Predefined((R.string.llm_model_group_gemma))

    data class LlaVA(
      override val serviceProvider: ChatServiceProvider = ChatServiceProvider.GROQ
    ) : Predefined((R.string.llm_model_group_llava))

    private val ordinal: Int = when (this) { // TODO: Refactor
      is OpenAI -> 0
      is Google -> 1
      is Alibaba -> 2
      is Anthropic -> 3
      is Llama -> 4
      is Glm -> 5
      is Moonshot -> 6
      is Stepfun -> 7
      is Baichuan -> 8
      is Taichu -> 9
      is Yi -> 10
      is Brain360 -> 11
      is Spark -> 12
      is Ernie -> 13
      is Mistral -> 14
      is Perplexity -> 15
      is Groq -> 16
      is Gemma -> 17
      is LlaVA -> 18
      is DeepSeek -> 19
    }

    override fun compareTo(other: ModelGroup): Int {
      return when (other) {
        is Predefined -> {
          val res = ordinal.compareTo(other.ordinal)
          if (res == 0) {
            serviceProvider.id.compareTo(other.serviceProvider.id)
          } else {
            res
          }
        }

        is Custom -> -1
      }
    }

    override val displayName: String
      get() = appStringResource(displayNameRes)
  }

  data class Custom(override val serviceProvider: ServiceProvider) : ModelGroup {
    override val displayName: String
      get() = "其他模型"

    override fun compareTo(other: ModelGroup): Int {
      return when (other) {
        is Predefined -> 1
        is Custom -> {
          if (serviceProvider.id == other.serviceProvider.id) {
            0
          } else {
            serviceProvider.id.compareTo(other.serviceProvider.id)
          }
        }
      }
    }
  }
}

// TODO: not exclude dall-e (image models) by now
// TODO: support ownedBy field
// TODO: support embedding and design a way to distinct embed and chat

@Parcelize
data class ModelCode(
  internal val code: String,
  internal val serviceProvider: ServiceProvider,
): Parcelable, Comparable<ModelCode> {
  companion object {
    val Unknown = ModelCode("unknown", ChatServiceProvider.OFFICIAL)

    fun fromFullCode(fullCode: String): ModelCode {
      val (providerId, code) = fullCode.split(':', limit = 2)

      // 检查是否是自定义提供商
      if (CustomChatServiceProvider.isCustomProviderId(providerId)) {
        // 获取自定义提供商
        val customProvider = CustomChatServiceProvider.getProviderById(providerId)
        return ModelCode(code, customProvider ?: ChatServiceProvider.OFFICIAL)
      }

      val provider = ChatServiceProvider.entries.find { it.id == providerId }
      require(provider != null) { "Invalid provider id: $providerId" }

      return ModelCode(code, provider)
    }

    /**
     * 参考 MIGRATION.md (30), 从旧的 full code 转换为新的 ModelCode
     */
    fun tryFromOldFullCode(fullCode: String): ModelCode {
      val hasNewSplitter = fullCode.contains(':')
      if (hasNewSplitter) {
        return fromFullCode(fullCode)
      }

      val (providerId, code) = fullCode.split('/', limit = 2)

      val provider = ChatServiceProvider.entries.find { it.id == providerId }
      require(provider != null) { "Invalid provider id: $providerId" }

      return ModelCode(code, provider)
    }
  }

  // TODO: 替换代码里的常量
  object Models {
    val Gpt4o = ModelCode("gpt-4o", ChatServiceProvider.OFFICIAL)
  }

  init {
    require(code.isNotEmpty()) { "Model code must not be empty" }
  }

  override fun compareTo(other: ModelCode): Int {
    // 先排序 service provider id, 再排序 code
    val res = serviceProvider.id.compareTo(other.serviceProvider.id)
    return if (res == 0) code.compareTo(other.code) else res
  }

  // TODO: remove this
  @Deprecated("toString is deprecated", ReplaceWith("code.lowercase()"))
  override fun toString(): String = code.lowercase()

  fun fullCode(): String = "${serviceProvider.id}:$code"

  val fullDisplayCode: String
    @Composable
    get() = "${serviceProvider.displayName}/$code"

  val modelGroup: ModelGroup
    get() = when (serviceProvider) {
      is CustomChatServiceProvider -> {
        ModelGroup.Custom(serviceProvider)
      }
      is ChatServiceProvider -> {
        when {
          // TODO：refactor
          code.startsWith("gpt", true) || isOpenAIReasoning(code) -> {
            ModelGroup.Predefined.OpenAI(serviceProvider as ChatServiceProvider)
          }

          code.startsWith("gemini", true) -> {
            ModelGroup.Predefined.Google(serviceProvider as ChatServiceProvider)
          }

          code.startsWith("claude", true) -> {
            ModelGroup.Predefined.Anthropic(serviceProvider as ChatServiceProvider)
          }

          code.startsWith("qwen", true) -> {
            ModelGroup.Predefined.Alibaba(serviceProvider as ChatServiceProvider)
          }

          code.startsWith("glm", true) -> {
            ModelGroup.Predefined.Glm(serviceProvider as ChatServiceProvider)
          }

          code in ZhipuModels.models.map { it.id.id.lowercase() } -> ModelGroup.Predefined.Glm(serviceProvider as ChatServiceProvider)

          code.startsWith("moonshot", true) -> ModelGroup.Predefined.Moonshot()
          code.startsWith("step", true) -> ModelGroup.Predefined.Stepfun()
          code.startsWith("baichuan", true) -> ModelGroup.Predefined.Baichuan()
          code.startsWith("taichu", true) -> ModelGroup.Predefined.Taichu()
          code.startsWith("yi", true) -> ModelGroup.Predefined.Yi()
          code.startsWith("360", true) -> ModelGroup.Predefined.Brain360()
          code in XunfeiModels.models.map { it.id.id.lowercase() } -> ModelGroup.Predefined.Spark()
          code.startsWith("ernie", true) -> ModelGroup.Predefined.Ernie()

          code.startsWith("mistral", true) ||
              code.startsWith("codestral", true) ||
              code.startsWith("pixtral", true) ||
              code.startsWith("open-mistral", true) ||
              code.startsWith("open-codestral", true) ||
              code.startsWith("open-mixtral", true) ||
              code.startsWith("mixtral") -> ModelGroup.Predefined.Mistral()

          code in PerplexityModels.models.map { it.id.id.lowercase() } -> ModelGroup.Predefined.Perplexity()

          code.startsWith("llama3-groq", true) -> ModelGroup.Predefined.Groq()

          code.startsWith("llama", true) -> {
            ModelGroup.Predefined.Llama(serviceProvider as ChatServiceProvider)
          }

          code.startsWith("gemma", true) -> {
            ModelGroup.Predefined.Gemma(serviceProvider as ChatServiceProvider)
          }

          // TODO: only support single user message?
  //      code.startsWith("llava", true) -> {
  //        ModelGroup.Predefined.LlaVA(serviceProvider)
  //      }

          code.startsWith("deepseek", true) -> {
            ModelGroup.Predefined.DeepSeek()
          }

          else -> ModelGroup.Custom(serviceProvider as ChatServiceProvider)
        }
      }
      else -> ModelGroup.Custom(ChatServiceProvider.OFFICIAL)
    }

  val iconId: Int
    @DrawableRes
    get() = when (serviceProvider) {
      is CustomChatServiceProvider -> {
        // Try to get model-specific icon first
        com.aigroup.aigroupmobile.utils.ModelIconUtils.getCustomModelIcon(code)
          ?: serviceProvider.logoIconId
      }
      else -> modelGroup.iconId
    }

  val contentColor: Color
    get() = when (serviceProvider) {
      is CustomChatServiceProvider -> Color.Unspecified
      else -> when (modelGroup) {
        is ModelGroup.Predefined.Taichu -> Color.Unspecified
        else -> Color.White
      }
    }

  val tintColor: Color
    get() = backColor ?: when (serviceProvider) {
      is CustomChatServiceProvider -> serviceProvider.backColor ?: Color(0xFF474747)
      else -> modelGroup.tintColor
    }

  val backColor: Color?
    get() = when (serviceProvider) {
      is CustomChatServiceProvider -> serviceProvider.backColor
      else -> {
        val backColor = modelGroup.backColor
        backColor ?: when (modelGroup) {
          is ModelGroup.Predefined.OpenAI -> {
            when {
              isOpenAIReasoning(code) -> Color(0xFF265DE6)
              isGpt4O(code) -> Color.Black
              isGpt4(code) -> Color(0xFF8872E2)
              else -> Color(0xFF39C252)
            }
          }

          else -> null
        }
      }
    }

  val description: String
    @Composable
    get() = when (serviceProvider) {
      is CustomChatServiceProvider -> stringResource(R.string.label_llm_model_custom_group_desc_simple, code)
      else -> when (modelGroup) {
        is ModelGroup.Custom -> stringResource(R.string.label_llm_model_custom_group_desc_simple, code)
        else -> stringResource(R.string.label_llm_model_group_desc_simple, modelGroup.displayName, code)
      }
    }

  val supportStream: Boolean
    get() = when (serviceProvider) {
      is CustomChatServiceProvider -> true // 假设自定义提供商支持流式响应
      else -> {
        val isO1 = isOpenAIReasoning(toString())
        !isO1
      }
    }

  val supportImage: Boolean
    get() = supportImage(code, serviceProvider)

  val supportVideo: Boolean
    get() = supportVideo(code, serviceProvider)

  val contextStr: String?
    get() = modelContext(code, serviceProvider)
}

private val ModelGroup.tintColor: Color
  // TODO: support gradient here?
  get() = when (this) {
    is ModelGroup.Predefined.OpenAI -> Color(0xFF39C252)
    is ModelGroup.Predefined.Google -> Color(0xFF79B7D5)
    is ModelGroup.Predefined.Anthropic -> Color(0xFFCC9B7A)
    is ModelGroup.Predefined.Alibaba -> Color(0xFF605BEC)
    is ModelGroup.Predefined.Llama -> Color(0xFF368EE2)
    is ModelGroup.Predefined.Glm -> Color(0xFF213CB9)
    is ModelGroup.Predefined.Moonshot -> Color(0xFF1A1D22)
    is ModelGroup.Predefined.Stepfun -> Color(0xFF005AFF)
    is ModelGroup.Predefined.Baichuan -> Color(0xFFFF6933)
    is ModelGroup.Predefined.Taichu -> Color(0xFFFFFFFF)
    is ModelGroup.Predefined.Yi -> Color(0xFF003425)
    is ModelGroup.Predefined.Brain360 -> Color(0xFF006ffb)
    is ModelGroup.Predefined.Spark -> Color(0xFF0070f0)
    is ModelGroup.Predefined.Ernie -> Color(0xFF167ADF)
    is ModelGroup.Custom -> Color(0xFF474747)
    is ModelGroup.Predefined.Mistral -> Color(0xfffd6f00)
    is ModelGroup.Predefined.Perplexity -> Color(0xFF22B8CD)
    is ModelGroup.Predefined.Groq -> Color(0xFFF55036)
    is ModelGroup.Predefined.Gemma -> Color(0xFF2E96FF)
    is ModelGroup.Predefined.LlaVA -> Color(0xFFCB2D30)
    is ModelGroup.Predefined.DeepSeek -> Color(0xFF4D6BFE)
  }

private val ModelGroup.backColor: Color?
  get() = when (this) {
    is ModelGroup.Predefined.Anthropic -> tintColor
    is ModelGroup.Predefined.Moonshot -> tintColor
    is ModelGroup.Predefined.Yi -> tintColor
    is ModelGroup.Custom -> tintColor
    else -> null
  }

private val ModelGroup.iconId: Int
  @DrawableRes
  get() = when (this) {
    is ModelGroup.Predefined.OpenAI -> R.drawable.ic_openai_icon
    is ModelGroup.Predefined.Google -> R.drawable.ic_gemini_icon
    is ModelGroup.Predefined.Anthropic -> R.drawable.ic_claude_icon
    is ModelGroup.Predefined.Alibaba -> R.drawable.ic_qwen_icon
    is ModelGroup.Predefined.Llama -> R.drawable.ic_llama_icon
    is ModelGroup.Predefined.Glm -> R.drawable.ic_glm_icon
    is ModelGroup.Predefined.Moonshot -> R.drawable.ic_moonshot_icon
    is ModelGroup.Custom -> R.drawable.ic_custom_bot_icon
    is ModelGroup.Predefined.Stepfun -> R.drawable.ic_stepfun_icon
    is ModelGroup.Predefined.Baichuan -> R.drawable.ic_baichuan_icon
    is ModelGroup.Predefined.Taichu -> R.drawable.ic_aimass_icon
    is ModelGroup.Predefined.Yi -> R.drawable.ic_lingyiwanwu_icon
    is ModelGroup.Predefined.Brain360 -> R.drawable.ic_brain360_icon
    is ModelGroup.Predefined.Spark -> R.drawable.ic_xunfei_icon
    is ModelGroup.Predefined.Ernie -> R.drawable.ic_ernie_icon
    is ModelGroup.Predefined.Mistral -> R.drawable.ic_mistral_icon
    is ModelGroup.Predefined.Perplexity -> R.drawable.ic_perplexity_icon
    is ModelGroup.Predefined.Groq -> R.drawable.ic_groq_icon
    is ModelGroup.Predefined.Gemma -> R.drawable.ic_gemma_icon
    is ModelGroup.Predefined.LlaVA -> R.drawable.ic_llava_icon
    is ModelGroup.Predefined.DeepSeek -> R.drawable.ic_deepseek_icon
  }

private fun isGpt4O(code: String): Boolean {
  return code.split('-').joinToString("").startsWith("gpt4o", true)
}

private fun isOpenAIReasoning(code: String): Boolean {
  val models = setOf("o1", "o3")

  return code.split('-').joinToString("").let {
    models.any { model -> it.startsWith(model, true) }
  }
}

private fun isGpt4(code: String): Boolean {
  if (isGpt4O(code)) {
    return false
  }
  return code.split('-').joinToString("").startsWith("gpt4", true)
}

private fun supportVideo(code: String, provider: ServiceProvider? = null): Boolean {
  // 检查是否是自定义提供商
  if (provider is CustomChatServiceProvider) {
    // 对于自定义提供商，我们需要从 CustomLLMModel 中获取信息
    // 但由于这里无法直接访问 CustomLLMModel，我们暂时返回 false
    // 实际应用中，应该通过 CustomLLMProviderRepository 获取模型信息
    return false
  }
  
  // 检查是否是自定义模型
  if (CustomChatServiceProvider.isCustomProviderId(code.split(':').firstOrNull() ?: "")) {
    // 对于自定义模型，我们需要从 CustomLLMModel 中获取信息
    return false
  }
  
  return getRawInfoOfModelCode(code)?.let {
    return it.supportVideo
  } ?: false
}

private fun supportImage(code: String, provider: ServiceProvider? = null): Boolean {
  // 检查是否是自定义提供商
  if (provider is CustomChatServiceProvider) {
    // 对于自定义提供商，我们需要从 CustomLLMModel 中获取信息
    // 但由于这里无法直接访问 CustomLLMModel，我们暂时返回 false
    // 实际应用中，应该通过 CustomLLMProviderRepository 获取模型信息
    return false
  }
  
  // 检查是否是自定义模型
  if (CustomChatServiceProvider.isCustomProviderId(code.split(':').firstOrNull() ?: "")) {
    // 对于自定义模型，我们需要从 CustomLLMModel 中获取信息
    return false
  }
  
  return getRawInfoOfModelCode(code)?.let {
    return it.supportVision
  } ?: false
}

private fun modelContext(code: String, provider: ServiceProvider? = null): String? {
  // 检查是否是自定义提供商
  if (provider is CustomChatServiceProvider) {
    // 对于自定义提供商，我们需要从 CustomLLMModel 中获取信息
    // 但由于这里无法直接访问 CustomLLMModel，我们暂时返回 null
    // 实际应用中，应该通过 CustomLLMProviderRepository 获取模型信息
    return null
  }
  
  // 检查是否是自定义模型
  if (CustomChatServiceProvider.isCustomProviderId(code.split(':').firstOrNull() ?: "")) {
    // 对于自定义模型，我们需要从 CustomLLMModel 中获取信息
    return null
  }
  
  return getRawInfoOfModelCode(code)?.context?.let {
    when {
      it >= 1000000 -> "${it / 1000000}M"
      it >= 1000 -> "${it / 1000}k"
      else -> it.toString()
    }
  }
}
