@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun ChartSelectors(
    data: ChartData<Number, Number>,
    selected: SnapshotStateList<Boolean>
) = ChipVerticalGrid(Modifier.padding(start = 16.dp, end = 16.dp), 8.dp) {
    data.labels.forEachIndexed { i, label ->
        val color = if (selected[i]) data.colors[i] else Color.LightGray
        BasicText(
            label,
            modifier = Modifier
                .background(color = color, shape = CircleShape)
                .clip(CircleShape)
                .clickable {
                    val new = !selected[i]
                    val selectedCount = selected.count { it }
                    if (!new && selectedCount == 1) return@clickable
                    selected[i] = new
                }
                .padding(8.dp)
        )
    }
}

@Composable
fun ChipVerticalGrid(
    modifier: Modifier = Modifier,
    spacing: Dp,
    content: @Composable () -> Unit
) = Layout(content, modifier) { measurables, constraints ->
    var currentRow = 0
    var currentOrigin = IntOffset.Zero
    val spacingValue = spacing.toPx().toInt()
    val placeables = measurables.map { measurable ->
        val placeable = measurable.measure(constraints)

        if (currentOrigin.x > 0f && currentOrigin.x + placeable.width > constraints.maxWidth) {
            currentRow += 1
            currentOrigin =
                currentOrigin.copy(x = 0, y = currentOrigin.y + placeable.height + spacingValue)
        }

        placeable to currentOrigin.also {
            currentOrigin = it.copy(x = it.x + placeable.width + spacingValue)
        }
    }

    layout(
        width = constraints.maxWidth,
        height = placeables.lastOrNull()?.run { first.height + second.y } ?: 0
    ) {
        placeables.forEach {
            val (placeable, origin) = it
            placeable.place(origin.x, origin.y)
        }
    }
}
