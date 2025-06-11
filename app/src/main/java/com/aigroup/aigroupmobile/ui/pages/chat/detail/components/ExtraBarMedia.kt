package com.aigroup.aigroupmobile.ui.pages.chat.detail.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.ui.components.MediaSelector
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.viewmodels.ChatBottomBarState

@Composable
fun ExtraBarMedia(
  onStateChanged: (ChatBottomBarState) -> Unit,
  bottomBarState: ChatBottomBarState,
  model: ModelCode? = null,
) {
  val onMediaSelected: (MediaItem?) -> Unit = {
    onStateChanged(bottomBarState.copy(mediaItem = it))
  }

  val supportVideo = model?.supportVideo == true

  Row(Modifier.padding(horizontal = 15.dp)) {
    MediaSelector.TakeGalleryButton(
      onMediaSelected = onMediaSelected,
      type = if (supportVideo) MediaSelector.MediaType.ImageAndVideo else MediaSelector.MediaType.Image,
    ) {
      Button(
        onClick = ::onClick, shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
          containerColor = AppCustomTheme.colorScheme.secondaryAction,
          contentColor = AppCustomTheme.colorScheme.onSecondaryAction,
        ),
        modifier = Modifier.weight(1f)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            ImageVector.vectorResource(R.drawable.ic_vision_icon_legacy),
            "",
            tint = AppCustomTheme.colorScheme.onSecondaryAction,
            modifier = Modifier.size(17.dp)
          )
          Spacer(Modifier.width(5.dp))
          Text(stringResource(R.string.label_gallery), style = MaterialTheme.typography.bodySmall)
        }
      }
    }

    Spacer(Modifier.width(9.dp))

    MediaSelector.TakePhotoButton(
      onMediaSelected = onMediaSelected,
    ) {
      Button(
        onClick = ::onClick, shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
          containerColor = AppCustomTheme.colorScheme.secondaryAction,
          contentColor = AppCustomTheme.colorScheme.onSecondaryAction,
        ),
        modifier = Modifier.weight(1f)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            ImageVector.vectorResource(R.drawable.ic_camera_icon),
            "",
            tint = AppCustomTheme.colorScheme.onSecondaryAction,
            modifier = Modifier.size(20.dp)
          )
          Spacer(Modifier.width(5.dp))
          Text(stringResource(R.string.label_take_photo), style = MaterialTheme.typography.bodySmall)
        }
      }
    }

    if (supportVideo) {
      Spacer(Modifier.width(9.dp))

      MediaSelector.TakeVideoButton(
        onMediaSelected = onMediaSelected,
      ) {
        Button(
          onClick = ::onClick, shape = MaterialTheme.shapes.small,
          colors = ButtonDefaults.buttonColors(
            containerColor = AppCustomTheme.colorScheme.secondaryAction,
            contentColor = AppCustomTheme.colorScheme.onSecondaryAction,
          ),
          modifier = Modifier.weight(1f)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              ImageVector.vectorResource(R.drawable.ic_video_icon),
              "",
              tint = AppCustomTheme.colorScheme.onSecondaryAction,
              modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(text = stringResource(R.string.label_take_video), style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
}