package com.aigroup.aigroupmobile.ui.pages.settings.components

import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.aigroup.aigroupmobile.Constants
import com.aigroup.aigroupmobile.LocalTextSpeaker
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.Screen
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.connect.images.ImageGenerateServiceProvider
import com.aigroup.aigroupmobile.connect.images.ImageModelCode
import com.aigroup.aigroupmobile.connect.images.resolutionString
import com.aigroup.aigroupmobile.connect.video.VideoModelCode
import com.aigroup.aigroupmobile.connect.voice.VoiceCode
import com.aigroup.aigroupmobile.connect.voice.VoiceServiceProvider
import com.aigroup.aigroupmobile.data.extensions.ColorSchemeIcon
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.extensions.icon
import com.aigroup.aigroupmobile.data.extensions.label
import com.aigroup.aigroupmobile.data.extensions.preferredTheme
import com.aigroup.aigroupmobile.ui.components.LittleSwitch
import com.aigroup.aigroupmobile.ui.components.SectionListItem
import com.aigroup.aigroupmobile.ui.components.SectionListSection
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.utils.common.getCurrentDesignedLocale
import com.aigroup.aigroupmobile.utils.common.getDesignedLocaleList
import com.aigroup.aigroupmobile.utils.common.isLocaleFollowSystem
import com.aigroup.aigroupmobile.utils.common.now
import com.aigroup.aigroupmobile.utils.common.readableName
import com.aigroup.aigroupmobile.utils.common.readableStr
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import compose.icons.AllIcons
import compose.icons.CssGgIcons
import compose.icons.FontAwesomeIcons
import compose.icons.cssggicons.ArrowDownO
import compose.icons.cssggicons.DarkMode
import compose.icons.cssggicons.EditMarkup
import compose.icons.cssggicons.Ethernet
import compose.icons.cssggicons.FileDocument
import compose.icons.cssggicons.Info
import compose.icons.cssggicons.Lock
import compose.icons.cssggicons.MenuCheese
import compose.icons.cssggicons.Trash
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Comment
import compose.icons.fontawesomeicons.regular.FileImage
import compose.icons.fontawesomeicons.solid.Cookie
import compose.icons.fontawesomeicons.solid.Share
import compose.icons.fontawesomeicons.solid.Trash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime


sealed class SettingItemActionStatus<out T> {
  data object Loading : SettingItemActionStatus<Nothing>()
  data class Success<T>(val data: T) : SettingItemActionStatus<T>()
  data class Error(val message: String) : SettingItemActionStatus<Nothing>()
}

fun <T> mutableSettingItemActionStatusOf(initial: T) =
  mutableStateOf<SettingItemActionStatus<T>>(SettingItemActionStatus.Success(initial))

data class CacheStatus(
  val mbSize: Double = 0.0,
)

