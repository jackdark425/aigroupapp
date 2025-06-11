package com.aigroup.aigroupmobile.ui.utils

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

@Composable
fun animateColorSchemeAsState(
  target: ColorScheme,
): ColorScheme {
  val animationSpec = remember {
    spring<Color>(stiffness = 500f)
  }
  return ColorScheme(
    primary = animateColorAsState(target.primary, animationSpec).value,
    onPrimary = animateColorAsState(target.onPrimary, animationSpec).value,
    primaryContainer = animateColorAsState(target.primaryContainer, animationSpec).value,
    onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, animationSpec).value,
    inversePrimary = animateColorAsState(target.inversePrimary, animationSpec).value,
    secondary = animateColorAsState(target.secondary, animationSpec).value,
    onSecondary = animateColorAsState(target.onSecondary, animationSpec).value,
    secondaryContainer = animateColorAsState(target.secondaryContainer, animationSpec).value,
    onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, animationSpec).value,
    tertiary = animateColorAsState(target.tertiary, animationSpec).value,
    onTertiary = animateColorAsState(target.onTertiary, animationSpec).value,
    tertiaryContainer = animateColorAsState(target.tertiaryContainer, animationSpec).value,
    onTertiaryContainer = animateColorAsState(target.onTertiaryContainer, animationSpec).value,
    background = animateColorAsState(target.background, animationSpec).value,
    onBackground = animateColorAsState(target.onBackground, animationSpec).value,
    surface = animateColorAsState(target.surface, animationSpec).value,
    onSurface = animateColorAsState(target.onSurface, animationSpec).value,
    surfaceVariant = animateColorAsState(target.surfaceVariant, animationSpec).value,
    onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, animationSpec).value,
    surfaceTint = animateColorAsState(target.surfaceTint, animationSpec).value,
    inverseSurface = animateColorAsState(target.inverseSurface, animationSpec).value,
    inverseOnSurface = animateColorAsState(target.inverseOnSurface, animationSpec).value,
    error = animateColorAsState(target.error, animationSpec).value,
    onError = animateColorAsState(target.onError, animationSpec).value,
    errorContainer = animateColorAsState(target.errorContainer, animationSpec).value,
    onErrorContainer = animateColorAsState(target.onErrorContainer, animationSpec).value,
    outline = animateColorAsState(target.outline, animationSpec).value,
    outlineVariant = animateColorAsState(target.outlineVariant, animationSpec).value,
    scrim = animateColorAsState(target.scrim, animationSpec).value,
    surfaceBright = animateColorAsState(target.surfaceBright, animationSpec).value,
    surfaceDim = animateColorAsState(target.surfaceDim, animationSpec).value,
    surfaceContainer = animateColorAsState(target.surfaceContainer, animationSpec).value,
    surfaceContainerHigh = animateColorAsState(target.surfaceContainerHigh, animationSpec).value,
    surfaceContainerHighest = animateColorAsState(target.surfaceContainerHighest, animationSpec).value,
    surfaceContainerLow = animateColorAsState(target.surfaceContainerLow, animationSpec).value,
    surfaceContainerLowest = animateColorAsState(target.surfaceContainerLowest, animationSpec).value,
  )
}