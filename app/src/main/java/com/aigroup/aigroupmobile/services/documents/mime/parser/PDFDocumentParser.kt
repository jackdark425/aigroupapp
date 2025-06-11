package com.aigroup.aigroupmobile.services.documents.mime.parser

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentParser
import java.io.InputStream

class PDFDocumentParser constructor(
  private val context: Context,
) : DocumentParser {
  init {
    PDFBoxResourceLoader.init(context)
  }

  override fun parse(inputStream: InputStream?): Document {
    val pdf = PDDocument.load(inputStream)
    val textStripper = PDFTextStripper()
    val text = textStripper.getText(pdf)
    return Document.document(text)
  }
}