package com.example.uikit.chart.view

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.uikit.chart.config.LineChartConfig
import com.example.uikit.chart.internal.MpLineChartRenderer
import com.example.uikit.chart.internal.readChartViewAttributes
import com.example.uikit.chart.model.LineChartModel
import com.github.mikephil.charting.charts.LineChart

class WalletLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val chart = LineChart(context)
    private val xmlAttributes = context.readChartViewAttributes(attrs, defStyleAttr)

    var configuration: LineChartConfig = LineChartConfig(
        showLegend = xmlAttributes.showLegend,
        interactive = xmlAttributes.interactive,
        animationDurationMs = xmlAttributes.animationDurationMs,
    )

    var noDataText: CharSequence = xmlAttributes.noDataText
        set(value) {
            field = value
            chart.setNoDataText(value.toString())
        }

    init {
        addView(
            chart,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        chart.setNoDataText(noDataText.toString())
    }

    fun render(model: LineChartModel) {
        MpLineChartRenderer.render(chart, model, configuration, noDataText)
    }

    fun render(model: LineChartModel, config: LineChartConfig) {
        configuration = config
        render(model)
    }

    fun clear() {
        chart.clear()
        chart.invalidate()
    }
}
