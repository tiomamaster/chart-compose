package com.gmail.tiomamaster.chart.compose

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.roundToInt

class ChartData<X : Number, Y : Number>(private val x: List<X>, private val y: List<List<Y>>) {

    private lateinit var xBounded: List<X>

    fun calcPaths(
        topYCoord: Float,
        chartWidth: Float,
        chartHeight: Float,
        leftBound: Float,
        rightBound: Float
    ): List<Path> {
        val leftBoundInd = (x.size * leftBound / chartWidth).toInt()
        val rightBoundInd = (x.size * rightBound / chartWidth).roundToInt()
        val yBounded = y.map { it.subList(leftBoundInd, rightBoundInd) }
        val maxY = yBounded.map { list -> list.maxByOrNull { it.toLong() } }
            .maxByOrNull { it!!.toLong() }!!.toLong()
        val minY = yBounded.map { list -> list.minByOrNull { it.toLong() } }
            .minByOrNull { it!!.toLong() }!!.toLong()
        val kY = chartHeight / (maxY - minY)
        val xCoordinates = calcXCoordinates(chartWidth, leftBoundInd, rightBoundInd)
        return yBounded.map { y ->
            Path().apply {
                xCoordinates.forEachIndexed { i, xCoord ->
                    val yCoord = (maxY - y[i].toLong()) * kY + topYCoord
                    if (i == 0) moveTo(xCoord, yCoord)
                    else lineTo(xCoord, yCoord)
                }
            }
        }
    }

    fun getXLabels(
        paint: Paint,
        width: Float,
        formatter: (xValue: X) -> String
    ): List<Pair<Float, String>> {
        val sampleText = formatter(xBounded.first())
        val textRect = Rect()
        paint.getTextBounds(sampleText, 0, sampleText.lastIndex, textRect)
        val maxCountOfXLabels = (width / (textRect.width() * 2)).roundToInt()
        val labelMaxWidth = width / maxCountOfXLabels
        return List(maxCountOfXLabels) {
            val center =
                if (it == 0) labelMaxWidth / 2 else (it + 1) * labelMaxWidth - labelMaxWidth / 2
            val coord = center - (textRect.width() / 2)
            val i = center * xBounded.lastIndex / width
            coord to formatter(xBounded[i.toInt()])
        }
    }

    private fun calcXCoordinates(width: Float, leftBoundInd: Int, rightBoundInd: Int): List<Float> {
        return x.subList(leftBoundInd, rightBoundInd).also { xBounded = it }.run {
            val xMin = first()
            val xMax = last()
            val k = width / (xMax.toLong() - xMin.toLong())
            map { x -> width - (xMax.toLong() - x.toLong()) * k }
        }
    }
}
