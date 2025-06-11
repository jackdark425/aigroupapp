package com.aigroup.aigroupmobile.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val AnimationSpec = TweenSpec<Float>(durationMillis = 200)
private const val DrawerPositionalThreshold = 0.5f
private val DrawerVelocityThreshold = 400.dp

enum class AdaptiveDrawerStateValue {
  Present,
  Backstage;

  val isPresent: Boolean
    get() = this == Present

  val opposite: AdaptiveDrawerStateValue
    get() = when (this) {
      Present -> Backstage
      Backstage -> Present
    }
}

@OptIn(ExperimentalFoundationApi::class)
class AdaptiveDrawerState(
  initialValue: AdaptiveDrawerStateValue,
  decaySpec: DecayAnimationSpec<Float>,
) {
  internal var windowSizeClass: WindowSizeClass? by mutableStateOf(null)
  internal var density: Density? by mutableStateOf(null)

  private fun requireWindowSizeClass() =
    requireNotNull(windowSizeClass) {
      "The windowSizeClass on AdaptiveDrawerState ($this) was not set. Did you use AdaptiveDrawerState" +
          " with the AdaptiveDrawer composables?"
    }

  private fun requireDensity() =
    requireNotNull(density) {
      "The density on AdaptiveDrawerState ($this) was not set. Did you use AdaptiveDrawerState" +
          " with the AdaptiveDrawer composables?"
    }

  val currentValue: AdaptiveDrawerStateValue
    get() = anchoredDraggableState.currentValue
  val targetValue: AdaptiveDrawerStateValue
    get() = anchoredDraggableState.targetValue

  val currentOffset: Float
    get() = anchoredDraggableState.offset

  val isOpen: Boolean
    get() = currentValue.isPresent

  val isClosed: Boolean
    get() = !isOpen

  val usingSplitLayout: Boolean
    get() = requireWindowSizeClass().windowWidthSizeClass != WindowWidthSizeClass.COMPACT

  suspend fun open() = animateTo(AdaptiveDrawerStateValue.Present)
  suspend fun adaptiveClose() {
    if (!usingSplitLayout) {
      animateTo(AdaptiveDrawerStateValue.Backstage)
    }
  }

  suspend fun adaptiveToggle() {
    if (!usingSplitLayout) {
      animateTo(targetValue.opposite)
    }
  }

  internal val anchoredDraggableState =
    AnchoredDraggableState(
      initialValue = initialValue,
      positionalThreshold = { it * DrawerPositionalThreshold },
      velocityThreshold = { with(requireDensity()) { DrawerVelocityThreshold.toPx() } },
      snapAnimationSpec = AnimationSpec,
      decayAnimationSpec = decaySpec,
    )

  fun requireOffset(): Float = anchoredDraggableState.requireOffset()

  private suspend fun animateTo(
    targetValue: AdaptiveDrawerStateValue,
    animationSpec: AnimationSpec<Float> = AnimationSpec,
    velocity: Float = anchoredDraggableState.lastVelocity
  ) {
    anchoredDraggableState.anchoredDrag(targetValue) { anchors, latestTarget ->
      val targetOffset = anchors.positionOf(latestTarget)
      if (!targetOffset.isNaN()) {
        var prev = if (currentOffset.isNaN()) 0f else currentOffset
        animate(prev, targetOffset, velocity, animationSpec) { value, velocity ->
          // Our onDrag coerces the value within the bounds, but an animation may
          // overshoot, for example a spring animation or an overshooting interpolator
          // We respect the user's intention and allow the overshoot, but still use
          // DraggableState's drag for its mutex.
          dragTo(value, velocity)
          prev = value
        }
      }
    }
  }

  companion object {
    fun Saver(
      decaySpec: DecayAnimationSpec<Float>,
      windowSizeClass: WindowSizeClass,
    ) = Saver<AdaptiveDrawerState, AdaptiveDrawerStateValue>(
      save = { it.currentValue },
      restore = {
        AdaptiveDrawerState(it, decaySpec).apply {
          this.windowSizeClass = windowSizeClass
        }
      }
    )
  }
}

