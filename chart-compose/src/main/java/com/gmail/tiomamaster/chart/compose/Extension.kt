package com.gmail.tiomamaster.chart.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun Dp.toPx() = with(LocalDensity.current) { this@toPx.toPx() }

@Composable
fun Float.toDp() = with(LocalDensity.current) { this@toDp.toDp() }

fun <T> List<T>.selected(selectedCharts: List<Boolean>) =
    selectedCharts.mapIndexedNotNull { i, isSelected -> if (isSelected) this[i] else null }
