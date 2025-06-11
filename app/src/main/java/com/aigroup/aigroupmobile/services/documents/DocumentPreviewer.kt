package com.aigroup.aigroupmobile.services.documents

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.data.models.LoadingStatus
import com.aigroup.aigroupmobile.data.models.data
import com.aigroup.aigroupmobile.data.models.mapSuccess
import com.aigroup.aigroupmobile.data.models.mutableLoadingStatusOf
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme

@Composable
fun DocumentPreviewer(doc: DocumentMediaItem, modifier: Modifier = Modifier) {
  var fileKbSize by remember { mutableLoadingStatusOf<Float>() }
  val component = rememberDocumentParseComponent(doc)

  LaunchedEffect(doc) {
    val size = doc.file.length() / 1024f
    fileKbSize = LoadingStatus.Success(size)
  }

  AnimatedContent(component.shouldShowMiniPreview(doc), label = "document_previewer_switcher") {
    val size = if (LocalInspectionMode.current) "2MB" else fileKbSize.mapSuccess { it.fileSizeString() }.data ?: ""

    if (it) {
      component.MiniPreview(doc, fileSizeString = size)
    } else {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(5.dp)
      ) {
        component.MimeIcon(size = 30.dp)

        Column {
          Text(
            doc.title,
            style = MaterialTheme.typography.labelLarge.copy(lineBreak = LineBreak.Paragraph),
          )

          Text(
            size,
            style = MaterialTheme.typography.labelSmall,
            color = AppCustomTheme.colorScheme.secondaryLabel
          )
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun DocumentPreviewerPreview() {
  AIGroupAppTheme {
    DocumentPreviewer(
      doc = createPreviewDoc()
    )
  }
}

private fun createPreviewDoc(): DocumentMediaItem {
  return DocumentMediaItem(
    "/sample/url",
    "text/plain",
    "Sample Document",
  )
}

private fun Float.fileSizeString(): String {
  return when {
    this < 1024 -> "%.0f KB".format(this)
    this < 1024 * 1024 -> "%.2f MB".format(this / 1024)
    this < 1024 * 1024 * 1024 -> "%.2f GB".format(this / 1024 / 1024)
    else -> "%.2f TB".format(this / 1024 / 1024 / 1024)
  }
}