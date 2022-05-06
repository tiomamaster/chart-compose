@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val animSpecX = tween<Float>(100, easing = LinearEasing)
private val animSpecY = tween<Float>(150, easing = LinearEasing)

private val chartPaint = Paint().apply {
    isAntiAlias = true
    isDither = true
    style = Paint.Style.STROKE
    strokeWidth = 5f
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

@Composable
internal fun Chart(
    modifier: Modifier = Modifier,
    data: ChartData<Number, Number>,
    selectedCharts: List<Boolean>,
    selectedColors: List<Int>,
    leftBound: Float,
    rightBound: Float,
    labelSettings: LabelSettings? = null
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val xLabelsHeight = labelSettings?.run { (labelsSize + xLabelsTopPadding).toPx() } ?: 0f
    val chartHeight = remember(labelSettings, canvasSize) {
        canvasSize.height - xLabelsHeight
    }
    val sourcePaths = remember(data, canvasSize, chartHeight) {
        if (canvasSize == Size.Zero) null
        else data.getOffsets(canvasSize.width, chartHeight).map {
            Path().apply {
                it.mapIndexed { i, offset ->
                    if (i == 0) moveTo(offset.x, offset.y)
                    else lineTo(offset.x, offset.y)
                }
            }
        }
    }
    val transforms by remember(leftBound, rightBound, sourcePaths) {
        derivedStateOf {
            if (sourcePaths == null) null
            else data.getTransforms(selectedCharts, leftBound, rightBound)
        }
    }
    val animTranslateX by animateFloatAsState(transforms?.translateX ?: 0f, animSpecX)
    val animTranslateY by animateFloatAsState(transforms?.translateY ?: 0f, animSpecY)
    val animScaleX by animateFloatAsState(transforms?.scaleX ?: 1f, animSpecX)
    val animScaleY by animateFloatAsState(transforms?.scaleY ?: 1f, animSpecY)
    val matrix = remember { Matrix() }
    val paths by remember(sourcePaths, animTranslateX, animTranslateY, animScaleX, animScaleY) {
        matrix.reset()
        matrix.setTranslate(animTranslateX, animTranslateY)
        matrix.preScale(animScaleX, animScaleY)
        derivedStateOf {
            sourcePaths?.selected(selectedCharts)?.map {
                Path().apply {
                    it.transform(matrix, this)
                }
            }
        }
    }
    Canvas(modifier) {
        if (canvasSize == Size.Zero) {
            canvasSize = size
        } else {
            val yLineCoords = labelSettings?.let { drawLines(chartHeight, it) }

            clipRect(bottom = chartHeight) {
                drawIntoCanvas {
                    paths?.forEachIndexed { i, path ->
                        it.nativeCanvas.drawPath(
                            path,
                            chartPaint.apply { color = selectedColors[i] })
                    }
                }
            }

            yLineCoords?.also {
                drawYLabels(it, data, labelSettings)
                drawXLabels(data, labelSettings.xLabelsFormatter)
            }
        }
    }
}

internal class LabelSettings(
    val labelsSize: Dp,
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
            getLabelsPaint(labelSettings.labelsSize.toPx())
        )
    }
}

private fun DrawScope.drawXLabels(
    data: ChartData<Number, Number>,
    formatter: ((xValue: Number) -> String)
) = data.getXLabels(labelsPaint!!, size.width, formatter).forEach { (coord, text) ->
    drawIntoCanvas {
        it.nativeCanvas.drawText(text, coord, size.height, labelsPaint!!)
    }
}
