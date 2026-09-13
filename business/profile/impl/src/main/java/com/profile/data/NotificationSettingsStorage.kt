package com.profile.data

import com.example.storage.MmkvStorage

object NotificationSettingsStorage {
    private const val STORAGE_ID = "notification_settings"
    private const val BUDGET_KEY = "budget_reminder"
    private const val BILL_KEY = "bill_reminder"

    fun isBudgetReminderEnabled() =
        MmkvStorage.getBoolean(STORAGE_ID, BUDGET_KEY, true)

    fun setBudgetReminderEnabled(enabled: Boolean) {
        MmkvStorage.putBoolean(STORAGE_ID, BUDGET_KEY, enabled)
    }

    fun isBillReminderEnabled() =
        MmkvStorage.getBoolean(STORAGE_ID, BILL_KEY, true)

    fun setBillReminderEnabled(enabled: Boolean) {
        MmkvStorage.putBoolean(STORAGE_ID, BILL_KEY, enabled)
    }
}
