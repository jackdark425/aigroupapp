package com.aigroup.aigroupmobile.services.documents

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.aigroup.aigroupmobile.LocalPathManager
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.ui.components.MediaSelector
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.utils.system.PathManager
import kotlinx.coroutines.launch

@Preview(showSystemUi = true)
@Composable
private fun DocumentComponentExample() {
  var doc: DocumentMediaItem? by remember { mutableStateOf(null) }
  val context = LocalContext.current
  val pathManager = PathManager(context)
  val coroutineScope = rememberCoroutineScope()
  val component = rememberDocumentParseComponent(doc)

  BackHandler {
    if (doc != null) {
      doc = null
    }
  }

  CompositionLocalProvider(LocalPathManager provides pathManager) {
    AIGroupAppTheme {
      Scaffold { innerPadding ->
        Box(
          Modifier
            .padding(innerPadding)
            .fillMaxSize(), contentAlignment = Alignment.Center) {

          AnimatedContent(doc != null, label = "") { hasDoc ->
            if (!hasDoc) {
              MediaSelector.TakeDocFileButton(onMediaSelected = { doc = it?.let { it as DocumentMediaItem } }) {
                TextButton({ this.onClick() }) { Text("Pick File") }
              }
            } else if (doc != null) {
              Column {
                DocumentPreviewer(doc!!, modifier = Modifier.clickable {
                  coroutineScope.launch { component.onPreview(doc!!) }
                })
                TextButton(onClick = {
                  coroutineScope.launch {
                    val parser = component.createParser(doc!!)
                    val document = doc!!.file.inputStream().use {
                      parser.parse(it)
                    }
                    println(document)
                  }
                }) { Text("Parse Document") }
                TextButton(onClick = { doc = null }) { Text("Pick Another File") }
              }
            }
          }

        }
      }
    }
  }
}