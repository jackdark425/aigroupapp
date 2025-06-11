@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.aigroup.aigroupmobile.ui.pages.chat.conversation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.twotone.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.BotAssistant
import com.aigroup.aigroupmobile.data.models.LargeLangBot
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.ui.components.AppDropdownMenuItem
import com.aigroup.aigroupmobile.ui.components.BotAvatar
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import compose.icons.CssGgIcons
import compose.icons.cssggicons.Trash
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import androidx.compose.material3.Text as Text1

@Composable
fun ConversationItem(
  title: String,
  subtitle: String? = null,
  time: String,
  botSenders: List<MessageSenderBot> = emptyList(),
  isPinned: Boolean,
  assistant: BotAssistant? = null,
  onClick: () -> Unit,
  onPin: () -> Unit = {},
  onCancelPin: () -> Unit = {},
  onDeleteSelf: () -> Unit = {},
  onCopySelf: () -> Unit = {},
  onShowSettings: () -> Unit = {},
  selected: Boolean = false,
  containerShape: Shape = MaterialTheme.shapes.small,
  showMenuButton: Boolean = true,
  usingSwipeAction: Boolean = true,
  modifier: Modifier = Modifier
) {
  Box(
    modifier.padding(vertical = if (isPinned) 0.dp else 0.dp)
  ) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val pinAction = SwipeAction(
      icon = rememberVectorPainter(Icons.TwoTone.Star),
      background = if (isPinned)
        MaterialTheme.colorScheme.errorContainer
      else
        MaterialTheme.colorScheme.primaryContainer,
      onSwipe = {
        if (isPinned) {
          onCancelPin()
        } else {
          onPin()
        }
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
          Text1(stringResource(R.string.label_delete_session))
        },
        text = {
          Text1(stringResource(R.string.label_confirm_delete_session))
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
            Text1(stringResource(R.string.label_confirm))
          }
        },
        dismissButton = {
          TextButton(
            onClick = { showDeleteConfirm = false },
            shape = MaterialTheme.shapes.medium
          ) {
            Text1(stringResource(R.string.label_cancel))
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
      val containerColor by animateColorAsState(
        when {
          selected -> MaterialTheme.colorScheme.surfaceContainerLow
          showMenu -> MaterialTheme.colorScheme.surfaceContainerHigh
          isPinned -> MaterialTheme.colorScheme.surfaceContainerLowest
          else -> MaterialTheme.colorScheme.surfaceContainerLowest
        },
        animationSpec = tween(250)
      )

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
              if (selected)
                Modifier.border(0.5.dp, MaterialTheme.colorScheme.surfaceDim, containerShape)
              else
                Modifier
            )
            .clip(containerShape)
            .clickable(onClick = onClick)
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 13.dp)

          // TODO: 实现拖拽功能
        ) {
          Row {
            // TODO: consider avatar
//            assistant?.let {
//              BotAvatar(assistant, size = 28.dp, modifier = Modifier.offset(y = 2.dp))
//              Spacer(Modifier.width(4.dp))
//            }
            Column {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
                Text1(
                  text = title,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Normal,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f)
                )

                if (isPinned) {
                  Icon(
                    Icons.Default.Star,
                    "",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                  )
                }
              }

              Spacer(Modifier.size(2.dp))

              if (subtitle != null) {
                Row(Modifier.fillMaxWidth()) {
                  Text1(
                    text = subtitle,
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

              Spacer(Modifier.size(5.dp))

              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
              ) {
                // model tag
                ModelTag(selected = false, sender = botSenders.first())

                // more bot
                if (botSenders.size > 1) {
                  Spacer(Modifier.width(4.dp))
                  Text(
                    stringResource(R.string.label_more_bot_sender, botSenders.size - 1),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppCustomTheme.colorScheme.secondaryLabel,
                    fontSize = 9.sp
                  )
                }

                // TODO: consider time
//              Spacer(Modifier.width(6.dp))
//              Text1(
//                text = time,
//                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
//                color = AppCustomTheme.colorScheme.secondaryLabel
//              )
              }
            }
          }
        }

        if (showMenuButton) {
          IconButton(
            onClick = { showMenu = true },
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .offset(y = if (subtitle != null) 8.dp else 5.dp),
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
                  text = {
                    Text(
                      stringResource(
                        if (isPinned)
                          R.string.label_unpinned_sessions
                        else
                          R.string.label_pinned_sessions
                      )
                    )
                  },
                  contentPadding = PaddingValues(start = 12.dp, end = 32.dp),
                  leadingIcon = {
                    Icon(
                      ImageVector.vectorResource(
                        if (isPinned) {
                          R.drawable.ic_unpin_icon
                        } else {
                          R.drawable.ic_pin_icon
                        },
                      ),
                      "",
                      modifier = Modifier.size(15.dp)
                    )
                  },
                  onClick = {
                    showMenu = false
                    if (isPinned) {
                      onCancelPin()
                    } else {
                      onPin()
                    }
                  }
                )
                AppDropdownMenuItem(
                  text = { Text(stringResource(R.string.label_menu_create_session_clone)) },
                  contentPadding = PaddingValues(start = 12.dp, end = 32.dp),
                  leadingIcon = {
                    Icon(
                      ImageVector.vectorResource(R.drawable.ic_copy_lite_icon),
                      "",
                      modifier = Modifier.size(15.dp)
                    )
                  },
                  onClick = {
                    showMenu = false
                    onCopySelf()
                  }
                )
                AppDropdownMenuItem(
                  text = { Text(stringResource(R.string.label_session_settings)) },
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
                  text = { Text(stringResource(R.string.label_delete_session)) },
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

@Composable
private fun ModelTag(selected: Boolean, sender: MessageSenderBot) {
  Box(
    modifier = Modifier
      .clip(MaterialTheme.shapes.medium)
      .background(
        if (selected) {
          MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
          MaterialTheme.colorScheme.surfaceContainer
        }
      )
      .padding(horizontal = 2.dp, vertical = 2.dp)
      .padding(end = 3.dp)
      .widthIn(max = 130.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      BotAvatar(sender, size = 12.dp)
      Spacer(Modifier.width(4.dp))
      Text1( // TODO: remove Text1
        sender.username,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 11.sp,
        modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
fun ConversationItemPreview() {
  val modelShowCase = listOf(
    ModelCode("gpt4", ChatServiceProvider.OFFICIAL),
    ModelCode("gemini", ChatServiceProvider.OFFICIAL),
    ModelCode("claude", ChatServiceProvider.OFFICIAL),
    ModelCode("qwen", ChatServiceProvider.OFFICIAL),
    ModelCode("llama", ChatServiceProvider.OFFICIAL),
    ModelCode("glm", ChatServiceProvider.OFFICIAL),
    ModelCode("custom", ChatServiceProvider.OFFICIAL)
  )
  val sender = listOf(
    MessageSenderBot(
      name = "Test Bot",
      description = "Hello, I am a test",
      langBot = LargeLangBot(AppPreferencesDefaults.defaultModelCode.fullCode())
    ),
    MessageSenderBot(
      name = "Test Bot",
      description = "Hello, I am a test",
      langBot = LargeLangBot(AppPreferencesDefaults.defaultModelCode.fullCode())
    ),
  )

  Column {
    for ((idx, model) in modelShowCase.withIndex()) {
      ConversationItem(
        title = "Title",
        subtitle = if (idx == 2) "SubTitle ".repeat(20) else "Subtitle",
        time = "10:00",
        botSenders = sender,
        isPinned = idx == 2,
        onClick = {},
        selected = idx == 0,
      )
    }
  }
}