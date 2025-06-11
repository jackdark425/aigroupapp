package com.aigroup.aigroupmobile.services.documents.mime

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.services.documents.DocumentParseComponent
import com.aigroup.aigroupmobile.services.documents.DocumentPreviewer
import com.aigroup.aigroupmobile.services.documents.mime.parser.MsDocDocumentParser
import com.aigroup.aigroupmobile.services.documents.mime.parser.MsDocxDocumentParser
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.utils.common.MimeTypes
import dev.langchain4j.data.document.DocumentParser


class MsDocDocument(context: Context) : DocumentParseComponent(context) {

  companion object {
    const val TAG = "MsDocDocument"
  }

  @Composable
  override fun MimeIcon(size: Dp) {
    Image(
      imageVector = ImageVector.vectorResource(R.drawable.ic_mime_word_icon),
      contentDescription = "Microsoft Doc Icon",
      modifier = Modifier.size(size),
    )
  }

  override fun createParser(docItem: DocumentMediaItem): DocumentParser {
    val isXml = MimeTypes.Application.MS_WORD_OPEN_XML.equalsMime(docItem.mimeType)

    return if (isXml) {
      MsDocxDocumentParser(context)
    } else {
      MsDocDocumentParser(context)
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
    "/sample/test.docx",
    MimeTypes.Application.MS_WORD_OPEN_XML.mimeType,
    "Sample Document",
  )
}