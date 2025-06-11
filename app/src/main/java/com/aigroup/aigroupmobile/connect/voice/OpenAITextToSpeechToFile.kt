package com.aigroup.aigroupmobile.connect.voice

import android.content.Context
import com.aallam.openai.api.audio.SpeechRequest
import com.aallam.openai.api.audio.Voice
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class OpenAIVoice(val displayName: String, val code: String) {
  Alloy("Alloy", "alloy"),
  Echo("Echo", "echo"),
  Fable("Fable", "fable"),
  Onyx("Onyx", "onyx"),
  Nova("Nova", "nova"),
  Shimmer("Shimmer", "shimmer"),
}

// TODO: report error like net timeout to toast

class OpenAITextToSpeechToFile(
  private val context: Context,
  private val ai: OpenAI
) : TextToSpeechToFile {
  override suspend fun speakToFile(text: String, voiceVariant: String, file: File) = withContext(Dispatchers.IO) {
    val response = ai.speech(
      SpeechRequest(
        model = ModelId("tts-1"),
        input = text,
        voice = Voice(voiceVariant),
      )
    )
    file.writeBytes(response)
  }
}