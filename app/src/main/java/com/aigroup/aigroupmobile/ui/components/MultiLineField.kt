@file:OptIn(ExperimentalMaterial3Api::class)

package com.aigroup.aigroupmobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale

@Preview(showSystemUi = true)
@Composable
fun ResizableInputFieldPreview() {
  var text by remember { mutableStateOf(TextFieldValue("")) }

  AIGroupAppTheme {
    Scaffold(
      containerColor = AppCustomTheme.colorScheme.groupedBackground
    ) { p ->
      Column(
        Modifier
          .padding(p)
          .padding(16.dp)
      ) {
        Spacer(Modifier.weight(1f))
        MultiLineField(
          value = text,
          onValueChange = { text = it },
          textStyle = MaterialTheme.typography.bodyMedium,
          prefixAction = {
            IconButton(onClick = {}) {
              Text("@", fontWeight = FontWeight.Bold)
            }
          },
          action = {
            IconButton(onClick = {}) {
              Icon(ImageVector.vectorResource(R.drawable.ic_send_fill_icon), "", Modifier.size(20.dp))
            }
          }
        )
      }
    }
  }
}

private val BASE_HEIGHT = 40.dp
private val CONTENT_PADDING_V = 8.dp
private val MAX_HEIGHT = 200.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiLineField(
  modifier: Modifier = Modifier,
  value: TextFieldValue,
  onValueChange: (TextFieldValue) -> Unit = {},
  textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
  placeholder: String = stringResource(R.string.hint_placeholder_textfield_default),
  onTextLayout: (TextLayoutResult) -> Unit = {},
  prefixAction: (@Composable () -> Unit)? = null,
  shape: Shape = MaterialTheme.shapes.extraLarge,
  minHeight: Dp = BASE_HEIGHT,
  innerFieldContentAlignment: Alignment = Alignment.CenterStart, // TODO: 临时补丁
  action: (@Composable () -> Unit)? = null,
) {
  val density = LocalDensity.current

  var textHeight by remember { mutableStateOf(BASE_HEIGHT) }
  var placeholderHeight by remember { mutableStateOf(BASE_HEIGHT) }
  val fieldHeight by remember {
    derivedStateOf {
      val height = max(textHeight, placeholderHeight)
      height + (CONTENT_PADDING_V * 2)
    }
  }

  BasicTextField(
    modifier = modifier
      .heightIn(min = minHeight, max = MAX_HEIGHT)
      .height(fieldHeight),
    value = value,
    onValueChange = onValueChange,
    onTextLayout = {
      with(density) {
        textHeight = min(it.size.height.toDp(), MAX_HEIGHT)
      }
      onTextLayout(it)
    },
    textStyle = textStyle,
  ) {
    TextFieldDefaults.DecorationBox(
      value = value.text,
      innerTextField = {
        // render waring when placeholder take multiple lines TODO
        Box(
          Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
          contentAlignment = innerFieldContentAlignment
        ) {
          it()
        }
      },
      singleLine = false,
      enabled = true,
      visualTransformation = VisualTransformation.None,
      shape = shape,
      colors = TextFieldDefaults.colors(
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
        cursorColor = AppCustomTheme.colorScheme.primaryAction,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      ),
      placeholder = {
        Box(
          Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
          contentAlignment = Alignment.CenterStart
        ) {
          Text(
            text = placeholder,
            style = textStyle,
            color = AppCustomTheme.colorScheme.secondaryLabel,
            onTextLayout = {
              with(density) {
                placeholderHeight = min(it.multiParagraph.height.toDp(), MAX_HEIGHT)
              }
            }
          )
        }
      },
      interactionSource = remember { MutableInteractionSource() },
      contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
        top = CONTENT_PADDING_V, bottom = CONTENT_PADDING_V,
        start = if (prefixAction == null) 15.dp else 6.dp,
      ),
      leadingIcon = prefixAction?.let {
        {
          Box(Modifier.fillMaxHeight()) {
            Box(
              Modifier
                .align(Alignment.BottomEnd)
                .height(BASE_HEIGHT)
                .padding(vertical = 1.dp)
            ) {
              prefixAction()
            }
          }
        }
      },
      trailingIcon = action?.let { action ->
        {
          Box(Modifier.fillMaxHeight()) {
            Box(
              Modifier
                .align(Alignment.BottomEnd)
                .height(BASE_HEIGHT)
                .padding(vertical = 1.dp)
            ) {
              action()
            }
          }
        }
      },
    )
  }
}
