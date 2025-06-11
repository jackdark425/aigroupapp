@file:OptIn(ExperimentalMaterial3Api::class)

package com.aigroup.aigroupmobile.ui.pages.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigroup.aigroupmobile.Constants
import com.aigroup.aigroupmobile.LocalTextSpeaker
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.connect.voice.VoiceServiceProvider
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.data.models.model
import com.aigroup.aigroupmobile.data.models.parseAssistantScheme
import com.aigroup.aigroupmobile.ui.components.BotAvatar
import com.aigroup.aigroupmobile.ui.components.MediaSelector
import com.aigroup.aigroupmobile.ui.components.SectionListItem
import com.aigroup.aigroupmobile.ui.components.SectionListSection
import com.aigroup.aigroupmobile.ui.components.SettingItemSelectionScope
import com.aigroup.aigroupmobile.ui.components.section
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.ui.utils.whenDarkMode
import com.aigroup.aigroupmobile.utils.common.localDateTime
import com.aigroup.aigroupmobile.utils.common.simpleDateStr
import com.aigroup.aigroupmobile.utils.system.OpenExternal
import com.aigroup.aigroupmobile.viewmodels.SessionSettingsViewModel
import compose.icons.CssGgIcons
import compose.icons.cssggicons.Trash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import org.mongodb.kbson.ObjectId


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSettingsPage(
  onBack: () -> Unit,
  viewModel: SessionSettingsViewModel = hiltViewModel()
) {
  val primaryBotSender by viewModel.primaryBotSender.observeAsState()
  val botSenders by viewModel.botSenders.observeAsState()
  val knowledgeDocs by viewModel.knowledgeDocs.observeAsState(initial = emptyList())

//  val senders by viewModel.senders.observeAsState(initial = emptyList())
  val voiceCode by viewModel.voiceCode.observeAsState()
  val coroutineScope = rememberCoroutineScope()
  val speaker = LocalTextSpeaker.current

  val context = LocalContext.current
  val hapic = LocalHapticFeedback.current

  // TODO: add to viewModel
  var indexingDocItemId: List<ObjectId> by remember {
    mutableStateOf(emptyList())
  }

  fun SettingItemSelectionScope.onSwitchPrimaryBot(bot: MessageSenderBot) {
    viewModel.switchPrimaryBot(bot)
    onDismiss()
    hapic.performHapticFeedback(HapticFeedbackType.LongPress)
  }

  Scaffold(
    containerColor = AppCustomTheme.colorScheme.groupedBackground,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.label_session_settings), fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "", Modifier.size(20.dp))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Transparent
        )
      )
    }
  ) { innerPadding ->
    LazyColumn(
      contentPadding = innerPadding,
      modifier = Modifier.padding(horizontal = 16.dp)
    ) {
      item {
        SectionListSection(stringResource(R.string.label_llm_model), showTitle = false) {
          primaryBotSender?.let { BotIntroCard(it) }
        }

        when {
          botSenders != null && botSenders!!.count() > 1 -> {
            Spacer(modifier = Modifier.height(4.dp))
          }

          else -> {
            Spacer(modifier = Modifier.height(16.dp))
          }
        }
      }

      if (botSenders != null && botSenders!!.count() > 1) {
        item {
          SectionListSection(stringResource(R.string.label_llm_model), showTitle = false) {
            SectionListItem(
              icon = ImageVector.vectorResource(R.drawable.ic_custom_bot_icon),
              title = stringResource(R.string.label_more_bot_sender, botSenders!!.count() - 1),
              noIconBg = true,
              iconModifier = Modifier.size(20.dp),
              modalContent = {
                val restSenders = botSenders!!.drop(1)
                restSenders.forEach {
                  createCustomView {
                    Box(
                      Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 9.dp)
                        .clip(MaterialTheme.shapes.medium)
                      // TODO: using lazy column?
                    ) {
                      BotIntroCard(it, normalBackground = true, onClick = {
                        onSwitchPrimaryBot(it)
                      })
                    }
                  }
                }
              }
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
        }
      }

      item {
        SectionListSection(stringResource(R.string.label_settings_general), showTitle = false) {
          var speakerLoading by remember { mutableStateOf(false) }

          SectionListItem(
            icon = ImageVector.vectorResource(R.drawable.ic_speaker_icon),
            title = stringResource(R.string.label_settings_tts_service_provider),
            noIconBg = true,
            iconModifier = Modifier.size(20.dp),
            trailingDetailContent = {
              Text(voiceCode?.serviceProvider?.displayName ?: "")
            },
            modalContent = {
              VoiceServiceProvider.entries.forEach {
                createItem(
                  it.displayName,
                  ImageVector.vectorResource(it.logoIconId),
                  selected = voiceCode?.serviceProvider == it
                ) {
                  viewModel.updateVoiceCode(it.defaultVoiceCode)
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
              Text(voiceCode?.displayVariant ?: "")
            },
            modalContent = {
              val provider = voiceCode?.serviceProvider

              provider?.let { provider ->
                provider.variantList.forEach {
                  createItem(
                    it.displayVariant,
                    ImageVector.vectorResource(R.drawable.ic_wave_icon),
                    selected = voiceCode?.variant == it.variant
                  ) {
                    viewModel.updateVoiceCode(it)
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
                speaker.speak(Constants.TestTtsText, voiceCode!!)
                speakerLoading = false
              }
            }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(16.dp))
      }

      item {
        SectionListSection("知识库", showTitle = true) {
          MediaSelector.TakeDocFileButton(
            onMediaSelected = {
              println("TakeFileButton onMediaSelected: $it")
              if (it is DocumentMediaItem) {
                // TODO: consider return specified type of MediaItem in MediaSelector functions
                viewModel.addKnowledgeDoc(it)
              }
            }
          ) {
            SectionListItem(
              icon = ImageVector.vectorResource(R.drawable.ic_add_docs),
              title = "添加文档", // i18n TODO
              noIconBg = true,
              iconModifier = Modifier.size(20.dp),
              onClick = { this.onClick() }
            )
          }

          if (knowledgeDocs.isNotEmpty()) {
            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 16.dp),
              thickness = 0.5.dp,
              color = MaterialTheme.colorScheme.surfaceDim
            )
          }

          knowledgeDocs.forEachIndexed { index, doc ->
            // 右滑删除
            val deleteAction = SwipeAction(
              icon = rememberVectorPainter(CssGgIcons.Trash),
              background = MaterialTheme.colorScheme.errorContainer,
              onSwipe = {
                viewModel.removeKnowledgeDoc(doc)
              }
            )

            SwipeableActionsBox(
              endActions = if (indexingDocItemId.contains(doc.id)) emptyList() else listOf(deleteAction),
              backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
              SectionListItem(
                // TODO: mime to icon
                icon = ImageVector.vectorResource(R.drawable.ic_doc_icon),
                title = doc.document!!.title,
                loading = indexingDocItemId.contains(doc.id),
                description = if (indexingDocItemId.contains(doc.id)) {
                  "正在索引"
                } else if (doc.knowledgeDocId == null) {
                  "未索引，点击立即索引"
                } else {
                  "已经索引"
                },// i18n TODO
                noIconBg = true,
                iconModifier = Modifier.size(20.dp),
                onClick = {
                  if (doc.knowledgeDocId != null) {
                    coroutineScope.launch {
                      OpenExternal.openDocMediaItemExternal(context, doc.document!!)
                    }
                    return@SectionListItem
                  }
                  if (indexingDocItemId.contains(doc.id)) {
                    return@SectionListItem
                  }

                  indexingDocItemId += doc.id
                  coroutineScope.launch {
                    viewModel.ragIndexDocument(doc)
                    withContext(Dispatchers.Main) {
                      indexingDocItemId -= doc.id
                    }
                  }
                }
              )
            }

            if (index < knowledgeDocs.size - 1) {
              HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.surfaceDim
              )
            }
          }
        }
      }

    }
  }
}

@Composable
private fun BotIntroCard(bot: MessageSenderBot, normalBackground: Boolean = false, onClick: (() -> Unit)? = null) {
  var cardSize by remember { mutableStateOf(IntSize.Zero) }
  val model = bot.langBot!!.model
  val createdAtStr = bot.inclusive.id.localDateTime.simpleDateStr

  Surface(onClick = onClick ?: {}, enabled = onClick != null) {
    ConstraintLayout(
      Modifier
        .fillMaxWidth()
        .onGloballyPositioned {
          cardSize = it.size
        }
        .let {
          if (!normalBackground) {
            it.background(
              brush = Brush.linearGradient(
                colorStops = arrayOf(
                  0.0f to Color.Transparent,
                  // TODO: 恢复暗黑模式效果
                  0.9f to Color(0xFFF3DBEC).whenDarkMode(Color.Transparent),
                  0.95f to Color(0xFFF6D6DA).whenDarkMode(Color.Transparent),
                  1.0f to Color(0xFFFAD9C3).whenDarkMode(Color.Transparent)
                ),
                start = Offset(0f, 0f),
                end = Offset(cardSize.width.toFloat(), cardSize.height.toFloat())
              )
            )
          } else {
            it.background(MaterialTheme.colorScheme.surfaceContainerLow)
          }
        }
        .padding(horizontal = 18.dp, vertical = 15.dp),
    ) {
      val (icon, title, subtitle, createdAt) = createRefs()

      BotAvatar(bot, 43.dp, Modifier.constrainAs(icon) {
        start.linkTo(parent.start)
        top.linkTo(parent.top, 10.dp)
      }, shape = MaterialTheme.shapes.medium)
      Text(
        when {
          bot.assistant != null -> {
            bot.username
          }

          else -> {
            stringResource(R.string.label_session_settings_chatbot, model)
          }
        },
        Modifier.constrainAs(title) {
          start.linkTo(icon.end, margin = 12.dp)
        },
        style = MaterialTheme.typography.titleMedium
      )

      val assistantScheme = remember(bot.assistant?.assistantSchemeStr) {
        bot.assistant?.parseAssistantScheme()
      }
      val providerName = remember(assistantScheme) { // TODO: 简化 parseAssistantScheme 使用
        when {
          assistantScheme != null -> {
            assistantScheme.metadata.author
          }

          else -> {
            model.serviceProvider.displayName
          }
        }
      }

      Row(
        Modifier.constrainAs(subtitle) {
          start.linkTo(title.start)
          top.linkTo(title.bottom)
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          stringResource(R.string.label_chatbot_come_from, providerName),
          style = MaterialTheme.typography.titleSmall
        )

        val model = assistantScheme?.configuration?.preferredModelCode?.let { ModelCode.fromFullCode(it) }
        model?.let { model ->
          Box(
            modifier = Modifier
              .clip(MaterialTheme.shapes.medium)
              .background(MaterialTheme.colorScheme.surfaceContainerLowest)
              .padding(horizontal = 2.dp, vertical = 2.dp)
              .padding(end = 3.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              BotAvatar(model, size = 12.dp)
              Spacer(Modifier.width(4.dp))
              Text(
                model.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 11.sp
              )
            }
          }
        }
      }
      Text(
        stringResource(R.string.label_chatbot_created_at, createdAtStr),
        Modifier.constrainAs(createdAt) {
          start.linkTo(title.start)
          top.linkTo(subtitle.bottom, margin = 10.dp)
        },
        style = MaterialTheme.typography.labelSmall,
        color = AppCustomTheme.colorScheme.secondaryLabel
      )
    }
  }
}
