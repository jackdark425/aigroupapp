package com.aigroup.aigroupmobile.utils.common

import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun ImageMediaItem.readAsBase64(): String {
  return readImageAsBase64(this)
}

@OptIn(ExperimentalEncodingApi::class)
private fun readImageAsBase64(image: ImageMediaItem): String {
  // TODO: using okio
  // TODO: database use correct mime type
  val bytes = image.file.inputStream().readBytes()
  val base64Str = Base64.Default.encode(bytes)
  val mime = guessImageTypeFromBase64(base64Str)
  return "data:${mime};base64,$base64Str"
}

// TODO: refactor, to utils
private val imageTypeMap = mapOf(
  '/' to "image/jpeg",
  'i' to "image/png",
  'R' to "image/gif",
  'U' to "image/webp",
  'Q' to "image/bmp"
)

private fun guessImageTypeFromBase64(str: String): String {
  val defaultType = "image/jpeg"
  if (str.isEmpty()) return defaultType

  val firstChar = str.first()
  return imageTypeMap[firstChar] ?: defaultType
}
