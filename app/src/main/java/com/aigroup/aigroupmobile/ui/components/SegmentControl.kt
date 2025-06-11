package com.aigroup.aigroupmobile.ui.components

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeConsumed
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme

@Preview
@Composable fun SegmentedDemo() {
  AIGroupAppTheme {
    Surface {
      Column(Modifier.padding(16.dp), verticalArrangement = spacedBy(16.dp)) {
        Text("SEGMENTS", style = MaterialTheme.typography.bodySmall)

        val twoSegments = remember { listOf("Foo", "Bar") }
        var selectedTwoSegment by remember { mutableStateOf(twoSegments.first()) }
        SegmentedControl(
          twoSegments,
          selectedTwoSegment,
          onSegmentSelected = { selectedTwoSegment = it }
        ) {
          SegmentText(it)
        }

        val threeSegments = remember { listOf("Foo", "Bar", "Some very long string") }
        var selectedThreeSegment by remember { mutableStateOf(threeSegments.first()) }
        SegmentedControl(
          threeSegments,
          selectedThreeSegment,
          onSegmentSelected = { selectedThreeSegment = it }
        ) {
          SegmentText(it)
        }
      }
    }
  }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable fun SegmentedDemoDark() {
  AIGroupAppTheme {
    Surface {
      Column(Modifier.padding(16.dp), verticalArrangement = spacedBy(16.dp)) {
        Text("SEGMENTS", style = MaterialTheme.typography.bodySmall)

        val twoSegments = remember { listOf("Foo", "Bar") }
        var selectedTwoSegment by remember { mutableStateOf(twoSegments.first()) }
        SegmentedControl(
          twoSegments,
          selectedTwoSegment,
          onSegmentSelected = { selectedTwoSegment = it }
        ) {
          SegmentText(it)
        }

        val threeSegments = remember { listOf("Foo", "Bar", "Some very long string") }
        var selectedThreeSegment by remember { mutableStateOf(threeSegments.first()) }
        SegmentedControl(
          threeSegments,
          selectedThreeSegment,
          onSegmentSelected = { selectedThreeSegment = it }
        ) {
          SegmentText(it)
        }
      }
    }
  }
}

private const val NO_SEGMENT_INDEX = -1

/** Padding inside the track. */
private val TRACK_PADDING = 2.dp

/** Additional padding to inset segments and the thumb when pressed. */
private val PRESSED_TRACK_PADDING = 1.dp

/** Padding inside individual segments. */
private val SEGMENT_PADDING = 5.dp

/** Alpha to use to indicate pressed state when unselected segments are pressed. */
private const val PRESSED_UNSELECTED_ALPHA = .6f
private val BACKGROUND_SHAPE = RoundedCornerShape(8.dp)

@Composable fun <T : Any> SegmentedControl(
  segments: List<T>,
  selectedSegment: T,
  onSegmentSelected: (T) -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable (T) -> Unit
) {
  val state = remember { SegmentedControlState() }
  state.segmentCount = segments.size
  state.selectedSegment = segments.indexOf(selectedSegment)
  state.onSegmentSelected = { onSegmentSelected(segments[it]) }

  // Animate between whole-number indices so we don't need to do pixel calculations.
  val selectedIndexOffset by animateFloatAsState(state.selectedSegment.toFloat())

  // Use a custom layout so that we can measure the thumb using the height of the segments. The thumb
  // is whole composable that draws itself – this layout is just responsible for placing it under
  // the correct segment.
  Layout(
    content = {
      // Each of these produces a single measurable.
      Thumb(state)
      Dividers(state)
      Segments(state, segments, content)
    },
    modifier = modifier
      .fillMaxWidth()
      .then(state.inputModifier)
      .background(MaterialTheme.colorScheme.surfaceContainerHigh, BACKGROUND_SHAPE)
      .padding(TRACK_PADDING)
  ) { measurables, constraints ->
    val (thumbMeasurable, dividersMeasurable, segmentsMeasurable) = measurables

    // Measure the segments first so we know how tall to make the thumb.
    val segmentsPlaceable = segmentsMeasurable.measure(constraints)
    state.updatePressedScale(segmentsPlaceable.height, this)

    // Now we can measure the thumb and dividers to be the right size.
    val thumbPlaceable = thumbMeasurable.measure(
      Constraints.fixed(
        width = segmentsPlaceable.width / segments.size,
        height = segmentsPlaceable.height
      )
    )
    val dividersPlaceable = dividersMeasurable.measure(
      Constraints.fixed(
        width = segmentsPlaceable.width,
        height = segmentsPlaceable.height
      )
    )

    layout(segmentsPlaceable.width, segmentsPlaceable.height) {
      val segmentWidth = segmentsPlaceable.width / segments.size

      // Place the thumb first since it should be drawn below the segments.
      thumbPlaceable.placeRelative(
        x = (selectedIndexOffset * segmentWidth).toInt(),
        y = 0
      )
      dividersPlaceable.placeRelative(IntOffset.Zero)
      segmentsPlaceable.placeRelative(IntOffset.Zero)
    }
  }
}

/**
 * Wrapper around [Text] that is configured to display appropriately inside of a [SegmentedControl].
 */
@Composable fun SegmentText(text: String) {
  Text(text, maxLines = 1, overflow = Ellipsis, color = MaterialTheme.colorScheme.onBackground)
}

/**
 * Draws the thumb (selected indicator) on a [SegmentedControl] track, underneath the [Segments].
 */
@Composable private fun Thumb(state: SegmentedControlState) {
  Box(
    Modifier
      .then(
        state.segmentScaleModifier(
          pressed = state.pressedSegment == state.selectedSegment,
          segment = state.selectedSegment
        )
      )
      .shadow(4.dp, BACKGROUND_SHAPE)
      .background(MaterialTheme.colorScheme.background, BACKGROUND_SHAPE)
  )
}

/**
 * Draws dividers between segments. No dividers are drawn around the selected segment.
 */
@Composable private fun Dividers(state: SegmentedControlState) {
  // Animate each divider independently.
  val alphas = (0 until state.segmentCount).map { i ->
    val selectionAdjacent = i == state.selectedSegment || i - 1 == state.selectedSegment
    animateFloatAsState(if (selectionAdjacent) 0f else 1f)
  }

  Canvas(Modifier.fillMaxSize()) {
    val segmentWidth = size.width / state.segmentCount
    val dividerPadding = TRACK_PADDING + PRESSED_TRACK_PADDING

    alphas.forEachIndexed { i, alpha ->
      val x = i * segmentWidth
      drawLine(
        Color.White,
        alpha = alpha.value,
        start = Offset(x, dividerPadding.toPx()),
        end = Offset(x, size.height - dividerPadding.toPx())
      )
    }
  }
}

/**
 * Draws the actual segments in a [SegmentedControl].
 */
@Composable private fun <T> Segments(
  state: SegmentedControlState,
  segments: List<T>,
  content: @Composable (T) -> Unit
) {
  CompositionLocalProvider(
    LocalTextStyle provides TextStyle(fontWeight = FontWeight.Medium)
  ) {
    Row(
      horizontalArrangement = spacedBy(TRACK_PADDING),
      modifier = Modifier
        .fillMaxWidth()
        .selectableGroup()
    ) {
      segments.forEachIndexed { i, segment ->
        val isSelected = i == state.selectedSegment
        val isPressed = i == state.pressedSegment

        // Unselected presses are represented by fading.
        val alpha by animateFloatAsState(if (!isSelected && isPressed) PRESSED_UNSELECTED_ALPHA else 1f)

        // We can't use Modifier.selectable because it does way too much: it does its own input
        // handling and wires into Compose's indicaiton/interaction system, which we don't want because
        // we've got our own indication mechanism.
        val semanticsModifier = Modifier.semantics(mergeDescendants = true) {
          selected = isSelected
          role = Role.Button
          onClick { state.onSegmentSelected(i); true }
          stateDescription = if (isSelected) "Selected" else "Not selected"
        }

        Box(
          Modifier
            // Divide space evenly between all segments.
            .weight(1f)
            .then(semanticsModifier)
            .padding(SEGMENT_PADDING)
            // Draw pressed indication when not selected.
            .alpha(alpha)
            // Selected presses are represented by scaling.
            .then(state.segmentScaleModifier(isPressed && isSelected, i))
            // Center the segment content.
            .wrapContentWidth()
        ) {
          content(segment)
        }
      }
    }
  }
}

private class SegmentedControlState {
  var segmentCount by mutableStateOf(0)
  var selectedSegment by mutableStateOf(0)
  var onSegmentSelected: (Int) -> Unit by mutableStateOf({})
  var pressedSegment by mutableStateOf(NO_SEGMENT_INDEX)

  /**
   * Scale factor that should be used to scale pressed segments (both the segment itself and the
   * thumb). When this scale is applied, exactly [PRESSED_TRACK_PADDING] will be added around the
   * element's usual size.
   */
  var pressedSelectedScale by mutableStateOf(1f)
    private set

  /**
   * Calculates the scale factor we need to use for pressed segments to get the desired padding.
   */
  fun updatePressedScale(controlHeight: Int, density: Density) {
    with(density) {
      val pressedPadding = PRESSED_TRACK_PADDING * 2
      val pressedHeight = controlHeight - pressedPadding.toPx()
      pressedSelectedScale = pressedHeight / controlHeight
    }
  }

  /**
   * Returns a [Modifier] that will scale an element so that it gets [PRESSED_TRACK_PADDING] extra
   * padding around it. The scale will be animated.
   *
   * The scale is also performed around either the left or right edge of the element if the [segment]
   * is the first or last segment, respectively. In those cases, the scale will also be translated so
   * that [PRESSED_TRACK_PADDING] will be added on the left or right edge.
   */
  @SuppressLint("ModifierFactoryExtensionFunction")
  fun segmentScaleModifier(
    pressed: Boolean,
    segment: Int,
  ): Modifier = Modifier.composed {
    val scale by animateFloatAsState(if (pressed) pressedSelectedScale else 1f)
    val xOffset by animateDpAsState(if (pressed) PRESSED_TRACK_PADDING else 0.dp)

    graphicsLayer {
      this.scaleX = scale
      this.scaleY = scale

      // Scales on the ends should gravitate to that edge.
      this.transformOrigin = TransformOrigin(
        pivotFractionX = when (segment) {
          0 -> 0f
          segmentCount - 1 -> 1f
          else -> .5f
        },
        pivotFractionY = .5f
      )

      // But should still move inwards to keep the pressed padding consistent with top and bottom.
      this.translationX = when (segment) {
        0 -> xOffset.toPx()
        segmentCount - 1 -> -xOffset.toPx()
        else -> 0f
      }
    }
  }

  /**
   * A [Modifier] that will listen for touch gestures and update the selected and pressed properties
   * of this state appropriately.
   *
   * Input will be reset if the [segmentCount] changes.
   */
  val inputModifier = Modifier.pointerInput(segmentCount) {
    val segmentWidth = size.width / segmentCount

    // Helper to calculate which segment an event occured in.
    fun segmentIndex(change: PointerInputChange): Int =
      ((change.position.x / size.width.toFloat()) * segmentCount)
        .toInt()
        .coerceIn(0, segmentCount - 1)

    forEachGesture {
      awaitPointerEventScope {
        val down = awaitFirstDown()

        pressedSegment = segmentIndex(down)
        val downOnSelected = pressedSegment == selectedSegment
        val segmentBounds = Rect(
          left = pressedSegment * segmentWidth.toFloat(),
          right = (pressedSegment + 1) * segmentWidth.toFloat(),
          top = 0f,
          bottom = size.height.toFloat()
        )

        // Now that the pointer is down, the rest of the gesture depends on whether the segment that
        // was "pressed" was selected.
        if (downOnSelected) {
          // When the selected segment is pressed, it can be dragged to other segments to animate the
          // thumb moving and the segments scaling.
          horizontalDrag(down.id) { change ->
            pressedSegment = segmentIndex(change)

            // Notify the SegmentedControl caller when the pointer changes segments.
            if (pressedSegment != selectedSegment) {
              onSegmentSelected(pressedSegment)
            }
          }
        } else {
          // When an unselected segment is pressed, we just animate the alpha of the segment while
          // the pointer is down. No dragging is supported.
          waitForUpOrCancellation(inBounds = segmentBounds)
            // Null means the gesture was cancelled (e.g. dragged out of bounds).
            ?.let { onSegmentSelected(pressedSegment) }
        }

        // In either case, once the gesture is cancelled, stop showing the pressed indication.
        pressedSegment = NO_SEGMENT_INDEX
      }
    }
  }
}

/**
 * Copy of nullary waitForUpOrCancellation that works with bounds that may not be at 0,0.
 */
private suspend fun AwaitPointerEventScope.waitForUpOrCancellation(inBounds: Rect): PointerInputChange? {
  while (true) {
    val event = awaitPointerEvent(PointerEventPass.Main)
    if (event.changes.all { it.changedToUp() }) {
      // All pointers are up
      return event.changes[0]
    }

    if (event.changes.any { it.consumed.downChange || !inBounds.contains(it.position) }) {
      return null // Canceled
    }

    // Check for cancel by position consumption. We can look on the Final pass of the
    // existing pointer event because it comes after the Main pass we checked above.
    val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
    if (consumeCheck.changes.any { it.positionChangeConsumed() }) {
      return null
    }
  }
}