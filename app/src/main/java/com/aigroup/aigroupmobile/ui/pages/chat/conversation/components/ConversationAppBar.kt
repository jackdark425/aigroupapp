package com.aigroup.aigroupmobile.ui.pages.chat.conversation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.ui.components.DarkToggleButton
import com.aigroup.aigroupmobile.ui.components.SegmentText
import com.aigroup.aigroupmobile.ui.components.SegmentedControl
import com.aigroup.aigroupmobile.ui.components.UserAvatar
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.ui.utils.clearFocusOnKeyboardDismiss
import compose.icons.CssGgIcons
import compose.icons.cssggicons.ArrowUp
import compose.icons.cssggicons.Close
import compose.icons.cssggicons.CloseO
import compose.icons.cssggicons.Collage
import compose.icons.cssggicons.MathPlus
import compose.icons.cssggicons.Search
import java.security.Key

object ConversationAppBar {

  enum class Mode {
    Conversation,
    Assistants,
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun TopAppBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior?,
    avatar: ImageMediaItem? = null,
    onHideDrawer: () -> Unit = {},
    showHideDrawerButton: Boolean = true,

    selectedMode: Mode = Mode.Conversation,
    onModeChange: (Mode) -> Unit = {},
  ) {
    androidx.compose.material3.TopAppBar(
      modifier = modifier,
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
      ),
      title = {
        SegmentedControl(
          remember { Mode.entries },
          selectedMode,
          modifier = Modifier.width(200.dp),
          onSegmentSelected = { onModeChange(it) },
        ) {
          SegmentText(
            when (it) {
              Mode.Conversation -> stringResource(R.string.label_session_history)
              Mode.Assistants -> stringResource(R.string.label_assistants)
            },
          )
        }
      },
      actions = {
        // TODO: 完成会话整理功能
//        IconButton(
//          onClick = { /*TODO*/ },
//        ) {
//          Icon(
//            imageVector = CssGgIcons.Collage,
//            contentDescription = "整理",
//          )
//        }

        if (showHideDrawerButton) {
          IconButton(
            onClick = {
              onHideDrawer()
            }
          ) {
            Icon(
              Icons.Default.KeyboardArrowRight, ""
            )
          }
        }
      },
      scrollBehavior = scrollBehavior,
//        colors = TopAppBarDefaults.topAppBarColors(
//            containerColor = Color.Transparent
//        ),
    )
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun TopAppBarSurface(
    modifier: Modifier = Modifier,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    content: @Composable () -> Unit,
  ) {
//    val colorTransitionFraction = scrollBehavior?.state?.overlappedFraction ?: 0f
//    val fraction = if (colorTransitionFraction > 0.01f) 1f else 0f
//    val appBarContainerColor by animateColorAsState(
//      targetValue = lerp(
//        colors.containerColor,
////            Color.Transparent,
//        colors.scrolledContainerColor,
//        FastOutLinearInEasing.transform(fraction),
//      ),
//      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
//      label = "TopBarSurfaceContainerColorAnimation",
//    )
    Surface(
      modifier = modifier.fillMaxWidth(),
      color = Color.Transparent,
      content = content,
    )
  }

  @Composable
  @OptIn(ExperimentalMaterial3Api::class)
  fun EmbeddedSearchBar(
    modifier: Modifier = Modifier,
    onSearch: ((String) -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onTop: () -> Unit = {},
    selectedMode: Mode = Mode.Conversation,
  ) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val topBtnAnimationBase = scrollBehavior?.state?.overlappedFraction ?: 0f

    fun onTopBtnClick() {
      onTop()
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 12.dp)
    ) {
      FilledIconButton(
        onClick = { onTopBtnClick() },
        modifier = Modifier.size((topBtnAnimationBase * 30).dp),
        colors = IconButtonDefaults.iconButtonColors(
          containerColor = AppCustomTheme.colorScheme.primaryAction,
          contentColor = AppCustomTheme.colorScheme.onPrimaryAction
        )
      ) {
        Icon(
          CssGgIcons.ArrowUp,
          "返回顶部",
        )
      }

      Spacer(modifier = Modifier.size((topBtnAnimationBase * 8).dp))

      BasicTextField(
        searchQuery,
        { searchQuery = it },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppCustomTheme.colorScheme.primaryLabel),
        maxLines = 1,
        keyboardOptions = KeyboardOptions(
          imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
          onSearch = {
            onSearch?.invoke(searchQuery)
          }
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(38.dp)
          .clearFocusOnKeyboardDismiss()
      ) { innerTextField ->
        TextFieldDefaults.DecorationBox(
          value = searchQuery,
          innerTextField = innerTextField,
          enabled = true,
          singleLine = true,
          visualTransformation = VisualTransformation.None,
          interactionSource = remember { MutableInteractionSource() },
          contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
            top = 0.dp,
            bottom = 0.dp,
            start = 0.dp
          ),
          shape = MaterialTheme.shapes.medium,
          colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            cursorColor = AppCustomTheme.colorScheme.primaryAction,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          ),
          leadingIcon = {
            Icon(
              CssGgIcons.Search,
              contentDescription = null,
              tint = AppCustomTheme.colorScheme.secondaryLabel,
              modifier = Modifier.size(18.dp)
            )
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(
                onClick = { searchQuery = ""; onSearch?.invoke("") },
              ) {
                Icon(
                  CssGgIcons.Close,
                  contentDescription = "清空搜索",
                  Modifier.size(15.dp)
                )
              }
            }
          },
          placeholder = {
            Text(
              // TODO: try kotlin realm TEXT 谓词
              when (selectedMode) {
                Mode.Conversation -> stringResource(R.string.hint_placeholder_search_session)
                Mode.Assistants -> stringResource(R.string.hint_placeholder_search_assistants)
              },
              style = MaterialTheme.typography.bodyMedium,
              color = AppCustomTheme.colorScheme.secondaryLabel
            )
          }
        )
      }
    }
  }

}