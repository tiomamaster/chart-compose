package com.gmail.tiomamaster.chart.view

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
