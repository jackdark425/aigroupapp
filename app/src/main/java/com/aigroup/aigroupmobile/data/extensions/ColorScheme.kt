package com.aigroup.aigroupmobile.data.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.ui.components.theme.ColorfulGradientLayer
import com.aigroup.aigroupmobile.ui.components.theme.GradientLayer
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme

private typealias ColorScheme = AppPreferences.ColorScheme

/**
 * The preferred theme for the color scheme. It means that this color scheme prefers a specific theme
 * and should be used with that theme.
 */
val ColorScheme.preferredTheme: AppPreferences.ThemeMode?
  get() = when (this) {
    AppPreferences.ColorScheme.PLAIN -> null
    AppPreferences.ColorScheme.CLASSIC -> null  // 允许在深色和浅色模式下都可用
    AppPreferences.ColorScheme.COLORFUL -> null  // 允许在深色和浅色模式下都可用
    else -> null
  }

val ColorScheme.availableInDarkMode: Boolean
  get() = this.preferredTheme == AppPreferences.ThemeMode.DARK || this.preferredTheme == null

val ColorScheme.availableInLightMode: Boolean
  get() = this.preferredTheme == AppPreferences.ThemeMode.LIGHT || this.preferredTheme == null

val ColorScheme.label: String
  @Composable
  get() = when (this) {
    AppPreferences.ColorScheme.PLAIN -> stringResource(R.string.label_color_scheme_plain)
    AppPreferences.ColorScheme.CLASSIC -> stringResource(R.string.label_color_scheme_classic)
    AppPreferences.ColorScheme.COLORFUL -> stringResource(R.string.label_color_scheme_colorful)
    else -> ""
  }

@Composable
fun ColorScheme.ColorSchemeIcon(
  size: Dp = 80.dp,
  shape: Shape = MaterialTheme.shapes.medium,
) {
  Box(
    Modifier
      .size(size)
      .border(0.5.dp, AppCustomTheme.colorScheme.tertiaryLabel.copy(0.1f), shape)
      .clip(shape)
  ) {
    when (this@ColorSchemeIcon) {
      ColorScheme.PLAIN -> {
        Box(
          Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        )
      }

      ColorScheme.CLASSIC -> {
        Box(
          Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        ) {
          GradientLayer(
            modifier = Modifier.fillMaxSize(),
            blur = 10.dp
          )
        }
      }

      ColorScheme.COLORFUL -> {
        Box(
          Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        ) {
          ColorfulGradientLayer(
            modifier = Modifier.fillMaxSize(),
            fadeOut = false
          )
        }
      }

      else -> {
        Box(
          Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        ) {
          Text(
            "🤔",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center)
          )
        }
      }
    }

    val preferredTheme = this@ColorSchemeIcon.preferredTheme
    if (preferredTheme != null) {
      Box(
        modifier = Modifier
          .padding(10.dp)
          .align(Alignment.BottomEnd)
      ) {
        Icon(preferredTheme.icon, "", modifier = Modifier.size(10.dp))
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun ColorSchemeIconPreview() {
  AIGroupAppTheme {
    Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ColorScheme.entries.filter {
        it.availableInLightMode
      }.forEach { colorScheme ->
        colorScheme.ColorSchemeIcon()
      }
    }
  }
}

@Preview
@Composable
fun ColorSchemeIconPreviewDark() {
  AIGroupAppTheme(darkTheme = true) {
    Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ColorScheme.entries.filter {
        it.availableInDarkMode
      }.forEach { colorScheme ->
        colorScheme.ColorSchemeIcon()
      }
    }
  }
}