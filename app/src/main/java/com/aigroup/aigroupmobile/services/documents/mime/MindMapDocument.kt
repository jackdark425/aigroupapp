package com.aigroup.aigroupmobile.services.documents.mime

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.services.chat.plugins.builtin.MindMapNode
import com.aigroup.aigroupmobile.services.chat.plugins.builtin.MindMapRenderer
import com.aigroup.aigroupmobile.services.chat.plugins.builtin.buildTestMindNode
import com.aigroup.aigroupmobile.services.documents.DocumentParseComponent
import com.aigroup.aigroupmobile.services.documents.DocumentPreviewer
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.utils.common.MimeTypes
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    "/sample/test.mm",
    MimeTypes.Application.FREEMIND.mimeType,
    "Sample MindMap",
  )
}

class MindMapDocument(context: Context) : DocumentParseComponent(context) {
  @Composable
  override fun MimeIcon(size: Dp) {
    Box(
      modifier = Modifier
        .clip(MaterialTheme.shapes.medium)
        .size(size)
        .background(MaterialColors.Green[100])
        .padding(6.dp)
    ) {
      Icon(
        ImageVector.vectorResource(R.drawable.ic_map_icon),
        contentDescription = "MindMap Icon",
        tint = MaterialColors.Green[900]
      )
    }
  }

  override fun shouldShowMiniPreview(doc: DocumentMediaItem): Boolean {
    return true
  }

  @Composable
  override fun MiniPreview(doc: DocumentMediaItem, fileSizeString: String) {
    val inspectionMode = LocalInspectionMode.current
    var content by remember {
      mutableStateOf(MindMapNode(0, ""))
    }

    LaunchedEffect(Unit) {
      if (inspectionMode) {
        content = buildTestMindNode().children.first()
      } else {
        // TODO: avoid using "ROOT" as root node and skip it here
        val rootNode = readDocToMindMap(doc).let { it.children.firstOrNull() ?: it }
        withContext(Dispatchers.Main) {
          content = rootNode
        }
      }
    }

    // TODO: fix, draw this too many times!!!
    MindMapRenderer(content)
  }

  private suspend fun readDocToMindMap(doc: DocumentMediaItem): MindMapNode {
    require(doc.mimeType == MimeTypes.Application.FREEMIND.mimeType) {
      "Unsupported mime type: ${doc.mimeType}"
    }

    return withContext(Dispatchers.IO) {
      MindMapNode.fromFreemindFile(doc.file)
    }
  }
}