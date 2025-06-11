package com.aigroup.aigroupmobile.ui.pages.assistant

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.repositories.AssistantRepository
import com.aigroup.aigroupmobile.ui.components.BotAvatar
import com.aigroup.aigroupmobile.ui.components.CommonMarkText
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.utils.common.fromDateString
import com.aigroup.aigroupmobile.utils.common.readableStr
import com.aigroup.aigroupmobile.viewmodels.AssistantViewModel
import com.aigroup.aigroupmobile.viewmodels.ChatConversationViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssistantStoreDetailPage(
  storeIdentifier: String,

  onBack: (() -> Unit)? = null,
  onNavigateToSession: (ChatSession) -> Unit,

  viewModel: AssistantViewModel = hiltViewModel(),
  conversationViewModel: ChatConversationViewModel = hiltViewModel() // TODO: is that right?
) {
  val context = LocalContext.current
  val repository = remember {
    AssistantRepository(context)
  }
  val assistant = remember(storeIdentifier) {
    repository.getByIdentifier(storeIdentifier)
  }

  if (assistant == null) {
    // TODO: empty page
    return
  }

  val localExist by viewModel.checkAssistantExists(assistant.identifier).collectAsStateWithLifecycle(false)
  val coroutineScope = rememberCoroutineScope()

  Scaffold(
    topBar = {
      Column {
        TopAppBar(
          title = { },
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
    val scrollState = rememberScrollState()
    val scrollStateInner = rememberScrollState()
    val hapic = LocalHapticFeedback.current

    Column(
      modifier = Modifier
        .padding(innerPadding)
        .verticalScroll(scrollState)
    ) {
      // assistant intro
      Row(
        modifier = Modifier.padding(horizontal = 13.dp)
      ) {
        // bot avatar
        // TODO: add border
        BotAvatar(assistant, size = 50.dp)

        Column() {
          Text(
            text = assistant.metadata.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 10.dp)
          )

          // created at
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp)
          ) {
            Text(
              text = assistant.metadata.author,
              style = MaterialTheme.typography.labelMedium,
              color = AppCustomTheme.colorScheme.secondaryLabel
            )

            Spacer(modifier = Modifier.width(5.dp))

            val createdAt = remember(assistant.metadata.createdAt) {
              LocalDate.fromDateString(assistant.metadata.createdAt)
            }
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
            assistant.metadata.tags.forEach { tag ->
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

      // description
      Text(
        assistant.metadata.description,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 23.dp),
        color = AppCustomTheme.colorScheme.primaryLabel
      )

      Spacer(modifier = Modifier.height(8.dp))


      // model tag
      val model = assistant.configuration.preferredModelCode?.let { ModelCode.fromFullCode(it) }
      model?.let { model ->
        Box(
          modifier = Modifier
            .padding(horizontal = 20.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .padding(end = 3.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              Modifier
                .clip(CircleShape)
                .background(model.tintColor.copy(alpha = 0.9f))
                .padding(2.dp),
            ) {
              Icon(
                painter = painterResource(model.iconId),
                contentDescription = "Avatar",
                tint = model.contentColor,
                modifier = Modifier
                  .size(9.dp)
              )
            }
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
        Spacer(modifier = Modifier.height(8.dp))
      }

      // start button
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
      ) {
        FilledIconButton(
          onClick = {
            if (localExist) {
              // show toast
              viewModel.removeLocalAssistant(assistant.identifier)
              Toast.makeText(
                context,
                context.getString(R.string.label_toast_assistant_deleted), Toast.LENGTH_SHORT
              ).show()
            } else {
              coroutineScope.launch {
                viewModel.addRemoteAssistantToLocal(assistant)
              }
              Toast.makeText(
                context,
                context.getString(R.string.label_toast_assistant_added),
                Toast.LENGTH_SHORT
              )
            }

            hapic.performHapticFeedback(HapticFeedbackType.LongPress)
          },
          shape = MaterialTheme.shapes.medium,
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
          )
        ) {
          AnimatedContent(localExist) {
            if (it) {
              Icon(
                imageVector = ImageVector.vectorResource(
                  R.drawable.ic_trash_icon,
                ),
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(19.dp)
              )
            } else {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        TextButton(
          onClick = {
            coroutineScope.launch {
              val botAssistant = viewModel.addRemoteAssistantToLocal(assistant)
              val conversation = conversationViewModel.createEmptySessionWithAssistant(botAssistant)
              onNavigateToSession(conversation)
            }
          },
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(
            containerColor = assistant.metadata.themeColor?.let {
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

      Spacer(modifier = Modifier.height(28.dp))

      // TODO: add icon here
      Text(
        stringResource(R.string.label_assistant_role),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp),
        fontSize = 17.sp
      )

      Spacer(modifier = Modifier.height(12.dp))

      // assistant configuration
      Box(
        // 一个有限大小的固定高度容器，如果文字溢出可以滚动
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
          .padding(horizontal = 14.dp)
          .clip(MaterialTheme.shapes.medium)
          .background(MaterialTheme.colorScheme.surfaceContainer)
          .verticalScroll(scrollStateInner)
      ) {
        // TODO: smaller text
        SelectionContainer {
          CommonMarkText(assistant.configuration.role, modifier = Modifier.padding(20.dp))
        }
      }

      Spacer(modifier = Modifier.height(50.dp))
    }
  }
}
