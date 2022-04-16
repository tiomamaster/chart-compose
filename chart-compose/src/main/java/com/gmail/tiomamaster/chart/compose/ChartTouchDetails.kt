@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

@OptIn(ExperimentalUnitApi::class, ExperimentalAnimationApi::class)
@Composable
internal fun ChartTouchDetails(
    visible: Boolean,
    offset: Int,
    chartWidth: Int,
    title: String,
    colors: List<Color>,
    labels: List<String>,
    yCoords: List<Float>,
    yValues: List<Number>,
    onDisappearFinished: () -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        if (visible) 1f else 0f,
        finishedListener = { if (it == 0f) onDisappearFinished() }
    )
    val animatedOffset by animateIntAsState(offset)
    Box(Modifier.offset { IntOffset(animatedOffset, 0) }) {
        val lineWidth = 1.dp
        val lineWidthPx = (lineWidth.toPx() / 2).roundToInt()
        Box(
            Modifier
                .offset { IntOffset(-lineWidthPx, 0) }
                .alpha(animatedAlpha)
                .fillMaxHeight()
                .width(lineWidth)
                .background(Color.LightGray)
        )

        val dotSize = 8.dp
        val halfDotSizePxNegative = (dotSize.toPx() / -2).toInt()
        val xOffset = halfDotSizePxNegative + lineWidthPx
        colors.forEachIndexed { i, color ->
            val yOffset = yCoords[i].roundToInt() + halfDotSizePxNegative
            Box(
                Modifier
                    .offset { IntOffset(xOffset, yOffset) }
                    .alpha(animatedAlpha)
                    .clip(CircleShape)
                    .border(2.dp, color, CircleShape)
                    .background(Color.White)
                    .size(dotSize)
            )
        }

        var infoOffset by remember { mutableStateOf(0) }
        Column(
            Modifier
                .offset { IntOffset(infoOffset, 0) }
                .alpha(animatedAlpha)
                .onSizeChanged {
                    infoOffset =
                        (it.width / -2).coerceIn(-offset, chartWidth - it.width - offset)
                }
                .background(Color.White)
                .border(Dp.Hairline, Color.Black, RoundedCornerShape(4.dp))
                .padding(8.dp)
                .animateContentSize(spring())
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
                    AnimatedContent(yValues[i], transitionSpec = {
                        if (targetState.toLong() > initialState.toLong()) {
                            slideInVertically { height -> height } + fadeIn() with
                                slideOutVertically { height -> -height } + fadeOut()
                        } else {
                            slideInVertically { height -> -height } + fadeIn() with
                                slideOutVertically { height -> height } + fadeOut()
                        }.using(
                            SizeTransform(clip = false)
                        )
                    }) {
                        BasicText(
                            it.toString(),
                            style = TextStyle.Default.copy(color, TextUnit(10f, TextUnitType.Sp))
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChartTouchDetailsPreview() = ChartTouchDetails(
    true,
    0,
    1080,
    "Aug 8",
    listOf(Color.Blue, Color.Red),
    listOf("1", "2"),
    listOf(1f, 2f),
    listOf(1121, 2224)
) {}
