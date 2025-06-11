package com.aigroup.aigroupmobile.ui.pages.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.ui.components.AppCopyRight
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay

/**
 * This page show when user first open the app
 */
@Composable
fun FirstIntroPage(
  modifier: Modifier = Modifier,
  goToWelcomePage: () -> Unit = {},
) {
  val logoAnimation by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.logo_animation))

  val isVisible = remember { MutableTransitionState(false) }
  val isInspectionMode = LocalInspectionMode.current
  val progress by animateLottieCompositionAsState(logoAnimation, isPlaying = isVisible.isIdle)

  LaunchedEffect(Unit) {
    isVisible.targetState = true
  }

  LaunchedEffect(progress) {
    if (progress == 1f) {
      delay(1000)
      goToWelcomePage()
    }
  }

  val animationWidth by animateDpAsState(
    if (isVisible.currentState) 100.dp else 200.dp,
    animationSpec = tween(500)
  )

  Scaffold { innerPadding ->
    Box(
      modifier = modifier
        .padding(innerPadding)
        .fillMaxSize()
    ) {
      Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))

        AnimatedVisibility(
          visibleState = isVisible,
          enter = fadeIn(tween(3000)),
        ) {
          Box(
            modifier = Modifier.widthIn(max = animationWidth)
          ) {
            LottieAnimation(
              composition = logoAnimation,
              progress = { if (isInspectionMode) 0.5f else progress },
            )
          }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(50.dp))
      }
    }
  }
}

@Preview(showSystemUi = true)
@Composable
private fun FirstIntroPagePreview() {
  AIGroupAppTheme {
    FirstIntroPage()
  }
}
