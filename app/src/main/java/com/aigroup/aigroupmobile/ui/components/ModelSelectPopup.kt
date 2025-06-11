package com.aigroup.aigroupmobile.ui.components

import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.LoadingStatus
import com.aigroup.aigroupmobile.connect.chat.ModelCode
import com.aigroup.aigroupmobile.data.models.data
import com.aigroup.aigroupmobile.data.models.loading
import com.aigroup.aigroupmobile.data.models.mapSuccess
import com.aigroup.aigroupmobile.ui.pages.settings.ModelSelectPage
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get

private sealed class ModelSelectPopupItem {
  data class Group(val group: String) : ModelSelectPopupItem()
  data class Model(val model: ModelCode) : ModelSelectPopupItem()
}

/**
 * [ModelSelectPopupItem] show only user favorite models on chat bottom bar to quick switch between models.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectPopup(
  modifier: Modifier = Modifier,
  onDismissRequest: () -> Unit = {},
  models: LoadingStatus<List<ModelCode>>,
  currentModel: ModelCode? = null,
  onSelectModel: (ModelCode) -> Unit,
) {
  val context = LocalContext.current
  val modelsDataSource by remember(models.loading) {
    derivedStateOf {
      models.mapSuccess {
        val groupTitle = ModelSelectPopupItem.Group(context.getString(R.string.label_stared_llm_model))
        listOf(groupTitle) + it.toSortedSet().map { ModelSelectPopupItem.Model(it) }
      }
    }
  }

  var showModelsModal by remember {
    mutableStateOf(false)
  }

  DropdownMenu(
    expanded = !models.loading,
    onDismissRequest = onDismissRequest,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    shape = MaterialTheme.shapes.small,
    shadowElevation = 7.dp,
    tonalElevation = 0.dp,
    offset = DpOffset(0.dp, (-10).dp),
    modifier = modifier
  ) {
    Box(
      modifier = Modifier
        .width(250.dp)
        .height(250.dp)
    ) {
      LazyColumn(Modifier.fillMaxSize()) {
        // 当前模型
        item {
          if (currentModel != null) {
            val included =
              modelsDataSource.data?.any { it is ModelSelectPopupItem.Model && it.model == currentModel } ?: false

            if (!included) {
              Column {
                Text(
                  stringResource(R.string.label_llm_popup_current_selected),
                  style = MaterialTheme.typography.bodySmall,
                  color = AppCustomTheme.colorScheme.secondaryLabel,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
                ModelItem(currentModel, onSelectModel, onDismissRequest, true)
              }
            }
          }
        }

        // 模型列表
        items(modelsDataSource.data ?: emptyList()) { item ->
          when (item) {
            is ModelSelectPopupItem.Group -> {
              Text(
                item.group,
                style = MaterialTheme.typography.bodySmall,
                color = AppCustomTheme.colorScheme.secondaryLabel,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
              )
            }

            is ModelSelectPopupItem.Model -> {
              ModelItem(item.model, onSelectModel, onDismissRequest, currentModel == item.model)
            }
          }
        }

        // 更多模型
        item {
          Spacer(Modifier.heightIn(5.dp))
          AppDropdownMenuItem(
            {
              Text(
                "探索更多模型", // TODO: i18n
                color = MaterialTheme.colorScheme.primary
              )
            },
            leadingIcon = null,
            onClick = {
              showModelsModal = true
//              onDismissRequest()
            }
          )
        }
      }
    }
  }

  // 更多模型弹窗
  if (showModelsModal) {
    ModelSelectModal(
      onDismissRequest = {
        showModelsModal = false
        onDismissRequest()
      },
      onSelectModel = onSelectModel
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelectModal(
  onDismissRequest: () -> Unit,
  onSelectModel: (ModelCode) -> Unit,
  sheetState: SheetState = rememberModalBottomSheetState()
) {
  ModalBottomSheet(
    sheetState = sheetState,
    onDismissRequest = onDismissRequest,
    modifier = Modifier.safeDrawingPadding()
  ) {
    ModelSelectPage(
      showTitle = false,
      skipCollectModels = true,
      onSelectModel = {
        onSelectModel(it)
        onDismissRequest()
      },
    )
  }
}

@Composable
private fun ModelItem(
  model: ModelCode,
  onSelectModel: (ModelCode) -> Unit,
  onDismissRequest: () -> Unit,
  selected: Boolean = false
) {
  AppDropdownMenuItem(
    {
      Text(
        model.toString(),
        modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
      )
    },
    leadingIcon = {
      Box(
        Modifier
          .clip(CircleShape)
          .background(model.tintColor)
          .padding(5.dp)
      ) {
        Icon(
          ImageVector.vectorResource(model.iconId),
          "",
          tint = model.contentColor,
          modifier = Modifier.size(15.dp)
        )
      }
    },
    trailingIcon = {
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
    },
    contentPadding = PaddingValues(horizontal = 12.dp),
    onClick = { onSelectModel(model); onDismissRequest() },
    modifier = Modifier.background(if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
  )
}

