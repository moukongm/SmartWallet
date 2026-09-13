package com.profile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.business.profile.impl.databinding.ProfileFragmentNotificationBinding
import com.example.common.base.BaseFragment
import com.profile.data.NotificationSettingsStorage

class NotificationSettingsFragment : BaseFragment<ProfileFragmentNotificationBinding>() {

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ProfileFragmentNotificationBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.profileNotificationBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.profileNotificationBudgetSwitch.isChecked =
            runCatching {
                NotificationSettingsStorage.isBudgetReminderEnabled()
            }.getOrDefault(true)
        binding.profileNotificationBillSwitch.isChecked =
            runCatching {
                NotificationSettingsStorage.isBillReminderEnabled()
            }.getOrDefault(true)

        binding.profileNotificationBudgetSwitch.setOnCheckedChangeListener { _, checked ->
            runCatching {
                NotificationSettingsStorage.setBudgetReminderEnabled(checked)
            }
        }
        binding.profileNotificationBillSwitch.setOnCheckedChangeListener { _, checked ->
            runCatching {
                NotificationSettingsStorage.setBillReminderEnabled(checked)
            }
        }
    }

    override fun initData() = Unit
}
