package com.aigroup.aigroupmobile.ui.pages.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.aigroup.aigroupmobile.LocalPathManager
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.ui.components.MediaSelector
import com.aigroup.aigroupmobile.ui.components.UserAvatar
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.utils.system.PathManager
import com.aigroup.aigroupmobile.viewmodels.WelcomeViewModel
import kotlinx.coroutines.flow.map

@Composable
fun WelcomeAvatarPage(
  modifier: Modifier = Modifier,
  initial: WelcomeInitial,
  goNextWelcomePage: (WelcomeInitial) -> Unit = {},
  onAvatarSelected: (MediaItem?) -> Unit = { _ -> },
  username: String?,
  avatar: ImageMediaItem?,
) {
  val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

  Scaffold(modifier) { innerPadding ->
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.headlineMedium) {
      Column(
        modifier = Modifier
          .padding(innerPadding)
          .fillMaxSize()
          .padding(horizontal = 32.dp),
        horizontalAlignment = if (windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT) {
          Alignment.Start
        } else {
          Alignment.CenterHorizontally
        }
      ) {
        Spacer(Modifier.weight(1f))

        Column() {
          UserAvatar(
            avatar = avatar
          )

          Spacer(Modifier.height(16.dp))

          Text(
            stringResource(R.string.label_welcome_greeting_to_user, username ?: ""),
            fontWeight = FontWeight.SemiBold
          )
          Text(stringResource(R.string.label_welcome_ask_for_custom_avatar), style = MaterialTheme.typography.titleMedium)

          Spacer(Modifier.height(16.dp))

          Row {
            MediaSelector.TakePhotoButton(
              onMediaSelected = onAvatarSelected
            ) {
              Box(
                modifier = Modifier
                  .clip(MaterialTheme.shapes.medium)
                  .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                  .clickable { this.onClick() }
                  .padding(horizontal = 20.dp, vertical = 16.dp)
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    ImageVector.vectorResource(R.drawable.ic_camera_icon), "",
                    tint = AppCustomTheme.colorScheme.secondaryLabel,
                    modifier = Modifier.size(30.dp)
                  )
                  Text(
                    stringResource(R.string.label_take_photo),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppCustomTheme.colorScheme.secondaryLabel
                  )
                }
              }
            }

            Spacer(Modifier.width(16.dp))

            MediaSelector.TakeGalleryButton(
              onMediaSelected = onAvatarSelected
            ) {
              Box(
                modifier = Modifier
                  .clip(MaterialTheme.shapes.medium)
                  .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                  .clickable { this.onClick() }
                  .padding(horizontal = 20.dp, vertical = 16.dp)
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    ImageVector.vectorResource(R.drawable.ic_vision_icon_legacy), "",
                    tint = AppCustomTheme.colorScheme.secondaryLabel,
                    modifier = Modifier.size(30.dp)
                  )
                  Text(
                    stringResource(R.string.label_gallery),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppCustomTheme.colorScheme.secondaryLabel
                  )
                }
              }
            }
          }

          Spacer(Modifier.height(8.dp))

          Button(
            onClick = {
              goNextWelcomePage(initial.copy(hasUserProfile = true))
            },
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
              containerColor = AppCustomTheme.colorScheme.primaryAction
            ),
          ) {
            Text(stringResource(R.string.label_welcome_button_continue))
          }
        }

        Spacer(Modifier.weight(1f))
      }
    }
  }
}

@Composable
fun WelcomeAvatarPage(
  modifier: Modifier = Modifier,
  initial: WelcomeInitial,
  goNextWelcomePage: (WelcomeInitial) -> Unit = {},
  viewModel: WelcomeViewModel = hiltViewModel()
) {
  val username by viewModel.userProfile.map { it?.username }.collectAsStateWithLifecycle(initialValue = null)
  val avatar by viewModel.userProfile.map { it?.avatar }.collectAsStateWithLifecycle(initialValue = null)

  WelcomeAvatarPage(
    modifier = modifier,
    initial = initial,
    goNextWelcomePage = goNextWelcomePage,
    username = username,
    avatar = avatar,
    onAvatarSelected = {
      viewModel.updateAvatar(it as ImageMediaItem)
    }
  )
}

@Preview
@Composable
private fun PreviewWelcomeAvatarPage() {
  val pathManager = PathManager(LocalContext.current)
  CompositionLocalProvider(LocalPathManager provides pathManager) {
    AIGroupAppTheme {
      WelcomeAvatarPage(
        initial = WelcomeInitial(
          hasUserProfile = false,
          hasFavoriteModel = false,
          emptySessionId = null,
          latestInitializedVersionCode = null
        ),
        username = "test",
        avatar = null
      )
    }
  }
}