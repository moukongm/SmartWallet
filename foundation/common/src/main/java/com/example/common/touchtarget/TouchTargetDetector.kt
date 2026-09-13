package com.example.common.touchtarget

import android.view.View
import android.view.ViewGroup
import com.example.common.R

class TouchTargetDetector(
    private val config: TouchTargetConfig
) {
    fun detect(
        root: View,
        coordinateView: View
    ): List<TouchTargetIssue> {
        if (root.width <= 0 || root.height <= 0) {
            return emptyList()
        }

        val issues = mutableListOf<TouchTargetIssue>()
        val coordinateLocation = IntArray(2)
        coordinateView.getLocationOnScreen(coordinateLocation)
        collectIssues(
            view = root,
            coordinateLocation = coordinateLocation,
            issues = issues
        )
        return issues
    }

    private fun collectIssues(
        view: View,
        coordinateLocation: IntArray,
        issues: MutableList<TouchTargetIssue>
    ) {
        if (!shouldInspect(view)) {
            return
        }

        if (isInteractive(view)) {
            createIssue(
                view = view,
                coordinateLocation = coordinateLocation
            )?.let(issues::add)
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                collectIssues(
                    view = view.getChildAt(index),
                    coordinateLocation = coordinateLocation,
                    issues = issues
                )
            }
        }
    }

    private fun shouldInspect(view: View): Boolean {
        if (view.visibility != View.VISIBLE) {
            return false
        }
        if (!view.isShown) {
            return false
        }
        if (view.alpha <= 0f) {
            return false
        }
        if (view.width <= 0 || view.height <= 0) {
            return false
        }
        if (!config.includeDisabledViews && !view.isEnabled) {
            return false
        }
        if (view.getTag(R.id.touch_target_check_ignore) == true) {
            return false
        }
        if (view.tag == IGNORE_TAG) {
            return false
        }
        return true
    }

    private fun isInteractive(view: View): Boolean {
        return view.isClickable ||
                view.hasOnClickListeners() ||
                (config.includeLongClickable && view.isLongClickable)
    }

    private fun createIssue(
        view: View,
        coordinateLocation: IntArray
    ): TouchTargetIssue? {
        val density = view.resources.displayMetrics.density
        val widthDp = view.width / density
        val heightDp = view.height / density
        val widthTooSmall = widthDp < config.minWidthDp
        val heightTooSmall = heightDp < config.minHeightDp
        if (!widthTooSmall && !heightTooSmall) {
            return null
        }

        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)

        val left = (viewLocation[0] - coordinateLocation[0]).toFloat()
        val top = (viewLocation[1] - coordinateLocation[1]).toFloat()
        return TouchTargetIssue(
            view = view,
            bounds = android.graphics.RectF(
                left,
                top,
                left + view.width,
                top + view.height
            ),
            widthDp = widthDp,
            heightDp = heightDp,
            viewName = resolveViewName(view)
        )
    }

    private fun resolveViewName(view: View): String {
        if (view.id == View.NO_ID) {
            return view.javaClass.simpleName
        }
        return runCatching {
            view.resources.getResourceEntryName(view.id)
        }.getOrDefault(view.javaClass.simpleName)
    }

    companion object {
        private const val IGNORE_TAG = "touch_target_ignore"
    }
}
