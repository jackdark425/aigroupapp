package com.aigroup.aigroupmobile.ui.pages.chat.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraBarContext(contextIncludeCount: Int?, onContextIncludeCountChange: (Int?) -> Unit) {
  Box(
    Modifier
      .padding(horizontal = 12.dp, vertical = 12.dp)
      .clip(MaterialTheme.shapes.large)
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .padding(vertical = 5.dp)
  ) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        val value = contextIncludeCount?.toString() ?: "1"
        BasicTextField(
          value = value,
          onValueChange = {
            val maxHistory = max(it.toIntOrNull() ?: 1, 1)
            onContextIncludeCountChange(maxHistory)
          },
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppCustomTheme.colorScheme.primaryLabel),
          modifier = Modifier
            .height(36.dp)
            .width(70.dp),
          singleLine = true,
          // 限制键盘类型
          keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number
          ),
        ) { innerTextField ->
          TextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = contextIncludeCount != null,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = remember { MutableInteractionSource() },
            contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
              top = 0.dp,
              bottom = 0.dp
            ),
            shape = MaterialTheme.shapes.medium,
            colors = TextFieldDefaults.colors(
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              disabledIndicatorColor = Color.Transparent,
              errorIndicatorColor = Color.Transparent,
              cursorColor = AppCustomTheme.colorScheme.primaryAction,

              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
          )
        }

        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(stringResource(R.string.label_no_limit_llm_context), style = MaterialTheme.typography.bodySmall, color = AppCustomTheme.colorScheme.primaryLabel)
          Spacer(Modifier.width(3.dp))
          Switch(
            contextIncludeCount == null,
            onCheckedChange = {
              onContextIncludeCountChange(if (it) null else 1)
            },
            colors = SwitchDefaults.colors(
              checkedTrackColor = AppCustomTheme.colorScheme.primaryAction,
            ),
            modifier = Modifier.scale(0.7f)
          )
        }
      }
      Spacer(modifier = Modifier.size(8.dp))

      Slider(
        value = contextIncludeCount?.toFloat() ?: 1f,
        onValueChange = {
          onContextIncludeCountChange(max(it.roundToInt(), 1))
        },
        enabled = contextIncludeCount != null,
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
        steps = 20,
        valueRange = 1f..50f
      )

      Spacer(modifier = Modifier.size(8.dp))
      Text(
        stringResource(R.string.label_desc_set_llm_context),
        style = MaterialTheme.typography.bodySmall,
        color = AppCustomTheme.colorScheme.secondaryLabel
      )
    }
  }
}

@Preview
@Composable
fun ExtraBarContextPreview() {
  AppCustomTheme {
    ExtraBarContext(5, {})
  }
}