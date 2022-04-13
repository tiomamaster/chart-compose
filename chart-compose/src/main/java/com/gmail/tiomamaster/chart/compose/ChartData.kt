package com.gmail.tiomamaster.chart.compose

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.roundToInt

data class ChartData<X : Number, Y : Number>(
    private val x: List<X>,
    private val y: List<List<Y>>,
    val colors: List<Color>,
    val labels: List<String>
) {
    private var width = 0f
    private var leftBoundInd: Int = 0
    private var rightBoundInd: Int = 0
    private lateinit var yBounded: List<List<Y>>
    private lateinit var xBounded: List<X>
    private var yMax = 0L
    private var kY = 0f
    private var xMax = 0L
    private var kX = 0f

    private val Y.yCoord get() = (yMax - toLong()) * kY
    private val X.xCoord get() = width - (xMax - toLong()) * kX
    private val Float.indexOfCoord get() = (this * xBounded.lastIndex / width).toInt()

    init {
        y.forEach {
            require(x.size == it.size) { "Each list in y should have tha same size as x list" }
        }
        require(colors.size == y.size) { "Colors list and y list should have the same size" }
        require(labels.size == y.size) { "Labels list and y list should have the same size" }
    }

    fun calcPaths(
        chartWidth: Float,
        chartHeight: Float,
        leftBound: Float,
        rightBound: Float
    ): List<Pair<Path, Color>> {
        width = chartWidth
        leftBoundInd = (x.size * leftBound / chartWidth).toInt()
        rightBoundInd = (x.size * rightBound / chartWidth).roundToInt()
        yBounded = y.map { it.subList(leftBoundInd, rightBoundInd) }
        val yMin = yBounded.map { list -> list.minByOrNull { it.toLong() } }
            .minByOrNull { it!!.toLong() }!!.toLong()
        yMax = yBounded.map { list -> list.maxByOrNull { it.toLong() } }
            .maxByOrNull { it!!.toLong() }!!.toLong()
        kY = chartHeight / (yMax - yMin)
        val xCoordinates = calcXCoordinates()
        return yBounded.mapIndexed { i, y ->
            Path().apply {
                xCoordinates.forEachIndexed { i, xCoord ->
                    val yCoord = y[i].yCoord
                    if (i == 0) moveTo(xCoord, yCoord)
                    else lineTo(xCoord, yCoord)
                }
            } to colors[i]
        }
    }

    fun getDetailsForCoord(
        touchXCoord: Float,
        xDetailsFormatter: (xValue: Number) -> String
    ): DetailsData = with(touchXCoord.indexOfCoord) {
        val yValues = yBounded.map { it[this] }
        val yCoords = yValues.map { it.yCoord }
        DetailsData(
            xDetailsFormatter(xBounded[this]),
            xBounded[this].xCoord,
            yCoords,
            yValues
        )
    }

    data class DetailsData(
        val title: String,
        val xCoord: Float,
        val yCoords: List<Float>,
        val yValues: List<Number>
    )

    fun getXLabels(
        paint: Paint,
        width: Float,
        formatter: (xValue: X) -> String
    ): List<Pair<Float, String>> {
        val maxCountOfXLabels = (width / (paint.textSize * 6)).roundToInt()
        val labelMaxWidth = width / maxCountOfXLabels
        val textRect = Rect()
        return List(maxCountOfXLabels) {
            val center =
                if (it == 0) labelMaxWidth / 2 else (it + 1) * labelMaxWidth - labelMaxWidth / 2
            val i = center.indexOfCoord
            val text = formatter(xBounded[i])
            val coord = center - (paint.calcLabelWidth(text, textRect) / 2)
            coord to text
        }
    }

    private fun Paint.calcLabelWidth(text: String, rect: Rect): Int {
        getTextBounds(text, 0, text.lastIndex, rect)
        return rect.width()
    }

    private fun calcXCoordinates(): List<Float> =
        x.subList(leftBoundInd, rightBoundInd).also { xBounded = it }.run {
            val xMin = first().toLong()
            xMax = last().toLong()
            kX = width / (xMax - xMin)
            map { x -> x.xCoord }
        }
}
