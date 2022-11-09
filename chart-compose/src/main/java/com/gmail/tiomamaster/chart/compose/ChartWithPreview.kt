@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ChartWithPreview(
    modifier: Modifier = Modifier,
    data: ChartData<*, *>,
    xLabelsFormatter: (xValue: Number) -> String,
    xDetailsFormatter: (xValue: Number) -> String
) = BoxWithConstraints(modifier) {
    val widthPx = maxWidth.toPx()
    val bigChartPaddingEnd = 16.dp
    val bigChartWidthPx = widthPx - bigChartPaddingEnd.toPx()
    val bigChartHeight = this@BoxWithConstraints.maxHeight * 4 / 6
    val labelsSize = 14.dp
    val yLabelsStartPadding = 16.dp
    val xLabelsTopPadding = 2.dp
    val labelSettings = remember {
        LabelSettings(labelsSize, yLabelsStartPadding, xLabelsTopPadding, xLabelsFormatter)
    }

    Column {
        var leftBound by remember { mutableStateOf(0f) }
        var rightBound by remember { mutableStateOf(bigChartWidthPx) }
        var touchXCoord by remember { mutableStateOf(-1f) }
        var isDetailsVisible by remember { mutableStateOf(false) }
        val selectedCharts = remember { mutableStateListOf(*Array(data.colors.size) { true }) }
        val selectedColors by remember(data.colors) { derivedStateOf { data.colors.selected(selectedCharts) } }
        val selectedLabels by remember(data.labels) { derivedStateOf { data.labels.selected(selectedCharts) } }

        Box(Modifier.height(IntrinsicSize.Max)) {
            Chart(
                Modifier
                    .fillMaxWidth()
                    .padding(end = bigChartPaddingEnd)
                    .height(bigChartHeight)
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
                                touchXCoord = new.coerceIn(0f, bigChartWidthPx)
                                isDetailsVisible = true
                            }
                        )
                    },
                data,
                selectedCharts,
                leftBound,
                rightBound,
                labelSettings
            )

            val detailsData = touchXCoord.takeUnless { it == -1f }
                ?.let { data.getDetailsForCoord(it, xDetailsFormatter) }
            ChartTouchDetails(
                bigChartHeight - labelsSize - xLabelsTopPadding,
                isDetailsVisible,
                detailsData?.xCoord?.roundToInt() ?: (widthPx / 2).roundToInt(),
                widthPx.roundToInt(),
                detailsData?.title ?: "",
                selectedColors,
                selectedLabels,
                detailsData?.yCoords ?: List(data.colors.size) { 0f },
                detailsData?.yValues ?: List(data.colors.size) { 0f }
            ) {
                touchXCoord = -1f
            }
        }

        Spacer(Modifier.height(16.dp))

        val chartPreviewData = remember { data.copy() }
        ChartPreview(
            Modifier
                .height(bigChartHeight / 5)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            chartPreviewData,
            selectedCharts,
            widthPx - 32.dp.toPx(),
            bigChartWidthPx
        ) { left, right ->
            leftBound = left.toInt().toFloat()
            rightBound = right.roundToInt().toFloat()
            isDetailsVisible = false
        }

        Spacer(Modifier.height(16.dp))

        ChartSelectors(data, selectedCharts)
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
