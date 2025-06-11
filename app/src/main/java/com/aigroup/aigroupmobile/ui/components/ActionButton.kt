package com.aigroup.aigroupmobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme

@Composable
fun ActionButton(
  title: String,
  icon: ImageVector,
  toggle: Boolean = false,
  enable: Boolean = true,
  loading: Boolean = false,
  titleFontSize: TextUnit = 13.sp,
  onClick: () -> Unit = {}
) {
  val contentColor by animateColorAsState(
    targetValue = if (toggle) AppCustomTheme.colorScheme.onPrimaryAction else AppCustomTheme.colorScheme.onSecondaryAction
  )
  val containerColor by animateColorAsState(
    targetValue = if (toggle) AppCustomTheme.colorScheme.primaryAction else AppCustomTheme.colorScheme.secondaryAction
  )

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    FilledIconButton(
      onClick = { onClick() },

      shape = MaterialTheme.shapes.medium,
      enabled = enable,
      colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = containerColor,
        contentColor = contentColor
      ),
      modifier = Modifier
        .border(0.5.dp, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.medium)
        .size(55.dp)
    ) {
      AnimatedContent(loading) {
        if (it) {
          CircularProgressIndicator(
            color = contentColor,
            strokeWidth = 3.dp,
            modifier = Modifier.size(20.dp)
          )
        } else {
          Icon(icon, title, modifier = Modifier.size(30.dp))
        }
      }
    }
    Spacer(Modifier.height(5.dp))
    Text(
      title,
      style = MaterialTheme.typography.bodyMedium.copy(lineHeight = (titleFontSize.value + 2.sp.value).sp),
      color = AppCustomTheme.colorScheme.secondaryLabel,
      fontSize = titleFontSize,
      textAlign = TextAlign.Center
    )
  }
}