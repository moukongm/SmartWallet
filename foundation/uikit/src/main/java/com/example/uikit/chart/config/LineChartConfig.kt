package com.example.uikit.chart.config

import androidx.annotation.ColorInt
import com.example.uikit.chart.formatter.ChartAxisFormatter
import com.example.uikit.chart.formatter.ChartValueFormatter

data class LineChartConfig(
    val showLegend: Boolean = false,
    val showGrid: Boolean = true,
    val interactive: Boolean = false,
    val animationDurationMs: Int = 0,
    val curvedLines: Boolean = false,
    val drawValues: Boolean = false,
    val drawCircles: Boolean = true,
    val lineWidthDp: Float = 2f,
    val circleRadiusDp: Float = 4f,
    val yAxisMinimum: Float? = null,
    val yAxisMaximum: Float? = null,
    @ColorInt val gridColor: Int = ChartColors.Grid,
    @ColorInt val axisTextColor: Int = ChartColors.TextSecondary,
    val xAxisFormatter: ChartAxisFormatter? = null,
    val yAxisFormatter: ChartValueFormatter? = null,
    val valueFormatter: ChartValueFormatter? = null,
)
