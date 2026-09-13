package com.profile.ui

import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.example.business.profile.impl.databinding.ProfileActivityLoginBinding
import com.example.common.base.BaseActivity
import com.example.common.router.RouterPath

@Route(path = RouterPath.USER_LOGIN_ACTIVITY)
class LoginActivity : BaseActivity<ProfileActivityLoginBinding>() {

    override fun getViewBinding(): ProfileActivityLoginBinding {
        return ProfileActivityLoginBinding.inflate(layoutInflater)
    }

    override fun initView() = Unit

    override fun initData() = Unit

    fun showRegister() {
        val currentFragment = supportFragmentManager
            .findFragmentById(binding.profileAuthFragmentContainer.id)
        if (currentFragment is RegisterFragment) return

        supportFragmentManager.beginTransaction()
            .replace(
                binding.profileAuthFragmentContainer.id,
                RegisterFragment()
            )
            .addToBackStack(RegisterFragment::class.java.name)
            .commit()
    }

    fun showLogin() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            supportFragmentManager.beginTransaction()
                .replace(
                    binding.profileAuthFragmentContainer.id,
                    LoginFragment()
                )
                .commit()
        }
    }

    fun openMainPage() {
        ARouter.getInstance()
            .build(RouterPath.MAIN_ACTIVITY)
            .navigation()
        finish()
    }
}
