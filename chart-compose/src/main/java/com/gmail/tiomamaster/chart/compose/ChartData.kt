package com.gmail.tiomamaster.chart.compose

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

data class ChartData<X, Y>(
    val x: List<X>,
    val y: List<List<Y>>,
    val colors: List<Color>,
    val labels: List<String>
) where X : Number, X : Comparable<X>, Y : Number, Y : Comparable<Y> {
    private var width = 0f
    private var height = 0f
    private lateinit var yBounded: List<List<Y>>
    private lateinit var xBounded: List<X>
    private lateinit var yMin: Y
    private lateinit var yMax: Y
    private lateinit var curYMax: Y
    private var kY = 0f
    private var curKY = 0f

    private val Y.yCoord get() = (curYMax - this).toFloat() * curKY
    private val xMin get() = x.first()
    private val xMax get() = x.last()
    private val X.xCoord get() = width - (xMax - this).toFloat() * (width / (xMax - xMin).toFloat())
    private val Float.indexOfCoord get() = (this * xBounded.lastIndex / width).roundToInt()

    private var scaleX = 1f
    private var translateX = 0f

    val colorsArgb: List<Int>

    init {
        y.forEach {
            require(x.size == it.size) { "Each list in y should have tha same size as x list" }
        }
        require(colors.size == y.size) { "Colors list and y list should have the same size" }
        require(labels.size == y.size) { "Labels list and y list should have the same size" }

        colorsArgb = colors.map { it.toArgb() }
    }

    internal fun getOffsets(chartWidth: Float, chartHeight: Float): List<List<Offset>> {
        width = chartWidth
        height = chartHeight

        yMin = y.minOf { it.min() }
        yMax = y.maxOf { it.max() }
        kY = chartHeight / (yMax.toFloat() - yMin.toFloat())
        val xCoordinates = x.run {
            xBounded = this
            map { x -> x.xCoord }
        }

        return y.map {
            it.mapIndexed { index, y ->
                val yCoord = (yMax - y).toFloat() * kY
                Offset(xCoordinates[index], yCoord)
            }
        }
    }

    internal fun getTransforms(
        selectedCharts: List<Boolean>,
        leftBound: Float,
        rightBound: Float
    ): Transforms {
        val leftBoundInd = (x.lastIndex * leftBound / width).toInt()
        val rightBoundInd = (x.lastIndex * rightBound / width).roundToInt()

        yBounded = y.selected(selectedCharts).map { it.subList(leftBoundInd, rightBoundInd + 1) }
        xBounded = x.subList(leftBoundInd, rightBoundInd + 1)

        val curYMin = yBounded.minOf { it.min() }
        curYMax = yBounded.maxOf { it.max() }
        curKY = height / (curYMax - curYMin).toFloat()

        scaleX = width / (rightBound - leftBound)
        val scaleY = curKY / kY
        translateX = -leftBound * scaleX
        val translateY = height - height * scaleY + (curYMin - yMin).toFloat() * curKY

        return Transforms(scaleX, scaleY, translateX, translateY)
    }

    internal data class Transforms(
        val scaleX: Float,
        val scaleY: Float,
        val translateX: Float,
        val translateY: Float
    )

    internal fun getYLabelByCoord(yCoord: Float): String? {
        if (!::curYMax.isInitialized) return null
        return (curYMax - yCoord / curKY).toString()
    }

    internal fun getDetailsForCoord(
        touchXCoord: Float,
        xDetailsFormatter: (xValue: Number) -> String
    ): DetailsData {
        var i = touchXCoord.indexOfCoord
        var xCoord = xBounded[i].xCoord * scaleX + translateX
        while (xCoord < 0 || xCoord > width) {
            i = if (xCoord < 0) i + 1
            else i - 1
            xCoord = xBounded[i].xCoord * scaleX + translateX
        }
        val yValues = yBounded.map { it[i] }
        val yCoords = yValues.map { it.yCoord }
        return DetailsData(
            xDetailsFormatter(xBounded[i]),
            xCoord,
            yCoords,
            yValues
        )
    }

    internal data class DetailsData(
        val title: String,
        val xCoord: Float,
        val yCoords: List<Float>,
        val yValues: List<Number>
    )

    internal fun getXLabels(
        paint: Paint,
        width: Float,
        formatter: (xValue: X) -> String
    ): List<XLabel> {
        val xLabelsStartCount = (width / (paint.textSize * 6)).roundToInt()
        val xLabelsCurCount = xLabelsStartCount * scaleX.roundToInt()
        val labelMaxWidth = width / xLabelsCurCount
        val textRect = Rect()
        return List(xLabelsCurCount) {
            val coord =
                if (it == 0) labelMaxWidth / 2 else (it + 1) * labelMaxWidth - labelMaxWidth / 2
            val i = (coord * x.lastIndex / width).roundToInt()
            val text = formatter(x[i])
            val offset = paint.calcLabelOffset(text, textRect)
            XLabel(coord, offset, text)
        }
    }

    internal data class XLabel(val coord: Float, val offset: Float, val text: String)

    private fun Paint.calcLabelOffset(text: String, rect: Rect): Float {
        getTextBounds(text, 0, text.lastIndex, rect)
        return rect.width().toFloat() / 2
    }

    private operator fun Number.minus(other: Number): Number {
        return when (this) {
            is Float -> this.toFloat() - other.toFloat()
            is Double -> this.toDouble() - other.toDouble()
            is Int -> this.toInt() - other.toInt()
            is Long -> this.toLong() - other.toLong()
            else -> throw UnsupportedOperationException(
                "${this::class} is not supported. Only Float, Double, Int and Long are supported " +
                        "for ChartData.x and ChartData.y values"
            )
        }
    }

    private operator fun Number.plus(other: Number): Number {
        return when (this) {
            is Float -> this.toFloat() + other.toFloat()
            is Double -> this.toDouble() + other.toDouble()
            is Int -> this.toInt() + other.toInt()
            is Long -> this.toLong() + other.toLong()
            else -> throw UnsupportedOperationException(
                "${this::class} is not supported. Only Float, Double, Int and Long are supported " +
                        "for ChartData.x and ChartData.y values"
            )
        }
    }

    private operator fun Number.div(other: Number): Number {
        return when (this) {
            is Float -> this.toFloat() / other.toFloat()
            is Double -> this.toDouble() / other.toDouble()
            is Int -> this.toInt() / other.toInt()
            is Long -> this.toLong() / other.toLong()
            else -> throw UnsupportedOperationException(
                "${this::class} is not supported. Only Float, Double, Int and Long are supported " +
                        "for ChartData.x and ChartData.y values"
            )
        }
    }

    private operator fun Number.times(other: Number): Number {
        return when (this) {
            is Float -> this.toFloat() * other.toFloat()
            is Double -> this.toDouble() * other.toDouble()
            is Int -> this.toInt() * other.toInt()
            is Long -> this.toLong() * other.toLong()
            else -> throw UnsupportedOperationException(
                "${this::class} is not supported. Only Float, Double, Int and Long are supported " +
                        "for ChartData.x and ChartData.y values"
            )
        }
    }
}
