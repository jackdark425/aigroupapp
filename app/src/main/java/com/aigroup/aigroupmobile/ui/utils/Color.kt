package com.aigroup.aigroupmobile.ui.utils

import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.aigroup.aigroupmobile.ui.theme.LocalUiMode
import com.aigroup.aigroupmobile.ui.theme.UiMode
import kotlin.math.min

@Composable
fun Color.whenDarkMode(darkMode: Color): Color {
  val isDarkTheme = LocalUiMode.current.value == UiMode.Dark
  val color by animateColorAsState(
    targetValue = when {
      isDarkTheme -> darkMode
      else -> this
    }
  )

  return color
}

// TODO
// https://stackoverflow.com/questions/33072365/how-to-darken-a-given-color-int
//@Composable
//fun Color.manipulate(factor: Float): Color {
//  return Color(manipulateColor(this.toArgb(), factor))
//}
//
//private fun manipulateColor(color: Int, factor: Float): Int {
//  val a: Int = android.graphics.Color.alpha(color)
//  val r = Math.round(android.graphics.Color.red(color) * factor).toInt()
//  val g = Math.round(android.graphics.Color.green(color) * factor).toInt()
//  val b = Math.round(android.graphics.Color.blue(color) * factor).toInt()
//  return android.graphics.Color.argb(
//    a.toFloat(),
//    min(r.toFloat(), 255.0f),
//    min(g.toFloat(), 255.0f),
//    min(b.toFloat(), 255.0f)
//  )
//}
//
