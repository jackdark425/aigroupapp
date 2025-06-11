package com.aigroup.aigroupmobile.services

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.aigroup.aigroupmobile.BuildConfig
import com.aigroup.aigroupmobile.connect.chat.officialAI
import com.aigroup.aigroupmobile.connect.voice.AndroidTextToSpeech
import com.aigroup.aigroupmobile.connect.voice.AzureTextToSpeechToFile
import com.aigroup.aigroupmobile.connect.voice.GenericTextToSpeech
import com.aigroup.aigroupmobile.connect.voice.OpenAITextToSpeechToFile
import com.aigroup.aigroupmobile.connect.voice.TextToSpeech
import com.aigroup.aigroupmobile.connect.voice.TextToSpeechToFile
import com.aigroup.aigroupmobile.connect.voice.VoiceCode
import com.aigroup.aigroupmobile.connect.voice.VoiceServiceProvider
import com.aigroup.aigroupmobile.dataStore
import com.aigroup.aigroupmobile.utils.common.getCurrentDesignedLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class TextSpeaker(
  private val context: Context,
  // TODO: using [PathManager]?
) {

  companion object {
    const val TAG = "TextSpeaker"
  }

  private var mediaPlayer: MediaPlayer? = null

  /**
   * all speakers except the TextToSpeechToFile
   */
  private var speakers = mutableListOf<TextToSpeech>()

  private suspend fun getTextToSpeech(provider: VoiceServiceProvider): GenericTextToSpeech {
    val locale = context.getCurrentDesignedLocale() // TODO: 考虑不支持的语言但是支持 tts，否则可能效果不正确

    when (provider) {
      VoiceServiceProvider.OPENAI_TTS, VoiceServiceProvider.OPENAI_TTS_HD -> {
        val ai = context.dataStore.data.first().officialAI()
        return OpenAITextToSpeechToFile(context, ai)
      }

      VoiceServiceProvider.AZURE -> {
        val apiKey = BuildConfig.azureApiKey
        val region = BuildConfig.azureRegion
        return AzureTextToSpeechToFile(apiKey, region)
      }

      VoiceServiceProvider.ANDROID -> {
        return AndroidTextToSpeech(context, locale)
      }
    }
  }

  private fun getBaseAudioFile(): File {
    val file = File(context.cacheDir, "tts")
    if (!file.exists()) {
      file.mkdirs()
    }
    return file
  }

  suspend fun speak(text: String, voiceCode: VoiceCode) {
    Log.i(TAG, "speak $text (${voiceCode.fullCode()})")
    withContext(Dispatchers.IO) {
      val speaker = getTextToSpeech(voiceCode.serviceProvider)

      try {

        when (speaker) {
          is TextToSpeechToFile -> {
            val fileName = UUID.randomUUID().toString() + ".wav" // TODO: seems openai default to mp3?
            val file = File(getBaseAudioFile(), fileName)
            speaker.speakToFile(text, voiceCode.variant, file)
            speakFile(file)
          }
          is TextToSpeech -> {
            releaseAllSpeakers()
            speakers.add(speaker)
            speaker.speak(text, voiceCode.variant)
          }
        }

      } catch (e: Exception) {
        Log.e(TAG, "Failed to speak text: $text", e)
      }
    }
  }

  /**
   * Release all speakers except the TextToSpeechToFile
   */
  private suspend fun releaseAllSpeakers() {
    speakers.forEach {
      it.stop()
    }
    speakers.clear()
  }

  private fun speakFile(file: File) {
    if (mediaPlayer?.isPlaying == true) {
      mediaPlayer!!.stop()
      mediaPlayer!!.reset()
    }

    if (mediaPlayer == null) {
      mediaPlayer = MediaPlayer()
    }

    try {
      Log.i(TAG, "Playing audio file: ${file.path}")
      mediaPlayer!!.setDataSource(file.path)
      mediaPlayer!!.prepare()
      mediaPlayer!!.start()

      mediaPlayer!!.setOnCompletionListener {
        Log.i(TAG, "Playing done, clear media player")
        mediaPlayer = null
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}

