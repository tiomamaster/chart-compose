package com.gmail.tiomamaster.chart

import androidx.compose.ui.graphics.Path

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


class ChartData<X : Number, Y : Number>(private val x: List<X>, private val y: List<Y>) {

    fun calcPath(
        topYCoordinate: Float,
        chartWidth: Float,
        chartHeight: Float,
        leftBound: Float,
        rightBound: Float
    ): Path =
        Path().apply {
            val newLeft = (x.size * leftBound / chartWidth).toInt()
            val newRight = (x.size * rightBound / chartWidth).toInt()
            val newY = y.subList(newLeft, newRight)
            val maxY = newY.maxByOrNull { it.toLong() }!!.toLong()
            val minY = newY.minByOrNull { it.toLong() }!!.toLong()
            val kY = chartHeight / (maxY - minY)
            calcXCoordinates(chartWidth, newLeft, newRight).forEachIndexed { i, x ->
                val yy = chartHeight - (maxY - newY[i].toLong()) * kY
                if (i == 0) {
                    moveTo(x, chartHeight - yy + topYCoordinate)
                } else {
                    lineTo(x, chartHeight - yy + topYCoordinate)
                }
            }
        }

    private fun calcXCoordinates(width: Float, leftBound: Int, rightBound: Int): List<Float> {
        return x.subList(leftBound, rightBound).run {
            val xMin = first()
            val xMax = last()
            val k = width / (xMax.toLong() - xMin.toLong())
            map { fl -> width - (xMax.toLong() - fl.toLong()) * k }
        }
    }
}
