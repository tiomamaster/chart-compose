@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.animation.core.*
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

private var labelsPaint: Paint = Paint().apply {
    isAntiAlias = true
    isDither = true
    color = android.graphics.Color.GRAY
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
    val lineYCoords =
        remember(chartHeight, labelSettings, transforms?.translateY, transforms?.scaleY) {
            if (labelSettings == null) return@remember null
            List(linesCount + 1) {
                ((chartHeight - it * (chartHeight - lineTopMargin) / linesCount) -
                        (transforms?.translateY ?: 0f)) / (transforms?.scaleY ?: 1f)
            }
        }
    val lineYCoordsAnim = remember(lineYCoords, animTranslateY, animScaleY) {
        if (labelSettings == null) return@remember null
        lineYCoords?.map { it * animScaleY + animTranslateY }
    }
    var xLabelsToAppear: List<Pair<Float, String>>? by remember { mutableStateOf(null) }
    var xLabelsToDisappear: List<Pair<Float, String>>? by remember { mutableStateOf(null) }
    var changeAlpha by remember { mutableStateOf(true) }
    val animAlpha by animateIntAsState(
        if (changeAlpha) 0 else 255,
        spring(stiffness = Spring.StiffnessVeryLow)
    )
    val xLabelsAppearPaint by remember(animAlpha) {
        val p = Paint(labelsPaint).apply { alpha = if (changeAlpha) 255 - animAlpha else animAlpha }
        mutableStateOf(p)
    }
    val xLabelsDisappearPaint by remember(animAlpha) {
        val p = Paint(labelsPaint).apply { alpha = if (changeAlpha) animAlpha else 255 - animAlpha }
        mutableStateOf(p)
    }
    val labelsSize = labelSettings?.labelsSize?.toPx() ?: 0f
    xLabelsToAppear = remember(labelSettings, canvasSize, animTranslateX, animScaleX) {
        if (labelSettings == null) return@remember null
        labelsPaint.textSize = labelsSize
        xLabelsAppearPaint.textSize = labelsSize
        xLabelsDisappearPaint.textSize = labelsSize
        data.getXLabels(
            labelsPaint,
            canvasSize.width,
            labelSettings.xLabelsFormatter
        ).map {
            it.coord * animScaleX + animTranslateX - it.offset to it.text
        }.also {
            if (xLabelsToAppear?.size != it.size && it.isNotEmpty()) {
                xLabelsAppearPaint.alpha = 0
                xLabelsDisappearPaint.alpha = 255
                changeAlpha = !changeAlpha
                xLabelsToDisappear = xLabelsToAppear
            }
        }
    }
    Canvas(modifier) {
        if (canvasSize == Size.Zero) {
            canvasSize = size
        } else {
            lineYCoordsAnim?.forEach {
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
                            chartPaint.apply { color = selectedColors[i] }
                        )
                    }
                }
            }

            if (lineYCoordsAnim == null) return@Canvas
            drawIntoCanvas {
                with(it.nativeCanvas) {
                    lineYCoordsAnim.forEach { y ->
                        drawText(
                            data.getYValueByCoord(y).toString(),
                            labelSettings!!.yLabelsStartPadding.toPx(),
                            y,
                            labelsPaint
                        )
                    }
                    xLabelsToAppear?.forEach { (coord, text) ->
                        drawText(text, coord, size.height, xLabelsAppearPaint)
                    }
                    xLabelsToDisappear?.forEach { (coord, text) ->
                        drawText(text, coord, size.height, xLabelsDisappearPaint)
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
