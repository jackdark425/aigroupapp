package com.aigroup.aigroupmobile.ui.pages.chat.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.utils.whenDarkMode
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.File

data class BotAbility(
  val icon: ImageVector,
  val title: String
)

@Composable
fun AbilityIntroItem(
  modifier: Modifier = Modifier,
  ability: BotAbility,
) {
  val (icon, title) = ability

  Box(
    modifier
      .background(
        MaterialTheme.colorScheme.surfaceContainer.whenDarkMode(MaterialTheme.colorScheme.surfaceContainerHighest),
        shape = MaterialTheme.shapes.medium
      )
      .padding(16.dp)
  ) {
    Column(Modifier.height(110.dp)) {
      Box(
        Modifier
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.secondaryContainer)
          .padding(10.dp)
      ) {
        Icon(
          icon, "",
          tint = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier.size(15.dp)
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      Text(title, style = MaterialTheme.typography.titleSmall)
    }
  }
}

@Preview
@Composable
fun AbilityIntroItemPreview() {
  AIGroupAppTheme {
    AbilityIntroItem(
      ability = BotAbility(FontAwesomeIcons.Solid.File, "File"),
      modifier = Modifier.width(200.dp)
    )
  }
}