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
    labelsPaint.textSize = labelSettings?.labelsSize?.toPx() ?: 0f
    val yLabels =
        getYLabels(labelSettings, transforms, chartHeight, animTranslateY, animScaleY, data)
    val xLabels = getXLabels(labelSettings, canvasSize, animTranslateX, animScaleX, data)

    Canvas(modifier) {
        if (canvasSize == Size.Zero) {
            canvasSize = size
        } else {
            drawLines(yLabels, labelSettings)

            drawPaths(chartHeight, paths, selectedColors)

            drawLabels(yLabels, xLabels, labelSettings)
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
    data: ChartData<Number, Number>
): Labels? {
    if (labelSettings == null) return null
    val topMargin = 32.dp.toPx()
    val labelsCount = 5
    var oldTranslateY by remember { mutableStateOf(transforms?.translateY ?: 0f) }
    var oldScaleY by remember { mutableStateOf(transforms?.scaleY ?: 1f) }
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
            it to data.getYValueByCoord(it).toString()
        }
    }
    val labelsAppear = remember(coordsAppear, animTranslateY, animScaleY) {
        coordsAppear.map { it * animScaleY + animTranslateY }.filter { it <= chartHeight }.map {
            it to data.getYValueByCoord(it).toString()
        }
    }
    return Labels(
        labelsDisappear,
        labelsAppear,
        paintDisappear,
        paintAppear
    )
}

@Composable
private fun getXLabels(
    labelSettings: LabelSettings?,
    canvasSize: Size,
    animTranslateX: Float,
    animScaleX: Float,
    data: ChartData<Number, Number>
): Labels? {
    if (labelSettings == null) return null
    var labelsAppear: List<Pair<Float, String>> by remember { mutableStateOf(emptyList()) }
    var labelsDisappear: List<Pair<Float, String>> by remember { mutableStateOf(emptyList()) }
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
    labelsAppear = remember(labelSettings, canvasSize, animTranslateX, animScaleX) {
        data.getXLabels(labelsPaint, canvasSize.width, labelSettings.xLabelsFormatter).map {
            it.coord * animScaleX + animTranslateX - it.offset to it.text
        }.also {
            if (labelsAppear.size != it.size && it.isNotEmpty()) {
                paintAppear.alpha = 0
                paintDisappear.alpha = 255
                changeAlpha = !changeAlpha
                labelsDisappear = labelsAppear
            }
        }
    }
    return Labels(labelsDisappear, labelsAppear, paintDisappear, paintAppear)
}

private fun DrawScope.drawLines(yLabels: Labels?, labelSettings: LabelSettings?) {
    yLabels?.labelsDisappear?.forEach { (coord, _) ->
        drawLine(
            Color.LightGray,
            Offset(labelSettings!!.yLabelsStartPadding.toPx(), coord + chartStrokeWidth),
            Offset(size.width, coord + chartStrokeWidth),
            alpha = yLabels.paintDisappear.alpha / 255f
        )
    }
    yLabels?.labelsAppear?.forEach { (coord, _) ->
        drawLine(
            Color.LightGray,
            Offset(labelSettings!!.yLabelsStartPadding.toPx(), coord + chartStrokeWidth),
            Offset(size.width, coord + chartStrokeWidth),
            alpha = yLabels.paintAppear.alpha / 255f
        )
    }
}

private fun DrawScope.drawPaths(
    chartHeight: Float,
    paths: List<Path>?,
    selectedColors: List<Int>
) = withTransform({
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

private fun DrawScope.drawLabels(
    yLabels: Labels?,
    xLabels: Labels?,
    labelSettings: LabelSettings?
) = drawIntoCanvas {
    with(it.nativeCanvas) {
        yLabels?.labelsDisappear?.forEach { (coord, text) ->
            drawText(
                text,
                labelSettings!!.yLabelsStartPadding.toPx(),
                coord,
                yLabels.paintDisappear
            )
        }
        yLabels?.labelsAppear?.forEach { (coord, text) ->
            drawText(
                text,
                labelSettings!!.yLabelsStartPadding.toPx(),
                coord,
                yLabels.paintAppear
            )
        }
        xLabels?.labelsDisappear?.forEach { (coord, text) ->
            drawText(text, coord, size.height, xLabels.paintDisappear)
        }
        xLabels?.labelsAppear?.forEach { (coord, text) ->
            drawText(text, coord, size.height, xLabels.paintAppear)
        }
    }
}

private class Labels(
    val labelsDisappear: List<Pair<Float, String>>,
    val labelsAppear: List<Pair<Float, String>>,
    val paintDisappear: Paint,
    val paintAppear: Paint
)

internal class LabelSettings(
    val labelsSize: Dp,
    val yLabelsStartPadding: Dp,
    val xLabelsTopPadding: Dp,
    val xLabelsFormatter: (xValue: Number) -> String
)
