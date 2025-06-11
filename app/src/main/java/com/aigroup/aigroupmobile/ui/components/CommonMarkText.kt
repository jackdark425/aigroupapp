package com.aigroup.aigroupmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aigroup.aigroupmobile.R
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle

@Composable
fun CommonMarkText(text: String, modifier: Modifier = Modifier) {
  RichText(
    modifier = modifier,
    style = RichTextStyle(
      codeBlockStyle = CodeBlockStyle(
        modifier = Modifier
          .clip(MaterialTheme.shapes.small)
          .background(MaterialTheme.colorScheme.surfaceContainerLow)
      ),
      stringStyle = RichTextStringStyle(
        codeStyle = SpanStyle(
          background = MaterialTheme.colorScheme.surfaceContainerLow,
          fontFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))
        ),
        linkStyle = SpanStyle(
          color = MaterialColors.Blue[800],
        )
      ),
      headingStyle = { level, style ->
        val fontSize = when (level) {
          1 -> 30.sp
          2 -> 24.sp
          3 -> 20.sp
          4 -> 18.sp
          5 -> 16.sp
          6 -> 14.sp
          else -> 14.sp
        }
        val weight = when (level) {
          1 -> FontWeight.Bold
          else -> FontWeight.Medium
        }
        style.copy(fontStyle = FontStyle.Normal, fontSize = fontSize, fontWeight = weight)
      }
    ),
  ) {
    Markdown(text)
  }
}