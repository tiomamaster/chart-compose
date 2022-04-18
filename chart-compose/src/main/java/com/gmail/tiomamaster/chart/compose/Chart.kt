@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun Chart(
    modifier: Modifier = Modifier,
    data: ChartData<Number, Number>,
    leftBound: Float,
    rightBound: Float,
    labelSettings: LabelSettings? = null
) {
    var chartWidth by remember { mutableStateOf(0f) }
    var chartHeight by remember { mutableStateOf(0f) }

    val paths = data.calcPaths(chartWidth, chartHeight, leftBound, rightBound)?.value
    Canvas(modifier) {
        chartWidth = size.width
        val xLabelsHeight = labelSettings?.run { (labelSize + xLabelsTopPadding).toPx() } ?: 0f
        chartHeight = size.height - xLabelsHeight

        if (paths == null) return@Canvas

        val yLineCoords = labelSettings?.let { drawLines(chartHeight, it) }

        paths.forEachIndexed { i, path ->
            drawPath(
                path = path,
                color = data.colors[i],
                style = Stroke(5f)
            )
        }

        yLineCoords?.also {
            drawYLabels(it, data, labelSettings)
            drawXLabels(data, labelSettings.xLabelsFormatter)
        }
    }
}

internal class LabelSettings(
    val labelSize: Dp,
    val yLabelsStartPadding: Dp,
    val xLabelsTopPadding: Dp,
    val xLabelsFormatter: (xValue: Number) -> String
)

private fun DrawScope.drawLines(chartHeight: Float, settings: LabelSettings): List<Float> {
    val linesCount = 5
    val k = (chartHeight - 32.dp.toPx()) / linesCount
    return List(linesCount + 1) {
        val y = chartHeight - it * k
        drawLine(
            Color.LightGray,
            Offset(settings.yLabelsStartPadding.toPx(), y),
            Offset(size.width, y)
        )
        y
    }
}

private fun DrawScope.drawYLabels(
    yLineCoords: List<Float>,
    data: ChartData<Number, Number>,
    labelSettings: LabelSettings
) = yLineCoords.forEach { y ->
    drawIntoCanvas {
        it.nativeCanvas.drawText(
            data.getYValueByCoord(y).toString(),
            labelSettings.yLabelsStartPadding.toPx(),
            y - 4.dp.toPx(),
            getLabelsPaint(labelSettings.labelSize.toPx())
        )
    }
}

private var labelsPaint: Paint? = null
private fun getLabelsPaint(txtSize: Float): Paint {
    if (labelsPaint == null) labelsPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 0f
        textSize = txtSize
        color = android.graphics.Color.GRAY
    }
    return labelsPaint!!
}

private fun DrawScope.drawXLabels(
    data: ChartData<Number, Number>,
    formatter: ((xValue: Number) -> String)
) = data.getXLabels(labelsPaint!!, size.width, formatter).forEach { (coord, text) ->
    drawIntoCanvas {
        it.nativeCanvas.drawText(text, coord, size.height, labelsPaint!!)
    }
}
