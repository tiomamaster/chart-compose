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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val animSpecX = tween<Float>(100, easing = LinearEasing)
private val animSpecY = tween<Float>(150, easing = LinearEasing)

private const val chartStrokeWidth = 5f
private val chartPaint = Paint().apply {
    isAntiAlias = true
    isDither = true
    style = Paint.Style.STROKE
    strokeWidth = chartStrokeWidth
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
        canvasSize.height - xLabelsHeight - chartStrokeWidth
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
    val lineTopMargin = 32.dp.toPx()
    val linesCount = 5
    val lineYCoords = remember(chartHeight, labelSettings) {
        if (labelSettings == null) return@remember null
        List(linesCount + 1) {
            chartHeight - it * (chartHeight - lineTopMargin) / linesCount
        }
    }
    val labelsSize = labelSettings?.labelsSize?.toPx() ?: 0f
    val xLabels = remember(labelSettings, canvasSize, animTranslateX, animScaleX) {
        if (labelSettings == null) return@remember null
        data.getXLabels(
            getLabelsPaint(labelsSize),
            canvasSize.width,
            labelSettings.xLabelsFormatter
        ).map {
            it.coord * animScaleX + animTranslateX - it.offset to it.text
        }
    }
    Canvas(modifier) {
        if (canvasSize == Size.Zero) {
            canvasSize = size
        } else {
            lineYCoords?.forEach {
                drawLine(
                    Color.LightGray,
                    Offset(labelSettings!!.yLabelsStartPadding.toPx(), it + chartStrokeWidth),
                    Offset(size.width, it + chartStrokeWidth)
                )
            }

            withTransform({
                clipRect(bottom = chartHeight + chartStrokeWidth)
                inset(0f, chartStrokeWidth / 2, 0f, chartStrokeWidth / 2)
            }) {
                drawIntoCanvas {
                    paths?.forEachIndexed { i, path ->
                        it.nativeCanvas.drawPath(
                            path,
                            chartPaint.apply { color = selectedColors[i] })
                    }
                }
            }

            if (lineYCoords == null) return@Canvas
            drawIntoCanvas {
                with(it.nativeCanvas) {
                    lineYCoords.forEach { y ->
                        drawText(
                            data.getYValueByCoord(y).toString(),
                            labelSettings!!.yLabelsStartPadding.toPx(),
                            y,
                            getLabelsPaint(labelSettings.labelsSize.toPx())
                        )
                    }

                    xLabels?.forEach { (coord, text) ->
                        drawText(text, coord, size.height, labelsPaint!!)
                    }
                }
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
