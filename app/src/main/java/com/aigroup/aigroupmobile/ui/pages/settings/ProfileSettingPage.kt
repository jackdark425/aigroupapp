@file:OptIn(ExperimentalMaterial3Api::class)

package com.aigroup.aigroupmobile.ui.pages.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aigroup.aigroupmobile.LocalPathManager
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.data.models.UserProfile
import com.aigroup.aigroupmobile.ui.components.MediaSelector
import com.aigroup.aigroupmobile.ui.components.SectionListItem
import com.aigroup.aigroupmobile.ui.components.SectionListSection
import com.aigroup.aigroupmobile.ui.components.UserAvatar
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.utils.system.PathManager
import com.aigroup.aigroupmobile.viewmodels.UserProfileViewModel
import compose.icons.CssGgIcons
import compose.icons.cssggicons.CardClubs
import compose.icons.cssggicons.Nametag
import compose.icons.cssggicons.Rename
import compose.icons.cssggicons.Share
import compose.icons.cssggicons.User

@Composable
private fun ProfileSettingPageInner(
  profile: UserProfile,
  onUpdateUsername: (String) -> Unit,
  onUpdateAvatar: (ImageMediaItem?) -> Unit,
  onBack: () -> Unit = {},
) {
  var showAvatarSelector by remember { mutableStateOf(false) }
  val context = LocalContext.current

  Scaffold(
    containerColor = AppCustomTheme.colorScheme.groupedBackground,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.label_user_profile), fontWeight = FontWeight.SemiBold) },
        // back button
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "", Modifier.size(20.dp))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Transparent
        )
      )
    },
  ) { innerPadding ->
    Column(
      Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      UserAvatar(
        avatar = profile.avatar,
        size = 70.dp,
        shape = RoundedCornerShape(28.dp),
      )
      Spacer(modifier = Modifier.size(8.dp))
      Text(
        profile.username,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = AppCustomTheme.colorScheme.primaryLabel
      )

      Spacer(modifier = Modifier.size(23.dp))

      SectionListSection {
        SectionListItem(
          icon = CssGgIcons.Rename,
          title = stringResource(R.string.label_button_edit_username),
          modalContent = {
            createInput(
              label = context.getString(R.string.hint_new_username),
              autoFocus = true,
              hideOnSubmit = true,
              initialValue = profile.username,
              showConfirmButton = true
            ) {
              println("new username: $it")
              onUpdateUsername(it)
            }
          },
          noIconBg = true
        )
        HorizontalDivider(
          modifier = Modifier.padding(horizontal = 16.dp),
          thickness = 0.5.dp,
          color = MaterialTheme.colorScheme.surfaceDim
        )
        SectionListItem(
          icon = Icons.Default.Face,
          title = stringResource(R.string.label_button_select_avatar),
          onClick = {
            showAvatarSelector = true
          },
          noIconBg = true
        )
        HorizontalDivider(
          modifier = Modifier.padding(horizontal = 16.dp),
          thickness = 0.5.dp,
          color = MaterialTheme.colorScheme.surfaceDim
        )
        SectionListItem(
          icon = Icons.Default.Refresh,
          title = stringResource(R.string.label_button_restore_avatar),
          onClick = {
            onUpdateAvatar(null)
          },
          noIconBg = true
        )


        // TODO: 分享用户卡片
//        HorizontalDivider(
//          modifier = Modifier.padding(horizontal = 16.dp),
//          color = MaterialTheme.colorScheme.surfaceDim
//        )
//        SectionListItem(
//          icon = CssGgIcons.Share,
//          title = "分享名片",
//        )
      }
    }
  }

  if (showAvatarSelector) {
    MediaSelector.BottomModal(
      onMediaSelected = {
        onUpdateAvatar(it as ImageMediaItem)
      },
      onDismiss = {
        showAvatarSelector = false
      },
      type = MediaSelector.MediaType.Image
    )
  }
}

@Composable
fun ProfileSettingPage(
  viewModel: UserProfileViewModel = hiltViewModel(),
  onBack: () -> Unit
) {
  // TODO: all flow collect and observeAsState using initial value or create custom status sealed class?
  // including AppPreferences.getDefaultInstance() (data store)
  val profile by viewModel.profile.observeAsState(UserProfile())

  ProfileSettingPageInner(
    profile = profile,
    onUpdateUsername = viewModel::updateUsername,
    onUpdateAvatar = viewModel::updateAvatar,
    onBack = onBack
  )
}

@Preview(showSystemUi = true)
@Composable
fun ProfileSettingPagePreview() {
  var profile by remember {
    mutableStateOf(UserProfile().apply {
      username = "jctaoo"
    })
  }
  val pathManager = PathManager(LocalContext.current)

  CompositionLocalProvider(LocalPathManager provides pathManager) {
    AIGroupAppTheme {
      ProfileSettingPageInner(
        profile = profile,
        onUpdateUsername = {
          profile = UserProfile().apply {
            username = it
            avatar = profile.avatar
          }
        },
        onUpdateAvatar = {
          profile = UserProfile().apply {
            username = profile.username
            avatar = it
          }
        },
        onBack = {},
      )
    }
  }
}