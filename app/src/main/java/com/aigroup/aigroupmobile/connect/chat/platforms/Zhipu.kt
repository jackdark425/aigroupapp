package com.aigroup.aigroupmobile.connect.chat.platforms

import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.Models

class ZhipuModels: Models {
  companion object {
    val models = listOf(
      Model(ModelId("glm-4-plus"), null, "zhipu"),
      Model(ModelId("glm-4-0520"), null, "zhipu"),
      Model(ModelId("glm-4-long"), null, "zhipu"),
      Model(ModelId("glm-4-airx"), null, "zhipu"),
      Model(ModelId("glm-4-air"), null, "zhipu"),
      Model(ModelId("glm-4-flashx"), null, "zhipu"),
      Model(ModelId("glm-4-flash"), null, "zhipu"),
      Model(ModelId("glm-4v"), null, "zhipu"),
      Model(ModelId("glm-4v-plus"), null, "zhipu"),
      Model(ModelId("glm-4"), null, "zhipu"),
      Model(ModelId("glm-4-alltools"), null, "zhipu"),
      Model(ModelId("glm-3-turbo"), null, "zhipu"),
      Model(ModelId("chatglm-3"), null, "zhipu"),
      Model(ModelId("emohaa"), null, "zhipu"),
      Model(ModelId("codegeex-4"), null, "zhipu"),
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
