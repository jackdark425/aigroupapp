@file:OptIn(ExperimentalFoundationApi::class)

package com.aigroup.aigroupmobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.ui.utils.advancedShadow
import com.aigroup.aigroupmobile.ui.utils.whenDarkMode
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Hourglass
import compose.icons.fontawesomeicons.solid.Moon
import compose.icons.fontawesomeicons.solid.Sun

@Composable
fun SectionListItem(
  icon: ImageVector? = null,
  title: String,
  description: String? = null,
  trailingDetailContent: @Composable (() -> Unit)? = null,
  trailingContent: @Composable (() -> Unit)? = null,
  onClick: (() -> Unit)? = null,
  danger: Boolean = false,
  loading: Boolean = false,
  iconContent: @Composable (() -> Unit)? = null,
  modalContent: (@Composable SettingItemSelectionScope.() -> Unit)? = null,
  iconTint: Color? = null,
  iconBg: Color? = null,

  contentPadding: PaddingValues? = null,
  titleTextStyle: TextStyle? = null,

  modifier: Modifier = Modifier,
  // TODO: remove this parameter using composable block
  iconModifier: Modifier = Modifier,
  noIconBg: Boolean = false
) {
  var showSettingSelectionModal by remember { mutableStateOf(false) }

  if (showSettingSelectionModal) {
    SettingItemSelection(
      onDismiss = { showSettingSelectionModal = false },
    ) {
      modalContent?.invoke(this)
    }
  }

  Box(
    modifier
      .padding(4.dp)
      .clip(MaterialTheme.shapes.medium)
      .clickable(
        enabled = onClick != null || modalContent != null,
        onClick = {
          if (modalContent != null) {
            showSettingSelectionModal = true
          } else {
            onClick?.invoke()
          }
        }
      )
      .let {
        if (contentPadding != null) {
          it.padding(contentPadding)
        } else {
          it.padding(
            horizontal = if (noIconBg) 18.dp else 12.dp,
            vertical = if (noIconBg) 12.dp else 8.dp
          )
        }
      }
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (noIconBg) {
        iconContent?.invoke() ?: icon?.let {
          Icon(
            it, null,
            modifier = iconModifier.size(16.dp),
            tint = iconTint ?: if (danger) MaterialTheme.colorScheme.error else LocalContentColor.current
          )
        }
      } else {
        Box(
          modifier = Modifier
            .advancedShadow(
              cornersRadius = 4.dp,
              offsetY = 1.dp,
              shadowBlurRadius = 2.dp,
              alpha = 0.2f,
              color = Color.Black.whenDarkMode(Color.DarkGray)
            )
            .background(iconBg ?: MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp))
            .padding(6.dp)
        ) {
          iconContent?.invoke() ?: icon?.let {
            Icon(
              it, null,
              modifier = iconModifier.size(14.dp),
              tint = iconTint ?: if (danger) MaterialTheme.colorScheme.error else LocalContentColor.current
            )
          }
        }
      }

      Spacer(modifier = Modifier.size(if (noIconBg) 16.dp else 12.dp))

      if (description == null) {
        Box(
          Modifier
            .padding(vertical = 3.dp)
            .weight(1f, fill = true)
        ) {
          Text(
            text = title,
            style = titleTextStyle ?: if (noIconBg) {
              MaterialTheme.typography.bodyLarge
            } else {
              MaterialTheme.typography.bodyMedium
            },
            modifier = Modifier
              .basicMarquee(animationMode = MarqueeAnimationMode.Immediately),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      } else {
        Column(modifier = Modifier.weight(1f, fill = true)) {
          Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = AppCustomTheme.colorScheme.secondaryLabel
          )
        }
      }

      if (trailingContent == null) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.End,
          modifier = Modifier.widthIn(max = 140.dp)
        ) {
          AnimatedContent(loading) {
            when (it) {
              true -> {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = AppCustomTheme.colorScheme.primaryAction
                )
              }

              else -> {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  trailingDetailContent?.let {
                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                      CompositionLocalProvider(LocalContentColor provides AppCustomTheme.colorScheme.secondaryLabel) {
                        Box(Modifier) {
                          it()
                        }
                      }
                    }
                  }
                  Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    null,
                    tint = AppCustomTheme.colorScheme.secondaryLabel.copy(alpha = 0.4f),
                    modifier = Modifier.size(12.dp)
                  )
                }
              }
            }
          }
        }
      } else {
        Box(Modifier.wrapContentSize()) {
          trailingContent()
        }
      }
    }
  }
}

@Composable
fun SectionListSection(
  sectionHeader: String? = null,
  showTitle: Boolean = true,

  titleTextStyle: TextStyle? = null,
  titleTextColor: Color? = null,
  topSpacing: Dp = 8.dp,
  sectionShape: Shape = MaterialTheme.shapes.large,

  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  Column(modifier) {
    if (sectionHeader != null && showTitle) {
      SectionListSectionHeader(sectionHeader, titleTextStyle, titleTextColor, topSpacing)
    } else {
      Spacer(modifier = Modifier.height(topSpacing))
    }
    Surface(
      shape = sectionShape,
      shadowElevation = 0.dp,
      tonalElevation = 0.dp,
      color = MaterialTheme.colorScheme.surfaceContainerLowest,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column {
        content()
      }
    }
  }
}

