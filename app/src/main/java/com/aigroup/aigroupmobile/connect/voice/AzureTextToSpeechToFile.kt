package com.aigroup.aigroupmobile.connect.voice

import android.util.Log
import com.microsoft.cognitiveservices.speech.CancellationReason
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class AzureVoice(val displayName: String, val code: String) {
  YunXi("云希", "zh-CN-YunxiNeural"),
  XiaoXiao("晓晓", "zh-CN-XiaoxiaoNeural"),
  YunYe("云野", "zh-CN-YunyeNeural"),
  XiaoHan("晓涵", "zh-CN-XiaohanNeural"),
  XiaoRui("晓睿", "zh-CN-XiaoruiNeural"),
  XiaoMo("晓墨", "zh-CN-XiaomoNeural"),
  YunXia("云夏", "zh-CN-YunxiaNeural"),
  YunZe("云泽", "zh-CN-YunzeNeural"),
}


// TODO: report error like net timeout to toast
// TODO: directly to speaker

class AzureTextToSpeechToFile(
  private val apiKey: String,
  private val region: String
) : TextToSpeechToFile {
  companion object {
    const val TAG = "AzureTextToSpeech"
  }

  override suspend fun speakToFile(text: String, voiceVariant: String, file: File) = withContext(Dispatchers.IO) {
    val speechConfig = SpeechConfig.fromSubscription(apiKey, region)
    speechConfig.speechSynthesisVoiceName = voiceVariant
    val audioConfig = AudioConfig.fromWavFileOutput(file.path)
    val speechSynthesizer = SpeechSynthesizer(speechConfig, audioConfig)

    val result = speechSynthesizer.SpeakTextAsync(text).get()

    if (result.reason === ResultReason.SynthesizingAudioCompleted) {
      Log.i(TAG, "Speech synthesized for text [$text]")
    } else if (result.reason === ResultReason.Canceled) {
      val cancellation = SpeechSynthesisCancellationDetails.fromResult(result)
      Log.w(TAG, "CANCELED: Reason=" + cancellation.reason)

      if (cancellation.reason == CancellationReason.Error) {
        Log.e(TAG, "CANCELED: ErrorCode=" + cancellation.errorCode)
        Log.e(TAG, "CANCELED: ErrorDetails=" + cancellation.errorDetails)
        Log.e(TAG, "CANCELED: Did you update the subscription info?")
      }
    }

    result.close()
    speechSynthesizer.close()
  }
}