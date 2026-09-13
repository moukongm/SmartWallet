package com.example.uikit.chart.model

import androidx.annotation.ColorInt

data class ChartSeries(
    val name: String,
    val points: List<ChartPoint>,
    @ColorInt val color: Int,
    val drawFilled: Boolean = false,
    @ColorInt val fillColor: Int = color,
)
