package com.example.uikit.chart.internal

import com.example.uikit.chart.formatter.ChartAxisFormatter
import com.example.uikit.chart.formatter.ChartValueFormatter
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlin.math.roundToInt

internal class MpXAxisFormatter(
    private val labels: List<String>,
    private val formatter: ChartAxisFormatter?,
) : ValueFormatter() {

    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        val index = value.roundToInt()
        val suggestedLabel = labels.getOrNull(index).orEmpty()
        return formatter?.format(value, suggestedLabel) ?: suggestedLabel
    }
}

internal class MpValueFormatter(
    private val formatter: ChartValueFormatter?,
    private val percentFallback: Boolean = false,
) : ValueFormatter() {

    override fun getFormattedValue(value: Float): String {
        return formatter?.format(value)
            ?: if (percentFallback) "${value.roundToInt()}%" else value.toString()
    }
}
