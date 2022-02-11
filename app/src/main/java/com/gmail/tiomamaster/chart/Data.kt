package com.gmail.tiomamaster.chart

import androidx.compose.ui.graphics.Path
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
        val xBounded = calcXCoordinates(chartWidth, leftInd, rightInd)
        return yBounded.map { y ->
            Path().apply {
                xBounded.forEachIndexed { i, x ->
                    val yCoord = (maxY - y[i].toLong()) * kY + topYCoord
                    if (i == 0) moveTo(x, yCoord)
                    else lineTo(x, yCoord)
                }
            }
        }
    }

    private fun calcXCoordinates(width: Float, leftBound: Int, rightBound: Int): List<Float> {
        return x.subList(leftBound, rightBound).run {
            val xMin = first()
            val xMax = last()
            val k = width / (xMax.toLong() - xMin.toLong())
            map { x -> width - (xMax.toLong() - x.toLong()) * k }
        }
    }
}
