package com.aigroup.aigroupmobile.ui.pages.assistant.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigroup.aigroupmobile.data.models.BotAssistant
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MessageSenderBot
import com.aigroup.aigroupmobile.data.models.RemoteAssistant
import com.aigroup.aigroupmobile.repositories.AssistantRepository
import com.aigroup.aigroupmobile.ui.components.BotAvatar
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.utils.common.fromDateString
import com.aigroup.aigroupmobile.utils.common.readableStr
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantStoreItem(assistant: RemoteAssistant, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.padding(8.dp),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ),
    onClick = onClick
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        // bot avatar
        // TODO: add border
        BotAvatar(assistant, size = 35.dp)

        Spacer(modifier = Modifier.width(10.dp))

        Column() {
          Text(
            text = assistant.metadata.title,
            style = MaterialTheme.typography.titleMedium,
          )

          // created at
          Row(
            verticalAlignment = Alignment.CenterVertically
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
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // assistant description
      Text(
        text = assistant.metadata.description,
        style = MaterialTheme.typography.bodyLarge,
        color = AppCustomTheme.colorScheme.primaryLabel,
        fontSize = 14.sp,
      )

      Spacer(modifier = Modifier.height(12.dp))

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
}

@Preview(showSystemUi = true)
@Composable
fun PreviewAssistantStoreItem() {
  val context = LocalContext.current
  val repo = AssistantRepository(context)

  val assistants = remember() {
    repo.getAssistants()
  }

  AIGroupAppTheme {
    Column {
      assistants.forEach {
        AssistantStoreItem(it, modifier = Modifier.fillMaxWidth())
      }
    }
  }
}