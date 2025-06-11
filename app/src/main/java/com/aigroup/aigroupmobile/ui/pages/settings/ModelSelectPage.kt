@file:OptIn(
  ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class,
  ExperimentalMaterial3Api::class
)

package com.aigroup.aigroupmobile.ui.pages.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.map
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.LoadingStatus
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.connect.chat.ChatServiceProvider
import com.aigroup.aigroupmobile.data.models.data
import com.aigroup.aigroupmobile.data.models.loading
import com.aigroup.aigroupmobile.data.models.mapSuccess
import com.aigroup.aigroupmobile.data.models.mutableLoadingStatusOf
import com.aigroup.aigroupmobile.repositories.ModelRepository
import com.aigroup.aigroupmobile.ui.components.ActionButton
import com.aigroup.aigroupmobile.ui.components.SectionListItem
import com.aigroup.aigroupmobile.ui.components.section
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.ui.utils.clearFocusOnKeyboardDismiss
import com.aigroup.aigroupmobile.ui.utils.withoutBottom
import com.aigroup.aigroupmobile.utils.previews.rememberTestAI
import com.aigroup.aigroupmobile.viewmodels.ConversationFilter
import com.aigroup.aigroupmobile.viewmodels.SettingsViewModel
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import compose.icons.CssGgIcons
import compose.icons.cssggicons.Search
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.shadow
import com.aigroup.aigroupmobile.connect.chat.CustomChatServiceProvider
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.aigroup.aigroupmobile.connect.chat.ServiceProvider

