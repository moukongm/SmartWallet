package com.example.uikit.chart.model

data class BarChartModel(
    val series: List<ChartSeries>,
    val xLabels: List<String> = emptyList(),
)
