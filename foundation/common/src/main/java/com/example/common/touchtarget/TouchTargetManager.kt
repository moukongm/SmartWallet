package com.example.common.touchtarget

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout

class TouchTargetManager(
    private val activity: Activity,
    private val config: TouchTargetConfig
) {

    private val handler = Handler(Looper.getMainLooper())
    private val detector = TouchTargetDetector(config)
    private var overlayView: TouchTargetOverlayView? = null
    private var started = false

    private val decorView: ViewGroup
        get() = activity.window.decorView as ViewGroup

    private val contentView: View?
        get() = activity.findViewById(android.R.id.content)

    private val refreshRunnable = Runnable { inspectNow() }
    private val globalLayoutListener =
        ViewTreeObserver.OnGlobalLayoutListener { scheduleRefresh() }
    private val scrollChangedListener =
        ViewTreeObserver.OnScrollChangedListener { scheduleRefresh() }

    fun start() {
        if (started || !config.enabled) return
        started = true

        val overlay = TouchTargetOverlayView(activity, config)
        overlayView = overlay
        decorView.addView(
            overlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        addViewTreeListeners()
        scheduleRefresh()
    }

    fun refresh() {
        if (started) scheduleRefresh()
    }

    fun stop() {
        if (!started) return
        started = false
        handler.removeCallbacks(refreshRunnable)
        removeViewTreeListeners()
        overlayView?.let { overlay ->
            overlay.clearIssues()
            if (overlay.parent === decorView) {
                decorView.removeView(overlay)
            }
        }
        overlayView = null
    }

    private fun scheduleRefresh() {
        if (!started) return
        handler.removeCallbacks(refreshRunnable)
        handler.postDelayed(refreshRunnable, config.refreshDelayMillis)
    }

    private fun inspectNow() {
        if (!started) return
        val root = contentView ?: return
        val overlay = overlayView ?: return

        if (root.width <= 0 || root.height <= 0 ||
            overlay.width <= 0 || overlay.height <= 0
        ) {
            overlay.post { scheduleRefresh() }
            return
        }

        overlay.updateIssues(
            detector.detect(
                root = root,
                coordinateView = overlay
            )
        )
        overlay.bringToFront()
    }

    private fun addViewTreeListeners() {
        decorView.viewTreeObserver.takeIf { it.isAlive }?.run {
            addOnGlobalLayoutListener(globalLayoutListener)
            addOnScrollChangedListener(scrollChangedListener)
        }
    }

    private fun removeViewTreeListeners() {
        decorView.viewTreeObserver.takeIf { it.isAlive }?.run {
            removeOnGlobalLayoutListener(globalLayoutListener)
            removeOnScrollChangedListener(scrollChangedListener)
        }
    }
}
