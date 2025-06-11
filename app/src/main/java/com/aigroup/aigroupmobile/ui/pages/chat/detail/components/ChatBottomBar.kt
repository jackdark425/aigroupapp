@file:OptIn(
  ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class,
  ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
  ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, ExperimentalPermissionsApi::class,
  ExperimentalLayoutApi::class, ExperimentalPermissionsApi::class, ExperimentalPermissionsApi::class,
)

package com.aigroup.aigroupmobile.ui.pages.chat.detail.components

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigroup.aigroupmobile.LocalPathManager
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.LargeLangBot
import com.aigroup.aigroupmobile.data.models.LoadingStatus
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.data.models.loading
import com.aigroup.aigroupmobile.data.models.model
import com.aigroup.aigroupmobile.data.models.mutableLoadingStatusOf
import com.aigroup.aigroupmobile.data.extensions.preferencesProperties
import com.aigroup.aigroupmobile.data.models.parseAssistantScheme
import com.aigroup.aigroupmobile.ui.components.ActionButton
import com.aigroup.aigroupmobile.ui.components.MicrophoneButton
import com.aigroup.aigroupmobile.ui.components.ModelSelectPopup
import com.aigroup.aigroupmobile.ui.components.MultiLineField
import com.aigroup.aigroupmobile.ui.pages.settings.ChatPropertiesPage
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.ui.utils.clearFocusOnKeyboardDismiss
import com.aigroup.aigroupmobile.utils.system.PathManager
import com.aigroup.aigroupmobile.services.SpeechRecognitionState
import com.aigroup.aigroupmobile.utils.previews.previewModelCode
import com.aigroup.aigroupmobile.utils.previews.rememberPermissionStateSafe
import com.aigroup.aigroupmobile.utils.previews.rememberTestAI
import com.aigroup.aigroupmobile.services.rememberSecretSpeechRecognitionState
import com.aigroup.aigroupmobile.viewmodels.ChatBottomBarState
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

private const val AtRuleTag = "at_rule"
private const val Tag = "ChatBottomBar"

enum class ChatBottomBarExtra(val height: Dp) {
  MEDIA(100.dp),
  PLUGIN(250.dp),
  PROPERTIES(320.dp),
  CONTEXT(240.dp),
}

interface ChatBottomBarUIState {
  val extraBar: ChatBottomBarExtra?
  val showSettingsBar: Boolean
  val showExtraBar: Boolean
  val showModelsPopup: Boolean
  val showMicButton: Boolean
  val extraMessage: String?

  fun putExtraMessage(message: String?)

  fun openExtraBar(extra: ChatBottomBarExtra)
  fun closeAllExtraBar()
  fun showSettingsBar()
  fun hideSettingsBar()

  fun openModelsPopup()
  fun hideModelsPopup()

  fun showMicButton()
  fun hideMicButton()

  fun toggleSettingsBar(onDone: (Boolean) -> Unit = {}) {
    if (showSettingsBar) {
      hideSettingsBar()
    } else {
      showSettingsBar()
    }
    onDone(showSettingsBar)
  }

  fun toggleModelsPopup(onDone: (Boolean) -> Unit = {}) {
    if (showModelsPopup) {
      hideModelsPopup()
    } else {
      openModelsPopup()
    }
    onDone(showModelsPopup)
  }

  fun toggleMicButton(onDone: (Boolean) -> Unit = {}) {
    if (showMicButton) {
      hideMicButton()
    } else {
      showMicButton()
    }
    onDone(showMicButton)
  }
}

private class MutableChatBottomBarUIState : ChatBottomBarUIState {
  override var showModelsPopup: Boolean by mutableStateOf(false)
    private set

  override var showSettingsBar: Boolean by mutableStateOf(false)
    private set

  override var extraBar: ChatBottomBarExtra? by mutableStateOf(null)
    private set

  override var showMicButton: Boolean by mutableStateOf(false)
    private set

  override var extraMessage: String? by mutableStateOf(null)
    private set

  override val showExtraBar: Boolean by derivedStateOf {
    extraBar != null
  }

  override fun openExtraBar(extra: ChatBottomBarExtra) {
    showSettingsBar()
    extraBar = extra
  }

  override fun closeAllExtraBar() {
    extraBar = null
  }

