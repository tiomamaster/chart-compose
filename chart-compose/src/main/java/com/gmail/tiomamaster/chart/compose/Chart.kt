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
private const val yAnimDuration = 250
private val animSpecY = tween<Float>(yAnimDuration, easing = LinearEasing)
private val animSpecYInt = tween<Int>(yAnimDuration, easing = LinearEasing)

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
    var oldTranslateY by remember { mutableStateOf(transforms?.translateY ?: 0f) }
    var oldScaleY by remember { mutableStateOf(transforms?.scaleY ?: 1f) }
    val lineYCoordsToDisappear =
        remember(chartHeight, labelSettings, transforms?.translateY, transforms?.scaleY) {
            if (labelSettings == null) return@remember null
            val coords = List(linesCount + 1) {
                ((chartHeight - it * (chartHeight - lineTopMargin) / linesCount) - oldTranslateY) / oldScaleY
            }
            oldTranslateY = transforms?.translateY ?: 0f
            oldScaleY = transforms?.scaleY ?: 1f
            coords
        }
    var changeHorizontalLinesAlpha by remember { mutableStateOf(true) }
    val lineYCoordsToAppear =
        remember(chartHeight, labelSettings, transforms?.translateY, transforms?.scaleY) {
            if (labelSettings == null) return@remember null
            changeHorizontalLinesAlpha = !changeHorizontalLinesAlpha
            List(linesCount + 1) {
                ((chartHeight - it * (chartHeight - lineTopMargin) / linesCount) - (transforms?.translateY
                    ?: 0f)) / (transforms?.scaleY ?: 1f)
            }
        }
    val horizontalLinesAlpha by animateFloatAsState(
        if (changeHorizontalLinesAlpha) 0f else 1f, animSpecY
    )
    val horizontalLinesAppearAlpha = remember(horizontalLinesAlpha) {
        if (changeHorizontalLinesAlpha) 1f - horizontalLinesAlpha else horizontalLinesAlpha
    }
    val horizontalLinesDisappearAlpha = remember(horizontalLinesAlpha) {
        if (changeHorizontalLinesAlpha) horizontalLinesAlpha else 1f - horizontalLinesAlpha
    }
    val yLabelsAlpha by animateIntAsState(if (changeHorizontalLinesAlpha) 0 else 255, animSpecYInt)
    val yLabelsAppearPaint = remember(yLabelsAlpha) {
        Paint(labelsPaint).apply {
            alpha = if (changeHorizontalLinesAlpha) 255 - yLabelsAlpha else yLabelsAlpha
        }
    }
    val yLabelsDisappearPaint = remember(yLabelsAlpha) {
        Paint(labelsPaint).apply {
            alpha = if (changeHorizontalLinesAlpha) yLabelsAlpha else 255 - yLabelsAlpha
        }
    }
    val lineYCoordsToDisappearAnim = remember(lineYCoordsToDisappear, animTranslateY, animScaleY) {
        lineYCoordsToDisappear?.map { it * animScaleY + animTranslateY }
            ?.filter { it <= chartHeight }
    }
    val lineYCoordsToAppearAnim = remember(lineYCoordsToAppear, animTranslateY, animScaleY) {
        lineYCoordsToAppear?.map { it * animScaleY + animTranslateY }?.filter { it <= chartHeight }
    }

    var xLabelsToAppear: List<Pair<Float, String>>? by remember { mutableStateOf(null) }
    var xLabelsToDisappear: List<Pair<Float, String>>? by remember { mutableStateOf(null) }
    var changeXLabelsAlpha by remember { mutableStateOf(true) }
    val xLabelsAlpha by animateIntAsState(
        if (changeXLabelsAlpha) 0 else 255, spring(stiffness = Spring.StiffnessVeryLow)
    )
    val xLabelsAppearPaint = remember(xLabelsAlpha) {
        Paint(labelsPaint).apply {
            alpha = if (changeXLabelsAlpha) 255 - xLabelsAlpha else xLabelsAlpha
        }
    }
    val xLabelsDisappearPaint = remember(xLabelsAlpha) {
        Paint(labelsPaint).apply {
            alpha = if (changeXLabelsAlpha) xLabelsAlpha else 255 - xLabelsAlpha
        }
    }
    val labelsSize = labelSettings?.labelsSize?.toPx() ?: 0f
    xLabelsToAppear = remember(labelSettings, canvasSize, animTranslateX, animScaleX) {
        if (labelSettings == null) return@remember null
        labelsPaint.textSize = labelsSize
        xLabelsAppearPaint.textSize = labelsSize
        xLabelsDisappearPaint.textSize = labelsSize
        data.getXLabels(labelsPaint, canvasSize.width, labelSettings.xLabelsFormatter).map {
            it.coord * animScaleX + animTranslateX - it.offset to it.text
        }.also {
            if (xLabelsToAppear?.size != it.size && it.isNotEmpty()) {
                xLabelsAppearPaint.alpha = 0
                xLabelsDisappearPaint.alpha = 255
                changeXLabelsAlpha = !changeXLabelsAlpha
                xLabelsToDisappear = xLabelsToAppear
            }
        }
    }
    Canvas(modifier) {
        if (canvasSize == Size.Zero) {
            canvasSize = size
        } else {
            lineYCoordsToDisappearAnim?.forEach {
                drawLine(
                    Color.LightGray,
                    Offset(labelSettings!!.yLabelsStartPadding.toPx(), it + chartStrokeWidth),
                    Offset(size.width, it + chartStrokeWidth),
                    alpha = horizontalLinesDisappearAlpha
                )
            }
            lineYCoordsToAppearAnim?.forEach {
                drawLine(
                    Color.LightGray,
                    Offset(labelSettings!!.yLabelsStartPadding.toPx(), it + chartStrokeWidth),
                    Offset(size.width, it + chartStrokeWidth),
                    alpha = horizontalLinesAppearAlpha
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

            if (lineYCoordsToDisappearAnim == null) return@Canvas
            drawIntoCanvas {
                with(it.nativeCanvas) {
                    lineYCoordsToDisappearAnim.forEach { y ->
                        drawText(
                            data.getYValueByCoord(y).toString(),
                            labelSettings!!.yLabelsStartPadding.toPx(),
                            y,
                            yLabelsDisappearPaint
                        )
                    }
                    lineYCoordsToAppearAnim!!.forEach { y ->
                        drawText(
                            data.getYValueByCoord(y).toString(),
                            labelSettings!!.yLabelsStartPadding.toPx(),
                            y,
                            yLabelsAppearPaint
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
