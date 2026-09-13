package com.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alibaba.android.arouter.launcher.ARouter
import com.budget.data.BudgetRepository
import com.example.business.budget.api.BudgetApiRoutes
import com.example.business.budget.api.BudgetDataService
import com.example.business.budget.api.model.BudgetOverview
import com.example.business.budget.api.model.BudgetSetting
import com.example.common.base.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val service: BudgetDataService
) : BaseViewModel(){
    val uiState = service
        .observeCurrentBudget()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetOverview()
        )

    fun saveBudget(
        totalInCents: Long,
        foodInCents: Long,
        trafficInCents: Long,
        shoppingInCents: Long
    ) {
        viewModelScope.launch {
            service.saveCurrentBudget(
                BudgetSetting(
                    totalBudgetInCents =
                        totalInCents,
                    categoryBudgets = mapOf(
                        BudgetRepository.CATEGORY_FOOD to foodInCents,
                        BudgetRepository.CATEGORY_TRAFFIC to trafficInCents,
                        BudgetRepository.CATEGORY_SHOPPING to shoppingInCents
                    ),
                    reminderEnabled = uiState.value.reminderEnabled
                )
            )
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        val state = uiState.value
        viewModelScope.launch {
            service.saveCurrentBudget(
                BudgetSetting(
                    totalBudgetInCents = state.totalBudgetInCents,
                    categoryBudgets = state.categories.associate {
                        it.name to it.budgetInCents
                    },
                    reminderEnabled = enabled
                )
            )
        }
    }

    class Factory: ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create (
            modelClass: Class<T>
        ): T {
            val service = ARouter.getInstance()
                .build(
                    BudgetApiRoutes.BUDGET_DATA_SERVICE
                )
                .navigation() as BudgetDataService
            return BudgetViewModel(service) as T
        }
    }
}
