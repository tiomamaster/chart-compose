@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
        var scale by remember { mutableFloatStateOf(1f) }
        val range = chartWidthPx / scale
        val startOffsetPx = chartWidthPx / (100f / startOffsetPercent)
        val minOffset = (range - chartWidthPx) / 2
        val maxOffset =
            ((chartWidthPx - range).absoluteValue / 2 + startOffsetPx / scale).absoluteValue
        var offset by remember { mutableFloatStateOf(startOffsetPx) }
        val scrollableState = rememberScrollableState {
            offset = (offset - it / scale).coerceIn(minOffset, maxOffset)
            it
        }
        val transformableState = rememberTransformableState { _, zoomChange, _, _ ->
            scale = (scale * zoomChange).coerceIn(minScale, maxScale)
            offset = offset.coerceIn(minOffset, maxOffset)
        }
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
                    .transformable(transformableState)
                    .scrollable(scrollableState, Orientation.Horizontal)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                tapXCoord = it.x
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
