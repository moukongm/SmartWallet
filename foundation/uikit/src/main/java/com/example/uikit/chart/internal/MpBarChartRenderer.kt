package com.example.uikit.chart.internal

import com.example.uikit.chart.config.BarChartConfig
import com.example.uikit.chart.model.BarChartModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry

internal object MpBarChartRenderer {

    fun render(
        chart: BarChart,
        model: BarChartModel,
        config: BarChartConfig,
        noDataText: CharSequence,
    ) {
        val dataSets = model.series.mapNotNull { series ->
            val entries = series.points
                .asSequence()
                .filter { it.x.isFinite() && it.y.isFinite() }
                .sortedBy { it.x }
                .map { BarEntry(it.x, it.y) }
                .toList()
            if (entries.isEmpty()) {
                null
            } else {
                BarDataSet(entries, series.name).apply {
                    color = series.color
                    setDrawValues(config.drawValues)
                    valueFormatter = MpValueFormatter(config.valueFormatter)
                    highLightColor = series.color
                }
            }
        }

        configureChart(chart, model, config, noDataText)
        if (dataSets.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        val data = BarData(dataSets)
        if (dataSets.size == 1) {
            data.barWidth = config.barWidth.coerceIn(0.05f, 1f)
        } else {
            val groupSpace = 0.2f
            val barSpace = 0.04f
            data.barWidth =
                ((1f - groupSpace) / dataSets.size - barSpace).coerceAtLeast(0.05f)
            data.groupBars(0f, groupSpace, barSpace)
        }
        chart.data = data
        chart.notifyDataSetChanged()
        if (config.animationDurationMs > 0) {
            chart.animateY(config.animationDurationMs)
        } else {
            chart.invalidate()
        }
    }

    private fun configureChart(
        chart: BarChart,
        model: BarChartModel,
        config: BarChartConfig,
        noDataText: CharSequence,
    ) {
        chart.description.isEnabled = false
        chart.legend.isEnabled = config.showLegend
        chart.setNoDataText(noDataText.toString())
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        chart.setFitBars(true)
        chart.setTouchEnabled(config.interactive)
        chart.isDragEnabled = config.interactive
        chart.setScaleEnabled(config.interactive)
        chart.isDoubleTapToZoomEnabled = config.interactive
        chart.setPinchZoom(config.interactive)

        val labels = model.xLabels.ifEmpty {
            model.series.firstOrNull()?.points?.map { it.label }.orEmpty()
        }
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawAxisLine(false)
            setDrawGridLines(false)
            textColor = config.axisTextColor
            granularity = 1f
            isGranularityEnabled = true
            valueFormatter = MpXAxisFormatter(labels, config.xAxisFormatter)
        }
        chart.axisLeft.apply {
            setDrawAxisLine(false)
            setDrawGridLines(config.showGrid)
            gridColor = config.gridColor
            textColor = config.axisTextColor
            config.yAxisMinimum?.let { axisMinimum = it } ?: resetAxisMinimum()
            config.yAxisMaximum?.let { axisMaximum = it } ?: resetAxisMaximum()
            valueFormatter = MpValueFormatter(config.yAxisFormatter)
        }
        chart.axisRight.isEnabled = false
    }
}
