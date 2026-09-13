package com.budget.data

import android.content.Context
import android.icu.util.Calendar
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.example.business.bill.api.BillApiRoutes
import com.example.business.bill.api.BillDataService
import com.example.business.bill.api.BillRecordType
import com.example.business.bill.api.model.BillQuery
import com.example.business.budget.api.BudgetApiRoutes
import com.example.business.budget.api.BudgetDataService
import com.example.business.budget.api.model.BudgetOverview
import com.example.business.budget.api.model.BudgetSetting
import com.example.business.budget.api.model.CategoryBudgetUsage
import com.example.common.router.RouterPath
import com.profile.api.AuthService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Route(path = BudgetApiRoutes.BUDGET_DATA_SERVICE)
class BudgetDataServiceImpl : BudgetDataService {
    private val repository = BudgetRepository()
    private lateinit var billDataService: BillDataService
    private lateinit var  authService: AuthService

    override fun init(context: Context?) {
        billDataService = ARouter.getInstance()
            .build(BillApiRoutes.BILL_DATA_SERVICE)
            .navigation() as BillDataService
        authService = ARouter.getInstance()
            .build(RouterPath.USER_AUTH_SERVICE)
            .navigation() as AuthService
    }

    override fun observeCurrentBudget(): Flow<BudgetOverview> {
        val calendar = Calendar.getInstance()
        val userId = authService.currentUserId().orEmpty()
        val monthKey = monthKey(calendar)
        val startAt = Calendar.getInstance().apply {
            clear()
            set(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                1
            )
        }.timeInMillis

        val endAt = Calendar.getInstance().apply {
            timeInMillis = startAt
            add(Calendar.MONTH, 1)
        }.timeInMillis

        val bills = billDataService.observeBills(
            BillQuery(
                startAt = startAt,
                endAtExclusive = endAt,
                type = BillRecordType.EXPENSE
            )
        )

        return combine(
            repository.observeBudget(
                userId,
                monthKey
            ),
            bills
        ) {
            setting, billList ->
            val used = billList.sumOf {
                it.amountInCents
            }

            val categoryUsed = billList
                .groupBy { it.category }
                .mapValues { (_, bills) ->
                    bills.sumOf {
                        it.amountInCents
                    }
                }

            BudgetOverview(
                totalBudgetInCents = setting.totalBudgetInCents,
                usedInCents = used,
                remainingInCents = setting.totalBudgetInCents - used,
                reminderEnabled = setting.reminderEnabled,
                categories = setting.categoryBudgets.map {
                    (name, budget) ->
                    CategoryBudgetUsage(
                        name = name,
                        budgetInCents = budget,
                        usedInCents = categoryUsed[name] ?: 0L
                    )
                }
            )
        }
    }

    override suspend fun saveCurrentBudget(setting: BudgetSetting) {
        val calendar = Calendar.getInstance()
        repository.saveBudget(
            userId =
                authService.currentUserId().orEmpty(),
            monthKey = monthKey(calendar),
            setting = setting
        )
    }
    private fun monthKey(
        calendar: Calendar
    ): String {
        return buildString {
            append(calendar.get(Calendar.YEAR))
            append("-")
            append(calendar.get(Calendar.MONTH) + 1)
        }
    }
}
