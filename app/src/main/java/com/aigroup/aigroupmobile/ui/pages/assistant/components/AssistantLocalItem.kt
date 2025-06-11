package com.aigroup.aigroupmobile.ui.pages.assistant.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.twotone.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigroup.aigroupmobile.data.models.BotAssistant
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.data.models.RemoteAssistant
import com.aigroup.aigroupmobile.data.models.parseAssistantScheme
import com.aigroup.aigroupmobile.repositories.AssistantRepository
import com.aigroup.aigroupmobile.ui.components.AppDropdownMenuItem
import com.aigroup.aigroupmobile.ui.components.BotAvatar
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.utils.common.fromDateString
import com.aigroup.aigroupmobile.utils.common.readableStr
import compose.icons.CssGgIcons
import compose.icons.cssggicons.Trash
import io.realm.kotlin.ext.realmListOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.serialization.encodeToString
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantLocalItem(
  assistant: BotAssistant,
  onClick: () -> Unit = {},

  onDeleteSelf: () -> Unit = {},
  onShowSettings: () -> Unit = {},
  onStartChat: () -> Unit = {},

  containerShape: Shape = MaterialTheme.shapes.small,
  showMenuButton: Boolean = true,
  usingSwipeAction: Boolean = true,

  modifier: Modifier = Modifier
) {
  val assistantScheme = remember(assistant.assistantSchemeStr) {
    assistant.parseAssistantScheme()
  }

  Box(modifier) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val pinAction = SwipeAction(
      icon = {
        Icon(
          ImageVector.vectorResource(R.drawable.ic_bubble_plus_icon),
          "",
          modifier = Modifier
            .padding(horizontal = 10.dp)
            .size(18.dp)
        )
      },
      background = MaterialTheme.colorScheme.primaryContainer,
      onSwipe = {
        // start chat
        onStartChat()
      }
    )

    val deleteAction = SwipeAction(
      icon = rememberVectorPainter(CssGgIcons.Trash),
      background = MaterialTheme.colorScheme.errorContainer,
      onSwipe = {
        showDeleteConfirm = true
      }
    )

    if (showDeleteConfirm) {
      AlertDialog(
        onDismissRequest = { showDeleteConfirm = false },
        title = {
          Text(stringResource(R.string.label_delete_assistant))
        },
        text = {
          Text(stringResource(R.string.label_confirm_delete_assistant))
        },
        confirmButton = {
          Button(
            onClick = onDeleteSelf,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError
            )
          ) {
            Text(stringResource(R.string.label_confirm))
          }
        },
        dismissButton = {
          TextButton(
            onClick = { showDeleteConfirm = false },
            shape = MaterialTheme.shapes.medium
          ) {
            Text(stringResource(R.string.label_cancel))
          }
        }
      )
    }

    SwipeableActionsBox(
      endActions = if (usingSwipeAction) listOf(pinAction) else emptyList(),
      startActions = if (usingSwipeAction) listOf(deleteAction) else emptyList(),
      backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
      val scale = animateFloatAsState(
        targetValue = if (showMenu) 0.95f else 1f,
        animationSpec = tween(300)
      )
      val containerColor = MaterialTheme.colorScheme.surfaceContainerLowest

      Box(
        Modifier.graphicsLayer {
          scaleX = scale.value
          scaleY = scale.value
          transformOrigin = TransformOrigin.Center
        }
      ) {
        Box(
          modifier = Modifier
            .then(
              Modifier.border(0.5.dp, MaterialTheme.colorScheme.surfaceDim, containerShape)
            )
            .clip(containerShape)
            .clickable(onClick = onClick)
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 13.dp)

          // TODO: 实现拖拽功能
        ) {
          Column {
            Row {
              BotAvatar(
                bot = assistant,
                size = 30.dp,
                modifier = Modifier.offset(y = 2.dp)
              )

              Spacer(Modifier.width(4.dp))

              Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
                  Text(
                    text = assistantScheme.metadata.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                  )
                }

                Spacer(Modifier.size(2.dp))

                Row(Modifier.fillMaxWidth()) {
                  Text(
                    text = assistantScheme.metadata.description,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Light,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = AppCustomTheme.colorScheme.secondaryLabel
                  )
                  Spacer(Modifier.width(8.dp))
                }
              }
            }

            Spacer(Modifier.size(10.dp))

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              // model tag
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
        }

        if (showMenuButton) {
          IconButton(
            onClick = { showMenu = true },
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .offset(8.dp),
          ) {
            Icon(painterResource(R.drawable.ic_more_icon), "", modifier = Modifier.size(17.dp))

            if (showMenu) {
              DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                shadowElevation = 5.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = MaterialTheme.shapes.medium,
              ) {
                AppDropdownMenuItem(
                  text = { Text(stringResource(R.string.label_assistant_settings)) },
                  contentPadding = PaddingValues(start = 12.dp, end = 32.dp),
                  leadingIcon = {
                    Icon(
                      ImageVector.vectorResource(R.drawable.ic_gear_icon),
                      "",
                      modifier = Modifier.size(15.dp)
                    )
                  },
                  onClick = {
                    showMenu = false
                    onShowSettings()
                  }
                )
                AppDropdownMenuItem(
                  danger = true,
                  text = { Text(stringResource(R.string.label_delete_assistant)) },
                  contentPadding = PaddingValues(start = 12.dp, end = 32.dp),
                  leadingIcon = {
                    Icon(
                      ImageVector.vectorResource(R.drawable.ic_trash_icon),
                      "",
                      modifier = Modifier.size(15.dp)
                    )
                  },
                  onClick = {
                    showMenu = false
                    showDeleteConfirm = true
                  }
                )
              }

            }
          }
        }
      }
    }
  }
}

@Preview(showSystemUi = true)
@Composable
fun PreviewAssistantLocalItem() {
  val context = LocalContext.current
  val repo = AssistantRepository(context)

  val assistants = remember() {
    repo.getAssistants().map { remote ->
      BotAssistant().apply {
        storeIdentifier = remote.identifier
        avatar = ImageMediaItem()
        tags = realmListOf(*remote.metadata.tags.toTypedArray())
        presetsPrompt = remote.configuration.role
        assistantSchemeStr = AssistantRepository.Json.encodeToString(remote)
      }
    }
  }

  AIGroupAppTheme {
    Column {
      assistants.forEach {
        AssistantLocalItem(it, modifier = Modifier.fillMaxWidth())
      }
    }
  }
}