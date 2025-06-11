package com.aigroup.aigroupmobile.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme

// TODO: refactor all Switch with .scale

@Composable
fun LittleSwitch(
  modifier: Modifier = Modifier,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Switch(
    // TODO: remove this hack
    modifier = modifier.scale(0.6f).requiredSize(width = 31.2.dp, height = 19.2.dp),
    checked = checked,
    onCheckedChange = onCheckedChange,
    colors = SwitchDefaults.colors(
      checkedTrackColor = AppCustomTheme.colorScheme.primaryAction
    ),
  )
}

@Preview(showBackground = true)
@Composable
fun LittleSwitchPreview() {
  AIGroupAppTheme {
    Row(verticalAlignment = Alignment.CenterVertically) {
      LittleSwitch(
        checked = true,
        onCheckedChange = {},
      )
      Switch(
        checked = true,
        onCheckedChange = {},
      )
    }
  }
}