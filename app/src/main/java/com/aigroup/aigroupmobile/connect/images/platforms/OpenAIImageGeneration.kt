package com.aigroup.aigroupmobile.connect.images.platforms

import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.model.ModelId
import com.aigroup.aigroupmobile.connect.images.ImageGenerator

class OpenAIImageGenerator(
  private val ai: com.aallam.openai.client.Images
): ImageGenerator {
  override suspend fun createImages(
    modelCode: String,
    resolution: String,
    prompt: String,
    count: Int
  ): List<String> {
    return ai.imageURL(
      creation = ImageCreation(
        prompt = prompt,
        model = ModelId(modelCode),
        n = count,
        size = ImageSize(resolution)
      )
    ).map { it.url }
  }
}