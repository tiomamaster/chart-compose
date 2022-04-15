@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

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
        var isDetailsVisible by remember { mutableStateOf(false) }

        Box(Modifier.height(IntrinsicSize.Max)) {
            Chart(
                Modifier
                    .fillMaxWidth()
                    .height(this@BoxWithConstraints.maxHeight * 3 / 4)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            touchXCoord = offset.x
                            isDetailsVisible = true
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                if (touchXCoord == -1f) touchXCoord = change.position.x
                                val new = touchXCoord + dragAmount
                                touchXCoord = new.coerceIn(0f, widthPx)
                                isDetailsVisible = true
                            }
                        )
                    },
                data,
                leftBound,
                rightBound
            )

            val detailsData = touchXCoord.takeUnless { it == -1f }
                ?.let { data.getDetailsForCoord(it, xDetailsFormatter) }
            ChartTouchDetails(
                isDetailsVisible,
                detailsData?.xCoord?.roundToInt() ?: (widthPx / 2).roundToInt(),
                widthPx.roundToInt(),
                detailsData?.title ?: "",
                data.colors,
                data.labels,
                detailsData?.yCoords ?: List(data.colors.size) { 0f },
                detailsData?.yValues ?: List(data.colors.size) { 0f }
            ) {
                touchXCoord = -1f
            }
        }

        XLabels(data, Modifier.fillMaxWidth().height(16.dp), xLabelsFormatter)

        ChartPreview(
            data.copy(),
            Modifier
                .fillMaxWidth()
                .height(this@BoxWithConstraints.maxHeight * 1 / 4)
                .padding(start = 16.dp, end = 16.dp),
            widthPx - 32.dp.toPx(),
            widthPx
        ) { left, right ->
            leftBound = left
            rightBound = right
            isDetailsVisible = false
        }
    }
}

@Preview
@Composable
private fun ChartWithPreviewPreview() = ChartWithPreview(
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
