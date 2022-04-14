@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

@OptIn(ExperimentalUnitApi::class)
@Composable
internal fun ChartTouchDetails(
    modifier: Modifier,
    title: String,
    colors: List<Color>,
    labels: List<String>,
    yCoords: List<Float>,
    yValues: List<Number>
) = Box(modifier) {
    val lineWidth = 1.dp
    Box(
        Modifier
            .fillMaxHeight()
            .width(lineWidth)
            .background(Color.LightGray)
    )

    val dotSize = 8.dp
    val halfDotSizePxNegative = (dotSize.toPx() / -2).toInt()
    val xOffset = halfDotSizePxNegative + (lineWidth.toPx() / 2).toInt()
    colors.forEachIndexed { i, color ->
        val yOffset = yCoords[i].roundToInt() + halfDotSizePxNegative
        Box(
            Modifier
                .offset { IntOffset(xOffset, yOffset) }
                .clip(CircleShape)
                .border(1.dp, color, CircleShape)
                .background(Color.White)
                .size(dotSize)
        )
    }

    var offset by remember { mutableStateOf(0) }
    Column(
        Modifier
            .offset { IntOffset(offset, 0) }
            .onSizeChanged { offset = it.width / -2 }
            .background(Color.White)
            .border(Dp.Hairline, Color.Black, RoundedCornerShape(4.dp))
            .padding(8.dp)
            .width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        BasicText(title)
        colors.forEachIndexed { i, color ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BasicText(
                    labels[i],
                    style = TextStyle.Default.copy(
                        fontSize = TextUnit(
                            10f,
                            TextUnitType.Sp
                        )
                    )
                )
                Spacer(Modifier.width(8.dp))
                BasicText(
                    yValues[i].toString(),
                    style = TextStyle.Default.copy(color, TextUnit(10f, TextUnitType.Sp))
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChartTouchDetailsPreview() = ChartTouchDetails(
    Modifier,
    "Aug 8",
    listOf(Color.Blue, Color.Red),
    listOf("1", "2"),
    listOf(1f, 2f),
    listOf(1121, 2224)
)
