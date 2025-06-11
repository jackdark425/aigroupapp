package com.aigroup.aigroupmobile.ui.components.theme

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.LocalUiMode
import com.aigroup.aigroupmobile.ui.theme.UiMode
import com.skydoves.cloudy.cloudy

private val color1 = Color(0xFFFAD9C3)

@Composable
fun GradientLayer(modifier: Modifier = Modifier, blur: Dp = 30.dp) {
  Box(
    Modifier
      .scale(1.15f)
      .fillMaxWidth()
      .let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
          it.blur(blur)
        else
          it.cloudy(radius = blur.value.toInt())
      }
      .then(modifier)
  ) {
    Image(
      ImageVector.vectorResource(R.drawable.gradient_background),
      "",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
fun GradientLayer2(modifier: Modifier = Modifier) {
  Box(
    Modifier
      .scale(1.15f)
      .fillMaxWidth()
      .let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
          it.blur(30.dp)
        else
          it.cloudy(radius = 60)
      }
      .then(modifier)
  ) {
    Image(
      ImageVector.vectorResource(R.drawable.gradient_background_2),
      "",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Preview(showSystemUi = true)
@Composable
fun GradientLayerPreview() {
  AIGroupAppTheme {
    GradientLayer(
      Modifier
    )
  }
}