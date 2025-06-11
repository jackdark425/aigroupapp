@file:OptIn(
  ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class,
  ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class
)

package com.aigroup.aigroupmobile.ui.pages.chat.detail.components

import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.data.models.UserProfile
import com.aigroup.aigroupmobile.data.models.model
import com.aigroup.aigroupmobile.ui.components.AppDropdownMenuItem
import com.aigroup.aigroupmobile.ui.components.BotAvatar
import com.aigroup.aigroupmobile.ui.components.SettingItemSelection
import com.aigroup.aigroupmobile.ui.components.UserAvatar
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import dev.chrisbanes.haze.HazeState

@Composable
fun ChatAppBar(
  modifier: Modifier = Modifier,

  hazeState: HazeState,
  title: String,

  onOpenDrawer: () -> Unit = {},
  onOpenUserProfile: () -> Unit = {},

  scrollBehavior: TopAppBarScrollBehavior,

  onUpdateTitle: (String) -> Unit = {},
  onDeleteChat: () -> Unit = {},
  onSummarySession: () -> Unit = {},

  onShowSettings: () -> Unit = {},

  bot: MessageSenderBot?,
  userProfile: UserProfile?,

  createNewSessionEnable: Boolean = false,
  onCreateNewSession: () -> Unit = {},

  showToggleDrawerButton: Boolean = true,
) {
  val coroutineScope = rememberCoroutineScope()

  var showMenu by remember { mutableStateOf(false) }

  var showEditTitle by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  val density = LocalDensity.current
  var titleWidth by remember { mutableStateOf(0.dp) }

  val topTitleInteractionSource = remember { MutableInteractionSource() }

  TopAppBar(
    modifier = Modifier,
    scrollBehavior = scrollBehavior,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(MaterialTheme.shapes.medium)
          .clickable(
            interactionSource = topTitleInteractionSource,
            indication = ripple()
          ) {
            showMenu = !showMenu
          }
          .onGloballyPositioned {
            titleWidth = with(density) {
              it.size.width.toDp()
            }
          }
          .padding(horizontal = 8.dp, vertical = 5.dp)
      ) {
        bot?.let { BotAvatar(it, size = 25.dp) }
        Spacer(Modifier.size(8.dp))
        Text(
          title,
          fontSize = 16.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          modifier = Modifier
            .padding(end = 5.dp)
            .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
        )
        Spacer(Modifier.width(7.dp))
        Icon(
          painter = painterResource(R.drawable.ic_chevron_right), "",
          tint = AppCustomTheme.colorScheme.secondaryLabel
        )
      }

      DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 8.dp,
        offset = DpOffset(0.dp, 10.dp),
      ) {
        if (bot != null) {
          AppDropdownMenuItem(
            leadingIcon = {
              Box(
                modifier = Modifier
                  .clip(MaterialTheme.shapes.medium)
                  .background(MaterialTheme.colorScheme.surfaceContainer)
                  .padding(horizontal = 3.dp, vertical = 3.dp)
                  .padding(end = 4.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  BotAvatar(bot, size = 20.dp)
                  Spacer(Modifier.width(5.dp))
                  Text(
                    bot.username,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 11.sp
                  )
                }
              }
            },
            text = {},
            onClick = {},
          )
          HorizontalDivider(
            Modifier.padding(horizontal = 8.dp),
            thickness = 0.5.dp
          )
        }
        AppDropdownMenuItem(
          onClick = { showEditTitle = true; showMenu = false },
          text = { Text(stringResource(R.string.label_menu_set_session_title)) },
          leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_edit_text_icon), "", Modifier.size(20.dp)) },
        )
        AppDropdownMenuItem(
          onClick = { onSummarySession(); showMenu = false },
          text = { Text(stringResource(R.string.label_button_summary_session)) },
          leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_chat_caption_icon), "", Modifier.size(15.dp)) },
        )
        AppDropdownMenuItem(
          onClick = { onShowSettings(); showMenu = false },
          text = { Text(stringResource(R.string.label_session_settings)) },
          leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_gear_icon), "", Modifier.size(15.dp)) },
        )
        AppDropdownMenuItem(
          onClick = { showDeleteDialog = true; showMenu = false },
          text = { Text(stringResource(R.string.label_delete_session), color = MaterialTheme.colorScheme.error) },
          leadingIcon = {
            Icon(
              ImageVector.vectorResource(R.drawable.ic_trash_icon), "",
              Modifier.size(15.dp),
              tint = MaterialTheme.colorScheme.error
            )
          },
        )
      }
    },

    navigationIcon = {
      if (showToggleDrawerButton) {
        IconButton(onClick = onOpenDrawer) {
          Icon(painterResource(R.drawable.ic_menu), "", Modifier.size(20.dp))
        }
      }
    },

    actions = {
      IconButton(onClick = onCreateNewSession, enabled = createNewSessionEnable) {
        Icon(painterResource(R.drawable.ic_bubble_plus_fill_icon), "", Modifier.size(20.dp))
      }
      Spacer(Modifier.size(8.dp))
      UserAvatar(size = 30.dp, avatar = userProfile?.avatar, onClick = { onOpenUserProfile() })
      Spacer(Modifier.size(10.dp))
    },

    colors = TopAppBarDefaults.largeTopAppBarColors(
      containerColor = Color.Transparent,
      scrolledContainerColor = Color.Transparent,
    ),
  )

  if (showEditTitle) {
    SettingItemSelection(
      onDismiss = { showEditTitle = false },
    ) {
      createInput(stringResource(R.string.hint_session_title), title, hideOnSubmit = true, autoFocus = true) {
        onUpdateTitle(it)
      }
    }
  }

  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = {
        showDeleteDialog = false
      },
      title = { Text(text = stringResource(R.string.label_delete_session)) },
      text = { Text(text = stringResource(R.string.label_confirm_delete_session)) },
      dismissButton = {
        Button(
          onClick = { showDeleteDialog = false },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
          )
        ) {
          Text(
            text = stringResource(R.string.label_cancel),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteDialog = false
            onDeleteChat()
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

//@Composable
//private fun BotTag(bot: MessageSenderBot) {
//  when {
//    bot.assistant != null -> {
//      Box(
//        Modifier
//          .clip(CircleShape)
//          .background(MaterialTheme.colorScheme.surfaceContainer)
//          .padding(3.dp),
//      ) {
//        Icon(
//          painter = painterResource(model.iconId),
//          contentDescription = "Avatar",
//          tint = model.contentColor,
//          modifier = Modifier
//            .size(10.dp)
//        )
//      }
//    }
//    bot.langBot != null -> {
//      val model = bot.langBot?.model!!
//      Box(
//        Modifier
//          .clip(CircleShape)
//          .background(model.tintColor.copy(alpha = 0.9f))
//          .padding(3.dp),
//      ) {
//        Icon(
//          painter = painterResource(model.iconId),
//          contentDescription = "Avatar",
//          tint = model.contentColor,
//          modifier = Modifier
//            .size(10.dp)
//        )
//      }
//    }
//  }
//}