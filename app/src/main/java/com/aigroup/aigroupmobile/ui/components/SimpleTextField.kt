package com.aigroup.aigroupmobile.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.ui.utils.clearFocusOnKeyboardDismiss

/**
 * A simple text field component with custom styling and behavior.
 *
 * @param value The current text value
 * @param onValueChange Callback when the text value changes
 * @param placeholder Optional placeholder text
 * @param modifier Optional modifier for the text field
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTextField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String = "",
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
  modifier: Modifier = Modifier
) {
  BasicTextField(
    value = value,
    onValueChange = onValueChange,
    textStyle = MaterialTheme.typography.bodyMedium.copy(
      color = AppCustomTheme.colorScheme.primaryLabel
    ),
    modifier = modifier
      .height(40.dp)
      .clearFocusOnKeyboardDismiss()
      .imePadding(),
    singleLine = true,
  ) { innerTextField ->
    TextFieldDefaults.DecorationBox(
      value = value,
      innerTextField = innerTextField,
      enabled = true,
      singleLine = true,
      placeholder = {
        Text(
          text = placeholder,
          color = AppCustomTheme.colorScheme.secondaryLabel
        )
      },
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
        unfocusedContainerColor = containerColor,
        focusedContainerColor = containerColor,
      ),
    )
  }
}

@Preview
@Composable
fun SimpleTextFieldPreview() {
  AppCustomTheme {
    SimpleTextField(
      value = "",
      onValueChange = {},
      placeholder = "Placeholder"
    )
  }
}

@Preview
@Composable
fun SimpleTextFieldWithTextPreview() {
  AppCustomTheme {
    SimpleTextField(
      value = "Sample Text",
      onValueChange = {},
    )
  }
}