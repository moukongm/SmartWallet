package com.example.business.budget.api

import com.alibaba.android.arouter.facade.template.IProvider
import com.example.business.budget.api.model.BudgetOverview
import com.example.business.budget.api.model.BudgetSetting
import kotlinx.coroutines.flow.Flow

interface BudgetDataService : IProvider {
    fun observeCurrentBudget(): Flow<BudgetOverview>

    suspend fun saveCurrentBudget(
        setting: BudgetSetting
    )
}
