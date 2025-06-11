package com.aigroup.aigroupmobile.ui.pages.chat.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigroup.aigroupmobile.data.models.BotAssistant
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.repositories.AssistantRepository
import com.aigroup.aigroupmobile.ui.pages.assistant.components.AssistantLocalItem
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.viewmodels.AssistantViewModel
import io.realm.kotlin.ext.realmListOf
import kotlinx.serialization.encodeToString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AtAssistantModalInner(
  assistants: List<BotAssistant> = emptyList(),
  onSelected: (BotAssistant) -> Unit = {},
  onDismissRequest: () -> Unit,
  sheetState: SheetState = rememberModalBottomSheetState(),
) {
  ModalBottomSheet(
    sheetState = sheetState,
    onDismissRequest = onDismissRequest,
    shape = MaterialTheme.shapes.medium.copy(
      bottomStart = CornerSize(0.dp),
      bottomEnd = CornerSize(0.dp)
    ),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    dragHandle =  {
      BottomSheetDefaults.DragHandle(
        color = MaterialTheme.colorScheme.surfaceContainerHigh
      )
    }
  ) {
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(bottom = 50.dp, top = 12.dp, start = 6.dp, end = 6.dp)
    ) {
      items(assistants) {
        AssistantLocalItem(
          modifier = Modifier
            .padding(horizontal = 9.dp)
            .animateItem(),
          assistant = it,
          onClick = {
            onSelected(it)
            onDismissRequest()
          },
          showMenuButton = false,
          usingSwipeAction = false
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtAssistantModal(
  onSelected: (BotAssistant) -> Unit = {},
  onDismissRequest: () -> Unit,
  viewModel: AssistantViewModel = hiltViewModel()
) {
  val assistants by viewModel.localAssistants.observeAsState()

  AtAssistantModalInner(
    assistants = assistants ?: emptyList(),
    onSelected = onSelected,
    onDismissRequest = onDismissRequest,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun AtAssistantModalPreview() {
  val context = LocalContext.current
  val repo = AssistantRepository(context)

  val assistants = remember() {
    repo.getAssistants().map { remote ->
      BotAssistant().apply {
        storeIdentifier = remote.identifier
        avatar = ImageMediaItem()
        tags = realmListOf(*remote.metadata.tags.toTypedArray())
        presetsPrompt = remote.configuration.role
        assistantSchemeStr = AssistantRepository.Json.encodeToString(remote)
      }
    }
  }

  AIGroupAppTheme {
    AtAssistantModalInner(
      assistants,
      sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Expanded
      ),
      onDismissRequest = { }
    )
  }
}