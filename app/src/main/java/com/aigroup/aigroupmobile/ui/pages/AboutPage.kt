@file:OptIn(ExperimentalMaterial3Api::class)

package com.aigroup.aigroupmobile.ui.pages

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import compose.icons.CssGgIcons
import compose.icons.FontAwesomeIcons
import compose.icons.cssggicons.Close
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Comment
import compose.icons.fontawesomeicons.solid.CommentDots
import compose.icons.fontawesomeicons.solid.Phone

@Composable
fun AboutPage(
  onBack: () -> Unit = {}
) {
  val appName = stringResource(R.string.app_name).replace(" ", "")
  val logoAnimation by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.logo_animation))
  val progress by animateLottieCompositionAsState(logoAnimation)

  Scaffold(
    containerColor = Color.Transparent,
  ) { innerPadding ->
    Box(
      Modifier.padding(innerPadding)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 32.dp)
          .padding(bottom = 30.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        Column {
          LottieAnimation(
            modifier = Modifier
              .widthIn(max = 90.dp)
              .aspectRatio(1f),
            composition = logoAnimation,
            progress = { progress },
          )

          Spacer(modifier = Modifier.height(20.dp))

          Text(
            "$appName APP",
            style = MaterialTheme.typography.headlineMedium,
            color = AppCustomTheme.colorScheme.primaryLabel
          )
          Text(
            stringResource(R.string.app_version_label),
            style = MaterialTheme.typography.titleSmall,
            color = AppCustomTheme.colorScheme.secondaryLabel
          )

          Spacer(modifier = Modifier.height(12.dp))
        }
      }

      FilledIconButton(
        onClick = {
          onBack()
        },
        modifier = Modifier.padding(16.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
          containerColor = AppCustomTheme.colorScheme.tintColor
        )
      ) {
        Icon(CssGgIcons.Close, contentDescription = stringResource(R.string.general_close), Modifier.size(20.dp))
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun AboutPagePreview() {
  AIGroupAppTheme {
    AboutPage()
  }
}
