package com.aigroup.aigroupmobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import compose.icons.CssGgIcons
import compose.icons.cssggicons.User
import compose.icons.cssggicons.Userlane

@Composable
fun UserAvatar(
  modifier: Modifier = Modifier,
  avatar: ImageMediaItem? = null,
  onClick: (() -> Unit)? = null,
  shape: Shape = CircleShape,
  size: Dp = 80.dp,
) {
  if (LocalInspectionMode.current || avatar == null) {
    Image(
      painter = painterResource(R.drawable.user_default_avatar),
      contentDescription = "avatar",
      contentScale = ContentScale.Crop,
      modifier = modifier
        .size(size)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .clickable(enabled = onClick != null) { onClick?.invoke() }
    )
  } else {
    AsyncImage(
      avatar.url,
      "预览",
      contentScale = ContentScale.Crop,
      modifier = modifier
        .size(size)
        .clip(shape)
        .clickable(enabled = onClick != null) { onClick?.invoke() }
    )
  }
}

@Preview
@Composable
private fun UserAvatarPreview() {
  UserAvatar(shape = RectangleShape)
}