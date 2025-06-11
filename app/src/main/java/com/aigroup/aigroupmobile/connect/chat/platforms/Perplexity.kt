package com.aigroup.aigroupmobile.connect.chat.platforms

import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.Models

class PerplexityModels: Models {
  companion object {
    val models = listOf(
      Model(ModelId("llama-3.1-sonar-small-128k-online"), null, "perplexity"),
      Model(ModelId("llama-3.1-sonar-large-128k-online"), null, "perplexity"),
      Model(ModelId("llama-3.1-sonar-huge-128k-online"), null, "perplexity"),
      Model(ModelId("llama-3.1-sonar-small-128k-chat"), null, "perplexity"),
      Model(ModelId("llama-3.1-sonar-large-128k-chat"), null, "perplexity"),
    )
  }

  override suspend fun model(modelId: ModelId, requestOptions: RequestOptions?): Model {
    val models = XunfeiModels.models
    return models.find { it.id == modelId } ?: error("Model not found")
  }

  override suspend fun models(requestOptions: RequestOptions?): List<Model> {
    return models
  }
}
