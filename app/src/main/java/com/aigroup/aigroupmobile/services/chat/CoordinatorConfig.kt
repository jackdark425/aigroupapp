package com.aigroup.aigroupmobile.services.chat

import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.data.models.knowledge.KnowledgeBase
import kotlin.reflect.KClass

sealed interface ChatModelCode {
  /**
   * 让调用方自行指定模型
   */
  data object Unspecified: ChatModelCode

  /**
   * 使用固定模型
   */
  data class Fixed(val model: ModelCode): ChatModelCode

  /**
   * 使用单一聊天上下文。且使用固定模型
   */
  data class SingleContextFixed(val model: ModelCode): ChatModelCode
}

/**
 * ## 媒体内容的后备采用策略
 *
 * 当主 LLM 不支持媒体内容时采用的策略
 */
enum class MediaCompatibilityHistory {
  /**
   * 使用 help text 或者 snapshot 辅助
   */
  HELP_PROMPT,

  /**
   * 跳过上下文中的媒体内容
   */
  SKIP;
}

/**
 * The configuration of the chat coordinator.
 */
data class CoordinatorConfig(
  /**
   * The maximum count of the chat history using in single llm api request or local invocation.
   */
  val maxHistoryCount: Int? = null,

  // TODO: 没有和 mediaCompatibilityHistory 统一
  val mediaRecognizeStrategy: Map<KClass<out MediaItem>, ChatModelCode> = emptyMap(),

  val preferredStreamResponse: Boolean = true,

  val mediaCompatibilityHistory: MediaCompatibilityHistory = MediaCompatibilityHistory.HELP_PROMPT,

  val fixedModel: ModelCode? = null,

  val knowledgeBase: KnowledgeBase? = null,
)