package com.example.uikit.chart.model

data class LineChartModel(
    val series: List<ChartSeries>,
    val xLabels: List<String> = emptyList(),
)
