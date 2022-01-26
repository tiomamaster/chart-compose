package com.gmail.tiomamaster.chart

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ChartWithPreview(data: ChartData<Number, Number>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val widthPx = maxWidth.toPx()
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val leftBound = remember { mutableStateOf(0f) }
            val rightBound = remember { mutableStateOf(widthPx) }
            Chart(
                data,
                Modifier.fillMaxWidth().height(this@BoxWithConstraints.maxHeight / 2),
                leftBound.value,
                rightBound.value
            )
            ChartPreview(
                data,
                Modifier.fillMaxWidth().height(this@BoxWithConstraints.maxHeight / 4),
                widthPx
            ) { left, right ->
                leftBound.value = left
                rightBound.value = right
            }
        }
    }
}

@Composable
fun Chart(
    data: ChartData<Number, Number>,
    modifier: Modifier = Modifier,
    leftBound: Float,
    rightBound: Float
) = Canvas(modifier) {
    val width = size.width
    val height = size.height

    drawPath(
        path = data.calcPath(0f, width, height, leftBound, rightBound),
        color = Color.Blue,
        style = Stroke(5f)
    )
}

@Composable
fun ChartPreview(
    data: ChartData<Number, Number>,
    modifier: Modifier = Modifier,
    width: Float,
    onBoundsChanged: (left: Float, right: Float) -> Unit
) {
    val boundWidth = 16.dp
    val boundWidthPx = boundWidth.toPx()
    val offsetLeft = remember { mutableStateOf(0f) }
    val offsetRight = remember { mutableStateOf(width - boundWidthPx) }

    Box(modifier) {
        Chart(data, Modifier.fillMaxSize(), 0f, width)

        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    end = width.toDp() - offsetLeft.value.toDp()
                )
                .background(Color.Gray.copy(alpha = 0.25f))
        )

        Box(
            Modifier
                .offset {
                    IntOffset(offsetLeft.value.roundToInt(), 0)
                }
                .background(Color.Gray.copy(alpha = 0.75f))
                .width(boundWidth)
                .fillMaxHeight()
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState {
                        val new = offsetLeft.value + it
                        if (new >= 0 && new < offsetRight.value - boundWidthPx * 2) {
                            offsetLeft.value = new
                            onBoundsChanged(
                                offsetLeft.value,
                                offsetRight.value + boundWidthPx
                            )
                        }
                        Log.d("Offset", "offset left = ${offsetLeft.value}")
                    })
        )

        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    start = boundWidth + offsetLeft.value.toDp(),
                    end = width.toDp() - offsetRight.value.toDp()
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState {
                        val newLeft = offsetLeft.value + it
                        val newRight = offsetRight.value + it
                        Log.d("lala", "r = $newRight, l = $newLeft")
                        if (newLeft <= 0 || newRight >= width - boundWidthPx) return@rememberDraggableState
                        if (newLeft >= 0 && newLeft < offsetRight.value - boundWidthPx * 2) {
                            offsetLeft.value = newLeft
                        }
                        if (newRight >= offsetLeft.value + boundWidthPx * 2 && newRight < width - boundWidthPx) {
                            offsetRight.value = newRight
                        }
                        onBoundsChanged(
                            offsetLeft.value,
                            offsetRight.value + boundWidthPx
                        )
                    }
                )
                .border(1.dp, Color.Gray)
        )

        Box(
            Modifier
                .offset {
                    IntOffset(offsetRight.value.roundToInt(), 0)
                }
                .background(Color.Gray.copy(alpha = 0.75f))
                .width(boundWidth)
                .fillMaxHeight()
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState {
                        val new = offsetRight.value + it
                        if (new >= offsetLeft.value + boundWidthPx * 2 && new < width - boundWidthPx) {
                            offsetRight.value = new
                            onBoundsChanged(
                                offsetLeft.value,
                                offsetRight.value + boundWidthPx
                            )
                        }
                        Log.d("Offset", "offset right= ${offsetRight.value}")
                    })
        )

        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    start = offsetRight.value.toDp() + boundWidth
                )
                .background(Color.Gray.copy(alpha = 0.25f))
        )
    }
}

@Composable
private fun Dp.toPx() = with(LocalDensity.current) { this@toPx.toPx() }

@Composable
private fun Float.toDp() = with(LocalDensity.current) { this@toDp.toDp() }

@Preview
@Composable
fun ChartPreview() = ChartWithPreview(
    ChartData(listOf(1, 2, 3, 4, 5), listOf(100, 55, 28, 99, 128))
)
