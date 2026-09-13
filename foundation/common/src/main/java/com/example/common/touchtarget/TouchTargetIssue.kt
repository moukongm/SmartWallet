package com.example.common.touchtarget

import android.graphics.RectF
import android.view.View

data class TouchTargetIssue(
    val view: View,
    val bounds: RectF,
    val widthDp: Float,
    val heightDp: Float,
    val viewName: String
)
