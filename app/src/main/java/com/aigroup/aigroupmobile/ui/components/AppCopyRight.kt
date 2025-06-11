package com.aigroup.aigroupmobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme

@Composable
fun AppCopyRight(modifier: Modifier = Modifier) {
  val appName = stringResource(id = R.string.app_name)

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      ImageVector.vectorResource(R.drawable.ic_cp_app_icon), "",
      tint = AppCustomTheme.colorScheme.tertiaryLabel
    )
    Text(
      "Copyright © 2024 $appName Rights Reserved.",
      style = MaterialTheme.typography.labelSmall,
      color = AppCustomTheme.colorScheme.tertiaryLabel,
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun AppCopyRightPreview() {
  AIGroupAppTheme {
    AppCopyRight()
  }
}