package com.aigroup.aigroupmobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Size defaults.
internal val MenuVerticalMargin = 48.dp
private val MenuListItemContainerHeight = 38.dp
private val DropdownMenuItemHorizontalPadding = 4.dp
internal val DropdownMenuVerticalPadding = 8.dp
private val DropdownMenuItemDefaultMinWidth = 112.dp
private val DropdownMenuItemDefaultMaxWidth = 280.dp

@Composable
internal fun AppDropdownMenuItem(
  text: @Composable () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  leadingIcon: @Composable (() -> Unit)? = {},
  trailingIcon: @Composable (() -> Unit)? = {},
  enabled: Boolean = true,
  contentPadding: PaddingValues = PaddingValues(start = 12.dp, end = 22.dp),
  itemMinHeight: Dp = MenuListItemContainerHeight,
  interactionSource: MutableInteractionSource? = null,
  danger: Boolean = false
) {
  Row(
    modifier =
    modifier
      .clickable(
        enabled = enabled,
        onClick = onClick,
//        interactionSource = interactionSource,
//        indication = rippleOrFallbackImplementation(true)
      )
      .fillMaxWidth()
      // Preferred min and max width used during the intrinsic measurement.
      .sizeIn(
        minWidth = DropdownMenuItemDefaultMinWidth,
        maxWidth = DropdownMenuItemDefaultMaxWidth,
        minHeight = itemMinHeight
      )
      .padding(contentPadding),
    verticalAlignment = Alignment.CenterVertically
  ) {
    val contentColor = if (danger) {
      MaterialTheme.colorScheme.error
    } else {
      MaterialTheme.colorScheme.onSurface
    }

    // TODO(b/271818892): Align menu list item style with general list item style.
    ProvideTextStyle(MaterialTheme.typography.labelLarge) {
      if (leadingIcon != null) {
        CompositionLocalProvider(
          LocalContentColor provides contentColor,
        ) {
          Box(Modifier.defaultMinSize(minWidth = 20.dp)) {
            leadingIcon()
          }
        }
      }
      CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
          Modifier
            .weight(1f)
            .padding(
              start = if (leadingIcon != null) {
                DropdownMenuItemHorizontalPadding
              } else {
                0.dp
              },
              end = if (trailingIcon != null) {
                DropdownMenuItemHorizontalPadding
              } else {
                0.dp
              }
            )
        ) {
          text()
        }
      }
      if (trailingIcon != null) {
        CompositionLocalProvider(
          LocalContentColor provides contentColor,
        ) {
          Box(Modifier.defaultMinSize(minWidth = 24.dp)) {
            trailingIcon()
          }
        }
      }
    }
  }
}