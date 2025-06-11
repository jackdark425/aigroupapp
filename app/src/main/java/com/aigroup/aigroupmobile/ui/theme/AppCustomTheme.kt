package com.aigroup.aigroupmobile.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val groupedBackgroundLight = Color(0xFFF2F2F7)
val groupedBackgroundDark = Color(0xFF1C1C1E)

val primaryActionLight = Color(0xFF292929)
val onPrimaryActionLight = Color.White
val primaryActionDark = Color(0xFFFEFEFE)
val onPrimaryActionDark = Color.Black

val secondaryActionLight = Color(0xFFFFFFFF)
val onSecondaryActionLight = Color(0xFF000000)
val secondaryActionDark = Color(0xFF31353E)
val onSecondaryActionDark = Color(0xFFFFFFFF)

val primaryLabel = Color(0xFF000000)
val primaryLabelDark = Color(0xFFFFFFFF)
val secondaryLabel = Color(0x993C3C43)
val secondaryLabelDark = Color(0x99EBEBF5)
val tertiaryLabel = Color(0x4D3C3C43)
val tertiaryLabelDark = Color(0x4DEBEBF5)

val tintColor = Color(0xFFFFA463)

data class AppThemeColorScheme(
  val groupedBackground: Color,

  val primaryAction: Color,
  val onPrimaryAction: Color,

  val secondaryAction: Color,
  val onSecondaryAction: Color,

  val primaryLabel: Color,
  val secondaryLabel: Color,
  val tertiaryLabel: Color,

  val tintColor: Color,
) {
  companion object {
    fun light(): AppThemeColorScheme = AppThemeColorScheme(
      groupedBackground = groupedBackgroundLight,
      primaryAction = primaryActionLight,
      onPrimaryAction = onPrimaryActionLight,
      secondaryAction = secondaryActionLight,
      onSecondaryAction = onSecondaryActionLight,
      primaryLabel = primaryLabel,
      secondaryLabel = secondaryLabel,
      tertiaryLabel = tertiaryLabel,
      tintColor = tintColor,
    )

    fun dark(): AppThemeColorScheme = AppThemeColorScheme(
      groupedBackground = groupedBackgroundDark,
      primaryAction = primaryActionDark,
      onPrimaryAction = onPrimaryActionDark,
      secondaryAction = secondaryActionDark,
      onSecondaryAction = onSecondaryActionDark,
      primaryLabel = primaryLabelDark,
      secondaryLabel = secondaryLabelDark,
      tertiaryLabel = tertiaryLabelDark,
      tintColor = tintColor,
    )
  }

  /**
   * Applies the some [ColorScheme] to the [AppThemeColorScheme].
   * This is a helper function for when user using dynamic color from wallpaper (monet-color).
   */
  fun applyMaterialThemeTint(colorScheme: ColorScheme): AppThemeColorScheme {
    return this.copy(
      primaryAction = colorScheme.primaryContainer,
      onPrimaryAction = colorScheme.onPrimaryContainer,
      secondaryAction = colorScheme.surfaceContainerLow,
      onSecondaryAction = colorScheme.onSurface,
    )
  }
}

val LocalAppColorScheme = staticCompositionLocalOf { AppThemeColorScheme.light() }

@Composable
fun AppCustomTheme(
  colorScheme: AppThemeColorScheme = LocalAppColorScheme.current,
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(LocalAppColorScheme provides colorScheme) {
    content()
  }
}

object AppCustomTheme {
  val colorScheme: AppThemeColorScheme
    @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current
}

@Composable
fun animateAppColorSchemeAsState(
  target: AppThemeColorScheme,
): AppThemeColorScheme {
  val animationSpec = remember {
    spring<Color>(stiffness = 500f)
  }
  return AppThemeColorScheme(
    groupedBackground = animateColorAsState(target.groupedBackground, animationSpec).value,
    primaryAction = animateColorAsState(target.primaryAction, animationSpec).value,
    onPrimaryAction = animateColorAsState(target.onPrimaryAction, animationSpec).value,
    secondaryAction = animateColorAsState(target.secondaryAction, animationSpec).value,
    onSecondaryAction = animateColorAsState(target.onSecondaryAction, animationSpec).value,
    primaryLabel = animateColorAsState(target.primaryLabel, animationSpec).value,
    secondaryLabel = animateColorAsState(target.secondaryLabel, animationSpec).value,
    tertiaryLabel = animateColorAsState(target.tertiaryLabel, animationSpec).value,
    tintColor = animateColorAsState(target.tintColor, animationSpec).value,
  )
}