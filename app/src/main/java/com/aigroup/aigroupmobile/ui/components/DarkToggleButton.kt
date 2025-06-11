package com.aigroup.aigroupmobile.ui.components

import android.view.WindowInsetsController
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.aigroup.aigroupmobile.ui.theme.LocalUiMode
import com.aigroup.aigroupmobile.ui.theme.UiMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DarkToggleButton(
  modifier: Modifier = Modifier,
  springSpec: SpringSpec<Float> = remember { spring() },
) {
  var uiMode by LocalUiMode.current
  val realSpringSpec = remember(springSpec) {
    spring(
      dampingRatio = springSpec.dampingRatio,
      stiffness = springSpec.stiffness,
      visibilityThreshold = 0.002f,
    )
  }
  IconButton(
    modifier = modifier,
    onClick = {
      uiMode = uiMode.toggle()
    },
  ) {
    val sunMoonState = when (uiMode) {
      UiMode.Default -> SunMoonState.Sun
      UiMode.Dark -> SunMoonState.Moon
    }

    Box(modifier = Modifier.padding(8.dp)) {
      SunMoonIcon(sunMoonState, springSpec = realSpringSpec)
    }
  }
}

private enum class SunMoonState {
  Sun,
  Moon,
}

private class SunMoonTransitionData(
  rotation: State<Float>,
  maskCxRatio: State<Float>,
  maskCyRatio: State<Float>,
  maskRadiusRatio: State<Float>,
  circleRadiusRatio: State<Float>,
  val surroundCircleScales: List<State<Float>>,
  val surroundCircleAlphas: List<State<Float>>,
) {
  val rotation by rotation
  val maskCxRatio by maskCxRatio
  val maskCyRatio by maskCyRatio
  val maskRadiusRatio by maskRadiusRatio
  val circleRadiusRatio by circleRadiusRatio
}

@Composable
private fun updateSunMoonTransitionData(
  sunMoonState: SunMoonState,
  springSpec: SpringSpec<Float>,
): SunMoonTransitionData {
  val transition = updateTransition(sunMoonState, label = "SunMoonTransition")

  val rotation = transition.animateFloat(
    transitionSpec = { springSpec },
    label = "rotation",
  ) { state ->
    when (state) {
      SunMoonState.Sun -> 180f
      SunMoonState.Moon -> 45f
    }
  }
  val maskCxRatio = transition.animateFloat(
    transitionSpec = { springSpec },
    label = "maskCxRatio",
  ) { state ->
    when (state) {
      SunMoonState.Sun -> 1f
      SunMoonState.Moon -> 0.5f
    }
  }
  val maskCyRatio = transition.animateFloat(
    transitionSpec = { springSpec },
    label = "maskCyRatio",
  ) { state ->
    when (state) {
      SunMoonState.Sun -> 0f
      SunMoonState.Moon -> 0.18f
    }
  }
  val maskRadiusRatio = transition.animateFloat(
    transitionSpec = { springSpec },
    label = "maskRadiusRatio",
  ) { state ->
    when (state) {
      SunMoonState.Sun -> 0.125f
      SunMoonState.Moon -> 0.35f
    }
  }
  val circleRadiusRatio = transition.animateFloat(
    transitionSpec = { springSpec },
    label = "circleRadiusRatio",
  ) { state ->
    when (state) {
      SunMoonState.Sun -> 0.2f
      SunMoonState.Moon -> 0.35f
    }
  }

  fun Transition.Segment<SunMoonState>.surroundTransitionSpec(i: Int): FiniteAnimationSpec<Float> {
    return if (SunMoonState.Moon.isTransitioningTo(SunMoonState.Sun)) {
      val delayUnit = (-springSpec.stiffness * 0.067f + 55).toInt().coerceIn(5, 50)
      tween(delayMillis = i * delayUnit)
    } else {
      springSpec
    }
  }

  val surroundCircleScales = List(SurroundCircleNum) { i ->
    transition.animateFloat(
      transitionSpec = { surroundTransitionSpec(i) },
      label = "surroundCirclesScale_$i",
    ) { state ->
      when (state) {
        SunMoonState.Sun -> 1f
        SunMoonState.Moon -> 0f
      }
    }
  }
  val surroundCircleAlphas = List(SurroundCircleNum) { i ->
    transition.animateFloat(
      transitionSpec = { surroundTransitionSpec(i) },
      label = "surroundCircleAlphas_$i",
    ) { state ->
      when (state) {
        SunMoonState.Sun -> 1f
        SunMoonState.Moon -> 0f
      }
    }
  }

  return remember(transition) {
    SunMoonTransitionData(
      rotation = rotation,
      maskCxRatio = maskCxRatio,
      maskCyRatio = maskCyRatio,
      maskRadiusRatio = maskRadiusRatio,
      circleRadiusRatio = circleRadiusRatio,
      surroundCircleScales = surroundCircleScales,
      surroundCircleAlphas = surroundCircleAlphas,
    )
  }
}

private const val SurroundCircleNum = 8

@Composable
private fun SunMoonIcon(
  sunMoonState: SunMoonState,
  modifier: Modifier = Modifier,
  springSpec: SpringSpec<Float>,
  fillColor: Color = MaterialTheme.colorScheme.onSurface,
) {
  val transitionData = updateSunMoonTransitionData(sunMoonState, springSpec)
  Canvas(
    modifier = modifier.aspectRatio(1f)
  ) {
    val sizePx = size.width

    drawContext.transform.rotate(transitionData.rotation)
    drawContext.canvas.withSaveLayer(
      bounds = drawContext.size.toRect(),
      paint = Paint()
    ) {
      drawCircle(
        color = fillColor,
        radius = sizePx * transitionData.circleRadiusRatio,
      )

      drawCircle(
        color = Color.Black,
        radius = sizePx * transitionData.maskRadiusRatio,
        center = Offset(
          x = size.width * transitionData.maskCxRatio,
          y = size.height * transitionData.maskCyRatio,
        ),
        blendMode = BlendMode.DstOut,
      )
    }

    repeat(SurroundCircleNum) { i ->
      scale(scale = transitionData.surroundCircleScales[i].value) {
        val radians = PI / 2 - i * 2 * PI / SurroundCircleNum
        val d = sizePx / 3
        val cx = center.x + d * cos(radians)
        val cy = center.y - d * sin(radians)
        drawCircle(
          color = fillColor,
          radius = sizePx * 0.05f,
          center = Offset(cx.toFloat(), cy.toFloat()),
          alpha = transitionData.surroundCircleAlphas[i].value.coerceIn(0f, 1f),
        )
      }
    }
  }
}
