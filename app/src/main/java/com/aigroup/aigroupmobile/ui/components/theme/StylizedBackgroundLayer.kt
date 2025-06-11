package com.aigroup.aigroupmobile.ui.components.theme

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.aigroup.aigroupmobile.data.extensions.availableInDarkMode
import com.aigroup.aigroupmobile.data.extensions.availableInLightMode
import com.aigroup.aigroupmobile.data.extensions.preferredTheme
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.ui.theme.LocalColorScheme
import com.aigroup.aigroupmobile.ui.theme.LocalUiMode
import com.aigroup.aigroupmobile.ui.theme.UiMode

@Composable
fun StylizedBackgroundLayer(
  colorScheme: AppPreferences.ColorScheme = LocalColorScheme.current,
  variant: Int = 1,
  @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
  val uiMode by LocalUiMode.current
  if (uiMode == UiMode.Dark && !colorScheme.availableInDarkMode) {
    return
  }
  if (uiMode == UiMode.Default && !colorScheme.availableInLightMode) {
    return
  }

  // TODO: using animation
  when (colorScheme) {
    AppPreferences.ColorScheme.PLAIN -> {
    }

    AppPreferences.ColorScheme.CLASSIC -> {
      when (variant) {
        1 -> GradientLayer(modifier)
        2 -> GradientLayer2(modifier)
        else -> {}
      }
    }

    AppPreferences.ColorScheme.COLORFUL -> {
      when (variant) {
        1 -> ColorfulGradientLayer(modifier = modifier)
        2 -> ColorfulGradientLayer(modifier = modifier, animation = false)
        else -> {}
      }
    }

    else -> {}
  }
}