package com.example.uikit.chart.model

import androidx.annotation.ColorInt

data class DonutSlice(
    val label: String,
    val value: Float,
    @ColorInt val color: Int,
)

data class DonutChartModel(
    val slices: List<DonutSlice>,
    val centerText: String = "",
    val centerSubText: String = "",
)