  override fun showSettingsBar() {
    showSettingsBar = true
  }

  override fun hideSettingsBar() {
    showSettingsBar = false
  }

  override fun openModelsPopup() {
    showSettingsBar()
    showModelsPopup = true
  }

  override fun hideModelsPopup() {
    showModelsPopup = false
  }

  override fun showMicButton() {
    showMicButton = true
  }

  override fun hideMicButton() {
    showMicButton = false
  }

  override fun putExtraMessage(message: String?) {
    extraMessage = message
  }
}

@Composable
fun rememberChatBottomBarUIState(): ChatBottomBarUIState {
  return remember { MutableChatBottomBarUIState() }
}

@OptIn(ExperimentalTextApi::class, ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChatBottomBar(
  bottomBarState: ChatBottomBarState,
  bottomBarUIState: ChatBottomBarUIState = rememberChatBottomBarUIState(),
  speechRecognitionState: SpeechRecognitionState = rememberSecretSpeechRecognitionState(),

  onStateChanged: (ChatBottomBarState) -> Unit = {},
  onSend: (content: String?) -> Unit = {},

  currentBot: LargeLangBot? = null,
  models: LoadingStatus<List<ModelCode>> = remember { LoadingStatus.Success(emptyList()) },
  onUpdateModels: () -> Unit = {},

  onSelectModel: (ModelCode) -> Unit = {},
  onUpdateProperties: (AppPreferences.LongBotProperties) -> Unit = {},
  onResetProperties: () -> Unit = {},

  contextIncludeCount: Int? = null,
  onContextIncludeCountChange: (Int?) -> Unit = {},
  pluginList: List<String> = emptyList(),
  enablePlugin: (String) -> Unit = {},
  disablePlugin: (String) -> Unit = {},

  hazeState: HazeState = HazeState()
) {
  val atRuleTextColor = MaterialTheme.colorScheme.primary

  val haptic = LocalHapticFeedback.current

  val inputFieldBlockColor = MaterialTheme.colorScheme.primaryContainer
  val inputFieldTextColor = MaterialTheme.colorScheme.onPrimaryContainer
  var inputText by remember {
    val initial = buildAnnotatedString {}
    mutableStateOf(TextFieldValue(initial))
  }

  var extraBarHeight by remember { mutableStateOf(0.dp) }
  val extraBar = bottomBarUIState.extraBar
  val showExtraBar = bottomBarUIState.showExtraBar
  val showSettingsBar = bottomBarUIState.showSettingsBar
  val showMediaPreviewer = bottomBarState.mediaItem != null
  var showAtAssistantsModal by remember { mutableStateOf(false) }

  val keyboardController = LocalSoftwareKeyboardController.current

  fun sendContent(content: String? = null) {
    keyboardController?.hide()
    onSend(content)
  }

  fun toggleSpeechRecognize() {
    keyboardController?.hide();
//    speechRecognitionState.toggle()
    bottomBarUIState.showMicButton()
  }

  fun closeAllExtraBars() {
    bottomBarUIState.closeAllExtraBar()
  }

  fun openBar(bar: ChatBottomBarExtra) {
    if (bottomBarUIState.extraBar == bar) {
      closeAllExtraBars()
    } else {
      bottomBarUIState.openExtraBar(bar)
    }
  }

  val audioPermSpeechRecognize =
    rememberPermissionStateSafe(android.Manifest.permission.RECORD_AUDIO) {
      if (it) {
        toggleSpeechRecognize()
      }
    }

  fun onMicrophoneClick() {
    if (!audioPermSpeechRecognize.status.isGranted) {
      if (audioPermSpeechRecognize.status.shouldShowRationale) {
        // TODO: show rationale
        // TODO: and 处理用户不给权限的情况
      } else {
        audioPermSpeechRecognize.launchPermissionRequest()
      }
    } else {
      toggleSpeechRecognize()
    }
  }

  fun showAtRuleModal() {
    showAtAssistantsModal = true
  }

  fun onAtRuleDetected() {
    Log.i(Tag, "at rule detected")
    keyboardController?.hide()
    showAtRuleModal()
  }

  LaunchedEffect(
    speechRecognitionState.isRecognizing,
    speechRecognitionState.recognizingText,
    speechRecognitionState.recognizedText
  ) {
    onStateChanged(bottomBarState.copyFromSpeechRecognition(speechRecognitionState))
  }

  LaunchedEffect(bottomBarState) {
    inputText = if (bottomBarState.isRecognizing) {
      inputText.copy(
        annotatedString = bottomBarState.fullInputText,
        selection = TextRange(bottomBarState.fullInputText.length),
      )
    } else {
      inputText.copy(
        annotatedString = bottomBarState.fullInputText,
      )
    }
  }

  val currentModel = currentBot?.model
  val focusManager = LocalFocusManager.current
  val focusRequester = remember { FocusRequester() }

  Box {
    Box(
      modifier = Modifier
        .hazeChild(hazeState)
        .fillMaxWidth(),
    ) {
      Column() {
        AnimatedVisibility(bottomBarUIState.extraMessage != null) {
          Row(
            Modifier
              .fillMaxWidth()
              .padding(vertical = 3.dp), horizontalArrangement = Arrangement.Center
          ) {
            Text(
              bottomBarUIState.extraMessage ?: "",
              style = MaterialTheme.typography.labelLarge,
              color = AppCustomTheme.colorScheme.secondaryLabel
            )
          }
        }

        // 输入框主要区域
        Box(
          Modifier.heightIn(64.dp)
        ) {
          Row(
            Modifier.padding(start = 1.dp, end = 12.dp),
            verticalAlignment = Alignment.Bottom
          ) {
            // 添加多媒体按钮
            BadgedBox(
              badge = {
                androidx.compose.animation.AnimatedVisibility(showMediaPreviewer) {
                  Badge(
                    Modifier.offset(x = -5.dp, y = 5.dp),
                    containerColor = MaterialColors.Blue[400],
                    contentColor = MaterialTheme.colorScheme.onPrimary
                  ) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_attach_icon), "", Modifier.size(12.dp))
                  }
                }
              },
            ) {
              IconButton(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  bottomBarUIState.toggleSettingsBar {
                    if (it) {
                      focusManager.clearFocus()
                    } else {
                      closeAllExtraBars()
                    }
                  }
                },
                colors = IconButtonDefaults.iconButtonColors(
                  contentColor = AppCustomTheme.colorScheme.primaryAction
                ),
              ) {
                AnimatedContent(showSettingsBar, label = "chat_bottom_bar_show_settings") {
                  if (it) {
                    Icon(Icons.Filled.KeyboardArrowDown, "")
                  } else {
                    Icon(Icons.Outlined.AddCircle, "")
                  }
                }
              }
            }

            Spacer(Modifier.width(0.dp))

            // 输入框
            Box(
              Modifier
                .weight(1f)
                .offset(y = -4.dp)
            ) {
              androidx.compose.animation.AnimatedVisibility(
                !bottomBarUIState.showMicButton,
                modifier = Modifier
                  .fillMaxWidth(),
              ) {
                MultiLineField(
                  value = inputText,
                  onValueChange = { changed ->
                    // detect type @ (TODO: better way)
                    if (changed.text != inputText.text && changed.selection.start == changed.selection.end && changed.text.isNotEmpty()) {
                      if (changed.text.length - inputText.text.length == 1) {
                        if (changed.text[changed.selection.start - 1] == '@') {
                          Log.d(Tag, "detected @")
                          onAtRuleDetected()
                        }
                      }
                    }

                    // find all at rule like @assistant @user style text and using bold style
                    val annotatedString = buildChatFieldAnnotatedString(changed.text, atRuleTextColor)
                    onStateChanged(bottomBarState.copy(inputText = annotatedString))
                    inputText = changed.copy(annotatedString = annotatedString) // TODO: 是否重复修改，参考 LaunchEffect
                  },
                  textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppCustomTheme.colorScheme.primaryLabel),
                  action = {
                    val asMicrophone = inputText.text.isEmpty() || speechRecognitionState.isRecognizing
                    FilledIconToggleButton(
                      checked = speechRecognitionState.isRecognizing,
                      onCheckedChange = {
                        if (asMicrophone) {
                          onMicrophoneClick()
                        } else {
                          sendContent()
                        }
                      },
                      modifier = Modifier.padding(vertical = 3.dp),
                      shape = MaterialTheme.shapes.extraLarge,
                      colors = IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = if (asMicrophone) {
                          MaterialTheme.colorScheme.surfaceContainerLow
                        } else {
                          AppCustomTheme.colorScheme.primaryAction
                        },
                        contentColor = if (asMicrophone) {
                          MaterialTheme.colorScheme.onSurface
                        } else {
                          AppCustomTheme.colorScheme.onPrimaryAction
                        },
                        checkedContainerColor = AppCustomTheme.colorScheme.primaryAction,
                        checkedContentColor = AppCustomTheme.colorScheme.onPrimaryAction,
                      )
                    ) {
                      AnimatedContent(asMicrophone, label = "chat_bottom_bar_send_mic") {
                        if (it) {
                          val micIcon = if (speechRecognitionState.isRecognizing) {
                            R.drawable.ic_mic_fill_icon
                          } else {
                            R.drawable.ic_mic_icon
                          }
                          Icon(
                            ImageVector.vectorResource(micIcon),
                            "",
                            Modifier.size(20.dp),
                            tint = LocalContentColor.current
                          )
                        } else {
                          Icon(
                            ImageVector.vectorResource(R.drawable.ic_send_fill_icon),
                            "",
                            Modifier.size(20.dp),
                            tint = LocalContentColor.current
                          )
                        }
                      }
                    }
                  },
                  prefixAction = {
                    FilledIconButton(
                      onClick = {
                        showAtRuleModal()
                      },
                      modifier = Modifier.padding(vertical = 3.dp),
                      colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface
                      )
                    ) {
                      Text("@", fontWeight = FontWeight.Bold)
                    }
                  },
                  placeholder = stringResource(R.string.hint_placeholder_chat_bottom_bar),
                  onTextLayout = { layoutResult ->
                    // TODO: at user 圆角矩形背景
                  },
                  modifier = Modifier
                    .border(0.5.dp, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.extraLarge)
                    .focusRequester(focusRequester)
                    .clearFocusOnKeyboardDismiss()
                    .onFocusChanged {
                      if (it.isFocused) {
                        closeAllExtraBars()
                      }
                      if (it.isFocused && showSettingsBar) {
                        bottomBarUIState.hideSettingsBar()
                      }
                    }
                    .onPreviewKeyEvent {
                      if (it.key == Key.Backspace && it.type == KeyEventType.KeyDown) {
                        // TODO: better solution
                        // check the pointer is next to the at rule's span
                        val atRules = inputText.annotatedString.getStringAnnotations(
                          tag = AtRuleTag,
                          start = 0,
                          end = inputText.selection.start
                        )

                        for (annotation in atRules) {
                          if (annotation.end >= inputText.selection.start) {
                            val start = min(annotation.start, inputText.selection.start)
                            val end = max(annotation.end, inputText.selection.end)
                            inputText = inputText.copy(
                              annotatedString = buildChatFieldAnnotatedString(
                                inputText.text.removeRange(start, end), // TODO: 考虑性能
                                atRuleTextColor
                              ),
                              selection = TextRange(annotation.start)
                            )
                            // TODO: do we need this? 这里直接清空了没有考虑到多个 assistant 的情况
                            onStateChanged(bottomBarState.copy(
                              inputText = inputText.annotatedString,
                              atAssistant = null
                            ))
                            return@onPreviewKeyEvent true
                          }
                        }
                      }
                      false
                    }
                )
              }

              androidx.compose.animation.AnimatedVisibility(
                bottomBarUIState.showMicButton,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(40.dp),
              ) {
                Box(
                  Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.extraLarge)
                ) {
                  MicrophoneButton(
                    showExtraInfo = {
                      bottomBarUIState.putExtraMessage(it)
                    },
                    onConfirm = {
                      sendContent(it)
                    },
                    onHide = {
                      bottomBarUIState.hideMicButton()
                    }
                  )
                }
              }
            }
          }
        }

        // Actions 区域
        AnimatedVisibility(
          showSettingsBar, modifier = Modifier
        ) {
          Box() {
            FlowRow(
              horizontalArrangement = Arrangement.SpaceAround,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp)
                .padding(bottom = 16.dp)
            ) {
              Box {
                ActionButton(
                  stringResource(R.string.label_llm_model),
                  ImageVector.vectorResource(R.drawable.ic_brain_icon),
                  toggle = bottomBarUIState.showModelsPopup,
                  loading = models.loading,
                ) {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  closeAllExtraBars()

                  bottomBarUIState.toggleModelsPopup {
                    onUpdateModels()
                  }
                }
                if (bottomBarUIState.showModelsPopup) {
                  ModelSelectPopup(
                    models = models,
                    currentModel = currentModel,
                    onSelectModel = onSelectModel,
                    onDismissRequest = { bottomBarUIState.hideModelsPopup() },
                  )
                }
              }
              BadgedBox(
                badge = {
                  androidx.compose.animation.AnimatedVisibility(showMediaPreviewer) {
                    Badge(
                      Modifier.offset(x = -5.dp, y = 0.dp),
                      containerColor = MaterialColors.Blue[400],
                      contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                      Icon(ImageVector.vectorResource(R.drawable.ic_attach_icon), "", Modifier.size(12.dp))
                    }
                  }
                }
              ) {
                ActionButton(
                  stringResource(R.string.label_play_camera), ImageVector.vectorResource(R.drawable.ic_camera_icon),
                  toggle = extraBar == ChatBottomBarExtra.MEDIA,
                  enable = currentModel?.supportImage ?: true
                ) {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  openBar(ChatBottomBarExtra.MEDIA)
                }
              }
              ActionButton(
                stringResource(R.string.label_plugin), ImageVector.vectorResource(R.drawable.ic_plugin_icon),
                toggle = extraBar == ChatBottomBarExtra.PLUGIN,
              ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                openBar(ChatBottomBarExtra.PLUGIN)
              }
              ActionButton(
                stringResource(R.string.label_llm_props), ImageVector.vectorResource(R.drawable.ic_temp_icon),
                toggle = extraBar == ChatBottomBarExtra.PROPERTIES,
              ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                openBar(ChatBottomBarExtra.PROPERTIES)
              }
              ActionButton(
                stringResource(R.string.label_llm_context), ImageVector.vectorResource(R.drawable.ic_history_icon),
                toggle = extraBar == ChatBottomBarExtra.CONTEXT,
              ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                openBar(ChatBottomBarExtra.CONTEXT)
              }
            }
          }
        }

        // Extra 区域的 placeholder 高度
        val bottomInsets = with(LocalDensity.current) {
          WindowInsets.systemBars.getBottom(this).toDp()
        }
        val imeVisible = WindowInsets.isImeVisible
        Box(
          Modifier
            .animateContentSize(
              animationSpec = tween(
                durationMillis = 300,
                delayMillis = 0,
              )
            )
            .height(
              when {
                showExtraBar -> extraBarHeight
                imeVisible -> 0.dp
                else -> bottomInsets
              }
            )
        ) {}
      }
    }

    val density = LocalDensity.current
    var extraBarRealHeight = extraBar?.height ?: 0.dp
    if (extraBar == ChatBottomBarExtra.MEDIA && showMediaPreviewer) {
      extraBarRealHeight += 100.dp
    }

    // extra bar
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
        .shadow(20.dp)
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .animateContentSize(
          animationSpec = tween(
            durationMillis = 300,
            delayMillis = 0,
          )
        )
        .height(extraBarRealHeight)
        .align(Alignment.BottomStart)
        .onGloballyPositioned {
          extraBarHeight = with(density) { it.size.height.toDp() }
        }
      // TODO: 划动手势 .anchoredDraggable()
    ) {
      Column {
        DragHandle()

        when {
          extraBar == ChatBottomBarExtra.MEDIA && showMediaPreviewer -> {
            ExtraBarMediaPreview(bottomBarState, onStateChanged)
          }

          extraBar == ChatBottomBarExtra.MEDIA -> {
            ExtraBarMedia(onStateChanged, bottomBarState, currentModel)
          }

          extraBar == ChatBottomBarExtra.PLUGIN -> {
            ExtraBarPlugin(pluginList, enablePlugin, disablePlugin)
          }

          extraBar == ChatBottomBarExtra.PROPERTIES -> {
            ChatPropertiesPage(
              showTitle = false,
              properties = currentBot?.preferencesProperties ?: AppPreferencesDefaults.defaultLongBotProperties,
              onChange = {
                onUpdateProperties(it)
              },
              expand = false,
              containerColor = Color.Transparent
            )
          }

          extraBar == ChatBottomBarExtra.CONTEXT -> {
            ExtraBarContext(contextIncludeCount, onContextIncludeCountChange)
          }
        }
      }
    }
  }

  if (showAtAssistantsModal) {
    AtAssistantModal(
      onSelected = { assistant ->
        // TODO: 性能考虑 better solution
        val scheme = assistant.parseAssistantScheme()

        // if selection is a @
        val selection = inputText.selection.start
        val deltaContent = if (selection > 0 && inputText.text[selection - 1] == '@') {
          scheme.metadata.title + " "
        } else {
          " @" + scheme.metadata.title + " "
        }
        val nextState = buildChatFieldAnnotatedString(
          inputText.text.substring(0, selection) + deltaContent + inputText.text.substring(selection),
          atRuleTextColor
        )
        val nextSelection = selection + deltaContent.length

        // insert assistant
        inputText = inputText.copy(
          annotatedString = nextState,
          selection = TextRange(nextSelection)
        )
        focusRequester.requestFocus()

        // update state
        onStateChanged(bottomBarState.copy(
          inputText = nextState, // TODO: need this?
          atAssistant = assistant
        ))
      },
      onDismissRequest = { showAtAssistantsModal = false }
    )
  }
}

