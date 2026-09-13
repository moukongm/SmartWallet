package com.example.smartwallet

import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.core.view.isVisible
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.common.base.BaseActivity
import com.example.common.router.RouterPath
import com.example.smartwallet.databinding.ActivityMainBinding

@Route(path = RouterPath.MAIN_ACTIVITY)
class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun getViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initView() {
        val navHost = supportFragmentManager
            .findFragmentById(binding.mainNavHost.id) as NavHostFragment
        binding.mainBottomNavigation
            .setupWithNavController(navHost.navController)
        binding.mainAddButton.setOnClickListener {
            if (navHost.navController.currentDestination?.id != R.id.addBillFragment) {
                navHost.navController.navigate(R.id.addBillFragment)
            }
        }
        navHost.navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.mainBottomBarContainer.isVisible =
                destination.id in TOP_LEVEL_DESTINATIONS
            if (
                destination.id == R.id.homeFragment
            ) {
                scheduleBillPreload()
            }
        }
    }

    override fun initData() {
        scheduleBillPreload()
    }

    private fun scheduleBillPreload() {
        binding.root.post {
            layoutPreloader.preload(
                com.example.business.bill.impl.R.layout.bill_fragment
            )
        }
    }

    companion object {
        private val TOP_LEVEL_DESTINATIONS = setOf(
            R.id.homeFragment,
            R.id.billFragment,
            R.id.statisticsFragment,
            R.id.profileFragment
        )
    }
}
