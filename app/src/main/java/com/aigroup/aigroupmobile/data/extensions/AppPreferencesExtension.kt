package com.aigroup.aigroupmobile.data.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.models.LargeLangBot
import compose.icons.CssGgIcons
import compose.icons.FontAwesomeIcons
import compose.icons.cssggicons.Block
import compose.icons.cssggicons.DarkMode
import compose.icons.cssggicons.Moon
import compose.icons.cssggicons.Sun
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ExclamationCircle

// TODO: split these into separate files (REFACTOR)
val AppPreferences.ThemeMode.label: String
  @Composable
  get() = when (this) {
    AppPreferences.ThemeMode.SYSTEM -> stringResource(R.string.label_theme_mode_follow_system)
    AppPreferences.ThemeMode.LIGHT -> stringResource(R.string.label_theme_mode_bright)
    AppPreferences.ThemeMode.DARK -> stringResource(R.string.label_theme_model_dark)
    else -> ""
  }

val AppPreferences.ThemeMode.icon: ImageVector
  get() = when (this) {
    AppPreferences.ThemeMode.SYSTEM -> CssGgIcons.DarkMode
    AppPreferences.ThemeMode.LIGHT -> CssGgIcons.Sun
    AppPreferences.ThemeMode.DARK -> CssGgIcons.Moon
    else -> CssGgIcons.Block
  }

val AppPreferences.ChatViewMode.label: String
  @Composable
  get() = when (this) {
    AppPreferences.ChatViewMode.DOCUMENT -> stringResource(R.string.label_chat_ui_mode_doc)
    AppPreferences.ChatViewMode.BUBBLE -> stringResource(R.string.label_chat_ui_mode_bubble)
    else -> "未知"
  }

val AppPreferences.ChatViewMode.icon: ImageVector
  @Composable
  get() = when (this) {
    AppPreferences.ChatViewMode.DOCUMENT -> ImageVector.vectorResource(R.drawable.ic_doc_mode_icon)
    AppPreferences.ChatViewMode.BUBBLE -> ImageVector.vectorResource(R.drawable.ic_bubble_caption_icon)
    else -> FontAwesomeIcons.Solid.ExclamationCircle
  }

fun LargeLangBot.applyPreferenceProperties(prop: AppPreferences.LongBotProperties): LargeLangBot {
  val propToApply = AppPreferencesDefaults.mergeDefaultLongBotProperties(prop)
  return this.apply {
    temperature = propToApply.temperature
    maxTokens = propToApply.maxTokens
    topP = propToApply.topP
    presencePenalty = propToApply.presencePenalty
    frequencyPenalty = propToApply.frequencyPenalty
  }
}

val LargeLangBot.preferencesProperties: AppPreferences.LongBotProperties
  get() = AppPreferencesDefaults.defaultLongBotProperties.toBuilder()
    .let {
      if (temperature != null) {
        it.setTemperature(temperature!!)
      } else {
        it
      }
    }
    .let {
      if (maxTokens != null) {
        it.setMaxTokens(maxTokens!!)
      } else {
        it
      }
    }
    .let {
      if (topP != null) {
        it.setTopP(topP!!)
      } else {
        it
      }
    }
    .let {
      if (presencePenalty != null) {
        it.setPresencePenalty(presencePenalty!!)
      } else {
        it
      }
    }
    .let {
      if (frequencyPenalty != null) {
        it.setFrequencyPenalty(frequencyPenalty!!)
      } else {
        it
      }
    }
    .build()