package com.aigroup.aigroupmobile.ui.pages.chat.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.services.chat.plugins.BuiltInPlugins
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get

@Composable
fun ExtraBarPlugin(
  pluginList: List<String>,
  enablePlugin: (String) -> Unit,
  disablePlugin: (String) -> Unit
) {
  LazyColumn(
    Modifier
      .padding(horizontal = 12.dp, vertical = 12.dp)
      .clip(MaterialTheme.shapes.large)
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .padding(vertical = 5.dp),
  ) {
    items(BuiltInPlugins.plugins) { plugin ->
      val code = plugin.name

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            Modifier
              .clip(MaterialTheme.shapes.extraSmall)
              .background(plugin.tintColor)
              .padding(5.dp)
          ) {
            Icon(
              plugin.icon(),
              "",
              tint = plugin.iconColor,
              modifier = Modifier.size(15.dp)
            )
          }
          Spacer(Modifier.width(6.dp))
          Text(plugin.displayName, style = MaterialTheme.typography.bodyMedium, color = AppCustomTheme.colorScheme.primaryLabel)
        }
        Spacer(Modifier.weight(1f))
        Switch(
          pluginList.contains(code),
          onCheckedChange = {
            if (it) {
              enablePlugin(code)
            } else {
              disablePlugin(code)
            }
          },
          colors = SwitchDefaults.colors(
            checkedTrackColor = AppCustomTheme.colorScheme.primaryAction,
          ),
          modifier = Modifier.scale(0.6f)
        )
      }
    }
  }
}