package com.example.uikit.chart.internal

import android.graphics.Color
import com.example.uikit.chart.config.LineChartConfig
import com.example.uikit.chart.model.LineChartModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

internal object MpLineChartRenderer {

    fun render(
        chart: LineChart,
        model: LineChartModel,
        config: LineChartConfig,
        noDataText: CharSequence,
    ) {
        val dataSets = model.series.mapNotNull { series ->
            val entries = series.points
                .asSequence()
                .filter { it.x.isFinite() && it.y.isFinite() }
                .sortedBy { it.x }
                .map { Entry(it.x, it.y) }
                .toList()

            if (entries.isEmpty()) {
                null
            } else {
                LineDataSet(entries, series.name).apply {
                    color = series.color
                    lineWidth = config.lineWidthDp.coerceIn(0.2f, 10f)
                    mode = if (config.curvedLines) {
                        LineDataSet.Mode.CUBIC_BEZIER
                    } else {
                        LineDataSet.Mode.LINEAR
                    }
                    setDrawValues(config.drawValues)
                    setDrawCircles(config.drawCircles)
                    setCircleColor(series.color)
                    circleRadius = config.circleRadiusDp.coerceIn(1f, 20f)
                    circleHoleRadius = (circleRadius * 0.55f).coerceAtLeast(0.5f)
                    circleHoleColor = Color.WHITE
                    setDrawFilled(series.drawFilled)
                    fillColor = series.fillColor
                    fillAlpha = 35
                    highLightColor = series.color
                    setDrawHorizontalHighlightIndicator(false)
                    valueFormatter = MpValueFormatter(config.valueFormatter)
                }
            }
        }

        configureChart(chart, model, config, noDataText)
        if (dataSets.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        chart.data = LineData(dataSets)
        chart.notifyDataSetChanged()
        if (config.animationDurationMs > 0) {
            chart.animateX(config.animationDurationMs)
        } else {
            chart.invalidate()
        }
    }

    private fun configureChart(
        chart: LineChart,
        model: LineChartModel,
        config: LineChartConfig,
        noDataText: CharSequence,
    ) {
        chart.description.isEnabled = false
        chart.legend.isEnabled = config.showLegend
        chart.setNoDataText(noDataText.toString())
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
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
