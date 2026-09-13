package com.example.uikit.chart.config

import androidx.annotation.ColorInt
import com.example.uikit.chart.formatter.ChartAxisFormatter
import com.example.uikit.chart.formatter.ChartValueFormatter

data class BarChartConfig(
    val showLegend: Boolean = false,
    val showGrid: Boolean = true,
    val interactive: Boolean = false,
    val animationDurationMs: Int = 0,
    val drawValues: Boolean = false,
    val barWidth: Float = 0.65f,
    val yAxisMinimum: Float? = 0f,
    val yAxisMaximum: Float? = null,
    @ColorInt val gridColor: Int = ChartColors.Grid,
    @ColorInt val axisTextColor: Int = ChartColors.TextSecondary,
    val xAxisFormatter: ChartAxisFormatter? = null,
    val yAxisFormatter: ChartValueFormatter? = null,
    val valueFormatter: ChartValueFormatter? = null,
)
