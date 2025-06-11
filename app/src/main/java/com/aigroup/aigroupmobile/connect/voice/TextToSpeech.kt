package com.aigroup.aigroupmobile.connect.voice

import java.io.File

sealed interface GenericTextToSpeech

interface TextToSpeechToFile: GenericTextToSpeech {
  suspend fun speakToFile(text: String, voiceVariant: String, file: File)
}

interface TextToSpeech: GenericTextToSpeech {
  suspend fun speak(text: String, voiceVariant: String)
  suspend fun stop()
}