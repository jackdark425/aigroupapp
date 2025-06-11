package com.aigroup.aigroupmobile.ui.pages.chat.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.Constants
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.VideoMediaItem
import com.aigroup.aigroupmobile.ui.components.MediaSelector
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.viewmodels.ChatBottomBarState
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExtraBarMediaPreview(
  bottomBarState: ChatBottomBarState,
  onStateChanged: (ChatBottomBarState) -> Unit
) {
  val isVideo = bottomBarState.mediaItem is VideoMediaItem
  val suggestions by remember { derivedStateOf { Constants.getQuestionOfMedia(isVideo) } }

  Row(
    Modifier
      .padding(horizontal = 6.dp, vertical = 12.dp)
      .padding(vertical = 5.dp),
  ) {
    MediaSelector.Previewer(
      media = bottomBarState.mediaItem!!,
    ) {
      onStateChanged(bottomBarState.copy(mediaItem = it))
    }

    Spacer(Modifier.width(10.dp))

    Column(
      Modifier.weight(1f)
    ) {
      Text(
        stringResource(R.string.label_guess_question_for_media_prompt),
        style = MaterialTheme.typography.bodySmall,
        color = AppCustomTheme.colorScheme.secondaryLabel
      )
      Spacer(Modifier.height(6.dp))
      FlowRow(
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Top),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),
      ) {
        for (suggestion in suggestions) {
          Box(
            Modifier
              .clip(MaterialTheme.shapes.small)
              .background(MaterialTheme.colorScheme.surfaceContainerLow)
              .clickable {
                onStateChanged(
                  bottomBarState.copy(inputText = buildAnnotatedString {
                    append(suggestion)
                  })
                )
              }
              .padding(vertical = 4.dp, horizontal = 8.dp)
          ) {
            Text(
              suggestion,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }
  }
}

@Preview
@Composable
fun ExtraBarMediaPreviewPreview() {
  AIGroupAppTheme {
    ExtraBarMediaPreview(
      ChatBottomBarState(
        mediaItem = ImageMediaItem()
      ),
      onStateChanged = {}
    )
  }
}