package com.aigroup.aigroupmobile.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun SimpleLottie(@androidx.annotation.RawRes resId: Int, modifier: Modifier = Modifier) {
  val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
  val progress by animateLottieCompositionAsState(composition)
  LottieAnimation(
    modifier = modifier.size(50.dp), // TODO: define size here?
    composition = composition,
    progress = { progress },
  )
}