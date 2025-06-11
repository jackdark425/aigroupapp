@file:OptIn(ExperimentalMaterial3Api::class)

package com.aigroup.aigroupmobile.ui.pages.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathUtils
import com.aigroup.aigroupmobile.LocalPathManager
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.Screen
import com.aigroup.aigroupmobile.data.models.UserProfile
import com.aigroup.aigroupmobile.ui.components.UserAvatar
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.ui.utils.border
import com.aigroup.aigroupmobile.utils.system.PathManager
import com.aigroup.aigroupmobile.utils.common.localDateTime
import com.aigroup.aigroupmobile.utils.common.readableStr
import com.aigroup.aigroupmobile.utils.common.simpleDateStr
import compose.icons.CssGgIcons
import compose.icons.FontAwesomeIcons
import compose.icons.cssggicons.ArrowRight
import compose.icons.cssggicons.EditUnmask
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Comment
import compose.icons.fontawesomeicons.regular.FrownOpen
import compose.icons.fontawesomeicons.solid.Pen
import compose.icons.fontawesomeicons.solid.PenNib
import compose.icons.fontawesomeicons.solid.PencilAlt
import compose.icons.fontawesomeicons.solid.Share
import compose.icons.fontawesomeicons.solid.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// TODO: remove this data class !!! (REFACTOR)
data class UserProfileInfo(
  val title: String,
  val icon: ImageVector,
  val content: String,
  val iconModifier: Modifier = Modifier,
)

@Composable
private fun UserProfileCard(info: UserProfileInfo) {
  val (title, icon, content) = info

  Card(
    modifier = Modifier
      .padding(8.dp),
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ),
  ) {
    Box(
      modifier = Modifier
        .width(130.dp)
        .height(80.dp)
        .padding(horizontal = 12.dp)
        .padding(top = 12.dp)
        .padding(bottom = 11.dp)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(text = title, style = MaterialTheme.typography.bodyMedium)
          Spacer(modifier = Modifier.weight(1f))
          Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = info.iconModifier
          )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
          text = content,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.secondary,
          fontWeight = FontWeight.Medium,
          fontSize = 19.sp,
          modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
        )
      }
    }
  }
}

@Composable
fun UserProfileContentData(
  modifier: Modifier,
  userProfile: UserProfile,
  onOpenPage: (Screen) -> Unit = { },
  userProfileInfo: List<UserProfileInfo> = listOf(),
) {
  Column(modifier = modifier.padding(top = 10.dp)) {
    Row(
      modifier = Modifier.padding(horizontal = 18.dp),
      verticalAlignment = Alignment.Bottom
    ) {
      UserAvatar(
        size = 58.dp,
        avatar = userProfile.avatar,
        shape = RoundedCornerShape(20.dp),
        onClick = {
          onOpenPage(Screen.UserProfileSetting)
        }
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            userProfile.username,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
          )
          Spacer(modifier = Modifier.width(9.dp))
          Icon(
            FontAwesomeIcons.Solid.Pen,
            "",
            tint = AppCustomTheme.colorScheme.secondaryLabel,
            modifier = Modifier
              .clip(MaterialTheme.shapes.small)
              .clickable {
                onOpenPage(Screen.UserProfileSetting)
              }
              .padding(5.dp)
              .size(15.dp)
          )
        }
        Text(
          stringResource(R.string.label_user_join_at, userProfile.id.localDateTime.simpleDateStr),
          style = MaterialTheme.typography.bodySmall,
          color = AppCustomTheme.colorScheme.secondaryLabel,
        )
        Spacer(Modifier.height(3.dp))
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Row(
      modifier = Modifier
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 8.dp),
    ) {
      for (info in userProfileInfo) {
        UserProfileCard(info)
      }
    }
  }
}

@Preview(showSystemUi = true)
@Composable
fun UserProfileContentDataPreview() {
  val pathManager = PathManager(LocalContext.current)

  CompositionLocalProvider(LocalPathManager provides pathManager) {
    Box(
      modifier = Modifier.fillMaxSize()
    ) {
      val bgColor1 = Color(0x64DBCEEF)
      val bgColor2 = Color(0x6499AFED)

      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            brush = Brush.linearGradient(
              colors = listOf(
                bgColor1,
                bgColor2,
              )
            )
          )
      )

      UserProfileContentData(
        Modifier.align(Alignment.Center),
        UserProfile().apply { username = "jctaoo" },
        userProfileInfo = listOf(
          UserProfileInfo("聊天会话", FontAwesomeIcons.Regular.Comment, "3个", iconModifier = Modifier.size(18.dp)),
          UserProfileInfo("聊天会话", FontAwesomeIcons.Regular.Comment, "3个", iconModifier = Modifier.size(18.dp)),
          UserProfileInfo("聊天会话", FontAwesomeIcons.Regular.Comment, "3个", iconModifier = Modifier.size(18.dp)),
        )
      )
    }
  }
}