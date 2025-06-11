package com.aigroup.aigroupmobile.ui.pages.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.RemoteAssistant
import com.aigroup.aigroupmobile.repositories.AssistantRepository
import com.aigroup.aigroupmobile.ui.pages.assistant.components.AssistantStoreItem
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.ui.utils.clearFocusOnKeyboardDismiss
import compose.icons.CssGgIcons
import compose.icons.cssggicons.Search

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantStorePage(
  onBack: () -> Unit = {},
  onGotoDetail: (RemoteAssistant) -> Unit = {}
) {
  var search by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  val context = LocalContext.current
  // TODO: move to viewmodel
  val repository = remember { AssistantRepository(context) }

  val assistants = remember {
    repository.getAssistants()
  }
  val searchedAssistants by remember(search) {
    derivedStateOf {
      assistants.filter {
        it.metadata.title.contains(search, ignoreCase = true)
            || it.metadata.description.contains(search, ignoreCase = true)
            || it.configuration.role.contains(search, ignoreCase = true)
      }
    }
  }

  Scaffold(
    topBar = {
      Column {
        TopAppBar(
          title = { Text(stringResource(R.string.label_assistants_store_title), fontWeight = FontWeight.SemiBold) },
          // back button
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, "", Modifier.size(20.dp))
            }
          },
        )

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
                stringResource(R.string.hint_placeholder_search_assistants),
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
      state = listState,
    ) {
      items(searchedAssistants) {
        AssistantStoreItem(
          it, modifier = Modifier.fillMaxWidth(),
          onClick = {
            onGotoDetail(it)
          }
        )
      }
    }
  }
}
