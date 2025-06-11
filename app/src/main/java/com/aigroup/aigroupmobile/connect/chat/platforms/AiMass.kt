package com.aigroup.aigroupmobile.connect.chat.platforms

import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.Models

class AiMassModels: Models {
  override suspend fun model(modelId: ModelId, requestOptions: RequestOptions?): Model {
    val models = models(requestOptions)
    return models.find { it.id == modelId } ?: error("Model not found")
  }

  override suspend fun models(requestOptions: RequestOptions?): List<Model> {
    return listOf(
      Model(ModelId("taichu_llm"), null, "aimass"),
    )
  }
}
