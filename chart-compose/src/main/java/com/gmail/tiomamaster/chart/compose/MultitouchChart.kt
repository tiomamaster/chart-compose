@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun MultitouchChart(
    modifier: Modifier = Modifier,
    data: ChartData<*, *>,
    xLabelsFormatter: (xValue: Number) -> String,
    xDetailsFormatter: (xValue: Number) -> String,
    minPointsOnChart: Int = 5,
    startOffsetPercent: Int = 25
) = BoxWithConstraints(modifier) {
    val widthPx = maxWidth.toPx()
    val bigChartPaddingEnd = 16.dp
    val chartWidthPx = widthPx - bigChartPaddingEnd.toPx()
    val chartHeight = this@BoxWithConstraints.maxHeight * 4 / 6
    val labelsSize = 14.dp
    val yLabelsStartPadding = 16.dp
    val xLabelsTopPadding = 2.dp
    val labelSettings = remember {
        LabelSettings(labelsSize, yLabelsStartPadding, xLabelsTopPadding, xLabelsFormatter)
    }

    Column {
        var tapXCoord by remember { mutableFloatStateOf(-1f) }
        var isDetailsVisible by remember { mutableStateOf(false) }

        val minScale = 1f
        val maxScale = remember(data.x.size, minPointsOnChart) {
            (data.x.size.toFloat() / minPointsOnChart.coerceAtLeast(2))
                .coerceAtLeast(minScale)
        }
        val scale = remember { mutableFloatStateOf(1f) }
        val range = chartWidthPx / scale.floatValue
        val startOffsetPx = chartWidthPx / (100f / startOffsetPercent)

        val decay = rememberSplineBasedDecay<Float>()
        val offsetAnimatable = remember { Animatable(startOffsetPx) }
        val offset = offsetAnimatable.value

        val (leftBound, rightBound) = remember(range, offset) {
            isDetailsVisible = false
            (((chartWidthPx - range) / 2) + offset) to
                    ((range + ((chartWidthPx - range) / 2)) + offset)
        }
        val selectedCharts = remember { mutableStateListOf(*Array(data.colors.size) { true }) }
        val selectedColors by remember { derivedStateOf { data.colors.selected(selectedCharts) } }
        val selectedLabels by remember { derivedStateOf { data.labels.selected(selectedCharts) } }

        Box(Modifier.height(IntrinsicSize.Max)) {
            Chart(
                Modifier
                    .fillMaxWidth()
                    .padding(end = bigChartPaddingEnd)
                    .height(chartHeight)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                tapXCoord = it.x
                                isDetailsVisible = true
                            }
                        )
                    }
                    .zoomAndScroll(
                        offsetAnimatable = offsetAnimatable,
                        scale = scale,
                        minScale = minScale,
                        maxScale = maxScale,
                        chartWidthPx = chartWidthPx,
                        startOffsetPx = startOffsetPx,
                        decay = decay
                    ),
                data,
                selectedCharts,
                leftBound,
                rightBound,
                labelSettings
            )

            val detailsData = tapXCoord.takeUnless { it == -1f }
                ?.let { data.getDetailsForCoord(it, xDetailsFormatter) }
            ChartTouchDetails(
                chartHeight - labelsSize - xLabelsTopPadding,
                isDetailsVisible,
                detailsData?.xCoord?.roundToInt() ?: (widthPx / 2).roundToInt(),
                widthPx.roundToInt(),
                detailsData?.title ?: "",
                selectedColors,
                selectedLabels,
                detailsData?.yCoords ?: List(data.colors.size) { 0f },
                detailsData?.yValues ?: List(data.colors.size) { 0f }
            ) {
                tapXCoord = -1f
            }
        }

        Spacer(Modifier.height(16.dp))

        ChartSelectors(data, selectedCharts)
    }
}

private fun Modifier.zoomAndScroll(
    offsetAnimatable: Animatable<Float, *>,
    scale: MutableFloatState,
    minScale: Float,
    maxScale: Float,
    chartWidthPx: Float,
    startOffsetPx: Float,
    decay: DecayAnimationSpec<Float>
) = pointerInput(Unit) {
    val velocityTracker = VelocityTracker()
    coroutineScope {
        val scope = this
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            scope.launch { offsetAnimatable.stop() }

            velocityTracker.resetTracking()
            velocityTracker.addPosition(down.uptimeMillis, down.position)

            var isMultiTouch = false
            var pastTouchSlop = false
            val touchSlop = viewConfiguration.touchSlop
            var zoom = 1f
            var pan = Offset.Zero

            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.any { it.isConsumed }
                if (!canceled) {
                    if (event.changes.size > 1) {
                        isMultiTouch = true
                        velocityTracker.resetTracking()
                    } else if (!isMultiTouch) {
                        val change = event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            velocityTracker.addPosition(
                                change.uptimeMillis,
                                change.position
                            )
                        }
                    }

                    val zoomChange = event.calculateZoom()
                    val panChange = event.calculatePan()

                    if (!pastTouchSlop) {
                        zoom *= zoomChange
                        pan += panChange

                        val centroidSize =
                            event.calculateCentroidSize(useCurrent = false)
                        val zoomMotion = abs(1 - zoom) * centroidSize
                        val panMotion = pan.getDistance()

                        if (zoomMotion > touchSlop || panMotion > touchSlop) {
                            pastTouchSlop = true
                        }
                    }

                    if (pastTouchSlop) {
                        val centroid =
                            event.calculateCentroid(useCurrent = false)
                        if (zoomChange != 1f || panChange != Offset.Zero) {
                            val oldScale = scale.floatValue
                            val newScale =
                                (oldScale * zoomChange).coerceIn(
                                    minScale,
                                    maxScale
                                )
                            scale.floatValue = newScale

                            val currentRange = chartWidthPx / newScale
                            val currentMinOffset =
                                (currentRange - chartWidthPx) / 2
                            val currentMaxOffset =
                                ((chartWidthPx - currentRange).absoluteValue / 2 + startOffsetPx / newScale).absoluteValue

                            val dx = centroid.x - chartWidthPx / 2
                            val zoomOffsetChange =
                                dx * (1f / oldScale - 1f / newScale)
                            val newOffset =
                                (offsetAnimatable.value - panChange.x / newScale + zoomOffsetChange)
                                    .coerceIn(
                                        currentMinOffset,
                                        currentMaxOffset
                                    )

                            scope.launch {
                                offsetAnimatable.snapTo(newOffset)
                            }
                        }
                        event.changes.forEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            } while (!canceled && event.changes.any { it.pressed })

            if (!isMultiTouch && pastTouchSlop) {
                val velocityX = velocityTracker.calculateVelocity().x
                if (abs(velocityX) > 100f) {
                    val currentScale = scale.floatValue
                    val currentRange = chartWidthPx / currentScale
                    val currentMinOffset = (currentRange - chartWidthPx) / 2
                    val currentMaxOffset =
                        ((chartWidthPx - currentRange).absoluteValue / 2 + startOffsetPx / currentScale).absoluteValue

                    offsetAnimatable.updateBounds(
                        currentMinOffset,
                        currentMaxOffset
                    )
                    scope.launch {
                        offsetAnimatable.animateDecay(-velocityX / currentScale, decay)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun TouchChartPreview() = MultitouchChart(
    Modifier
        .background(Color.White)
        .padding(top = 16.dp, bottom = 16.dp),
    ChartData(
        listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
        listOf(listOf(100, 44, 28, 99, 128, 33, 78, 33, 10, 155)),
        listOf(Color.Red),
        listOf("1")
    ),
    { "X label" },
    { "Aug 11" }
)
