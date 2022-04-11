@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import android.annotation.SuppressLint
import android.graphics.Paint
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import kotlin.math.roundToInt

@Composable
fun ChartWithPreviewDemo(data: ChartData<Number, Number>) {
    @SuppressLint("SimpleDateFormat")
    val formatter = SimpleDateFormat("MMM dd")
    ChartWithPreview(data, Modifier.padding(16.dp), formatter::format)
}

@Composable
fun ChartWithPreview(
    data: ChartData<Number, Number>,
    modifier: Modifier = Modifier,
    xLabelsFormatter: (xValue: Number) -> String
) {
    BoxWithConstraints(modifier) {
        val widthPx = maxWidth.toPx()
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            var leftBound by remember { mutableStateOf(0f) }
            var rightBound by remember { mutableStateOf(widthPx) }
            Chart(
                data,
                Modifier.fillMaxWidth().height(this@BoxWithConstraints.maxHeight / 2),
                leftBound,
                rightBound
            )
            XLabels(data, Modifier.fillMaxWidth().height(16.dp), xLabelsFormatter)
            ChartPreview(
                data,
                Modifier.fillMaxWidth().height(this@BoxWithConstraints.maxHeight / 5),
                widthPx
            ) { left, right ->
                leftBound = left
                rightBound = right
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
    data.calcPaths(0f, size.width, size.height, leftBound, rightBound).forEach { (path, color) ->
        drawPath(
            path = path,
            color = color,
            style = Stroke(5f)
        )
    }
}

private var xLabelsPaint: Paint? = null

private fun getXLabelsPaint(txtSize: Float): Paint {
    if (xLabelsPaint == null) xLabelsPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 0f
        textSize = txtSize
        color = android.graphics.Color.GRAY
    }
    return xLabelsPaint!!
}

@Composable
fun XLabels(
    data: ChartData<Number, Number>,
    modifier: Modifier = Modifier,
    formatter: (xValue: Number) -> String
) = Canvas(modifier) {
    val paint = getXLabelsPaint(size.height)
    data.getXLabels(paint, size.width, formatter).forEach { (coord, text) ->
        drawIntoCanvas {
            it.nativeCanvas.drawText(text, coord, size.height, paint)
        }
    }
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
    var offsetLeft by remember { mutableStateOf(0f) }
    var offsetRight by remember { mutableStateOf(width - boundWidthPx) }

    Box(modifier) {
        Chart(
            data,
            Modifier
                .fillMaxSize()
                .padding(start = boundWidth, end = boundWidth),
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
                    offsetLeft,
                    offsetRight + boundWidthPx
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
                            offsetLeft,
                            offsetRight + boundWidthPx
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
                    offsetLeft,
                    offsetRight + boundWidthPx
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
}

@Composable
fun BoundControl(offset: Float, width: Dp, isLeft: Boolean, onDrag: (delta: Float) -> Unit) = Box(
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

@Preview
@Composable
fun ChartPreview() = ChartWithPreview(
    ChartData(listOf(1, 2, 3, 4, 5), listOf(listOf(100, 55, 28, 99, 128)), emptyList()), Modifier
) { "X label" }