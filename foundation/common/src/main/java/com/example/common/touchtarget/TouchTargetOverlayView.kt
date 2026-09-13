package com.example.common.touchtarget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

class TouchTargetOverlayView(
    context: Context,
    private val config: TouchTargetConfig
) : View(context) {

    private var issues: List<TouchTargetIssue> = emptyList()

    private val density = resources.displayMetrics.density

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.borderColor
        style = Paint.Style.STROKE
        strokeWidth = config.borderWidthDp * density
    }

    private val labelBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.borderColor
        style = Paint.Style.FILL
    }

    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            config.labelTextSizeSp,
            resources.displayMetrics
        )
    }

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setWillNotDraw(false)
    }

    fun updateIssues(newIssues: List<TouchTargetIssue>) {
        issues = newIssues
        invalidate()
    }

    fun clearIssues() {
        issues = emptyList()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        issues.forEach { issue ->
            drawIssue(canvas, issue)
        }
    }

    private fun drawIssue(canvas: Canvas, issue: TouchTargetIssue) {
        val halfBorder = borderPaint.strokeWidth / 2f
        val borderBounds = RectF(issue.bounds).apply {
            inset(halfBorder, halfBorder)
        }
        canvas.drawRect(borderBounds, borderPaint)

        if (config.showSizeLabel) {
            drawLabel(canvas, issue)
        }
    }

    private fun drawLabel(canvas: Canvas, issue: TouchTargetIssue) {
        val text = buildString {
            append(issue.viewName)
            append(" ")
            append(issue.widthDp.roundToInt())
            append("×")
            append(issue.heightDp.roundToInt())
            append("dp")
        }

        val horizontalPadding = 5f * density
        val verticalPadding = 3f * density
        val textWidth = labelTextPaint.measureText(text)
        val fontMetrics = labelTextPaint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top
        val labelWidth = textWidth + horizontalPadding * 2
        val labelHeight = textHeight + verticalPadding * 2

        val left = issue.bounds.left.coerceIn(
            0f,
            (width - labelWidth).coerceAtLeast(0f)
        )
        var top = issue.bounds.top - labelHeight
        if (top < 0f) {
            top = issue.bounds.top
        }
        top = top.coerceAtMost((height - labelHeight).coerceAtLeast(0f))

        val backgroundBounds = RectF(
            left,
            top,
            left + labelWidth,
            top + labelHeight
        )
        canvas.drawRect(backgroundBounds, labelBackgroundPaint)

        val textBaseline = top + verticalPadding - fontMetrics.top
        canvas.drawText(
            text,
            left + horizontalPadding,
            textBaseline,
            labelTextPaint
        )
    }
}
