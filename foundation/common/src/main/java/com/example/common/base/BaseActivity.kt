package com.example.common.base

import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.example.common.preload.AsyncLayoutPreLoader
import com.example.common.preload.LayoutPreloadOwner
import com.example.common.touchtarget.TouchTargetConfig
import com.example.common.touchtarget.TouchTargetManager

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity(), LayoutPreloadOwner {

    protected lateinit var binding: VB
    private var touchTargetManager: TouchTargetManager? = null

    private var internalLayoutPreloader: AsyncLayoutPreLoader? = null

    final override val layoutPreloader: AsyncLayoutPreLoader
        get() {
            return internalLayoutPreloader ?: AsyncLayoutPreLoader(this)
                .also {
                    internalLayoutPreloader = it
                }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initImmersiveStatusBar()
        binding = getViewBinding()
        setContentView(binding.root)
        initView()
        initData()
        initializeTouchTargetInspection()
    }

    override fun onResume() {
        super.onResume()
        requestTouchTargetInspection()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        internalLayoutPreloader ?.clear()
    }

    override fun onDestroy() {
        touchTargetManager?.stop()
        touchTargetManager = null

        internalLayoutPreloader ?.close()
        internalLayoutPreloader = null
        super.onDestroy()
    }

    internal fun requestTouchTargetInspection() {
        touchTargetManager?.refresh()
    }

    protected open fun provideTouchTargetConfig() = TouchTargetConfig()

    protected open fun isTouchTargetInspectionEnabled(): Boolean {
        return applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private fun initializeTouchTargetInspection() {
        val config = provideTouchTargetConfig()
        if (!isTouchTargetInspectionEnabled() || !config.enabled) return

        touchTargetManager = TouchTargetManager(this, config).also {
            it.start()
        }
    }

    private fun initImmersiveStatusBar() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        var systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
        ) {
            systemUiVisibility =
                systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        window.decorView.systemUiVisibility = systemUiVisibility
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
    }

    abstract fun getViewBinding(): VB

    abstract fun initView()

    abstract fun initData()
}
