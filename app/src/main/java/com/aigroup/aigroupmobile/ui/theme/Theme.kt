package com.aigroup.aigroupmobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.aigroup.aigroupmobile.MainApplication
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.ui.utils.animateColorSchemeAsState

private val lightScheme = lightColorScheme(
  primary = primaryLight,
  onPrimary = onPrimaryLight,
  primaryContainer = primaryContainerLight,
  onPrimaryContainer = onPrimaryContainerLight,
  secondary = secondaryLight,
  onSecondary = onSecondaryLight,
  secondaryContainer = secondaryContainerLight,
  onSecondaryContainer = onSecondaryContainerLight,
  tertiary = tertiaryLight,
  onTertiary = onTertiaryLight,
  tertiaryContainer = tertiaryContainerLight,
  onTertiaryContainer = onTertiaryContainerLight,
  error = errorLight,
  onError = onErrorLight,
  errorContainer = errorContainerLight,
  onErrorContainer = onErrorContainerLight,
  background = backgroundLight,
  onBackground = onBackgroundLight,
  surface = surfaceLight,
  onSurface = onSurfaceLight,
  surfaceVariant = surfaceVariantLight,
  onSurfaceVariant = onSurfaceVariantLight,
  outline = outlineLight,
  outlineVariant = outlineVariantLight,
  scrim = scrimLight,
  inverseSurface = inverseSurfaceLight,
  inverseOnSurface = inverseOnSurfaceLight,
  inversePrimary = inversePrimaryLight,
  surfaceDim = surfaceDimLight,
  surfaceBright = surfaceBrightLight,
  surfaceContainerLowest = surfaceContainerLowestLight,
  surfaceContainerLow = surfaceContainerLowLight,
  surfaceContainer = surfaceContainerLight,
  surfaceContainerHigh = surfaceContainerHighLight,
  surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
  primary = primaryDark,
  onPrimary = onPrimaryDark,
  primaryContainer = primaryContainerDark,
  onPrimaryContainer = onPrimaryContainerDark,
  secondary = secondaryDark,
  onSecondary = onSecondaryDark,
  secondaryContainer = secondaryContainerDark,
  onSecondaryContainer = onSecondaryContainerDark,
  tertiary = tertiaryDark,
  onTertiary = onTertiaryDark,
  tertiaryContainer = tertiaryContainerDark,
  onTertiaryContainer = onTertiaryContainerDark,
  error = errorDark,
  onError = onErrorDark,
  errorContainer = errorContainerDark,
  onErrorContainer = onErrorContainerDark,
  background = backgroundDark,
  onBackground = onBackgroundDark,
  surface = surfaceDark,
  onSurface = onSurfaceDark,
  surfaceVariant = surfaceVariantDark,
  onSurfaceVariant = onSurfaceVariantDark,
  outline = outlineDark,
  outlineVariant = outlineVariantDark,
  scrim = scrimDark,
  inverseSurface = inverseSurfaceDark,
  inverseOnSurface = inverseOnSurfaceDark,
  inversePrimary = inversePrimaryDark,
  surfaceDim = surfaceDimDark,
  surfaceBright = surfaceBrightDark,
  surfaceContainerLowest = surfaceContainerLowestDark,
  surfaceContainerLow = surfaceContainerLowDark,
  surfaceContainer = surfaceContainerDark,
  surfaceContainerHigh = surfaceContainerHighDark,
  surfaceContainerHighest = surfaceContainerHighestDark,
)

@Composable
fun AIGroupAppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),

  uiModePreferences: AppPreferences.ThemeMode = AppPreferences.ThemeMode.SYSTEM,
  colorScheme: AppPreferences.ColorScheme = AppPreferences.ColorScheme.CLASSIC,

  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,

  content: @Composable () -> Unit
) {
  // TODO: REFACTOR，fix these code
  // this is fix for access resources from Previewer in Android Studio
  if (LocalInspectionMode.current) {
    MainApplication.resources = LocalContext.current.resources
  }

  val uiMode = rememberSaveable {
    mutableStateOf(uiModePreferences.toUiMode(darkTheme))
  }

  // TODO: 避免这种 modified api 来标注临时改变 theme mode
  LaunchedEffect(uiModePreferences, uiMode.value.modified) {
    if (!uiMode.value.modified) {
      uiMode.value = uiModePreferences.toUiMode(darkTheme)
    }
  }

  // TODO: 避免提供 mutableStateOf
  CompositionLocalProvider(
    LocalUiMode provides uiMode,
    LocalColorScheme provides colorScheme
  ) {
    val isDarkTheme = LocalUiMode.current.value == UiMode.Dark

    val materialColorScheme = when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (isDarkTheme) {
          dynamicDarkColorScheme(context)
        } else {
          dynamicLightColorScheme(context)
        }
      }

      isDarkTheme -> darkScheme
      else -> lightScheme
    }
    val appColorScheme = when {
      isDarkTheme -> AppThemeColorScheme.dark()
      else -> AppThemeColorScheme.light()
    }.let {
      if (dynamicColor) {
        it.applyMaterialThemeTint(materialColorScheme)
      } else {
        it
      }
    }

    MaterialTheme(
      colorScheme = animateColorSchemeAsState(materialColorScheme),
      typography = Typography,
    ) {
      AppCustomTheme(
        colorScheme = animateAppColorSchemeAsState(appColorScheme)
      ) {
        content()
      }
    }
  }
}

enum class UiMode {
  Default,
  Dark;

  // TODO: 优化写法
  /**
   * 用于标注 ui mode 是否为用户临时修改
   */
  var modified = false;

  fun toggle(): UiMode {
    return when (this) {
      Default -> Dark
      Dark -> Default
    }.apply {
      modified = true
    }
  }
}

val LocalUiMode = staticCompositionLocalOf<MutableState<UiMode>> {
  error("UiMode not provided")
}

private fun AppPreferences.ThemeMode.toUiMode(isSystemInDarkTheme: Boolean): UiMode {
  return when (this) {
    AppPreferences.ThemeMode.SYSTEM, AppPreferences.ThemeMode.UNRECOGNIZED -> {
      if (isSystemInDarkTheme) UiMode.Dark else UiMode.Default
    }

    AppPreferences.ThemeMode.LIGHT -> UiMode.Default
    AppPreferences.ThemeMode.DARK -> UiMode.Dark
  }
}

val LocalColorScheme = staticCompositionLocalOf<AppPreferences.ColorScheme> {
  error("ColorScheme not provided")
}