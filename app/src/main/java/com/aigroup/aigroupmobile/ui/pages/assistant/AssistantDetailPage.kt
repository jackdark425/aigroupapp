package com.aigroup.aigroupmobile.ui.pages.assistant

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appQuantityStringResource
import com.aigroup.aigroupmobile.appStringResource
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.data.models.botSenders
import com.aigroup.aigroupmobile.data.models.lastModifyDateTime
import com.aigroup.aigroupmobile.data.models.parseAssistantScheme
import com.aigroup.aigroupmobile.data.models.primaryBotSender
import com.aigroup.aigroupmobile.data.models.sessionDisplaySubtitle
import com.aigroup.aigroupmobile.data.models.sessionDisplayTitle
import com.aigroup.aigroupmobile.repositories.AssistantRepository
import com.aigroup.aigroupmobile.ui.components.BotAvatar
import com.aigroup.aigroupmobile.ui.components.CommonMarkText
import com.aigroup.aigroupmobile.ui.components.MediaSelector
import com.aigroup.aigroupmobile.ui.components.SectionListItem
import com.aigroup.aigroupmobile.ui.components.SectionListSection
import com.aigroup.aigroupmobile.ui.components.section
import com.aigroup.aigroupmobile.ui.pages.chat.conversation.components.ConversationItem
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.utils.common.fromDateString
import com.aigroup.aigroupmobile.utils.common.localDateTime
import com.aigroup.aigroupmobile.utils.common.readableStr
import com.aigroup.aigroupmobile.utils.system.OpenExternal
import com.aigroup.aigroupmobile.viewmodels.AssistantSettingViewModel
import com.aigroup.aigroupmobile.viewmodels.AssistantViewModel
import com.aigroup.aigroupmobile.viewmodels.ChatConversationViewModel
import compose.icons.CssGgIcons
import compose.icons.cssggicons.Trash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssistantDetailPage(
  onBack: (() -> Unit)? = null,
  onNavigateToSession: (ChatSession) -> Unit,

  viewModel: AssistantSettingViewModel = hiltViewModel(),
  conversationViewModel: ChatConversationViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val repository = remember {
    AssistantRepository(context)
  }

  val assistantLocal by viewModel.assistant.observeAsState()
  val assistantScheme = remember(assistantLocal?.assistantSchemeStr) {
    assistantLocal?.parseAssistantScheme()
  }
  val knowledgeDocs by viewModel.knowledgeDocs.collectAsStateWithLifecycle(emptyList())

  val coroutineScope = rememberCoroutineScope()

  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

  LaunchedEffect(assistantLocal) {
    println(assistantLocal?.id)
  }

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      Column {
        CenterAlignedTopAppBar(
          scrollBehavior = scrollBehavior,
          title = {
            val titleOpacity by animateFloatAsState(scrollBehavior.state.overlappedFraction)

            assistantScheme?.let {
              Text(
                it.metadata.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.alpha(titleOpacity)
              )
            }
          },
          // back button
          navigationIcon = {
            if (onBack != null) {
              IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "", Modifier.size(20.dp))
              }
            }
          },
        )
      }
    },
  ) { innerPadding ->
    val scrollStateInner = rememberScrollState()
    val hapic = LocalHapticFeedback.current

    if (assistantLocal == null || assistantScheme == null) {
      // TODO: empty page
      return@Scaffold
    }

    val relatedConversations by conversationViewModel.findSessionWithAssistant(assistantLocal!!)
      .collectAsStateWithLifecycle(initialValue = emptyList())

    LazyColumn(
      modifier = Modifier,
      contentPadding = innerPadding,
    ) {
      // assistant intro
      item {
        Row(
          modifier = Modifier.padding(horizontal = 13.dp)
        ) {
          // bot avatar
          // TODO: add border
          BotAvatar(assistantLocal!!, size = 50.dp)

          Column() {
            Text(
              text = assistantScheme.metadata.title,
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.padding(start = 10.dp)
            )

            // created at
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(start = 10.dp)
            ) {
              val createdAt = assistantLocal!!.id.localDateTime
              Text(
                text = createdAt.readableStr,
                style = MaterialTheme.typography.labelMedium,
                color = AppCustomTheme.colorScheme.secondaryLabel
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // assistant tags
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              assistantScheme.metadata.tags.forEach { tag ->
                Box(
                  modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text(
                    tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(18.dp))
      }

      // description
      item {
        Text(
          assistantScheme.metadata.description,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(horizontal = 23.dp),
          color = AppCustomTheme.colorScheme.primaryLabel
        )
        Spacer(modifier = Modifier.height(8.dp))
      }

      // model tag and conversations count
      item {
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          val model = assistantScheme.configuration.preferredModelCode?.let { ModelCode.fromFullCode(it) }
          model?.let { model ->
            Box(
              modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
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

          // related chat session count
          Box(
            modifier = Modifier
              .clip(MaterialTheme.shapes.medium)
              .background(MaterialTheme.colorScheme.surfaceContainer)
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            val count = relatedConversations.size
            Text(
              // TODO: why 3 params need (check all plurals) and zero not work???
              pluralStringResource(R.plurals.label_assistant_conversations_count, count, count),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              lineHeight = 11.sp
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      // start button
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        ) {
          TextButton(
            onClick = {
              coroutineScope.launch {
                assistantLocal?.let {
                  val conversation = conversationViewModel.createEmptySessionWithAssistant(it)
                  withContext(Dispatchers.Main) {
                    onNavigateToSession(conversation)
                  }
                }
              }
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
              containerColor = assistantScheme.metadata.themeColor?.let {
                Color(it)
              } ?: AppCustomTheme.colorScheme.primaryAction,
            )
          ) {
            Text(
              stringResource(R.string.label_button_start_chat_assistant),
              fontWeight = FontWeight.Bold
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      // TODO using tab layout here

      // related conversations
      section(
        appStringResource(R.string.label_assistant_related_conversation),
        contentPadding = PaddingValues(horizontal = 16.dp),
        data = relatedConversations
      ) {
        ConversationItem(
          title = it.sessionDisplayTitle,
          subtitle = it.sessionDisplaySubtitle,
          time = it.lastModifyDateTime.readableStr,
          isPinned = false,
          assistant = it.primaryBotSender?.botSender?.assistant,
          onClick = {
            onNavigateToSession(it)
          },
          botSenders = it.botSenders,
          onCancelPin = {
            conversationViewModel.unpinChatSession(it)
          },
          showMenuButton = false,
          usingSwipeAction = false // TODO: support it here
        )
      }

      item {
        // TODO: using section contentPadding or topSpacing
        Spacer(modifier = Modifier.height(12.dp))
      }

      // show knowledge base
      section(
        "知识库",
        contentPadding = PaddingValues(horizontal = 16.dp),
        data = knowledgeDocs
      ) {
        // 右滑删除
        val deleteAction = SwipeAction(
          icon = rememberVectorPainter(CssGgIcons.Trash),
          background = MaterialTheme.colorScheme.errorContainer,
          onSwipe = {
            coroutineScope.launch {
//              viewModel.removeKnowledgeDoc(it)
            }
          }
        )

        SwipeableActionsBox(
          // TODO: support delete here?
//          endActions = listOf(deleteAction),
          backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
          SectionListItem(
            icon = ImageVector.vectorResource(R.drawable.ic_doc_icon),
            title = it.document!!.title,
            noIconBg = true,
            iconModifier = Modifier.size(20.dp),
            onClick = {
              coroutineScope.launch {
                OpenExternal.openDocMediaItemExternal(context, it.document!!)
              }
            }
          )
        }
      }

      item {
        // TODO: using section contentPadding or topSpacing
        Spacer(modifier = Modifier.height(12.dp))
      }

      section(
        appStringResource(R.string.label_assistant_role),
        contentPadding = PaddingValues(horizontal = 16.dp),
        data = listOf(AssistantDetailRow.Role(assistantScheme.configuration.role))
      ) {
        when (it) {
          is AssistantDetailRow.Role -> {
            Box(
              // 一个有限大小的固定高度容器，如果文字溢出可以滚动
              modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .verticalScroll(scrollStateInner)
            ) {
              // TODO: smaller text
              SelectionContainer {
                CommonMarkText(it.role, modifier = Modifier.padding(20.dp))
              }
            }
          }
        }
      }

      // extra padding
      item {
        Spacer(modifier = Modifier.height(158.dp))
      }
    }
  }
}

private sealed interface AssistantDetailRow {
  data class Role(val role: String) : AssistantDetailRow
}