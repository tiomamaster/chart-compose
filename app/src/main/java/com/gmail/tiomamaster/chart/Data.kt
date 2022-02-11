package com.gmail.tiomamaster.chart

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.graphics.Path
import java.text.SimpleDateFormat
import kotlin.math.roundToInt

data class Chart(
    val colors: Colors?,
    val columns: List<Any>?,
    val names: Names?,
    val types: Types?
)

data class Names(
    val y0: String?,
    val y1: String?,
    val y2: String?,
    val y3: String?
)

data class Colors(
    val y0: String?,
    val y1: String?,
    val y2: String?,
    val y3: String?
)

data class Types(
    val x: String?,
    val y0: String?,
    val y1: String?,
    val y2: String?,
    val y3: String?
)


class ChartData<X : Number, Y : Number>(private val x: List<X>, private val y: List<List<Y>>) {

    private val formatter = SimpleDateFormat("MMM dd")

    private lateinit var xBounded: List<X>

    fun calcPaths(
        topYCoord: Float,
        chartWidth: Float,
        chartHeight: Float,
        leftBound: Float,
        rightBound: Float
    ): List<Path> {
        val leftInd = (x.size * leftBound / chartWidth).toInt()
        val rightInd = (x.size * rightBound / chartWidth).roundToInt()
        val yBounded = y.map { it.subList(leftInd, rightInd) }
        val maxY =
            yBounded.map { list -> list.maxByOrNull { it.toLong() } }
                .maxByOrNull { it!!.toLong() }!!.toLong()
        val minY =
            yBounded.map { list -> list.minByOrNull { it.toLong() } }
                .minByOrNull { it!!.toLong() }!!.toLong()
        val kY = chartHeight / (maxY - minY)
        val xCoordinates = calcXCoordinates(chartWidth, leftInd, rightInd)
        return yBounded.map { y ->
            Path().apply {
                xCoordinates.forEachIndexed { i, x ->
                    val yCoord = (maxY - y[i].toLong()) * kY + topYCoord
                    if (i == 0) moveTo(x, yCoord)
                    else lineTo(x, yCoord)
                }
            }
        }
    }

    fun getXLabels(
        paint: Paint,
        width: Float
    ): List<Pair<Float, String>> {
        val sampleText = "Aug 17"
        val textRect = Rect()
        paint.getTextBounds(sampleText, 0, sampleText.lastIndex, textRect)
        val maxCountOfXLabels = (width / (textRect.width() * 2)).roundToInt()
        val ki = xBounded.lastIndex / width
        val labelMaxWidth = width / maxCountOfXLabels
        return List(maxCountOfXLabels) {
            val center =
                if (it == 0) labelMaxWidth / 2 else (it + 1) * labelMaxWidth - labelMaxWidth / 2
            val coord = center - (textRect.width() / 2)
            val i = center * ki
            coord to formatter.format(xBounded[i.toInt()])
        }
    }

    private fun calcXCoordinates(width: Float, leftBound: Int, rightBound: Int): List<Float> {
        return x.subList(leftBound, rightBound).also { xBounded = it }.run {
            val xMin = first()
            val xMax = last()
            val k = width / (xMax.toLong() - xMin.toLong())
            map { x -> width - (xMax.toLong() - x.toLong()) * k }
        }
    }
}