@Composable()
fun SettingItems(
  onOpenPage: (Screen) -> Unit = {},
  appPreferences: AppPreferences = AppPreferences.getDefaultInstance(),
  update: (AppPreferences.Builder.() -> AppPreferences) -> Unit = {},
  onShareApp: () -> Unit = {},

  // cache
  cacheStatus: SettingItemActionStatus<CacheStatus> = SettingItemActionStatus.Success(CacheStatus()),
  onClearCache: () -> Unit = {},

  // userdata
  localDataStatus: SettingItemActionStatus<Unit> = SettingItemActionStatus.Success(Unit),
  onClearUserData: () -> Unit = {},

  // backup
  backupStatus: SettingItemActionStatus<LocalDateTime?> = SettingItemActionStatus.Success(
    null
  ),
  onUserDataBackup: () -> Unit = {},
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val cacheFixedStr = when (cacheStatus) {
    is SettingItemActionStatus.Success -> {
      val size = cacheStatus.data.mbSize
      String.format("%.1f", size)
    }

    else -> "0.0"
  }
  val backupStatusStr = when (backupStatus) {
    is SettingItemActionStatus.Success -> {
      val time = backupStatus.data
      time?.readableStr?.let { stringResource(R.string.label_settings_backup_created_at, it) }
        ?: stringResource(R.string.label_settings_backup_no_backup)
    }

    else -> ""
  }

  var showClearCacheDialog by remember { mutableStateOf(false) }
  var showClearUserDataDialog by remember { mutableStateOf(false) }

  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.padding(horizontal = 12.dp)
  ) {
    SectionListSection(context.getString(R.string.label_settings_general), showTitle = false) {
      SectionListItem(
        CssGgIcons.Lock, stringResource(R.string.label_settings_chat_token),
        onClick = {
          onOpenPage(Screen.TokenSetting)
        },
        noIconBg = true,
      )
      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )
      SectionListItem(
        ImageVector.vectorResource(R.drawable.ic_custom_bot_icon),
        stringResource(R.string.label_settings_llm_model_default),
        onClick = {
          onOpenPage(Screen.ModelSelect)
        },
        trailingDetailContent = {
          Text(
            if (appPreferences.defaultImageModel.isNotEmpty()) {
              ModelCode.fromFullCode(appPreferences.defaultModelCode).fullDisplayCode
            } else {
              ""
            },
            modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
          )
        },
        noIconBg = true,
      )
      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )
      SectionListItem(
        ImageVector.vectorResource(R.drawable.ic_custom_bot_icon),
        stringResource(R.string.label_settings_custom_llm_providers),
        onClick = {
          onOpenPage(Screen.CustomLLMProviderList)
        },
        noIconBg = true,
      )
      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )
      SectionListItem(
        CssGgIcons.MenuCheese, stringResource(R.string.label_settings_llm_props_default),
        onClick = {
          onOpenPage(Screen.BotPropertiesSetting)
        },
        noIconBg = true,
      )
    }
    SectionListSection(stringResource(R.string.label_settings_text2img), showTitle = false) {
      // TODO: 优化 appPreferences 的默认值！或者改成 Loading Status
      val imageModelCode =
        if (appPreferences.defaultImageModel.isEmpty())
          ImageModelCode.Unknown
        else
          ImageModelCode.fromFullCode(appPreferences.defaultImageModel)

      SectionListItem(
        ImageVector.vectorResource(R.drawable.ic_vision_icon_legacy),
        stringResource(R.string.label_settings_text2img_service_provider),
        iconModifier = Modifier.scale(1.3f),
        onClick = {},
        trailingDetailContent = { Text(imageModelCode.serviceProvider.displayName) },
        modalContent = {
          ImageGenerateServiceProvider.entries.forEach {
            createItem(
              it.displayName,
              ImageVector.vectorResource(it.logoIconId),
              selected = imageModelCode.serviceProvider == it
            ) {
              update {
                this.setDefaultImageModel(
                  ImageModelCode(it, it.models.keys.first()).fullCode()
                ).build()
                this.setDefaultImageResolution(
                  it.models.values.first().supportsResolutions.first().resolutionString
                ).build()
              }
            }
          }
        },
        noIconBg = true,
      )
      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )
      SectionListItem(
        ImageVector.vectorResource(R.drawable.ic_vision_icon_legacy),
        stringResource(R.string.label_settings_text2img_model),
        iconModifier = Modifier.scale(1.3f),
        onClick = {},
        trailingDetailContent = { Text(imageModelCode.fullDisplayName()) },
        modalContent = {
          imageModelCode.serviceProvider.models.entries.forEach { (modelId, info) ->
            val model = ImageModelCode(imageModelCode.serviceProvider, modelId)
            createItem(
              model.fullDisplayName(),
              ImageVector.vectorResource(model.serviceProvider.logoIconId),
              selected = imageModelCode.model == model.model
            ) {
              update {
                this.setDefaultImageModel(model.fullCode()).build()
                this.setDefaultImageResolution(
                  info.supportsResolutions.first().resolutionString
                ).build()
              }
            }
          }
        },
        noIconBg = true,
      )

      val modelInfo = imageModelCode.modelInfo

      modelInfo?.let {
        HorizontalDivider(
          modifier = Modifier.padding(horizontal = 20.dp),
          thickness = 0.5.dp,
          color = MaterialTheme.colorScheme.surfaceDim
        )
        SectionListItem(
          FontAwesomeIcons.Regular.FileImage, stringResource(R.string.label_settings_text2img_resolution),
          onClick = {},
          trailingDetailContent = { Text(appPreferences.defaultImageResolution) },
          modalContent = {
            val len = modelInfo.supportsResolutions.size
            modelInfo.supportsResolutions.forEachIndexed { index, it ->
              val resolutionColor = lerp(MaterialColors.Gray[400], Color.Black, (index.toFloat() + 1) / len.toFloat())
              createItem(
                it.resolutionString,
                ImageVector.vectorResource(R.drawable.ic_vision_icon_legacy),
                selected = appPreferences.defaultImageResolution == it.resolutionString,
                tintColor = resolutionColor,
              ) {
                update {
                  this.setDefaultImageResolution(it.resolutionString).build()
                }
              }
            }
          },
          noIconBg = true,
        )
      }

      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )
      SectionListItem(
        ImageVector.vectorResource(R.drawable.ic_image_number_icon),
        stringResource(R.string.label_settings_text2img_number),
        onClick = {},
        trailingDetailContent = { Text(appPreferences.imageN.toString()) },
        modalContent = {
          createSlider(
            stringResource(R.string.label_settings_text2img_number),
            value = appPreferences.imageN.toFloat(),
            onValueChange = {
              update {
                this.setImageN(it.toInt()).build()
              }
            },
            steps = 2,
            range = 1f..4f,
          )
        },
        noIconBg = true,
      )
    }
    SectionListSection(stringResource(R.string.label_settings_text2video), showTitle = false) {
      // TODO: 优化 appPreferences 的默认值！或者改成 Loading Status
      val videoModelCode =
        if (appPreferences.defaultVideoModel.isEmpty())
          VideoModelCode.CogVideoX // TODO: should using unknown
        else
          VideoModelCode.fromFullCode(appPreferences.defaultVideoModel)

      SectionListItem(
        ImageVector.vectorResource(R.drawable.ic_video_icon),
        stringResource(R.string.label_settings_text2video_model),
        iconModifier = Modifier.scale(1.3f),
        onClick = {},
        trailingDetailContent = { Text(videoModelCode.fullDisplayName()) },
        modalContent = {
          VideoModelCode.entries.forEach {
            createItem(
              it.fullDisplayName(),
              ImageVector.vectorResource(it.logoIconId),
              selected = videoModelCode == it
            ) {
              update {
                this.setDefaultVideoModel(it.fullCode()).build()
              }
            }
          }
        },
        noIconBg = true,
      )
    }
    SectionListSection(stringResource(R.string.label_settings_tts), showTitle = false) {
      val speaker = LocalTextSpeaker.current
      var speakerLoading by remember { mutableStateOf(false) }

      // TODO: 优化 appPreferences 的默认值！或者改成 Loading Status
      val voiceCode =
        if (appPreferences.voiceCode.isEmpty())
          VoiceCode.Unknown
        else
          VoiceCode.fromFullCode(appPreferences.voiceCode)

      SectionListItem(
        icon = ImageVector.vectorResource(R.drawable.ic_speaker_icon),
        title = stringResource(R.string.label_settings_tts_service_provider),
        noIconBg = true,
        iconModifier = Modifier.size(20.dp),
        trailingDetailContent = {
          Text(voiceCode.serviceProvider.displayName)
        },
        modalContent = {
          VoiceServiceProvider.entries.forEach {
            createItem(
              it.displayName,
              ImageVector.vectorResource(it.logoIconId),
              selected = voiceCode.serviceProvider == it
            ) {
              update {
                setVoiceCode(it.defaultVoiceCode.fullCode()).build()
              }
            }
          }
        }
      )
      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )
      SectionListItem(
        icon = ImageVector.vectorResource(R.drawable.ic_wave_icon),
        title = stringResource(R.string.label_settings_tts_timbre),
        noIconBg = true,
        iconModifier = Modifier.size(20.dp),
        trailingDetailContent = {
          Text(voiceCode.displayVariant)
        },
        modalContent = {
          val provider = voiceCode.serviceProvider

          provider.variantList.forEach {
            createItem(
              it.displayVariant,
              ImageVector.vectorResource(R.drawable.ic_wave_icon),
              selected = voiceCode.variant == it.variant
            ) {
              update {
                setVoiceCode(it.fullCode()).build()
              }
            }
          }
        }
      )
      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )
      SectionListItem(
        icon = ImageVector.vectorResource(R.drawable.ic_play_icon),
        title = stringResource(R.string.label_settings_tts_test),
        noIconBg = true,
        iconModifier = Modifier.size(20.dp),
        loading = speakerLoading,
        onClick = {
          coroutineScope.launch {
            speakerLoading = true
            speaker.speak(Constants.TestTtsText, voiceCode)
            speakerLoading = false
          }
        }
      )
    }
    SectionListSection(stringResource(R.string.label_settings_search_online), showTitle = false) {
      val isEnable = appPreferences.serviceToken.hasSerper()

      SectionListItem(
        ImageVector.vectorResource(R.drawable.ic_search_icon), stringResource(R.string.label_settings_serper_service),
        onClick = {
          onOpenPage(Screen.SerperSetting)
        },
        trailingDetailContent = {
          Text(
            if (isEnable)
              stringResource(R.string.label_settings_item_already_set)
            else
              stringResource(R.string.label_settings_item_not_set)
          )
        },
        noIconBg = true,
      )
    }
    SectionListSection(stringResource(R.string.label_settings_appearance), showTitle = false) {
      SectionListItem(
        FontAwesomeIcons.Regular.Comment, stringResource(R.string.label_settings_chat_detail_page_mode),
        onClick = {},
        trailingDetailContent = { Text(appPreferences.chatViewMode.label) },
        modalContent = {
          for (mode in AppPreferences.ChatViewMode.entries) {
            if (mode != AppPreferences.ChatViewMode.UNRECOGNIZED) {
              createItem(mode.label, mode.icon, iconSize = 25.dp) {
                update {
                  this.setChatViewMode(mode).build()
                }
              }
            }
          }
        },
        noIconBg = true,
      )

      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )
      SectionListItem(
        CssGgIcons.DarkMode, stringResource(R.string.label_settings_app_theme),
        noIconBg = true,
        trailingDetailContent = { Text(appPreferences.uiMode.label) },
        modalContent = {
          for (theme in AppPreferences.ThemeMode.entries) {
            if (theme != AppPreferences.ThemeMode.UNRECOGNIZED) {
              createItem(theme.label, theme.icon) {
                update {
                  this.setUiMode(theme).build()
                }
              }
            }
          }
        }
      )

      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )

      // Color Scheme
      SectionListItem(
        CssGgIcons.EditMarkup, stringResource(R.string.label_settings_color_scheme),
        noIconBg = true,
        trailingDetailContent = { Text(appPreferences.colorScheme.label) },
        modalContent = {
          val availableColorScheme = AppPreferences.ColorScheme.entries.filter {
            it != AppPreferences.ColorScheme.UNRECOGNIZED &&
                (it.preferredTheme == null || it.preferredTheme == appPreferences.uiMode)
          }
          createCustomView {
            LazyVerticalGrid(
              columns = GridCells.Adaptive(minSize = 80.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              contentPadding = PaddingValues(horizontal = 12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(availableColorScheme) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                      update {
                        this
                          .setColorScheme(it)
                          .build()
                      }
                      onDismiss()
                    }
                    .padding(5.dp)
                ) {
                  it.ColorSchemeIcon()
                  Text(
                    text = it.label,
                    modifier = Modifier.padding(top = 5.dp),
                    style = MaterialTheme.typography.bodySmall,
                  )
                }
              }
            }
          }
        }
      )

      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )

      // Using android dynamic color (莫奈取色)
      SectionListItem(
        iconContent = {
          Box(
            Modifier
              .size(16.dp)
              .clip(CircleShape)
              .background(AppCustomTheme.colorScheme.primaryAction))
        },
        title = stringResource(R.string.label_settings_using_dynamic_color),
        description = stringResource(R.string.label_settings_desc_using_dynamic_color),
        noIconBg = true,
        trailingContent = {
          LittleSwitch(
            checked = appPreferences.usingAndroidDynamicColor,
            onCheckedChange = {
              update {
                this.setUsingAndroidDynamicColor(it).build()
              }
            },
            modifier = Modifier.padding(start = 12.dp),
          )
        }
      )

      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )

      val locale = context.getCurrentDesignedLocale()
      val localeList = context.getDesignedLocaleList()

      SectionListItem(
        ImageVector.vectorResource(R.drawable.ic_globe_icon),
        stringResource(R.string.label_settings_app_language),
        noIconBg = true,
        trailingDetailContent = {
          Text(
            if (context.isLocaleFollowSystem())
              context.getString(R.string.label_settings_lang_auto)
            else
              locale.readableName,
            modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
          )
        },
        modalContent = {
          // TODO: selected prop
          createItem(
            context.getString(R.string.label_settings_lang_auto),
            selected = context.isLocaleFollowSystem(),
          ) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
          }

          for (i in 0..<localeList.size()) {
            val loc = localeList.get(i)
            createItem(
              loc.getDisplayName(loc),
              tintColor = Color.Unspecified,
              selected = locale == loc
            ) {
              val appLocale = LocaleListCompat.forLanguageTags(loc.toLanguageTag())
              AppCompatDelegate.setApplicationLocales(appLocale)
            }
          }
        }
      )
    }
    SectionListSection(stringResource(R.string.label_settings_privacy_data), showTitle = false) {
      SectionListItem(
        CssGgIcons.FileDocument, stringResource(R.string.label_settings_backup_data),
        trailingDetailContent = { Text(backupStatusStr) },
        onClick = {
          onUserDataBackup()
        },
        loading = backupStatus is SettingItemActionStatus.Loading,
        noIconBg = true,
      )
      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )
      SectionListItem(
        CssGgIcons.Trash, stringResource(R.string.label_settings_clear_cache),
        trailingDetailContent = { Text("${cacheFixedStr}MB") },
        onClick = {
          showClearCacheDialog = true
        },
        loading = cacheStatus is SettingItemActionStatus.Loading,
        noIconBg = true,
      )
      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )
      SectionListItem(
        FontAwesomeIcons.Solid.Trash, stringResource(R.string.label_settings_clear_data),
        danger = true,
        onClick = {
          showClearUserDataDialog = true
        },
        loading = localDataStatus is SettingItemActionStatus.Loading,
        noIconBg = true,
      )
    }
    SectionListSection(stringResource(R.string.label_settings_others), showTitle = false) {
      val version = LocalContext.current.packageManager.getPackageInfo(
        LocalContext.current.packageName, 0
      ).versionName
      SectionListItem(
        CssGgIcons.ArrowDownO, stringResource(R.string.label_settings_check_update),
        description = stringResource(R.string.label_settings_current_version, version),
        onClick = {
          Toast.makeText(context, context.getString(R.string.toast_already_latest_version), Toast.LENGTH_LONG).show()
        },
        noIconBg = true,
      )

      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )

      val appName = stringResource(R.string.app_name)
      SectionListItem(
        CssGgIcons.Info, stringResource(R.string.label_settings_about, appName),
        onClick = {
          onOpenPage(Screen.About)
        },
        noIconBg = true,
      )
      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceDim
      )
      SectionListItem(
        FontAwesomeIcons.Solid.Share, stringResource(R.string.label_settings_share, appName),
        onClick = {
          onShareApp()
        },
        noIconBg = true,
      )
    }
  }

  // dialogs
  if (showClearCacheDialog) {
    AlertDialog(
      onDismissRequest = {
        showClearCacheDialog = false
      },
      title = { Text(text = stringResource(R.string.label_settings_clear_cache)) },
      text = { Text(text = stringResource(R.string.label_confirm_clear_cache, cacheFixedStr)) },
      confirmButton = { // 6
        Button(
          onClick = {
            showClearCacheDialog = false
            onClearCache()
          }
        ) {
          Text(
            text = stringResource(R.string.label_confirm),
          )
        }
      }
    )
  }

  if (showClearUserDataDialog) {
    AlertDialog(
      onDismissRequest = {
        showClearUserDataDialog = false
      },
      title = { Text(text = stringResource(R.string.label_settings_clear_data)) },
      text = { Text(text = stringResource(R.string.label_confirm_clear_data)) },
      confirmButton = {
        Button(
          onClick = {
            showClearUserDataDialog = false
            onClearUserData()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          )
        ) {
          Text(
            text = stringResource(R.string.label_confirm),
          )
        }
      }
    )
  }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewSettingsMainContent() {
  var preferences by remember { mutableStateOf(AppPreferences.getDefaultInstance()) }
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var cacheStatus by remember {
    mutableSettingItemActionStatusOf(CacheStatus(12.3))
  }
  var backupStatus by remember {
    mutableSettingItemActionStatusOf<LocalDateTime?>(null)
  }
  var localDataStatus by remember {
    mutableSettingItemActionStatusOf(Unit)
  }

  AIGroupAppTheme {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
    ) {
      SettingItems(
        appPreferences = preferences,
        update = {
          preferences = it.invoke(preferences.toBuilder())
        },
        // backup
        backupStatus = backupStatus,
        onUserDataBackup = {
          backupStatus = SettingItemActionStatus.Loading
          coroutineScope.launch(Dispatchers.IO) {
            delay(2000)
            withContext(Dispatchers.Main) {
              backupStatus = SettingItemActionStatus.Success(LocalDateTime.now)
            }
          }
        },
        // cache
        cacheStatus = cacheStatus,
        // TODO: move theses actions including toast to SettingItems call (this is just preview)
        onClearCache = {
          cacheStatus = SettingItemActionStatus.Loading
          coroutineScope.launch(Dispatchers.IO) {
            delay(2000)
            withContext(Dispatchers.Main) {
              Toast.makeText(context, context.getString(R.string.toast_cache_cleared), Toast.LENGTH_SHORT).show()
              cacheStatus = SettingItemActionStatus.Success(CacheStatus(0.0))
            }
          }
        },
        // localData
        localDataStatus = localDataStatus,
        onClearUserData = {
          localDataStatus = SettingItemActionStatus.Loading
          coroutineScope.launch(Dispatchers.IO) {
            delay(2000)
            withContext(Dispatchers.Main) {
              Toast.makeText(context, context.getString(R.string.toast_data_cleared), Toast.LENGTH_SHORT).show()
              localDataStatus = SettingItemActionStatus.Success(Unit)
            }
          }
        }
      )
    }
  }
}

