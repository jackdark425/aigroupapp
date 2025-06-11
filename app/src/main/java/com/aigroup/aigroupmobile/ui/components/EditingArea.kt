@file:OptIn(ExperimentalMaterial3Api::class)

package com.aigroup.aigroupmobile.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.aigroup.aigroupmobile.R
import compose.icons.CssGgIcons
import compose.icons.cssggicons.Close
import compose.icons.cssggicons.Expand
import kotlinx.coroutines.launch

@Composable
fun EditingArea(
  modifier: Modifier = Modifier,
  initialText: String = "",
  onDismissRequest: () -> Unit = {},
  onSubmit: (String) -> Unit = {},
) {
  val coroutineScope = rememberCoroutineScope()
  var text by remember { mutableStateOf(initialText) }
  var expand by remember { mutableStateOf(false) }

  val state = if (LocalInspectionMode.current) {
    rememberStandardBottomSheetState()
  } else {
    rememberModalBottomSheetState(skipPartiallyExpanded = true)
  }

  ModalBottomSheet(
    onDismissRequest = { onDismissRequest() },
    sheetState = state,
    containerColor = MaterialTheme.colorScheme.background,
    modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
  ) {
    Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
          title = { Text(stringResource(R.string.label_menu_edit_msg), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
          navigationIcon = {
            IconButton(onClick = {
              coroutineScope.launch {
                state.hide()
              }
            }) {
              Icon(CssGgIcons.Close, "")
            }
          },
          actions = {
            IconButton(onClick = {
              coroutineScope.launch {
//                state.expand()
                expand = !expand
              }
            }) {
              Icon(CssGgIcons.Expand, "")
            }
          },
          windowInsets = WindowInsets(0, 0, 0, 0),
        )
      },
      bottomBar = {
        BottomAppBar(
          containerColor = MaterialTheme.colorScheme.background,
        ) {
          Spacer(modifier = Modifier.weight(1f))
          TextButton(shape = MaterialTheme.shapes.medium, onClick = {
            onDismissRequest()
          }) { Text(stringResource(R.string.label_cancel)) }
          Spacer(modifier = Modifier.width(8.dp))
          Button(shape = MaterialTheme.shapes.medium, onClick = {
            onSubmit(text)
            onDismissRequest()
          }) { Text(stringResource(R.string.label_confirm)) }
          Spacer(modifier = Modifier.width(10.dp))
        }
      },
      modifier = Modifier
        .animateContentSize()
        .fillMaxHeight(if (expand) 1f else 0.5f)
    ) { innerPadding ->
      Column(
        modifier = Modifier.padding(innerPadding)
      ) {
        TextField(
          value = text,
          onValueChange = { text = it },
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp),
          colors = TextFieldDefaults.colors(
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
          )
        )
      }
    }
  }
}

@Preview(showSystemUi = true)
@Composable
private fun EditingAreaPreview() {
  EditingArea()
}