@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

private var xLabelsPaint: Paint? = null

private fun getXLabelsPaint(txtSize: Float): Paint {
    if (xLabelsPaint == null) xLabelsPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 0f
        textSize = txtSize
        color = android.graphics.Color.GRAY
    }
    return xLabelsPaint!!
}

@Composable
internal fun XLabels(
    data: ChartData<Number, Number>,
    modifier: Modifier = Modifier,
    formatter: (xValue: Number) -> String
) = Canvas(modifier) {
    val paint = getXLabelsPaint(size.height)
    data.getXLabels(paint, size.width, formatter).forEach { (coord, text) ->
        drawIntoCanvas {
            it.nativeCanvas.drawText(text, coord, size.height, paint)
        }
    }
}
