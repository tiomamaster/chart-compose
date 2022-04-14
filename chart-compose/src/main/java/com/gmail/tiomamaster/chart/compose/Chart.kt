@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
internal fun Chart(
    modifier: Modifier = Modifier,
    data: ChartData<Number, Number>,
    leftBound: Float,
    rightBound: Float
) = Canvas(modifier) {
    data.calcPaths(size.width, size.height, leftBound, rightBound).forEach { (path, color) ->
        drawPath(
            path = path,
            color = color,
            style = Stroke(5f)
        )
    }
}
