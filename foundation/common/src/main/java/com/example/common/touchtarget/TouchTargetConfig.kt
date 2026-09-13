package com.example.common.touchtarget

import android.graphics.Color

data class TouchTargetConfig(
    val enabled: Boolean = true,
    val minWidthDp: Float = 48f,
    val minHeightDp: Float = 48f,
    val includeLongClickable: Boolean = true,
    val includeDisabledViews: Boolean = false,
    val showSizeLabel: Boolean = true,
    val borderColor: Int = Color.RED,
    val borderWidthDp: Float = 2f,
    val labelTextSizeSp: Float = 11f,
    val refreshDelayMillis: Long = 150L
)
