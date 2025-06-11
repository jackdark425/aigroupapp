package com.aigroup.aigroupmobile.utils.common

import android.R.id
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import com.aigroup.aigroupmobile.R
import java.util.Locale


@Composable
@ReadOnlyComposable
fun getLocale(): Locale? {
  val configuration = LocalConfiguration.current
  return ConfigurationCompat.getLocales(configuration).get(0)
}

val Locale.readableName: String
  @Composable
  @ReadOnlyComposable
  get() {
    val locale = getLocale()
    return getDisplayName(locale)
  }

/**
 * 获取 App 支持的语言列表
 */
fun Context.getDesignedLocaleList(): LocaleList {
  val baseConfiguration = Configuration(resources.configuration)
  val result = mutableSetOf<Locale>()

  // add default locale
  // NOTE: 当替换默认语言时需要更改这里的默认语言
  val defaultLocale = Locale.ENGLISH
  result.add(defaultLocale)
  val reference = createConfigurationContext(
    baseConfiguration.apply {
      setLocale(defaultLocale)
      setLocales(LocaleList(defaultLocale))
    }
  ).getString(R.string.lang_code)

  for (locale in assets.locales) {
    // all locale here, our mission is filter out the designed locale
    if (locale.isEmpty()) continue

    val testLocale = Locale.forLanguageTag(locale)

    val config = baseConfiguration.apply {
      setLocale(testLocale)
      setLocales(LocaleList(testLocale))
    }
    val langTag = createConfigurationContext(config).getString(R.string.lang_code)

    if (reference != langTag) {
      result.add(Locale.forLanguageTag(langTag))
    }
  }

  return LocaleList(*result.toTypedArray())
}


/**
 * 获取当前语言，注意获取的是 Android 采用的 APP 支持的语言，而不是一定是用户设置的语言，
 * 这在用户使用 APP 不支持的语言时很有用
 */
fun Context.getCurrentDesignedLocale(): Locale {
  val langCode = getString(R.string.lang_code)
  return Locale.forLanguageTag(langCode)
}

// TODO: don't design as Context extension
fun Context.isLocaleFollowSystem(): Boolean {
  return AppCompatDelegate.getApplicationLocales().isEmpty
}