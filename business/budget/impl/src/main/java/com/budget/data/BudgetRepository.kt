package com.budget.data

import com.example.storage.MmkvStorage
import com.example.business.budget.api.model.BudgetSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class BudgetRepository {
    private val refreshVersion = MutableStateFlow(0)
    fun observeBudget(
        userId: String,
        monthKey: String
    ): Flow<BudgetSetting> {
        return refreshVersion.map {
            getBudget(userId, monthKey)
        }
    }

    fun saveBudget(
        userId: String,
        monthKey: String,
        setting: BudgetSetting
    ) {
        MmkvStorage.putLong(
            STORAGE_ID,
            key(userId, monthKey, KEY_TOTAL),
            setting.totalBudgetInCents
        )

        MmkvStorage.putLong(
            STORAGE_ID,
            key(userId, monthKey, KEY_FOOD),
            setting.categoryBudgets[CATEGORY_FOOD]
                ?: 0L
        )

        MmkvStorage.putLong(
            STORAGE_ID,
            key(userId, monthKey, KEY_TRAFFIC),
            setting.categoryBudgets[CATEGORY_TRAFFIC]
                ?: 0L
        )

        MmkvStorage.putLong(
            STORAGE_ID,
            key(userId, monthKey, KEY_SHOPPING),
            setting.categoryBudgets[CATEGORY_SHOPPING]
                ?: 0L
        )

        MmkvStorage.putBoolean(
            STORAGE_ID,
            key(userId, monthKey, KEY_REMINDER),
            setting.reminderEnabled
        )

        refreshVersion.value += 1
    }

    private fun getBudget(
        userId: String,
        monthKey: String
    ): BudgetSetting {
        return BudgetSetting(
            totalBudgetInCents = MmkvStorage.getLong(
                STORAGE_ID,
                key(userId, monthKey,KEY_TOTAL),
                500_000L
            ),
            categoryBudgets = mapOf(
                CATEGORY_FOOD to MmkvStorage.getLong(
                    STORAGE_ID,
                    key(userId, monthKey, KEY_FOOD),
                    150_000L
                ),
                CATEGORY_TRAFFIC to MmkvStorage.getLong(
                    STORAGE_ID,
                    key(userId,monthKey, KEY_TRAFFIC),
                    80_000L
                ),
                CATEGORY_SHOPPING to MmkvStorage.getLong(
                    STORAGE_ID,
                    key(userId, monthKey, KEY_SHOPPING),
                    100_000L
                )
            ),
            reminderEnabled =
                MmkvStorage.getBoolean(
                    STORAGE_ID,
                    key(
                        userId,
                        monthKey,
                        KEY_REMINDER
                    ),
                    true
                )

        )
    }

    private fun key(
        userId: String,
        monthKey: String,
        name: String
    ): String {
        return "${userId}_${monthKey}_$name"
    }

    companion object {
        private const val STORAGE_ID = "smart_wallet_budget"
        private const val KEY_TOTAL = "total"
        private const val KEY_FOOD = "food"
        private const val KEY_TRAFFIC = "traffic"
        private const val KEY_SHOPPING = "shopping"
        private const val KEY_REMINDER = "reminder"
        const val CATEGORY_FOOD = "餐饮"
        const val CATEGORY_TRAFFIC = "交通"
        const val CATEGORY_SHOPPING = "购物"
    }
}