@Composable
fun SectionListSectionHeader(
  title: String,
  titleTextStyle: TextStyle? = null,
  titleTextColor: Color? = null,
  topSpacing: Dp = 8.dp,
  modifier: Modifier = Modifier
) {
  Text(
    text = title,
    style = titleTextStyle ?: MaterialTheme.typography.titleSmall,
    color = titleTextColor ?: MaterialTheme.colorScheme.tertiary,
    modifier = modifier.padding(horizontal = 8.dp, vertical = topSpacing)
  )
}

@Composable
fun SectionListSectionFooter(footer: String, modifier: Modifier = Modifier) {
  Text(
    text = footer,
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.tertiary,
    modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp)
  )
}

fun <T> LazyListScope.section(
  title: String,
  data: List<T> = emptyList(),
  footer: String? = null,
  key: ((T) -> String)? = null,
  stickyHeader: Boolean = true,
  // TODO: conflect with TopSpacing
  contentPadding: PaddingValues = PaddingValues(0.dp),
  itemContent: @Composable (T) -> Unit = {}
) {
  if (data.isNotEmpty()) {
    if (stickyHeader) {
      stickyHeader(key = title) {
        SectionListSectionHeader(
          title,
          modifier = Modifier
            .padding(
              top = contentPadding.calculateTopPadding(),
              start = (contentPadding.calculateStartPadding(LocalLayoutDirection.current) - 3.dp).coerceAtLeast(0.dp),
              end = (contentPadding.calculateEndPadding(LocalLayoutDirection.current) - 3.dp).coerceAtLeast(0.dp)
            )
            .animateItem()
        )
      }
    } else {
      item(key = title) {
        SectionListSectionHeader(
          title,
          modifier = Modifier
            .padding(
              top = contentPadding.calculateTopPadding(),
              start = (contentPadding.calculateStartPadding(LocalLayoutDirection.current) - 3.dp).coerceAtLeast(0.dp),
              end = (contentPadding.calculateEndPadding(LocalLayoutDirection.current) - 3.dp).coerceAtLeast(0.dp)
            )
            .animateItem()
        )
      }
    }
  }

  // TODO: key should using better solution
  items(data.count(), key = key?.let { key -> { key(data[it]) + "-$title" } }) { idx ->
    val shape = when {
      data.count() == 1 -> MaterialTheme.shapes.large
      idx == 0 -> MaterialTheme.shapes.large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
      idx == data.size - 1 -> MaterialTheme.shapes.large.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp))
      else -> RectangleShape
    }
    Surface(
      shape = shape,
      shadowElevation = 0.dp,
      tonalElevation = 0.dp,
      color = MaterialTheme.colorScheme.surfaceContainerLowest,
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
          end = contentPadding.calculateEndPadding(LocalLayoutDirection.current)
        )
        .animateItem(),
    ) {
      Column(Modifier.fillMaxSize()) {
        itemContent(data[idx])
        if (idx < data.size - 1) {
          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.surfaceDim
          )
        }
      }
    }

    if (idx == data.lastIndex) {
      Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
    }
  }

  if (footer != null && data.isNotEmpty()) {
    item(key = footer) {
      SectionListSectionFooter(
        footer,
        modifier = Modifier
          .padding(
            bottom = contentPadding.calculateBottomPadding(),
            start = contentPadding.calculateStartPadding(LocalLayoutDirection.current) - 3.dp,
            end = contentPadding.calculateEndPadding(LocalLayoutDirection.current) - 3.dp
          )
          .animateItem()
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
fun PreviewSectionList() {
  AIGroupAppTheme {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .background(AppCustomTheme.colorScheme.groupedBackground)
    ) {
      Column {
        SectionListSection() {
          SectionListItem(FontAwesomeIcons.Regular.Hourglass, "Subscription")
          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceDim
          )
          SectionListItem(FontAwesomeIcons.Regular.Hourglass, "Subscription", trailingContent = {
            LittleSwitch(
              checked = true,
              onCheckedChange = { },
              modifier = Modifier,
            )
          })
        }

        SectionListSection("Account") {
          SectionListItem(FontAwesomeIcons.Regular.Hourglass, "Subscription")
          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceDim
          )
          SectionListItem(FontAwesomeIcons.Regular.Hourglass, "Profile")
          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceDim
          )
          SectionListItem(FontAwesomeIcons.Regular.Hourglass, "Settings")
          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceDim
          )
          SectionListItem(FontAwesomeIcons.Regular.Hourglass, "Danger Item", danger = true)
          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceDim
          )
          // loading item
          SectionListItem(FontAwesomeIcons.Regular.Hourglass, "Loading Item", loading = true)
          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceDim
          )
          // setting selection item
          SectionListItem(
            FontAwesomeIcons.Regular.Hourglass,
            "Setting Selection Item",
            modalContent = {
              createItem("Dark Mode", FontAwesomeIcons.Solid.Moon) {}
              Spacer(Modifier.height(8.dp))
              createItem("Light Mode", FontAwesomeIcons.Solid.Sun) {}
            }
          )
          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceDim
          )
          SectionListItem(
            FontAwesomeIcons.Regular.Hourglass,
            "LongItem LongItem LongItemLongItemLongItemLongItem"
          )
          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceDim
          )
          SectionListItem(
            FontAwesomeIcons.Regular.Hourglass,
            "LongItem LongItem",
            trailingDetailContent = {
              Text("DetailDetailDetailDetailDetailDetail")
            }
          )
          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceDim
          )
          SectionListItem(
            FontAwesomeIcons.Regular.Hourglass,
            "LongItem LongItem",
            description = "ssssssssssssssssssssssssssssssssssssssssssss"
          )
        }
      }
    }
  }
}