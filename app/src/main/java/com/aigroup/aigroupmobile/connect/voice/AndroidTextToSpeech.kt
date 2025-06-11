package com.aigroup.aigroupmobile.connect.voice

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import java.util.Locale

enum class AndroidSystemVoice(val displayName: String, val code: String) {
  Default("Default", "default"),
}

private typealias AndroidTTS = android.speech.tts.TextToSpeech

// see https://android-developers.googleblog.com/2009/09/introduction-to-text-to-speech-in.html

class AndroidTextToSpeech(
  private val context: Context,
  private val locale: Locale,
) : TextToSpeech {

  companion object {
    const val TAG = "AndroidTextToSpeech"
  }

  private lateinit var speaker: android.speech.tts.TextToSpeech

  override suspend fun speak(text: String, voiceVariant: String) {
    // see https://enderhoshi.github.io/2019/11/25/Android%20TextToSpeech%20%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%97/
    // Vivo TTS 似乎没有成功应用系统 tts 设置
    // 捕获错误，所有 speaker 适配长文字

    speaker = AndroidTTS(context) { status ->
      if (status == AndroidTTS.SUCCESS) {
        Log.d(TAG, "Initialization Success")
      } else {
        Log.d(TAG, "Initialization Failed")
      }
      speaker.language = locale
      speaker.speak(text, AndroidTTS.QUEUE_FLUSH, null, null)
    }
  }

  override suspend fun stop() {
    speaker.stop()
  }
}