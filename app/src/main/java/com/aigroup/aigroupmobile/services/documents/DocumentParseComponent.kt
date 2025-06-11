package com.aigroup.aigroupmobile.services.documents

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.services.documents.mime.MindMapDocument
import com.aigroup.aigroupmobile.services.documents.mime.MsDocDocument
import com.aigroup.aigroupmobile.services.documents.mime.PdfDocument
import com.aigroup.aigroupmobile.utils.common.MimeTypes
import com.aigroup.aigroupmobile.utils.system.OpenExternal
import dev.langchain4j.data.document.DocumentParser
import dev.langchain4j.data.document.parser.TextDocumentParser

@Composable
fun rememberDocumentParseComponent(doc: DocumentMediaItem?): DocumentParseComponent {
  // TODO: support null doc param to show file placeholder
  val context = LocalContext.current
  val component by remember(doc) {
    derivedStateOf {
      if (doc == null) {
        DocumentParseComponent(context)
      } else {
        DocumentParseComponent.component(context, doc.mimeType)
      }
    }
  }
  return component
}

open class DocumentParseComponent(
  protected val context: Context
) {

  companion object {
    private val registry = mutableMapOf(
      // pdf
      MimeTypes.Application.PDF.mimeType to { ctx: Context -> PdfDocument(ctx) },
      // doc
      MimeTypes.Application.MS_WORD.mimeType to { ctx: Context -> MsDocDocument(ctx) },
      MimeTypes.Application.MS_WORD_OPEN_XML.mimeType to { ctx: Context -> MsDocDocument(ctx) },
      // mindmap
      MimeTypes.Application.FREEMIND.mimeType to { ctx: Context -> MindMapDocument(ctx) },
    )

    fun register(mimeType: String, component: (Context) -> DocumentParseComponent) {
      registry[mimeType] = component
    }

    fun component(context: Context, mimeType: String): DocumentParseComponent {
      return registry[mimeType]?.invoke(context) ?: DocumentParseComponent(context)
    }
  }

  @Composable
  open fun MimeIcon(size: Dp) {
    Icon(
      ImageVector.vectorResource(R.drawable.ic_doc_icon),
      contentDescription = "Document Icon",
      modifier = Modifier.size(size)
    )
  }

  open suspend fun onPreview(doc: DocumentMediaItem): Boolean {
    OpenExternal.openDocMediaItemExternal(context, doc)
    return true
  }

  open fun shouldShowMiniPreview(doc: DocumentMediaItem): Boolean {
    return false
  }

  @Composable
  open fun MiniPreview(doc: DocumentMediaItem, fileSizeString: String) {

  }

  open fun createParser(docItem: DocumentMediaItem): DocumentParser {
    return TextDocumentParser()
  }

}