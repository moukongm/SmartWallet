package com.example.uikit.chart.config

import androidx.annotation.ColorInt
import com.example.uikit.chart.formatter.ChartValueFormatter

data class DonutChartConfig(
    val showLegend: Boolean = false,
    val interactive: Boolean = false,
    val animationDurationMs: Int = 0,
    val usePercentValues: Boolean = true,
    val drawValues: Boolean = true,
    val holeRadiusPercent: Float = 62f,
    val transparentCircleRadiusPercent: Float = 66f,
    val sliceSpaceDp: Float = 1f,
    val rotationAngle: Float = 270f,
    val centerTextSizeSp: Float = 24f,
    val centerSubTextSizeSp: Float = 13f,
    @ColorInt val centerTextColor: Int = ChartColors.TextPrimary,
    @ColorInt val centerSubTextColor: Int = ChartColors.TextSecondary,
    @ColorInt val valueTextColor: Int = ColorWhite,
    val valueTextSizeSp: Float = 12f,
    val valueFormatter: ChartValueFormatter? = null,
) {
    companion object {
        private const val ColorWhite: Int = -0x1
    }
}