@OptIn(ExperimentalTextApi::class)
private fun buildChatFieldAnnotatedString(
  text: String,
  atRuleTextColor: Color
) = buildAnnotatedString {
  val atRegex = Regex("@[^\\s]+")
  var lastIndex = 0
  atRegex.findAll(text).forEach {
    val range = it.range
    val start = range.first
    val end = range.last + 1
    if (start > lastIndex) {
      append(text.substring(lastIndex, start))
    }
    withAnnotation(AtRuleTag, "ignored") { // TODO: whats is ignored
      withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = atRuleTextColor)) {
        append(text.substring(start, end))
      }
    }
    lastIndex = end
  }
  if (lastIndex < text.length) {
    append(text.substring(lastIndex))
  }
}

@Composable
private fun DragHandle() {
  Row(
    Modifier
      .padding(top = 10.dp, bottom = 6.dp)
      .fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
  ) {
    Box(
      Modifier
        .clip(MaterialTheme.shapes.large)
        .size(width = 40.dp, height = 5.dp)
        .background(MaterialTheme.colorScheme.surfaceDim)
    )
  }
}

@Preview(device = "id:pixel_8_pro", showSystemUi = true, name = "InputBar with Actions")
@Composable
fun ChatBottomBarPreview() {
  val state = remember { mutableStateOf(ChatBottomBarState()) }
  val hazeState = remember { HazeState() }
  val pathManager = PathManager(LocalContext.current)
  var models: LoadingStatus<List<ModelCode>> by remember { mutableLoadingStatusOf(initial = emptyList()) }
  val coroutineScope = rememberCoroutineScope()
  val ai = rememberTestAI()

  CompositionLocalProvider(LocalPathManager provides pathManager) {
    AIGroupAppTheme {
      Scaffold(
        modifier = Modifier
          .fillMaxSize()
          .imePadding(),
        bottomBar = {
          ChatBottomBar(
            state.value,
            bottomBarUIState = rememberChatBottomBarUIState().apply {
              this.showSettingsBar()
            },
            models = models,
            hazeState = hazeState,
            onUpdateModels = {
              println("start loading models")
              models = LoadingStatus.Loading
              coroutineScope.launch(Dispatchers.IO) {
                val value = ai.models().filter { it.ownedBy != "vertex" }
                  .map { ModelCode(it.id.id, ChatServiceProvider.OFFICIAL) }
                withContext(Dispatchers.Main) {
                  models = LoadingStatus.Success(value)
                }
              }
            },
            onStateChanged = { state.value = it },
            currentBot = LargeLangBot().apply {
              largeLangModelCode = previewModelCode.fullCode()
            },
          )
        }
      ) {
        LazyColumn(
          modifier = Modifier.haze(
            state = hazeState,
            style = HazeDefaults.style(
              backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
              blurRadius = 50.dp,
              noiseFactor = 10f
            )
          ),
          reverseLayout = true
        ) {
          item {
            Spacer(modifier = Modifier.height(100.dp))
          }
        }
      }
    }
  }
}

