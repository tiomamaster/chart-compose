@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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

var labelsPaint: Paint = Paint().apply {
    isAntiAlias = true
    isDither = true
    color = android.graphics.Color.GRAY
}

@Composable
internal fun Chart(
    modifier: Modifier = Modifier,
    data: ChartData<*, *>,
    selectedCharts: List<Boolean>,
    leftBound: Float,
    rightBound: Float,
    labelSettings: LabelSettings? = null,
    chartStart: Float = 0f
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val xLabelsHeight = labelSettings?.run { (labelsSize + xLabelsTopPadding).toPx() } ?: 0f
    val chartHeight = remember(labelSettings, canvasSize) {
        canvasSize.height - xLabelsHeight - chartStrokeWidth
    }
    val pathLengths = selectedCharts.map {
        animateIntAsState(if (it) data.x.size else 1, animSpecYInt).value
    }

    val chartWidth = remember(canvasSize.width, chartStart) {
        (canvasSize.width - chartStart).coerceAtLeast(1f)
    }

    val sourcePaths = remember(data, canvasSize, chartHeight, pathLengths, chartWidth) {
        if (canvasSize == Size.Zero) null
        else data.getOffsets(chartWidth, chartHeight).mapIndexed { i, offsets ->
            val lastIndex = pathLengths[i]
            Path().apply {
                offsets.mapIndexed { i, offset ->
                    if (i == lastIndex) return@apply
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
    val paths =
        remember(
            sourcePaths,
            animTranslateX,
            animTranslateY,
            animScaleX,
            animScaleY,
            chartStart
        ) {
            matrix.reset()
            matrix.setTranslate(animTranslateX + chartStart, animTranslateY)
            matrix.preScale(animScaleX, animScaleY)
            sourcePaths?.map {
                Path().apply {
                    it.transform(matrix, this)
                }
            }
        }
    val pathAlphas =
        selectedCharts.map { animateIntAsState(if (it) 255 else 0, animSpecYInt).value }
    labelsPaint.textSize = labelSettings?.labelsSize?.toPx() ?: 0f
    val yLabels =
        getYLabels(labelSettings, transforms, chartHeight, animTranslateY, animScaleY, data)
    val xLabels = getXLabels(
        labelSettings,
        canvasSize,
        chartStart,
        chartWidth,
        animTranslateX,
        animScaleX,
        data
    )

    Canvas(modifier) {
        if (canvasSize == Size.Zero) {
            canvasSize = size
        } else {
            drawLines(yLabels, xLabels, labelSettings, chartStart)
            drawPaths(chartStart, chartHeight, paths, data.colorsArgb, pathAlphas)
            drawLabels(yLabels, xLabels, labelSettings, chartStart)
        }
    }
}

@Composable
private fun getYLabels(
    labelSettings: LabelSettings?,
    transforms: ChartData.Transforms?,
    chartHeight: Float,
    animTranslateY: Float,
    animScaleY: Float,
    data: ChartData<*, *>
): AnimatedLabels? {
    if (labelSettings == null || chartHeight <= 0f) return null
    val topMargin = 32.dp.toPx()
    val labelsCount = 5
    var oldTranslateY by remember { mutableFloatStateOf(transforms?.translateY ?: 0f) }
    var oldScaleY by remember { mutableFloatStateOf(transforms?.scaleY ?: 1f) }
    val coordsDisappear = remember(chartHeight, transforms?.translateY, transforms?.scaleY) {
        val coords = List(labelsCount + 1) {
            ((chartHeight - it * (chartHeight - topMargin) / labelsCount) - oldTranslateY) / oldScaleY
        }
        oldTranslateY = transforms?.translateY ?: 0f
        oldScaleY = transforms?.scaleY ?: 1f
        coords
    }
    var changeAlpha by remember { mutableStateOf(true) }
    val labelsAlpha by animateIntAsState(if (changeAlpha) 0 else 255, animSpecYInt)
    val paintAppear = remember(labelsAlpha) {
        Paint(labelsPaint).apply {
            alpha = if (changeAlpha) 255 - labelsAlpha else labelsAlpha
        }
    }
    val paintDisappear = remember(labelsAlpha) {
        Paint(labelsPaint).apply {
            alpha = if (changeAlpha) labelsAlpha else 255 - labelsAlpha
        }
    }
    val coordsAppear = remember(chartHeight, transforms?.translateY, transforms?.scaleY) {
        changeAlpha = !changeAlpha
        paintAppear.alpha = 0
        paintDisappear.alpha = 255
        List(labelsCount + 1) {
            ((chartHeight - it * (chartHeight - topMargin) / labelsCount) - (transforms?.translateY
                ?: 0f)) / (transforms?.scaleY ?: 1f)
        }
    }
    val labelsDisappear = remember(coordsDisappear, animTranslateY, animScaleY) {
        coordsDisappear.map { it * animScaleY + animTranslateY }.filter { it <= chartHeight }.map {
            AnimatedLabel(it, it + chartStrokeWidth, data.getYLabelByCoord(it) ?: "")
        }
    }
    val labelsAppear = remember(coordsAppear, animTranslateY, animScaleY) {
        coordsAppear.map { it * animScaleY + animTranslateY }.filter { it <= chartHeight }.map {
            AnimatedLabel(it, it + chartStrokeWidth, data.getYLabelByCoord(it) ?: "")
        }
    }
    return AnimatedLabels(labelsDisappear, labelsAppear, paintDisappear, paintAppear)
}

@Composable
private fun getXLabels(
    labelSettings: LabelSettings?,
    canvasSize: Size,
    chartStart: Float,
    chartWidth: Float,
    animTranslateX: Float,
    animScaleX: Float,
    data: ChartData<*, *>
): AnimatedLabels? {
    if (labelSettings == null || canvasSize == Size.Zero || chartWidth <= 1f) return null
    var labelsAppear: List<AnimatedLabel> by remember { mutableStateOf(emptyList()) }
    var labelsDisappear: List<AnimatedLabel> by remember { mutableStateOf(emptyList()) }
    var changeAlpha by remember { mutableStateOf(true) }
    val labelsAlpha by animateIntAsState(
        if (changeAlpha) 0 else 255, spring(stiffness = Spring.StiffnessVeryLow)
    )
    val paintAppear = remember(labelsAlpha) {
        Paint(labelsPaint).apply {
            alpha = if (changeAlpha) 255 - labelsAlpha else labelsAlpha
        }
    }
    val paintDisappear = remember(labelsAlpha) {
        Paint(labelsPaint).apply {
            alpha = if (changeAlpha) labelsAlpha else 255 - labelsAlpha
        }
    }
    labelsAppear =
        remember(labelSettings, canvasSize, chartWidth, chartStart, animTranslateX, animScaleX) {
            data.getXLabels(labelsPaint, chartWidth, labelSettings.xLabelsFormatter).map {
                val lineCoord = chartStart + it.coord * animScaleX + animTranslateX
                AnimatedLabel(lineCoord - it.offset, lineCoord, it.text)
            }.also {
                if (labelsAppear.isNotEmpty() && labelsAppear.size != it.size) {
                    paintAppear.alpha = 0
                    paintDisappear.alpha = 255
                    changeAlpha = !changeAlpha
                    labelsDisappear = labelsAppear
                }
            }
        }
    return AnimatedLabels(labelsDisappear, labelsAppear, paintDisappear, paintAppear)
}

private fun DrawScope.drawLines(
    yLabels: AnimatedLabels?,
    xLabels: AnimatedLabels?,
    labelSettings: LabelSettings?,
    chartStart: Float
) {
    if (yLabels != null) {
        drawHorizontalLines(
            yLabels.labelsDisappear,
            yLabels.paintDisappear,
            labelSettings!!,
            chartStart
        )
        drawHorizontalLines(yLabels.labelsAppear, yLabels.paintAppear, labelSettings, chartStart)
    }
    if (xLabels != null) {
        drawVerticalLines(
            xLabels.labelsDisappear,
            xLabels.paintDisappear,
            labelSettings!!,
            chartStart
        )
        drawVerticalLines(xLabels.labelsAppear, xLabels.paintAppear, labelSettings, chartStart)
    }
}

private fun DrawScope.drawHorizontalLines(
    labels: List<AnimatedLabel>, paint: Paint, labelSettings: LabelSettings, chartStart: Float
) = labels.forEach {
    val startX = if (chartStart > 0f) chartStart else labelSettings.yLabelsStartPadding.toPx()
    drawLine(
        Color.LightGray,
        Offset(startX, it.lineCoord),
        Offset(size.width, it.lineCoord),
        alpha = paint.alpha / 255f
    )
}

private fun DrawScope.drawVerticalLines(
    labels: List<AnimatedLabel>, paint: Paint, labelSettings: LabelSettings, chartStart: Float
) = labels.forEach {
    if (it.lineCoord >= chartStart - 1f && it.lineCoord <= size.width + 1f) {
        drawLine(
            Color.LightGray, Offset(it.lineCoord, 0f), Offset(
                it.lineCoord,
                size.height - labelSettings.labelsSize.toPx() - labelSettings.xLabelsTopPadding.toPx()
            ), alpha = paint.alpha / 255f
        )
    }
}

private fun DrawScope.drawPaths(
    chartStart: Float,
    chartHeight: Float,
    paths: List<Path>?,
    colors: List<Int>,
    alphas: List<Int>
) = withTransform({
    clipRect(
        left = chartStart,
        bottom = chartHeight + chartStrokeWidth
    )
    inset(0f, chartStrokeWidth / 2, 0f, chartStrokeWidth / 2)
}) {
    drawIntoCanvas {
        paths?.forEachIndexed { i, path ->
            it.nativeCanvas.drawPath(path, chartPaint.apply {
                color = colors[i]
                alpha = alphas[i]
            })
        }
    }
}

private fun DrawScope.drawLabels(
    yLabels: AnimatedLabels?,
    xLabels: AnimatedLabels?,
    labelSettings: LabelSettings?,
    chartStart: Float
) = drawIntoCanvas { c ->
    with(c.nativeCanvas) {
        yLabels?.labelsDisappear?.forEach {
            drawText(
                it.text,
                labelSettings!!.yLabelsStartPadding.toPx(),
                it.coord,
                yLabels.paintDisappear
            )
        }
        yLabels?.labelsAppear?.forEach {
            drawText(
                it.text,
                labelSettings!!.yLabelsStartPadding.toPx(),
                it.coord,
                yLabels.paintAppear
            )
        }
        if (chartStart > 0f) {
            withTransform({
                clipRect(left = chartStart, bottom = size.height + 100f)
            }) {
                xLabels?.labelsDisappear?.forEach {
                    drawText(it.text, it.coord, size.height, xLabels.paintDisappear)
                }
                xLabels?.labelsAppear?.forEach {
                    drawText(it.text, it.coord, size.height, xLabels.paintAppear)
                }
            }
        } else {
            xLabels?.labelsDisappear?.forEach {
                drawText(it.text, it.coord, size.height, xLabels.paintDisappear)
            }
            xLabels?.labelsAppear?.forEach {
                drawText(it.text, it.coord, size.height, xLabels.paintAppear)
            }
        }
    }
}

private class AnimatedLabels(
    val labelsDisappear: List<AnimatedLabel>,
    val labelsAppear: List<AnimatedLabel>,
    val paintDisappear: Paint,
    val paintAppear: Paint
)

private class AnimatedLabel(
    val coord: Float, val lineCoord: Float, val text: String
)

internal class LabelSettings(
    val labelsSize: Dp,
    val yLabelsStartPadding: Dp,
    val xLabelsTopPadding: Dp,
    val xLabelsFormatter: (xValue: Number) -> String
)
