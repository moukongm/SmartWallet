package com.example.uikit.chart.internal

import android.content.Context
import android.util.AttributeSet
import com.example.uikit.R

internal data class ChartViewAttributes(
    val noDataText: String,
    val interactive: Boolean,
    val showLegend: Boolean,
    val animationDurationMs: Int,
)

internal fun Context.readChartViewAttributes(
    attrs: AttributeSet?,
    defStyleAttr: Int,
): ChartViewAttributes {
    val values = obtainStyledAttributes(
        attrs,
        R.styleable.WalletChartView,
        defStyleAttr,
        0,
    )
    return try {
        ChartViewAttributes(
            noDataText = values.getString(
                R.styleable.WalletChartView_walletChartNoDataText,
            ) ?: getString(R.string.foundation_chart_no_data),
            interactive = values.getBoolean(
                R.styleable.WalletChartView_walletChartInteractive,
                false,
            ),
            showLegend = values.getBoolean(
                R.styleable.WalletChartView_walletChartShowLegend,
                false,
            ),
            animationDurationMs = values.getInt(
                R.styleable.WalletChartView_walletChartAnimationDuration,
                0,
            ).coerceAtLeast(0),
        )
    } finally {
        values.recycle()
    }
}
