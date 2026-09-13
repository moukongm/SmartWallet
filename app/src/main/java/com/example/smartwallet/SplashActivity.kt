package com.example.smartwallet

import com.alibaba.android.arouter.launcher.ARouter
import com.example.common.base.BaseActivity
import com.example.common.router.RouterPath
import com.example.smartwallet.databinding.ActivitySplashBinding
import com.profile.api.AuthService

class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    override fun getViewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun initData() {

    }

    override fun initView() {
        checkLogin()
    }

    private fun checkLogin() {
        binding.root.post {
            val authService = ARouter.getInstance()
                .build(RouterPath.USER_AUTH_SERVICE)
                .navigation() as? AuthService
            val targetPath = if (authService?.isLoggedIn() == true) {
                RouterPath.MAIN_ACTIVITY
            } else {
                RouterPath.USER_LOGIN_ACTIVITY
            }

            ARouter.getInstance()
                .build(targetPath)
                .navigation()
            finish()
        }
    }

}
