package com.gmail.tiomamaster.chart.compose

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

data class ChartData<X : Number, Y : Number>(
    private val x: List<X>,
    private val y: List<List<Y>>,
    val colors: List<Color>,
    val labels: List<String>
) {
    private var width = 0f
    private var height = 0f
    private lateinit var yBounded: List<List<Y>>
    private lateinit var xBounded: List<X>
    private var yMin = 0L
    private var yMax = 0L
    private var curYMax = 0L
    private var kY = 0f
    private var curKY = 0f

    private val Y.yCoord get() = (curYMax - toLong()) * curKY
    private val xMin get() = x.first().toLong()
    private val xMax get() = x.last().toLong()
    private val X.xCoord get() = width - (xMax - toLong()) * (width / (xMax - xMin))
    private val Float.indexOfCoord get() = (this * xBounded.lastIndex / width).roundToInt()

    private var scaleX = 1f
    private var translateX = 0f

    init {
        y.forEach {
            require(x.size == it.size) { "Each list in y should have tha same size as x list" }
        }
        require(colors.size == y.size) { "Colors list and y list should have the same size" }
        require(labels.size == y.size) { "Labels list and y list should have the same size" }
    }

    internal fun getOffsets(chartWidth: Float, chartHeight: Float): List<List<Offset>> {
        width = chartWidth
        height = chartHeight

        yMin = y.map { list -> list.minByOrNull { it.toLong() } }
            .minByOrNull { it!!.toLong() }!!.toLong()
        yMax = y.map { list -> list.maxByOrNull { it.toLong() } }
            .maxByOrNull { it!!.toLong() }!!.toLong()
        kY = chartHeight / (yMax - yMin)
        val xCoordinates = x.run {
            xBounded = this
            map { x -> x.xCoord }
        }

        return y.map {
            it.mapIndexed { index, y ->
                val yCoord = (yMax - y.toLong()) * kY
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

        val curYMin = yBounded.map { list -> list.minByOrNull { it.toLong() } }
            .minByOrNull { it!!.toLong() }!!.toLong()
        curYMax = yBounded.map { list -> list.maxByOrNull { it.toLong() } }
            .maxByOrNull { it!!.toLong() }!!.toLong()
        curKY = height / (curYMax - curYMin)

        scaleX = width / (rightBound - leftBound)
        val scaleY = curKY / kY
        translateX = -leftBound * scaleX
        val translateY = height - height * scaleY + (curYMin - yMin) * curKY

        return Transforms(scaleX, scaleY, translateX, translateY)
    }

    internal data class Transforms(
        val scaleX: Float,
        val scaleY: Float,
        val translateX: Float,
        val translateY: Float
    )

    internal fun getYValueByCoord(yCoord: Float): Long = (curYMax - yCoord / curKY).toLong()

    internal fun getDetailsForCoord(
        touchXCoord: Float,
        xDetailsFormatter: (xValue: Number) -> String
    ): DetailsData {
        var i = touchXCoord.indexOfCoord
        var xCoord = xBounded[i].xCoord * scaleX + translateX
        while (xCoord < 0 || xCoord > width) {
            i = when {
                xCoord < 0 -> i + 1
                xCoord > width -> i - 1
                else -> i
            }
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
            val offset = paint.calcLabelWidth(text, textRect).toFloat() / 2
            XLabel(coord, offset, text)
        }
    }

    internal data class XLabel(val coord: Float, val offset: Float, val text: String)

    private fun Paint.calcLabelWidth(text: String, rect: Rect): Int {
        getTextBounds(text, 0, text.lastIndex, rect)
        return rect.width()
    }
}

