package com.aigroup.aigroupmobile.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.services.SpeechRecognitionState
import com.aigroup.aigroupmobile.utils.previews.rememberPermissionStateSafe
import com.aigroup.aigroupmobile.services.rememberSecretSpeechRecognitionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import compose.icons.CssGgIcons
import compose.icons.cssggicons.Keyboard
import kotlinx.coroutines.launch

private const val TAG = "MicrophoneButton"

@Composable
fun MicrophoneButton(
  modifier: Modifier = Modifier,
  onHide: () -> Unit = {},
  speechRecognitionState: SpeechRecognitionState = rememberSecretSpeechRecognitionState(),
  showExtraInfo: (String?) -> Unit = {},
  onConfirm: (String) -> Unit = {},
) {
  val hapticFeedback = LocalHapticFeedback.current
  val coroutineScope = rememberCoroutineScope()

  var areaSize = remember { DpSize.Zero }
  val density = LocalDensity.current

  val context = LocalContext.current

  fun showNormalExtra() {
    showExtraInfo(context.getString(R.string.label_micro_button_interaction_guide))
  }

  val interactionSource = remember { MutableInteractionSource() }
  val isHolding by interactionSource.collectIsPressedAsState()

  // TODO: 在 SpeechRecognitionState 实现一个 suspend 的 stop 方法
  var waitingForResult by remember { mutableStateOf(false) }

  LaunchedEffect(speechRecognitionState.isRecognizing) {
    if (!speechRecognitionState.isRecognizing && waitingForResult) {
      onConfirm(speechRecognitionState.result)
      waitingForResult = false
    }
  }

  fun onDown() {
    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    showNormalExtra()
    speechRecognitionState.start()
    waitingForResult = true
  }

  fun onDrag(offset: Offset) {
    val dpOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
    if (dpOffset.x <= 0.dp || dpOffset.y <= 0.dp || dpOffset.x >= areaSize.width || dpOffset.y >= areaSize.height) {
      showExtraInfo(context.getString(R.string.label_micro_button_release_cancel))
    } else {
      showNormalExtra()
    }
  }

  fun onUp(offset: Offset) {
    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    showExtraInfo(null)
    speechRecognitionState.stop()

    val dpOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
    if (dpOffset.x <= 0.dp || dpOffset.y <= 0.dp || dpOffset.x >= areaSize.width || dpOffset.y >= areaSize.height) {
      Log.i(TAG, "Canceled")
      waitingForResult = false
    } else {
      // ready to confirm
      Log.i(TAG, "Confirmed")
    }
  }

  Box(
    modifier
      .fillMaxSize()
      .background(AppCustomTheme.colorScheme.primaryAction),
  ) {
    Surface(
      color = AppCustomTheme.colorScheme.secondaryAction,
      modifier = Modifier
        .fillMaxSize()
        .indication(interactionSource, ripple())
        .onGloballyPositioned {
          with(density) {
            areaSize = DpSize(it.size.width.toDp(), it.size.height.toDp())
          }
        }
        .pointerInput(Unit) {
          awaitEachGesture {
            val downEvent = awaitFirstDown()
            Log.i(TAG, "Down: ${downEvent.position}")
            onDown()

            val press = PressInteraction.Press(downEvent.position)
            coroutineScope.launch {
              interactionSource.emit(press)
            }

            while (true) {
              val event = awaitDragOrCancellation(downEvent.id)
              if (event == null) {
                Log.i(TAG, "Drag: null (cancel)")
                coroutineScope.launch {
                  interactionSource.emit(PressInteraction.Cancel(press))
                }
                break
              }
              if (event.changedToUp()) {
                Log.i(TAG, "Up: ${event.position}")
                onUp(event.position)
                coroutineScope.launch {
                  interactionSource.emit(PressInteraction.Release(press))
                }
                break
              }
//              Log.i(TAG, "Drag: ${event.position}")
              onDrag(event.position)
              event.consume()
            }
          }
        },
    ) {
      Box(Modifier.padding(end = 25.dp)) {
        CompositionLocalProvider(LocalContentColor provides AppCustomTheme.colorScheme.onSecondaryAction) {
          CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
            AnimatedContent(isHolding, modifier = Modifier.align(Alignment.Center)) {
              if (it) {
                Text(stringResource(R.string.label_micro_button_recognizing))
              } else {
                Text(stringResource(R.string.label_micro_button_hold_to_speak))
              }
            }
          }
        }
      }
    }

    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 2.dp), horizontalArrangement = Arrangement.End
    ) {
      FilledIconToggleButton(
        checked = false,
        onCheckedChange = {
          onHide()
        },
        modifier = Modifier.padding(vertical = 3.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = IconButtonDefaults.filledIconToggleButtonColors(
          containerColor = AppCustomTheme.colorScheme.primaryAction,
          contentColor = AppCustomTheme.colorScheme.onPrimaryAction,
        )
      ) {
        Icon(
          CssGgIcons.Keyboard,
          "",
          Modifier.size(20.dp),
          tint = LocalContentColor.current
        )
      }
    }
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview(showBackground = true)
@Composable
fun MicrophoneButtonPreview() {
  val audioPermSpeechRecognize =
    rememberPermissionStateSafe(android.Manifest.permission.RECORD_AUDIO) {
      if (it) {
      }
    }

  LaunchedEffect(Unit) {
    audioPermSpeechRecognize.launchPermissionRequest()
  }

  var extraInfo by remember { mutableStateOf<String?>(null) }

  AIGroupAppTheme {
    Column(Modifier.padding(16.dp)) {
      Text(extraInfo ?: "MicrophoneButton")

      Spacer(Modifier.height(20.dp))

      Box(
        Modifier
          .fillMaxWidth()
          .clip(MaterialTheme.shapes.extraLarge)
          .height(40.dp)
      ) {
        MicrophoneButton(
          showExtraInfo = {
            extraInfo = it
          },
          onConfirm = {
            println("Send: $it")
          }
        )
      }
    }
  }
}