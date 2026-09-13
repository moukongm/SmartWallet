package com.example.uikit.chart.formatter

fun interface ChartValueFormatter {
    fun format(value: Float): String
}

fun interface ChartAxisFormatter {
    fun format(value: Float, suggestedLabel: String): String
}