@Composable
fun rememberAdaptiveDrawerState(
  initialWindowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
): AdaptiveDrawerState {
  val splineBasedDecay = rememberSplineBasedDecay<Float>()
  val initial = if (initialWindowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT)
    AdaptiveDrawerStateValue.Backstage
  else
    AdaptiveDrawerStateValue.Present

  return rememberSaveable(saver = AdaptiveDrawerState.Saver(splineBasedDecay, initialWindowSizeClass)) {
    AdaptiveDrawerState(initial, splineBasedDecay).apply {
      windowSizeClass = initialWindowSizeClass
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdaptiveDrawer(
  modifier: Modifier = Modifier,
  state: AdaptiveDrawerState = rememberAdaptiveDrawerState(),
  gesturesEnabled: Boolean = true,

  compactDrawerFraction: Float = 0.8f,
  expandedFixedDrawerWidth: Dp = 300.dp,

  drawerContent: @Composable () -> Unit,
  content: @Composable () -> Unit,
) {
  val scope = rememberCoroutineScope()

  var anchorsInitialized by remember { mutableStateOf(false) }
  val density = LocalDensity.current
  val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
  val expandedFixedDrawerWidthInt = with(density) { expandedFixedDrawerWidth.roundToPx() }
  SideEffect {
    state.density = density
    state.windowSizeClass = windowSizeClass
  }

  val width = windowSizeClass.windowWidthSizeClass // TODO: get it from state for State consistency and avoiding confusion
  LaunchedEffect(width, anchorsInitialized) {
    if (!anchorsInitialized) return@LaunchedEffect

    if (width == WindowWidthSizeClass.EXPANDED && state.isClosed) {
      state.open()
    } else if (width == WindowWidthSizeClass.COMPACT && state.isOpen) {
      state.adaptiveClose()
    }
  }

  // TODO: 实现可预测返回
  BackHandler(state.isOpen && width == WindowWidthSizeClass.COMPACT) {
    scope.launch { state.adaptiveClose() }
  }

  // TODO: 支持 rtl
  val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
  Box(
    modifier.anchoredDraggable(
      state = state.anchoredDraggableState,
      orientation = Orientation.Horizontal,
      enabled = width == WindowWidthSizeClass.COMPACT && gesturesEnabled,
      reverseDirection = isRtl
    )
  ) {
    val fraction =
      state.anchoredDraggableState.progress(AdaptiveDrawerStateValue.Backstage, AdaptiveDrawerStateValue.Present)
    val isCompact = !state.usingSplitLayout

    Layout(
      content = {
        Surface(
          modifier = Modifier,
          tonalElevation = 0.dp,
          shadowElevation = if (isCompact) 50.dp * fraction else 0.dp,
          shape = if (isCompact) RoundedCornerShape(0.dp, 26.dp, 26.dp, 0.dp) else RectangleShape,
          contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer),
          color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
          drawerContent()
        }
        Row(
          Modifier.blur(if (isCompact) 5.dp * fraction else 0.dp)
        ) {
          if (!isCompact && state.isOpen) {
            VerticalDivider(
              modifier = Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.background),
              color = MaterialTheme.colorScheme.scrim,
              thickness = 0.5.dp,
            )
          }
          Box {
            content()
            if (isCompact) {
              Scrim(
                open = state.isOpen,
                onClose = {
                  if (gesturesEnabled) {
                    scope.launch { state.adaptiveClose() }
                  }
                },
                fraction = { fraction },
                color = MaterialTheme.colorScheme.scrim.copy(0.1f)
              )
            }
          }
        }
      }
    ) { measurables, constraints ->
      val sheetWidth = when (width) {
        WindowWidthSizeClass.COMPACT -> (constraints.maxWidth * compactDrawerFraction).roundToInt()
        WindowWidthSizeClass.MEDIUM -> (constraints.maxWidth * 0.39).roundToInt() // TODO: 这是折叠屏？
        WindowWidthSizeClass.EXPANDED -> expandedFixedDrawerWidthInt
        else -> error("Unsupported window width size class: $width")
      }
      val sheetPlaceable = measurables[0].measure(constraints.copy(maxWidth = sheetWidth))

      val offsetOrZero = if (state.currentOffset.isNaN()) 0f else state.requireOffset()
      val contentConstraints = constraints.let {
        if (!isCompact) {
          it.copy(maxWidth = it.maxWidth - sheetPlaceable.width - offsetOrZero.roundToInt())
        } else {
          it
        }
      }
      val contentPlaceable = measurables[1].measure(contentConstraints)

      val containerWidth = when (width) {
        WindowWidthSizeClass.COMPACT -> contentPlaceable.width
        else -> constraints.maxWidth
      }
      layout(containerWidth, contentPlaceable.height) {
        val currentClosedAnchor = state.anchoredDraggableState.anchors.positionOf(AdaptiveDrawerStateValue.Backstage)
        val calculatedClosedAnchor = -sheetPlaceable.width.toFloat()

        if (!anchorsInitialized || currentClosedAnchor != calculatedClosedAnchor) {
          if (!anchorsInitialized) {
            anchorsInitialized = true
          }
          state.anchoredDraggableState.updateAnchors(
            DraggableAnchors {
              AdaptiveDrawerStateValue.Backstage at calculatedClosedAnchor
              AdaptiveDrawerStateValue.Present at 0f
            }
          )
        }

        val compactOffset = (sheetPlaceable.width * 0.8 * fraction).roundToInt()
        val contentOffset = if (isCompact) {
          compactOffset
        } else {
          0
        }

        contentPlaceable.placeRelative(
          sheetPlaceable.width + state.requireOffset().roundToInt() - contentOffset,
          0
        )
        sheetPlaceable.placeRelative(state.requireOffset().roundToInt(), 0)
      }
    }
  }
}

@Composable
private fun Scrim(open: Boolean, onClose: () -> Unit, fraction: () -> Float, color: Color) {
  val dismissDrawer =
    if (open) {
      Modifier.pointerInput(onClose) { detectTapGestures { onClose() } }
    } else {
      Modifier
    }

  Canvas(
    Modifier
      .fillMaxSize()
      .then(dismissDrawer)
  ) { drawRect(color, alpha = fraction()) }
}