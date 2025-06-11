@file:OptIn(ExperimentalPermissionsApi::class)

package com.aigroup.aigroupmobile.ui.pages.chat.detail.components

import android.icu.text.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.Constants
import com.aigroup.aigroupmobile.GuideQuestion
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.BotAssistant
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.LargeLangBot
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.data.models.RemoteAssistant
import com.aigroup.aigroupmobile.data.models.UserProfile
import com.aigroup.aigroupmobile.data.models.model
import com.aigroup.aigroupmobile.data.models.parseAssistantScheme
import com.aigroup.aigroupmobile.repositories.AssistantRepository
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.utils.common.getLocale
import com.aigroup.aigroupmobile.utils.common.instant
import com.aigroup.aigroupmobile.utils.common.now
import com.aigroup.aigroupmobile.utils.common.readableTimePeriod
import com.aigroup.aigroupmobile.utils.previews.rememberPermissionStateSafe
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import io.realm.kotlin.ext.realmListOf
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.encodeToString
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatIntroPage(
  botSender: MessageSenderBot,
  userProfile: UserProfile = UserProfile(),
  onSelectGuideQuestion: (GuideQuestion) -> Unit = {},

  onOpenSelectModel: () -> Unit = {},
  onOpenUploadMedia: () -> Unit = {},
  onStartMicrophone: () -> Unit = {},
  navigateToAssistantStore: () -> Unit = {},

  isRecording: Boolean = false,

  lastSessionTitle: String? = null,
  onNavigateToLastSession: () -> Unit = {},
) {
  val scrollViewState = rememberLazyListState()
  val keyboardController = LocalSoftwareKeyboardController.current
  val assistantScheme = remember(botSender.assistant?.assistantSchemeStr) {
    botSender.assistant?.parseAssistantScheme()
  }

  LaunchedEffect(scrollViewState) {
    snapshotFlow { scrollViewState.isScrollInProgress }
      .collect {
        if (it) {
          keyboardController?.hide()
        }
      }
  }

  val locale = getLocale()
  val currentDate = remember(locale) { LocalDateTime.now }
  val currentDateStr by remember {
    derivedStateOf {
      val df = DateFormat.getInstanceForSkeleton("MMMdEEEE", locale)
      df.format(Date.from(currentDate.instant.toJavaInstant()))
    }
  }

  // TODO: 把这一套放到 [utils.SpeechRecognition] 里面, ChatBottomBar 里的权限逻辑同理
  fun toggleSpeechRecognize() {
    keyboardController?.hide()
    onStartMicrophone()
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
        // TODO: and 处理用户不同意的情况
      } else {
        audioPermSpeechRecognize.launchPermissionRequest()
      }
    } else {
      toggleSpeechRecognize()
    }
  }

  Column(
    Modifier
      .fillMaxSize()
      .padding(vertical = 16.dp),
  ) {
    Column(Modifier.padding(horizontal = 18.dp)) {
      Text(
        currentDateStr.toString(),
        style = MaterialTheme.typography.bodyMedium,
        color = AppCustomTheme.colorScheme.secondaryLabel
      )
      Spacer(modifier = Modifier.height(2.dp))

      val assistant = botSender.assistant
      when {
        assistant != null -> {
          Text(
            stringResource(R.string.label_bot_greeting_prompt_assistant, botSender.username),
            style = MaterialTheme.typography.titleMedium,
            color = AppCustomTheme.colorScheme.primaryLabel
          )
        }
        else -> {
          Text(
            stringResource(R.string.label_bot_greeting_prompt, currentDate.readableTimePeriod),
            style = MaterialTheme.typography.titleMedium,
            color = AppCustomTheme.colorScheme.primaryLabel
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Column(
      Modifier
        .fillMaxWidth()
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
      ) {
        Column(
          Modifier
            .padding(vertical = 10.dp, horizontal = 12.dp)
            .padding(bottom = 6.dp)
        ) {
          Text(
            text = stringResource(R.string.label_ask_me_prompt),
            style = MaterialTheme.typography.bodySmall,
            color = AppCustomTheme.colorScheme.secondaryLabel
          )
          Spacer(modifier = Modifier.height(8.dp))

          val questions = remember(botSender.assistant) {
            when {
              assistantScheme != null -> {
                assistantScheme.startPrompts.map {
                  GuideQuestion(question = it)
                }
              }
              else -> Constants.BotGuideQuestions
            }
          }
          FlowRow(
            modifier = Modifier,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
          ) {
            for (item in questions) {
              Box(
                Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { onSelectGuideQuestion(item) }
                  .background(MaterialTheme.colorScheme.surfaceContainer)
                  .padding(horizontal = 10.dp, vertical = 5.dp)
              ) {
                Text(item.question, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
              }
            }
          }

          // try assistant store
          if (botSender.assistant == null) {
            Spacer(modifier = Modifier.height(10.dp))
            // rich text with link to assistant store
            val text = buildAnnotatedString {
              append(stringResource(R.string.label_try_assistant_store_prefix))
              withLink(LinkAnnotation.Clickable("store", linkInteractionListener = {
                navigateToAssistantStore()
              })) {
                withStyle(
                  style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                  )
                ) {
                  append(stringResource(R.string.label_assistants_store))
                }
              }
            }
            Text(
              text,
              style = MaterialTheme.typography.bodySmall,
              color = AppCustomTheme.colorScheme.secondaryLabel,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(13.dp))

      Row(
        Modifier
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
      ) {
        FeatureItem(
          label = stringResource(R.string.label_select_other_llm_label),
          bgColor = MaterialColors.Orange[50],
          frontColor = MaterialColors.Orange[400],
          icon = R.drawable.ic_brain_icon,
        ) {
          onOpenSelectModel()
        }
        FeatureItem(
          label = stringResource(R.string.label_upload_media),
          bgColor = MaterialColors.Green[50],
          frontColor = MaterialColors.Green[400],
          icon = R.drawable.ic_vision_icon_legacy,
        ) {
          onOpenUploadMedia()
        }
        FeatureItem(
          label = if (isRecording) stringResource(R.string.label_talking) else stringResource(R.string.label_start_talk),
          bgColor = MaterialColors.Purple[50],
          frontColor = MaterialColors.Purple[400],
          icon = R.drawable.ic_mic_icon,
          enabled = !isRecording
        ) {
          onMicrophoneClick()
        }
      }

      Spacer(modifier = Modifier.height(13.dp))

      AnimatedVisibility(lastSessionTitle != null) {
        Surface(
          modifier = Modifier
            .padding(horizontal = 14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLowest,
          shape = MaterialTheme.shapes.medium,
          onClick = onNavigateToLastSession
        ) {
          Row(
            Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              stringResource(R.string.label_last_chat_about), style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = AppCustomTheme.colorScheme.secondaryLabel
            )
            Spacer(Modifier.width(4.dp))
            Text(
              lastSessionTitle!!,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(Modifier.width(20.dp))

            Icon(
              painterResource(R.drawable.ic_chevron_right),
              "",
              tint = AppCustomTheme.colorScheme.secondaryLabel,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureItem(
  bgColor: Color,
  frontColor: Color,
  label: String,
  @DrawableRes icon: Int,

  enabled: Boolean = true,

  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    shape = MaterialTheme.shapes.medium,
    onClick = onClick,
    enabled = enabled
  ) {
    Box(
      Modifier.padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
      Column(Modifier.padding(end = 12.dp)) {
        Box(
          Modifier
            .clip(CircleShape)
            .background(bgColor)
            .padding(7.dp)
        ) {
          Icon(
            painterResource(icon),
            "",
            tint = frontColor,
            modifier = Modifier.size(30.dp)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(label, style = MaterialTheme.typography.bodyMedium)
      }

      Icon(
        painterResource(R.drawable.ic_arrow_right_up),
        "",
        tint = AppCustomTheme.colorScheme.secondaryLabel,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .size(12.dp)
      )
    }
  }
}

@Preview(showSystemUi = true)
@Composable
fun ChatIntroPagePreview() {
  val sender = MessageSenderBot(
    name = "Bot",
    description = "A chat bot",
    langBot = LargeLangBot(AppPreferencesDefaults.defaultModelCode.fullCode()),
  )
  AIGroupAppTheme {
    AppCustomTheme {
      Scaffold(
        containerColor = AppCustomTheme.colorScheme.groupedBackground
      ) {
        ChatIntroPage(
          botSender = sender,
          userProfile = UserProfile().apply { username = "jctaoo" }
        )
      }
    }
  }
}

@Preview(showSystemUi = true)
@Composable
fun ChatIntroPageWithAssistantPreview() {
  val ctx = LocalContext.current
  val assistant = AssistantRepository(ctx).getAssistants().first()
  val localAssistant = BotAssistant().apply {
    storeIdentifier = assistant.identifier
    avatar = ImageMediaItem()
    tags = realmListOf(*assistant.metadata.tags.toTypedArray())
    presetsPrompt = assistant.configuration.role
    assistantSchemeStr = AssistantRepository.Json.encodeToString(assistant)
  }
  val sender = MessageSenderBot(
    name = assistant.metadata.title,
    description = assistant.metadata.description,
    langBot = LargeLangBot(AppPreferencesDefaults.defaultModelCode.fullCode()),
    assistant = localAssistant,
  )

  AIGroupAppTheme {
    AppCustomTheme {
      Scaffold(
        containerColor = AppCustomTheme.colorScheme.groupedBackground
      ) {
        ChatIntroPage(
          botSender = sender,
          userProfile = UserProfile().apply { username = "jctaoo" },
        )
      }
    }
  }
}