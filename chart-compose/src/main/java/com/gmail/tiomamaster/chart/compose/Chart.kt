@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import android.annotation.SuppressLint
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import java.text.SimpleDateFormat
import kotlin.math.roundToInt

@SuppressLint("SimpleDateFormat")
@Composable
fun ChartWithPreviewDemo(data: ChartData<Number, Number>) {
    val labelsFormatter = SimpleDateFormat("MMM dd")
    val detailsFormatter = SimpleDateFormat("EE, dd MMM yyyy")
    ChartWithPreview(
        Modifier.padding(top = 16.dp, bottom = 16.dp),
        data,
        labelsFormatter::format,
        detailsFormatter::format
    )
}

@Composable
fun ChartWithPreview(
    modifier: Modifier = Modifier,
    data: ChartData<Number, Number>,
    xLabelsFormatter: (xValue: Number) -> String,
    xDetailsFormatter: (xValue: Number) -> String
) = BoxWithConstraints(modifier) {
    val widthPx = maxWidth.toPx()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        var leftBound by remember { mutableStateOf(0f) }
        var rightBound by remember { mutableStateOf(widthPx) }
        var touchXCoord by remember { mutableStateOf(-1f) }
        Box(Modifier.height(IntrinsicSize.Max)) {
            Chart(
                Modifier
                    .fillMaxWidth()
                    .height(this@BoxWithConstraints.maxHeight / 2)
                    .pointerInput(Unit) {
                        detectTapGestures { offset -> touchXCoord = offset.x }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                if (touchXCoord == -1f) touchXCoord = change.position.x
                                val newX = touchXCoord + dragAmount
                                touchXCoord = newX.coerceIn(0f, widthPx)
                            }
                        )
                    },
                data,
                leftBound,
                rightBound
            )
            if (touchXCoord != -1f) {
                val detailsData = data.getDetailsForCoord(touchXCoord, xDetailsFormatter)
                Details(
                    Modifier.offset { IntOffset(detailsData.xCoord.roundToInt(), 0) },
                    detailsData.title,
                    data.colors,
                    data.labels,
                    detailsData.yCoords,
                    detailsData.yValues
                )
            }
        }

        XLabels(data, Modifier.fillMaxWidth().height(16.dp), xLabelsFormatter)

        ChartPreview(
            data.copy(),
            Modifier
                .fillMaxWidth()
                .height(this@BoxWithConstraints.maxHeight / 5)
                .padding(start = 16.dp, end = 16.dp),
            widthPx - 32.dp.toPx(),
            32.dp.toPx()
        ) { left, right ->
            leftBound = left
            rightBound = right
            touchXCoord = -1f
        }
    }
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun Details(
    modifier: Modifier,
    title: String,
    colors: List<Color>,
    labels: List<String>,
    yCoords: List<Float>,
    yValues: List<Number>
) = Box(modifier) {
    val lineWidth = 1.dp
    Box(
        Modifier
            .fillMaxHeight()
            .width(lineWidth)
            .background(Color.LightGray)
    )

    val dotSize = 8.dp
    val halfDotSizePxNegative = (dotSize.toPx() / -2).toInt()
    val xOffset = halfDotSizePxNegative + (lineWidth.toPx() / 2).toInt()
    colors.forEachIndexed { i, color ->
        val yOffset = yCoords[i].roundToInt() + halfDotSizePxNegative
        Box(
            Modifier
                .offset { IntOffset(xOffset, yOffset) }
                .clip(CircleShape)
                .border(1.dp, color, CircleShape)
                .background(Color.White)
                .size(dotSize)
        )
    }

    var offset by remember { mutableStateOf(0) }
    Column(
        Modifier
            .offset { IntOffset(offset, 0) }
            .onSizeChanged { offset = it.width / -2 }
            .background(Color.White)
            .border(Dp.Hairline, Color.Black, RoundedCornerShape(4.dp))
            .padding(8.dp)
            .width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BasicText(title)
        colors.forEachIndexed { i, color ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BasicText(
                    labels[i],
                    style = TextStyle.Default.copy(
                        fontSize = TextUnit(
                            10f,
                            TextUnitType.Sp
                        )
                    )
                )
                Spacer(Modifier.width(8.dp))
                BasicText(
                    yValues[i].toString(),
                    style = TextStyle.Default.copy(color, TextUnit(10f, TextUnitType.Sp))
                )
            }
        }
    }
}

@Composable
fun Chart(
    modifier: Modifier = Modifier,
    data: ChartData<Number, Number>,
    leftBound: Float,
    rightBound: Float
) = Canvas(modifier) {
    data.calcPaths(size.width, size.height, leftBound, rightBound).forEach { (path, color) ->
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
    totalPadding: Float,
    onBoundsChanged: (left: Float, right: Float) -> Unit
) {
    val boundWidth = 16.dp
    val boundWidthPx = boundWidth.toPx()
    var offsetLeft by remember { mutableStateOf(0f) }
    var offsetRight by remember { mutableStateOf(width - boundWidthPx) }

    Box(modifier) {
        Chart(
            Modifier
                .fillMaxSize()
                .padding(start = boundWidth, end = boundWidth),
            data,
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
                    offsetRight + boundWidthPx + totalPadding
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
                            offsetRight + boundWidthPx + totalPadding
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
                    offsetRight + boundWidthPx + totalPadding
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
    Modifier,
    ChartData(
        listOf(1, 2, 3, 4, 5),
        listOf(listOf(100, 55, 28, 99, 128)),
        listOf(Color.Red),
        listOf("1")
    ),
    { "X label" },
    { "Aug 11" }
)

@Preview
@Composable
fun DetailsPreview() = Details(
    Modifier,
    "Aug 8",
    listOf(Color.Blue, Color.Red),
    listOf("1", "2"),
    listOf(1f, 2f),
    listOf(1121, 2224)
)
