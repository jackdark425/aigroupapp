@file:OptIn(
  ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class,
  ExperimentalMaterial3Api::class
)

package com.aigroup.aigroupmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Check
import compose.icons.fontawesomeicons.solid.Moon
import compose.icons.fontawesomeicons.solid.Sun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
private fun SettingsSelectionItem(
  label: String,
  icon: ImageVector? = null,
  tintColor: Color = LocalContentColor.current,
  iconSize: Dp = 20.dp,
  selected: Boolean  = false,
  onClick: () -> Unit = {}
) {
  Row(
    Modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.medium)
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (icon != null) {
      Icon(icon, label, modifier = Modifier.size(iconSize), tint = tintColor)
    }
    Spacer(Modifier.width(12.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Normal
    )
    Spacer(Modifier.weight(1f))
    if (selected) {
      // TODO: do this
//      Icon(
//        imageVector = FontAwesomeIcons.Solid.Check,
//        contentDescription = "Selected",
//        tint = AppCustomTheme.colorScheme.primaryAction,
//        modifier = Modifier.size(15.dp)
//      )
    }
  }
}

class SettingItemSelectionScope internal constructor(
  private val state: SheetState,
  private val coroutineScope: CoroutineScope,
  val onDismiss: () -> Unit = {}
) {

  @Composable
  fun createItem(
    label: String,
    icon: ImageVector? = null,
    tintColor: Color = LocalContentColor.current,
    iconSize: Dp = 20.dp,
    selected: Boolean = false,
    onClick: () -> Unit = {}
  ) {
    SettingsSelectionItem(label, icon, tintColor, iconSize, selected) {
      coroutineScope.launch {
        state.hide()
        onClick()
        onDismiss()
      }
    }
  }

  @Composable
  fun createCustomView(
    content: @Composable () -> Unit
  ) {
    content()
  }

  // TODO: support focus control (click send button to next focus)
  @Composable
  fun createInput(
    label: String = "",
    initialValue: String = "",
    secret: Boolean = false,
    hideOnSubmit: Boolean = false,
    autoFocus: Boolean = false,
    showConfirmButton: Boolean = false,
    onSubmit: (String) -> Unit = {},
  ) {
    var text by remember {
      mutableStateOf(
        TextFieldValue(initialValue, TextRange(initialValue.length))
      )
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
      if (autoFocus) {
        delay(200L)
        focusRequester.requestFocus()
      }
    }

    Row(
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      TextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        placeholder = { Text("") }, // TODO: placeholder not show
        shape = MaterialTheme.shapes.medium,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        colors = TextFieldDefaults.colors(
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
          disabledIndicatorColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions(
          keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text
        ),
        keyboardActions = KeyboardActions(
          onDone = {
            onSubmit(text.text)
            if (hideOnSubmit) {
              defaultKeyboardAction(ImeAction.Done)
              coroutineScope.launch {
                delay(200L)
                state.hide()
                onDismiss()
              }
            }
          }
        ),
        modifier = Modifier
          .focusRequester(focusRequester)
          .weight(1f)
      )

      if (showConfirmButton) {
        Button(
          onClick = {
            onSubmit(text.text)
            if (hideOnSubmit) {
              coroutineScope.launch {
                delay(200L)
                state.hide()
                onDismiss()
              }
            }
          },
          shape = MaterialTheme.shapes.medium,
          colors = ButtonDefaults.buttonColors(
            containerColor = AppCustomTheme.colorScheme.primaryAction,
            contentColor = AppCustomTheme.colorScheme.onPrimaryAction
          ),
          modifier = Modifier.padding(start = 8.dp)
        ) {
          Text(stringResource(R.string.label_confirm))
        }
      }
    }
  }

  @Composable
  fun createSlider(
    label: String,
    value: Float,
    steps: Int = 20,
    range: ClosedFloatingPointRange<Float> = 1f..50f,
    onValueChange: (Float) -> Unit
  ) {
    var thumbValue by remember(value) {
      mutableStateOf(value)
    }

    Column(Modifier.padding(vertical = 10.dp, horizontal = 8.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = AppCustomTheme.colorScheme.secondaryLabel,
        modifier = Modifier.padding(horizontal = 8.dp)
      )
      Slider(
        modifier = Modifier.padding(horizontal = 8.dp),
        value = thumbValue,
        onValueChange = {
          thumbValue = it
        },
        onValueChangeFinished = {
          onValueChange(thumbValue)
        },
        enabled = true,
        thumb = {
          Box(
            Modifier
              .size(width = 10.dp, height = 30.dp)
              .clip(MaterialTheme.shapes.small)
              .background(AppCustomTheme.colorScheme.primaryAction)
          )
        },
        colors = SliderDefaults.colors(
          thumbColor = AppCustomTheme.colorScheme.primaryAction,
          activeTrackColor = AppCustomTheme.colorScheme.primaryAction,
          inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainer,
          inactiveTickColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        steps = steps,
        valueRange = range,
      )
    }
  }

}

@Composable
fun SettingItemSelection(
  onDismiss: () -> Unit = {},
  content: @Composable SettingItemSelectionScope.() -> Unit
) {
  val sheetState = if (LocalInspectionMode.current)
    rememberStandardBottomSheetState(
      skipHiddenState = false
    )
  else
    rememberModalBottomSheetState()

  val coroutineScope = rememberCoroutineScope()
  val contentScope = remember(sheetState, coroutineScope) {
    SettingItemSelectionScope(sheetState, coroutineScope) {
      onDismiss()
    }
  }

  ModalBottomSheet(
    sheetState = sheetState,
    onDismissRequest = onDismiss,
    shape = MaterialTheme.shapes.medium.copy(
      bottomStart = CornerSize(0.dp),
      bottomEnd = CornerSize(0.dp)
    ),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    dragHandle =  {
      BottomSheetDefaults.DragHandle(
        color = MaterialTheme.colorScheme.surfaceContainerHigh
      )
    }
//    windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
  ) {
    Box(
      Modifier
        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
        .padding(horizontal = 8.dp)
        .padding(bottom = 26.dp)
    ) {
      Column {
        contentScope.content()
      }
    }
  }
}

@Preview(showSystemUi = true)
@Composable
fun SettingItemSelectionPreview() {
  val isPreview = LocalInspectionMode.current
  var show by remember { mutableStateOf(isPreview) }
  var sliderValue by remember { mutableStateOf(20f) }

  Scaffold { p ->
    Button({ show = !show }, modifier = Modifier.padding(p)) {
      Text("show")
    }
  }

  if (show) {
    SettingItemSelection(
      onDismiss = { show = false }
    ) {
      createInput("Token", secret = true, hideOnSubmit = true)
      Spacer(Modifier.height(8.dp))
      createSlider("Slider", sliderValue) { sliderValue = it }
      Spacer(Modifier.height(8.dp))
      createItem("Dark Mode", FontAwesomeIcons.Solid.Moon) {}
      Spacer(Modifier.height(8.dp))
      createItem("Light Mode", FontAwesomeIcons.Solid.Sun) {}
      Spacer(Modifier.height(8.dp))
      createItem("Light Mode") {}
    }
  }
}