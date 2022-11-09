@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun ChartPreview(
    modifier: Modifier = Modifier,
    data: ChartData<*, *>,
    selectedCharts: List<Boolean>,
    width: Float,
    bigChartWidth: Float,
    onBoundsChanged: (left: Float, right: Float) -> Unit
) = Box(modifier) {
    val boundWidth = 16.dp
    val boundWidthPx = boundWidth.toPx()
    val k = bigChartWidth / (width - boundWidthPx * 2)
    var offsetLeft by remember { mutableStateOf(0f) }
    var offsetRight by remember { mutableStateOf(width - boundWidthPx) }

    Chart(
        Modifier
            .fillMaxSize()
            .padding(start = boundWidth, end = boundWidth),
        data,
        selectedCharts,
        0f,
        width - boundWidthPx * 2
    )

    Box(
        Modifier
            .fillMaxSize()
            .padding(
                start = boundWidth,
                end = width.toDp() - offsetLeft.toDp() - boundWidth
            )
            .background(Color.Gray.copy(alpha = 0.15f))
    )

    BoundControl(offsetLeft, boundWidth, true) { delta ->
        val new = offsetLeft + delta
        if (new >= 0 && new < offsetRight - boundWidthPx * 4) {
            offsetLeft = new
            onBoundsChanged(
                offsetLeft * k,
                (offsetRight - boundWidthPx) * k
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(
                start = boundWidth + offsetLeft.toDp(),
                end = width.toDp() - offsetRight.toDp()
            )
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState {
                    val newLeft = offsetLeft + it
                    val newRight = offsetRight + it
                    if (newLeft <= 0 || newRight >= width - boundWidthPx) return@rememberDraggableState
                    if (newLeft >= 0 && newLeft < offsetRight - boundWidthPx * 2) {
                        offsetLeft = newLeft
                    }
                    if (newRight >= offsetLeft + boundWidthPx * 2 && newRight < width - boundWidthPx) {
                        offsetRight = newRight
                    }
                    onBoundsChanged(
                        offsetLeft * k,
                        (offsetRight - boundWidthPx) * k
                    )
                }
            )
            .border(1.dp, Color.Gray.copy(alpha = 0.5f))
    )

    BoundControl(offsetRight, boundWidth, false) { delta ->
        val new = offsetRight + delta
        if (new >= offsetLeft + boundWidthPx * 4 && new < width - boundWidthPx) {
            offsetRight = new
            onBoundsChanged(
                offsetLeft * k,
                (offsetRight - boundWidthPx) * k
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(
                start = offsetRight.toDp() + boundWidth - boundWidth,
                end = boundWidth
            )
            .background(Color.Gray.copy(alpha = 0.15f))
    )
}

@Composable
private fun BoundControl(
    offset: Float,
    width: Dp,
    isLeft: Boolean,
    onDrag: (delta: Float) -> Unit
) = Box(
    Modifier
        .offset {
            IntOffset(offset.roundToInt(), 0)
        }
        .draggable(
            orientation = Orientation.Horizontal,
            state = rememberDraggableState(onDrag)
        )
        .background(
            Color.Gray.copy(alpha = 0.5f),
            if (isLeft) RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
            else RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
        )
        .width(width)
        .fillMaxHeight()
) {
    BasicText("||", Modifier.align(Alignment.Center), TextStyle(Color.White))
}
