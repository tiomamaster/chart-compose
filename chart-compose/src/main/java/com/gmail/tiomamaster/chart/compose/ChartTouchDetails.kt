@file:Suppress("FunctionName")

package com.gmail.tiomamaster.chart.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
internal fun ChartTouchDetails(
    height: Dp,
    visible: Boolean,
    offset: Int,
    outerViewFullWidth: Int,
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
    Box(
        Modifier
            .offset { IntOffset(animatedOffset, 0) }
            .height(height)) {
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

        var infoOffset by remember { mutableIntStateOf(0) }
        Column(
            Modifier
                .offset { IntOffset(infoOffset, 0) }
                .alpha(animatedAlpha)
                .onSizeChanged {
                    infoOffset =
                        (it.width / -2).coerceIn(-offset, outerViewFullWidth - it.width - offset)
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
                        style = TextStyle.Default.copy(fontSize = 10.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                    AnimatedContent(
                        targetState = yValues[i],
                        transitionSpec = {
                            if (targetState.toLong() > initialState.toLong()) {
                                (slideInVertically { h -> h } + fadeIn()) togetherWith
                                        (slideOutVertically { h -> -h } + fadeOut())
                            } else {
                                (slideInVertically { h -> -h } + fadeIn()) togetherWith
                                        (slideOutVertically { h -> h } + fadeOut())
                            }.using(
                                SizeTransform(clip = false)
                            )
                        },
                        label = "ChartDetailsAnimatedContent"
                    ) { value ->
                        BasicText(
                            value.toString(),
                            style = TextStyle.Default.copy(color = color, fontSize = 10.sp)
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
    100.dp,
    true,
    0,
    1080,
    "Aug 8",
    listOf(Color.Blue, Color.Red),
    listOf("1", "2"),
    listOf(1f, 2f),
    listOf(1121, 2224)
) {}
