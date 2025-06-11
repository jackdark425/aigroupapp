package com.aigroup.aigroupmobile.services.documents.mime.parser

import android.content.Context
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentParser
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFSDT
import java.io.InputStream

class MsDocDocumentParser constructor(
  private val context: Context
): DocumentParser {
  override fun parse(inputStream: InputStream?): Document {
    val doc = HWPFDocument(inputStream)
    val extractor = WordExtractor(doc)
    val textList = mutableListOf<String>()

    extractor.paragraphText.forEach {
      textList.add(it)
    }

    val text = textList.joinToString("\n\n")
    return Document.document(text)
  }
}

class MsDocxDocumentParser constructor(
  private val context: Context
): DocumentParser {
  override fun parse(inputStream: InputStream?): Document {
    val doc = XWPFDocument(inputStream)
    val textList = mutableListOf<String>()

    val iterator = doc.bodyElementsIterator
    while (iterator.hasNext()) {
      val element = iterator.next()
      if (element is XWPFSDT) {
        textList.add(element.content.text)
      } else {
        element.body.paragraphs?.forEach {
          textList.add(it.text)
        }
      }
    }

    val text = textList.joinToString("\n\n")
    return Document.document(text)
  }
}