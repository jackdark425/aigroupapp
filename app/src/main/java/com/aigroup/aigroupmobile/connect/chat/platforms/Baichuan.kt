package com.aigroup.aigroupmobile.connect.chat.platforms

import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.Models

class BaichuanModels: Models {
  override suspend fun model(modelId: ModelId, requestOptions: RequestOptions?): Model {
    val models = models(requestOptions)
    return models.find { it.id == modelId } ?: error("Model not found")
  }

  override suspend fun models(requestOptions: RequestOptions?): List<Model> {
    return listOf(
      Model(ModelId("Baichuan4"), null, "baichuan"),
      Model(ModelId("Baichuan4-Turbo"), null, "baichuan"),
      Model(ModelId("Baichuan4-Air"), null, "baichuan"),
      Model(ModelId("Baichuan3-Turbo"), null, "baichuan"),
      Model(ModelId("Baichuan3-Turbo-128k"), null, "baichuan"),
      Model(ModelId("Baichuan2-Turbo"), null, "baichuan"),
    )
  }
}