@Composable
private fun ModelSelectPageInner(
  pinModels: LoadingStatus<List<ModelCode>> = LoadingStatus.Success(emptyList()),
  repository: ModelRepository, // TODO: Refactor, dont use repository here

  onBack: (() -> Unit)? = null,
  onSelectModel: (ModelCode) -> Unit = { },
  showTitle: Boolean = true,

  onPinModel: (ModelCode) -> Unit = { },
  onCancelPinModel: (ModelCode) -> Unit = { },

  skipCollectModels: Boolean = false
) {
  var search by remember { mutableStateOf("") }
  var filter by remember { mutableStateOf<ServiceProvider>(ChatServiceProvider.OFFICIAL) }
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(filter) {
    // 使用新的 refreshModelsIfNeeded 方法
    repository.refreshModelsIfNeeded(filter)
  }

  val availableProviders by repository.allAvailableProviders.map {
    LoadingStatus.Success(it)
  }.collectAsStateWithLifecycle(initialValue = LoadingStatus.Loading)

  val searchedPinModels by remember(pinModels) {
    derivedStateOf {
      pinModels.mapSuccess {
        it.filter { it.toString().contains(search, ignoreCase = true) }
      }
    }
  }
  
  // 使用 when 表达式获取模型
  val specificModels = when (filter) {
    is CustomChatServiceProvider -> repository.customProviderModels(filter.id)
    is ChatServiceProvider -> repository.models(filter as ChatServiceProvider)
  }.collectAsStateWithLifecycle(initialValue = LoadingStatus.Loading)
  
  val specificModelsSearched by remember(specificModels.value, search) {
    derivedStateOf {
      specificModels.value?.mapSuccess {
        it.data.filter { it.toString().contains(search, ignoreCase = true) }
      }
    }
  }

  val context = LocalContext.current

  val listState = rememberLazyListState()

  LaunchedEffect(specificModelsSearched) {
    when (specificModelsSearched) {
      is LoadingStatus.Error -> {
        // when its loading error, scroll to the end of list to show entire error message to user
        val totalCount = listState.layoutInfo.totalItemsCount
        listState.scrollToItem(totalCount - 1)
      }

      else -> {}
    }
  }

  Scaffold(
    topBar = {
      Column {
        if (showTitle) {
          TopAppBar(
            title = { Text(stringResource(R.string.label_select_llm_model), fontWeight = FontWeight.SemiBold) },
            // back button
            navigationIcon = {
              if (onBack != null) {
                IconButton(onClick = onBack) {
                  Icon(Icons.AutoMirrored.Filled.ArrowBack, "", Modifier.size(20.dp))
                }
              }
            },
          )
        }

        // TODO: english keyboard only
        BasicTextField(
          value = search,
          onValueChange = { search = it },
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppCustomTheme.colorScheme.primaryLabel),
          maxLines = 1,
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .padding(bottom = 6.dp)
            .height(48.dp)
            .clearFocusOnKeyboardDismiss()
        ) { innerTextField ->
          TextFieldDefaults.DecorationBox(
            value = search,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = remember { MutableInteractionSource() },
            contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
              top = 0.dp,
              bottom = 0.dp
            ),
            shape = MaterialTheme.shapes.medium,
            colors = TextFieldDefaults.colors(
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              disabledIndicatorColor = Color.Transparent,
              errorIndicatorColor = Color.Transparent,
              cursorColor = AppCustomTheme.colorScheme.primaryAction,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            leadingIcon = {
              Icon(
                CssGgIcons.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
              )
            },
            placeholder = {
              Text(
                stringResource(R.string.hint_placeholder_search_llm_model),
                style = MaterialTheme.typography.bodyMedium,
                color = AppCustomTheme.colorScheme.secondaryLabel
              )
            }
          )
        }
      }
    },
    containerColor = AppCustomTheme.colorScheme.groupedBackground
  ) { innerPadding ->
    LazyColumn(
      contentPadding = innerPadding,
      state = listState
    ) {
      // Favorite models
      if (!skipCollectModels) {
        section(
          context.getString(R.string.label_stared_llm_model),
          searchedPinModels.data ?: emptyList(),
          context.getString(R.string.label_description_favorite_model),
          contentPadding = PaddingValues(horizontal = 16.dp),
          key = { it.fullCode() + "-pinned" }) { model ->
          ModelListItem(
            model, onSelectModel, isPinned = true, onPin = { onPinModel(model) },
            onCancelPin = { onCancelPinModel(model) },
            showDetail = true
          )
        }

        item {
          val showSpacing = searchedPinModels.data?.isNotEmpty() == true
          Spacer(Modifier.size(if (showSpacing) 18.dp else 0.dp))
        }
      } else {
        item {
          Text(
            "Tips: 左划模型可以添加到常用模型并直接显示在聊天界面",
            style = MaterialTheme.typography.labelMedium,
            color = AppCustomTheme.colorScheme.secondaryLabel,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }
      }

      availableProviders.mapSuccess { providers ->
        // Provider Filter

        // There is no need to show the filter if there is only one provider
        if (providers.count() > 1) {
          item(key = "filter") {
            Row(
              Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .animateItem()
            ) {
              for (item in providers) {
                FilterChip(
                  selected = filter.id == item.id,
                  onClick = {
                    filter = item
                  },
                  leadingIcon = {
                    Icon(
                      ImageVector.vectorResource(item.logoIconId), "",
                      modifier = Modifier.size(15.dp)
                    )
                  },
                  label = { Text(item.displayName, style = MaterialTheme.typography.labelMedium) },
                  border = BorderStroke(1.dp, Color.Transparent),
                  colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    selectedContainerColor = AppCustomTheme.colorScheme.primaryAction,
                    selectedLabelColor = AppCustomTheme.colorScheme.onPrimaryAction,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedLeadingIconColor = AppCustomTheme.colorScheme.onPrimaryAction,
                    iconColor = AppCustomTheme.colorScheme.primaryLabel,
                  ),
                )
                Spacer(modifier = Modifier.width(8.dp))
              }
            }
          }
        }

        // Chat Service Provider Introduction
        item(key = "provider-intro") {
          ServiceProviderIntroCard(filter)
        }

        specificModelsSearched?.let {
          when (it) {
            is LoadingStatus.Loading -> {
              item(key = "loading-${filter.id}") {
                Box(
                  Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 34.dp)
                    .animateItem()
                ) {
                  CircularProgressIndicator(
                    Modifier
                      .align(Alignment.Center)
                      .size(25.dp),
                    color = AppCustomTheme.colorScheme.primaryLabel,
                    strokeWidth = 5.dp,
                  )
                }
              }
            }

            is LoadingStatus.Error -> {
              item(key = "error-${filter.id}") {
                ModelLoadingError(it, onRetry = {
                  coroutineScope.launch {
                    repository.refreshModelsIfNeeded(filter)
                  }
                })
              }
            }

            is LoadingStatus.Success -> {
              for (group in it.data.keys) {
                val modelItems = it.data[group]!!
                section(
                  group.fullDisplayName,
                  modelItems.toList(),
                  contentPadding = PaddingValues(horizontal = 16.dp),
                  key = { it.fullCode() }
                ) { model ->
                  ModelListItem(
                    model, onSelectModel, isPinned = false, onPin = { onPinModel(model) },
                    onCancelPin = { onCancelPinModel(model) }
                  )
                }
                item {
                  if (modelItems.isNotEmpty()) {
                    Spacer(Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        }
      }

      item {
        Spacer(Modifier.size(58.dp))
      }
    }
  }
}


@Composable
private fun ModelListItem(
  model: ModelCode,
  onSelectModel: (ModelCode) -> Unit,

  isPinned: Boolean = false,
  onPin: () -> Unit = {},
  onCancelPin: () -> Unit = {},

  showDetail: Boolean = false
) {
  val pinAction = SwipeAction(
    icon = rememberVectorPainter(Icons.TwoTone.Star),
    background = if (isPinned)
      MaterialTheme.colorScheme.errorContainer
    else
      MaterialTheme.colorScheme.primaryContainer,
    onSwipe = {
      if (isPinned) {
        onCancelPin()
      } else {
        onPin()
      }
    }
  )

  SwipeableActionsBox(
    endActions = listOf(pinAction),
    backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.surfaceContainerLowest
  ) {
    SectionListItem(
      title = model.code,
      icon = ImageVector.vectorResource(model.iconId),
      iconTint = Color.Unspecified,
      iconBg = model.backColor,
      onClick = {
        onSelectModel(model)
      },
      description = if (showDetail) {
        model.fullDisplayCode
      } else {
        null
      },
      trailingContent = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (model.supportImage) {
            Box(
              Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialColors.Green[50])
                .padding(2.dp)
            ) {
              Icon(
                ImageVector.vectorResource(R.drawable.ic_vision_icon_legacy), "",
                tint = MaterialColors.Green[800],
                modifier = Modifier.size(17.dp),
              )
            }
            Spacer(Modifier.width(5.dp))
          }
          if (model.supportVideo) {
            Box(
              Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialColors.Blue[50])
                .padding(2.dp)
            ) {
              Icon(
                ImageVector.vectorResource(R.drawable.ic_video_icon), "",
                tint = MaterialColors.Blue[800],
                modifier = Modifier.size(17.dp),
              )
            }
            Spacer(Modifier.width(5.dp))
          }
          if (model.contextStr != null) {
            Box(
              Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 3.dp, vertical = 2.dp),
            ) {
              Text(model.contextStr!!, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    )
  }
}

@Composable
fun ModelSelectPage(
  onBack: (() -> Unit)? = null,
  showTitle: Boolean = true,
  skipCollectModels: Boolean = false,
  viewModel: SettingsViewModel = hiltViewModel(),
  onSelectModel: ((ModelCode) -> Unit)? = null,
) {
  val pinnedModels by viewModel.favoriteModels.map { LoadingStatus.Success(it) }.observeAsState(
    initial = LoadingStatus.Loading
  )

  ModelSelectPageInner(
    pinModels = pinnedModels,
    repository = viewModel.modelRepository,
    showTitle = showTitle,
    onBack = onBack,
    onSelectModel = {
      if (onSelectModel != null) {
        onSelectModel(it)
      } else {
        viewModel.updateDefaultModel(it)
      }
      onBack?.invoke()
    },
    onPinModel = viewModel::pinModelCode,
    onCancelPinModel = viewModel::cancelPinModelCode,
    skipCollectModels = skipCollectModels
  )
}

@Composable
private fun LazyItemScope.ModelLoadingError(
  it: LoadingStatus.Error,
  onRetry: () -> Unit = {}
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  fun copyFullError() {
    val text = it.message
    clipboardManager.setText(AnnotatedString(text))

    // https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#duplicate-notifications
    // Only show a toast for Android 12 and lower.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      Toast.makeText(context, context.getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
    }
  }

  Column(
    Modifier
      .fillMaxWidth()
      .padding(horizontal = 18.dp, vertical = 34.dp)
      .animateItem(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(13.dp)
  ) {
    Text(
      buildAnnotatedString {
        val style = MaterialTheme.typography.titleLarge

        withStyle(style = SpanStyle(fontSize = style.fontSize, fontWeight = FontWeight.SemiBold)) {
          append(stringResource(R.string.label_failed_to_load_models_for_provider))
        }
        append(System.lineSeparator())
        append(System.lineSeparator())
        withStyle(style = SpanStyle(color = AppCustomTheme.colorScheme.secondaryLabel)) {
          append(it.message)
        }
      },
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center
    )

    Row(
      horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
      // TODO: REFACTOR 提取公共 widgets，primary button and secondary button
      FilledTonalButton(
        onClick = {
          copyFullError()
        },
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
          containerColor = AppCustomTheme.colorScheme.secondaryAction,
          contentColor = AppCustomTheme.colorScheme.onSecondaryAction
        ),
      ) {
        Text(stringResource(R.string.label_button_copy_error_message))
      }

      FilledTonalButton(
        onClick = {
          onRetry()
        },
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
          containerColor = AppCustomTheme.colorScheme.primaryAction,
          contentColor = AppCustomTheme.colorScheme.onPrimaryAction
        ),
      ) {
        Text(stringResource(R.string.label_button_retry_on_error))
      }
    }

  }
}


@Preview(showBackground = true)
@Composable
private fun ModelLoadingErrorPreview() {
  AIGroupAppTheme {
    LazyColumn {
      item {
        ModelLoadingError(LoadingStatus.Error("Test Message"))
      }
    }
  }
}


@Composable
private fun LazyItemScope.ServiceProviderIntroCard(
  filter: ServiceProvider
) {
  // create a card with a description, have background color and rounded corners
  // show icon and description
  Box(
    Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp, horizontal = 16.dp)
      .clip(MaterialTheme.shapes.medium)
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .padding(vertical = 18.dp, horizontal = 20.dp)
      .animateItem()
  ) {
    Column(
      horizontalAlignment = Alignment.Start,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        Modifier
          .size(30.dp)
          .shadow(2.dp, clip = true, shape = MaterialTheme.shapes.small)
          .background(filter.backColor ?: MaterialTheme.colorScheme.background)
          .padding(5.dp)
      ) {
        // 使用 when 表达式处理图标的颜色
        val iconTint = when (filter) {
          is ChatServiceProvider -> if (filter == ChatServiceProvider.OFFICIAL) LocalContentColor.current else Color.Unspecified
          is CustomChatServiceProvider -> Color.Unspecified
        }
        
        Icon(
          ImageVector.vectorResource(filter.logoIconId),
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(20.dp)
        )
      }
      Column {
        Text(filter.displayName, style = MaterialTheme.typography.titleMedium)
        Text(
          filter.description,
          style = MaterialTheme.typography.bodySmall,
          color = AppCustomTheme.colorScheme.secondaryLabel
        )
      }
    }
  }
}

// preview for ServiceProviderIntroCard
@Preview(showBackground = true, backgroundColor = 0xFFFF0000)
@Composable
private fun ServiceProviderIntroCardPreview() {
  AIGroupAppTheme {
    LazyColumn {
      item {
        ServiceProviderIntroCard(ChatServiceProvider.OFFICIAL)
      }
    }
  }
}