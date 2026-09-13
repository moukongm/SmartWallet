package com.home.viewmodel

import android.icu.util.Calendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alibaba.android.arouter.launcher.ARouter
import com.example.business.bill.api.BillApiRoutes
import com.example.business.bill.api.BillDataService
import com.example.business.bill.api.BillRecordType
import com.example.business.bill.api.model.Bill
import com.example.business.bill.api.model.BillQuery
import com.example.business.budget.api.BudgetApiRoutes
import com.example.business.budget.api.BudgetDataService
import com.example.common.base.BaseViewModel
import com.example.common.router.RouterPath
import com.profile.api.AuthService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class HomeViewModel(
    private val billDataService: BillDataService,
    private val budgetDataService: BudgetDataService,
    private val authService: AuthService
) : BaseViewModel() {
    data class DailyTrend(
        val label: String,
        val incomeInCents: Long,
        val expenseInCents: Long
    )

    data class HomeUiState(
        val isLoading: Boolean = true,
        val greeting: String = buildGreeting(),
        val todayIncomeInCents: Long = 0,
        val todayExpenseInCents: Long = 0,
        val monthlyIncomeInCents: Long = 0,
        val monthlyExpenseInCents: Long = 0,
        val monthlyBalanceInCents: Long = 0,
        val remainingBudgetInCents: Long? = null,
        val dailyTrends: List<DailyTrend> = emptyList(),
        val recentBills: List<Bill> = emptyList(),
        val avatarUri: String = "",
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var observationJob: Job? = null
    private var observedTodayStart: Long? = null

    init {
        refresh(force = true)
    }

    fun refresh(force: Boolean = false) {
        val todayStart = startOfToday(System.currentTimeMillis())
        if (
            !force &&
            observedTodayStart == todayStart &&
            observationJob?.isActive == true
        ) return

        observedTodayStart = todayStart
        observationJob?.cancel()
        observationJob = observeHomeData(todayStart)
    }

    private fun observeHomeData(todayStart: Long): Job {
        val tomorrowStart = addDays(todayStart, 1)
        val monthStart = startOfMonth(todayStart)
        val nextMonthStart = addMonths(monthStart, 1)
        val sevenDaysStart = addDays(todayStart, -6)

        return viewModelScope.launch {
            try {
                authService.refreshProfile()
                launch {
                    authService.observeProfile().collect { profile ->
                        _uiState.value = _uiState.value.copy(
                            avatarUri = profile?.avatarUri.orEmpty()
                        )
                    }
                }
                combine(
                    billDataService.observeBills(
                        BillQuery(
                            startAt = todayStart,
                            endAtExclusive = tomorrowStart
                        )
                    ),
                    billDataService.observeBills(
                        BillQuery(
                            startAt = monthStart,
                            endAtExclusive = nextMonthStart
                        )
                    ),
                    billDataService.observeBills(
                        BillQuery(
                            startAt = sevenDaysStart,
                            endAtExclusive = tomorrowStart
                        )
                    ),
                    billDataService.observeBills(
                        BillQuery(limit = 3)
                    ),
                    budgetDataService.observeCurrentBudget()
                ) {
                    todayBills,
                    monthBills,
                    sevenDayBills,
                    recentBills,
                    budget ->
                    val todayIncome = todayBills
                        .filter {  it.type == BillRecordType.INCOME }
                        .sumOf { it.amountInCents }
                    val todayExpense = todayBills
                        .filter { it.type == BillRecordType.EXPENSE }
                        .sumOf { it.amountInCents }

                    val monthlyIncome = monthBills
                        .filter { it.type == BillRecordType.INCOME }
                        .sumOf { it.amountInCents }

                    val monthlyExpense = monthBills
                        .filter { it.type == BillRecordType.EXPENSE }
                        .sumOf { it.amountInCents }

                    HomeUiState(
                        isLoading = false,
                        greeting = buildGreeting(),
                        todayIncomeInCents = todayIncome,
                        todayExpenseInCents = todayExpense,
                        monthlyIncomeInCents = monthlyIncome,
                        monthlyExpenseInCents = monthlyExpense,
                        monthlyBalanceInCents =
                            monthlyIncome - monthlyExpense,
                        remainingBudgetInCents =
                            budget.remainingInCents,
                        dailyTrends = buildDailyTrends(
                            startAt = sevenDaysStart,
                            bills = sevenDayBills
                        ),
                        recentBills = recentBills,
                        avatarUri = _uiState.value.avatarUri,
                        errorMessage = null
                    )
                }.collect { state -> _uiState.value = state }

            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message
                        ?: "首页数据加载失败"
                )
            }
        }
    }

    private fun buildDailyTrends(
        startAt: Long,
        bills: List<Bill>
    ): List<DailyTrend> {
        return (0 until 7).map { index ->
            val dayStart = addDays(startAt, index)
            val dayEnd = addDays(dayStart, 1)

            val dayBills = bills.filter {
                it.occurredAt >= dayStart &&
                        it.occurredAt < dayEnd
            }

            DailyTrend(
                label = SimpleDateFormat(
                    "M/d",
                    Locale.CHINA
                ).format(dayStart),
                incomeInCents = dayBills
                    .filter { it.type == BillRecordType.INCOME }
                    .sumOf { it.amountInCents },
                expenseInCents = dayBills
                    .filter { it.type == BillRecordType.EXPENSE }
                    .sumOf { it.amountInCents }
            )
        }
    }

    private fun startOfToday(time: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun startOfMonth(time: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = time
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun addDays(
        time: Long,
        days: Int
    ): Long {
        return Calendar.getInstance().apply {
            timeInMillis = time
            add(Calendar.DAY_OF_MONTH, days)
        }.timeInMillis
    }

    private fun addMonths(
        time: Long,
        months: Int
    ): Long {
        return Calendar.getInstance().apply {
            timeInMillis = time
            add(Calendar.MONTH, months)
        }.timeInMillis
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {
            require(
                modelClass.isAssignableFrom(
                    HomeViewModel::class.java
                )
            )
            val billService = ARouter.getInstance()
                .build(BillApiRoutes.BILL_DATA_SERVICE)
                .navigation() as? BillDataService
            requireNotNull(billService) {
                "无法获取 BillDataService，请检查 bill:impl 是否已打包"
            }
            val budgetService = ARouter.getInstance()
                .build(BudgetApiRoutes.BUDGET_DATA_SERVICE)
                .navigation() as? BudgetDataService
            requireNotNull(budgetService) {
                "无法获取 BudgetDataService，请检查 budget:impl 是否已打包"
            }
            val authService = ARouter.getInstance()
                .build(RouterPath.USER_AUTH_SERVICE)
                .navigation() as? AuthService
            requireNotNull(authService) {
                "无法获取 AuthService，请检查 profile:impl 是否已打包"
            }
            return HomeViewModel(
                billService,
                budgetService,
                authService
            ) as T
        }
    }

    companion object {

        private fun buildGreeting(): String {
            val hour = Calendar.getInstance()
                .get(Calendar.HOUR_OF_DAY)

            return when (hour) {
                in 5..11 -> "早上好 👋"
                in 12..13 -> "中午好 👋"
                in 14..17 -> "下午好 👋"
                else -> "晚上好 👋"
            }
        }
    }
}
