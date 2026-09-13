package com.example.common.preload

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.asynclayoutinflater.appcompat.AsyncAppCompatFactory
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import java.lang.ref.WeakReference

class AsyncLayoutPreLoader(
    activity: Activity
) {
    private val activityReference =
        WeakReference(activity)

    private val mainHandler = Handler(Looper.getMainLooper())

    private var asyncInflater:
            AsyncLayoutInflater? = AsyncLayoutInflater(
                activity,
        AsyncAppCompatFactory()
            )

    private val cachedViews = mutableMapOf<Int, View>()

    private val runningRequests = mutableMapOf<Int, Long>()

    private var requestSequence = 0L

    private var closed = false

    fun preload(
        @LayoutRes layoutRes: Int
    ) {
        if (
            Looper.myLooper() != Looper.getMainLooper()
        ) {
            mainHandler.post {
                preload(layoutRes)
            }
            return
        }

        if (closed) return
        val activity = activityReference.get() ?: return
        if (
            activity.isFinishing || activity.isDestroyed
        ) {
            return
        }

        if ( cachedViews.containsKey(layoutRes)) {
            return
        }

        if (runningRequests.containsKey(layoutRes)) {
            return
        }

        val inflater = asyncInflater ?: return

        val requestId = ++requestSequence

        runningRequests[layoutRes] = requestId

        inflater.inflate(
            layoutRes,
            null
        ) {
            view, _, _ ->
            handleInflateFinished(
                layoutRes = layoutRes,
                requestId = requestId,
                view = view
            )
        }
    }

    @MainThread
    fun take(
        @LayoutRes layoutRes: Int
    ): View? {
        checkMainThread()

        if (closed) return null

        runningRequests.remove(layoutRes)

        val cachedView = cachedViews.remove(layoutRes) ?: return null
        return cachedView.takeIf {
            it.parent == null
        }
    }

    @MainThread
    fun isReady(
        @LayoutRes layoutRes: Int
    ): Boolean {
        checkMainThread()
        return  !closed && cachedViews.containsKey(layoutRes)
    }

    @MainThread
    fun clear() {
        checkMainThread()
        cachedViews.clear()
        runningRequests.clear()
    }

    @MainThread
    fun close() {
        checkMainThread()
        if (closed) return

        closed = true

        cachedViews.clear()
        runningRequests.clear()
        mainHandler.removeCallbacksAndMessages(null)
        asyncInflater = null
        activityReference.clear()
    }

    @MainThread
    private fun handleInflateFinished(
        @LayoutRes layoutRes: Int,
        requestId: Long,
        view: View
    ) {
        checkMainThread()

        if (closed) return

        if (runningRequests[layoutRes] != requestId) {
            return
        }

        runningRequests.remove(layoutRes)

        val activity = activityReference.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        if (view.parent != null) {
            return
        }

        cachedViews[layoutRes] = view

    }

    private fun checkMainThread() {
        check(
            Looper.myLooper() == Looper.getMainLooper()
        ) {
            "布局预加载缓存只能在主线程访问"
        }
    }
}
