package com.aigroup.aigroupmobile.services

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aigroup.aigroupmobile.BuildConfig
import com.microsoft.cognitiveservices.speech.CancellationDetails
import com.microsoft.cognitiveservices.speech.CancellationReason
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SessionEventArgs
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognitionCanceledEventArgs
import com.microsoft.cognitiveservices.speech.SpeechRecognitionEventArgs
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig


interface SpeechRecognitionState {
  val recognizedText: String
  val recognizingText: String?
  val isRecognizing: Boolean
  val result: String
  fun start()
  fun stop()
  fun toggle()
}

private class MutableSpeechRecognitionState(
  val recognition: SpeechRecognition
): SpeechRecognitionState {
  override var recognizedText by mutableStateOf<String>("")
  override var recognizingText by mutableStateOf<String?>(null)
  override var isRecognizing by mutableStateOf(false)
  override var result by mutableStateOf("")

  override fun toString(): String {
    return "MutableSpeechRecognitionState(recognizedText='$recognizedText', recognizingText='$recognizingText', isRecognizing=$isRecognizing)"
  }

  override fun start() {
    isRecognizing = true
    result = ""
    recognition.recognizeMicrophone(
      onRecognizing = {
        recognizingText = it
      },
      onRecognized = {
        recognizedText += it
        result += it
        recognizingText = null
      },
      onStop = {
        isRecognizing = false
        recognizedText = ""
        recognizingText = null
      }
    )
  }

  override fun stop() {
    recognition.stopRecognition()
    // TODO: need this as onStop will do these. and check the fullInputText Logic in ChatBottomBar
    isRecognizing = false
    recognizedText = ""
    recognizingText = null
  }

  override fun toggle() {
    if (isRecognizing) {
      stop()
    } else {
      start()
    }
  }
}

@Composable
fun rememberSecretSpeechRecognitionState(): SpeechRecognitionState {
  // TODO: place in secrets
  return rememberSpeechRecognitionState(BuildConfig.azureApiKey, BuildConfig.azureRegion)
}

@Composable
internal fun rememberSpeechRecognitionState(
  speechKey: String,
  speechRegion: String
): SpeechRecognitionState {
  val state = remember(speechKey, speechRegion) {
    MutableSpeechRecognitionState(
      SpeechRecognition(speechKey, speechRegion)
    )
  }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    lifecycleOwner.lifecycle.addObserver(state.recognition)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(state.recognition)
    }
  }

  return state
}

class SpeechRecognition(
  private val speechKey: String,
  private val speechRegion: String
) : DefaultLifecycleObserver {

  companion object {
    private const val TAG = "SpeechRecognition"
  }

  inner class Recognition(
    val recognizer: SpeechRecognizer,
    val speechConfig: SpeechConfig,
    val audioConfig: AudioConfig
  ) {
    fun stopAndClose() {
      recognizer.stopContinuousRecognitionAsync().get()
      recognizer.close()
      speechConfig.close()
      audioConfig.close()
    }
  }

  private var recognition: Recognition? = null

  fun recognizeMicrophone(
    onRecognizing: (String) -> Unit = {},
    onRecognized: (String) -> Unit = {},
    onStop: () -> Unit = {}
  ) {
    if (recognition != null) {
      recognition!!.stopAndClose()
    }
    Log.i(TAG, "开始语音识别...")

    val speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion).also {
      it.speechRecognitionLanguage = "zh-CN"
    }

    val audioConfig = AudioConfig.fromDefaultMicrophoneInput()
    val speechRecognizer = SpeechRecognizer(speechConfig, audioConfig)

    speechRecognizer.recognizing.addEventListener { s: Any?, e: SpeechRecognitionEventArgs ->
      onRecognizing(e.result.text)
      Log.i(TAG, "RECOGNIZING: Text=" + e.result.text)
    }

    speechRecognizer.recognized.addEventListener { s: Any?, e: SpeechRecognitionEventArgs ->
      if (e.result.reason == ResultReason.RecognizedSpeech) {
        onRecognized(e.result.text)
        Log.i(TAG, "RECOGNIZED: Text=" + e.result.text)
      } else if (e.result.reason == ResultReason.NoMatch) {
        Log.w(TAG, "NO-MATCH: Speech could not be recognized.")
      }
    }

    // TODO: 错误显示在界面上
    speechRecognizer.canceled.addEventListener { s: Any?, e: SpeechRecognitionCanceledEventArgs ->
      val cancellation = CancellationDetails.fromResult(e.result)
      Log.i(TAG, "CANCELED: Reason=" + cancellation.reason)

      if (cancellation.reason == CancellationReason.Error) {
        Log.e(TAG , "CANCELED: ErrorCode=" + cancellation.errorCode)
        Log.e(TAG, "CANCELED: ErrorDetails=" + cancellation.errorDetails)
        Log.e(TAG, "CANCELED: Did you set the speech resource key and region values?")
      }
    }

//    speechRecognizer.sessionStarted.addEventListener { s: Any?, e: SessionEventArgs? ->
//      println("\n    Session started event.")
//    }

    speechRecognizer.sessionStopped.addEventListener { s: Any?, e: SessionEventArgs? ->
      Log.i(TAG, "Session stopped event.")
      onStop()
    }

    speechRecognizer.startContinuousRecognitionAsync().get()

    recognition = Recognition(speechRecognizer, speechConfig, audioConfig)
  }

  fun stopRecognition() {
    if (recognition != null) {
      recognition?.stopAndClose()
      recognition = null
      Log.i(TAG, "已经停止语音识别")
    }
  }

  override fun onStop(owner: LifecycleOwner) {
    super.onStop(owner)
//    println("onStop called")
    stopRecognition()
  }

}
