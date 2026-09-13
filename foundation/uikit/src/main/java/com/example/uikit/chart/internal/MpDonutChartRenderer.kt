package com.example.uikit.chart.internal

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import com.example.uikit.chart.config.DonutChartConfig
import com.example.uikit.chart.model.DonutChartModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

internal object MpDonutChartRenderer {

    fun render(
        chart: PieChart,
        model: DonutChartModel,
        config: DonutChartConfig,
        noDataText: CharSequence,
    ) {
        val slices = model.slices.filter { it.value.isFinite() && it.value > 0f }
        configureChart(chart, model, config, noDataText)
        if (slices.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        val entries = slices.map { PieEntry(it.value, it.label) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = slices.map { it.color }
            sliceSpace = config.sliceSpaceDp.coerceAtLeast(0f)
            selectionShift = 0f
            setDrawValues(config.drawValues)
            valueTextColor = config.valueTextColor
            valueTextSize = config.valueTextSizeSp.coerceAtLeast(1f)
            valueFormatter = MpValueFormatter(
                formatter = config.valueFormatter,
                percentFallback = config.usePercentValues,
            )
        }

        chart.data = PieData(dataSet)
        chart.notifyDataSetChanged()
        if (config.animationDurationMs > 0) {
            chart.animateY(config.animationDurationMs)
        } else {
            chart.invalidate()
        }
    }

    private fun configureChart(
        chart: PieChart,
        model: DonutChartModel,
        config: DonutChartConfig,
        noDataText: CharSequence,
    ) {
        chart.description.isEnabled = false
        chart.legend.isEnabled = config.showLegend
        chart.setNoDataText(noDataText.toString())
        chart.setUsePercentValues(config.usePercentValues)
        chart.setDrawEntryLabels(false)
        chart.isDrawHoleEnabled = true
        chart.holeRadius = config.holeRadiusPercent.coerceIn(0f, 100f)
        chart.transparentCircleRadius =
            config.transparentCircleRadiusPercent.coerceIn(chart.holeRadius, 100f)
        chart.setHoleColor(Color.TRANSPARENT)
        chart.setTransparentCircleColor(Color.TRANSPARENT)
        chart.rotationAngle = config.rotationAngle
        chart.isRotationEnabled = config.interactive
        chart.setTouchEnabled(config.interactive)
        chart.isHighlightPerTapEnabled = config.interactive
        chart.setDrawCenterText(model.centerText.isNotEmpty() || model.centerSubText.isNotEmpty())
        chart.setCenterTextSize(config.centerTextSizeSp.coerceAtLeast(1f))
        chart.setCenterTextColor(config.centerTextColor)
        chart.centerText = buildCenterText(model, config)
    }

    private fun buildCenterText(
        model: DonutChartModel,
        config: DonutChartConfig,
    ): CharSequence {
        if (model.centerSubText.isEmpty()) {
            return model.centerText
        }
        val text = SpannableStringBuilder()
            .append(model.centerText)
            .append('\n')
            .append(model.centerSubText)
        val titleEnd = model.centerText.length
        text.setSpan(
            AbsoluteSizeSpan(config.centerTextSizeSp.toInt(), true),
            0,
            titleEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        text.setSpan(
            ForegroundColorSpan(config.centerTextColor),
            0,
            titleEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        text.setSpan(
            AbsoluteSizeSpan(config.centerSubTextSizeSp.toInt(), true),
            titleEnd + 1,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        text.setSpan(
            ForegroundColorSpan(config.centerSubTextColor),
            titleEnd + 1,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        return text
    }
}
