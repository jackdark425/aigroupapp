package com.aigroup.aigroupmobile.connect.chat.platforms

import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.Models

class XunfeiModels: Models {
  companion object {
    val models = listOf(
      Model(ModelId("general"), null, "xunfei"),
      Model(ModelId("generalv3"), null, "xunfei"),
      Model(ModelId("pro-128k"), null, "xunfei"),
      Model(ModelId("generalv3.5"), null, "xunfei"),
      Model(ModelId("max-32k"), null, "xunfei"),
      Model(ModelId("4.0Ultra"), null, "xunfei"),
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
