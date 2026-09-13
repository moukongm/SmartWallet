package com.statistics.viewmodel

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
import com.example.business.budget.api.model.BudgetOverview
import com.example.common.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatisticsViewModel(
    private val billDataService: BillDataService,
    private val budgetDataService: BudgetDataService
) : BaseViewModel() {

    enum class TrendMode {
        DAY,
        MONTH,
        YEAR
    }

    data class SelectedMonth(
        val year: Int,
        val month: Int
    )

    data class CategoryShare(
        val name: String,
        val amountInCents: Long,
        val ratio: Float
    )

    data class TrendPoint(
        val label: String,
        val amountInCents: Long
    )

    data class StatisticsUiState(
        val isLoading: Boolean = true,
        val selectedYear: Int = currentMonth().year,
        val selectedMonth: Int = currentMonth().month,
        val trendMode: TrendMode = TrendMode.MONTH,
        val incomeInCents: Long = 0,
        val expenseInCents: Long = 0,
        val balanceInCents: Long = 0,
        val categories: List<CategoryShare> = emptyList(),
        val trendPoints: List<TrendPoint> = emptyList(),
        val errorMessage: String? = null
    )

    private data class TimeRange(
        val startAt: Long,
        val endAtExclusive: Long
    )

    private val selectedMonth = MutableStateFlow(currentMonth())
    private val trendMode = MutableStateFlow(TrendMode.MONTH)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        selectedMonth,
        trendMode
    ) { month, mode -> month to mode }
        .flatMapLatest { (month, mode) ->
            observeStatistics(month, mode)
                .onStart {
                    emit(
                        StatisticsUiState(
                            isLoading = true,
                            selectedYear = month.year,
                            selectedMonth = month.month,
                            trendMode = mode
                        )
                    )
                }
                .catch { exception ->
                    emit(
                        StatisticsUiState(
                            isLoading = false,
                            selectedYear = month.year,
                            selectedMonth = month.month,
                            trendMode = mode,
                            errorMessage = exception.message
                                ?: "统计数据加载失败"
                        )
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatisticsUiState()
        )

    val budgetState = budgetDataService
        .observeCurrentBudget()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetOverview()
        )

    fun selectMonth(year: Int, month: Int) {
        require(month in Calendar.JANUARY..Calendar.DECEMBER)
        selectedMonth.value = SelectedMonth(year, month)
    }

    fun selectTrendMode(mode: TrendMode) {
        trendMode.value = mode
    }

    private fun observeStatistics(
        month: SelectedMonth,
        mode: TrendMode
    ): Flow<StatisticsUiState> {
        val monthRange = monthRange(month)
        val trendRange = trendRange(month, mode)

        val monthBills = billDataService.observeBills(
            BillQuery(
                startAt = monthRange.startAt,
                endAtExclusive = monthRange.endAtExclusive
            )
        )
        val trendBills = billDataService.observeBills(
            BillQuery(
                startAt = trendRange.startAt,
                endAtExclusive = trendRange.endAtExclusive,
                type = BillRecordType.EXPENSE
            )
        )

        return combine(monthBills, trendBills) {
                billsInMonth, billsInTrend ->
            buildUiState(
                month = month,
                mode = mode,
                monthBills = billsInMonth,
                trendBills = billsInTrend,
                trendRange = trendRange
            )
        }
    }

    private fun buildUiState(
        month: SelectedMonth,
        mode: TrendMode,
        monthBills: List<Bill>,
        trendBills: List<Bill>,
        trendRange: TimeRange
    ): StatisticsUiState {
        val income = monthBills
            .filter { it.type == BillRecordType.INCOME }
            .sumOf { it.amountInCents }
        val expenseBills = monthBills.filter {
            it.type == BillRecordType.EXPENSE
        }
        val expense = expenseBills.sumOf { it.amountInCents }

        return StatisticsUiState(
            isLoading = false,
            selectedYear = month.year,
            selectedMonth = month.month,
            trendMode = mode,
            incomeInCents = income,
            expenseInCents = expense,
            balanceInCents = income - expense,
            categories = buildCategoryShares(expenseBills, expense),
            trendPoints = buildTrendPoints(
                month = month,
                mode = mode,
                range = trendRange,
                bills = trendBills
            )
        )
    }

    private fun buildCategoryShares(
        expenseBills: List<Bill>,
        totalExpense: Long
    ): List<CategoryShare> {
        if (totalExpense <= 0) return emptyList()

        val grouped = expenseBills
            .groupBy { it.category.ifBlank { UNCATEGORIZED } }
            .mapValues { (_, bills) ->
                bills.sumOf { it.amountInCents }
            }
            .toList()
            .sortedByDescending { it.second }

        val topCategories = grouped
            .filter { it.first != OTHER_CATEGORY }
            .take(3)
        val topNames = topCategories.mapTo(mutableSetOf()) { it.first }
        val otherAmount = grouped
            .filter {
                it.first == OTHER_CATEGORY || it.first !in topNames
            }
            .sumOf { it.second }

        return buildList {
            topCategories.forEach { (name, amount) ->
                add(createCategoryShare(name, amount, totalExpense))
            }
            if (otherAmount > 0) {
                add(
                    createCategoryShare(
                        OTHER_CATEGORY,
                        otherAmount,
                        totalExpense
                    )
                )
            }
        }
    }

    private fun createCategoryShare(
        name: String,
        amountInCents: Long,
        totalExpense: Long
    ): CategoryShare {
        return CategoryShare(
            name = name,
            amountInCents = amountInCents,
            ratio = amountInCents.toFloat() / totalExpense.toFloat()
        )
    }

    private fun buildTrendPoints(
        month: SelectedMonth,
        mode: TrendMode,
        range: TimeRange,
        bills: List<Bill>
    ): List<TrendPoint> {
        return when (mode) {
            TrendMode.DAY -> buildDailyPoints(
                range.startAt,
                range.endAtExclusive,
                bills
            )
            TrendMode.MONTH -> buildMonthlyDailyPoints(month, bills)
            TrendMode.YEAR -> buildYearlyMonthPoints(month.year, bills)
        }
    }

    private fun buildDailyPoints(
        startAt: Long,
        endAtExclusive: Long,
        bills: List<Bill>
    ): List<TrendPoint> {
        val formatter = SimpleDateFormat("M/d", Locale.CHINA)
        val result = mutableListOf<TrendPoint>()
        var dayStart = startAt

        while (dayStart < endAtExclusive) {
            val dayEnd = addDays(dayStart, 1)
            result += TrendPoint(
                label = formatter.format(dayStart),
                amountInCents = amountBetween(bills, dayStart, dayEnd)
            )
            dayStart = dayEnd
        }
        return result
    }

    private fun buildMonthlyDailyPoints(
        month: SelectedMonth,
        bills: List<Bill>
    ): List<TrendPoint> {
        val calendar = calendarFor(month.year, month.month, 1)
        val dayCount = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        return (1..dayCount).map { day ->
            val dayStart = calendarFor(
                month.year,
                month.month,
                day
            ).timeInMillis
            val dayEnd = addDays(dayStart, 1)
            val showLabel = day == 1 || (day - 1) % 5 == 0 ||
                day == dayCount

            TrendPoint(
                label = if (showLabel) {
                    "${month.month + 1}/$day"
                } else {
                    ""
                },
                amountInCents = amountBetween(bills, dayStart, dayEnd)
            )
        }
    }

    private fun buildYearlyMonthPoints(
        year: Int,
        bills: List<Bill>
    ): List<TrendPoint> {
        return (Calendar.JANUARY..Calendar.DECEMBER).map { month ->
            val start = calendarFor(year, month, 1).timeInMillis
            val end = Calendar.getInstance().apply {
                timeInMillis = start
                add(Calendar.MONTH, 1)
            }.timeInMillis

            TrendPoint(
                label = "${month + 1}月",
                amountInCents = amountBetween(bills, start, end)
            )
        }
    }

    private fun amountBetween(
        bills: List<Bill>,
        startAt: Long,
        endAtExclusive: Long
    ): Long {
        return bills.asSequence()
            .filter {
                it.occurredAt >= startAt &&
                    it.occurredAt < endAtExclusive
            }
            .sumOf { it.amountInCents }
    }

    private fun monthRange(month: SelectedMonth): TimeRange {
        val start = calendarFor(month.year, month.month, 1)
        val end = start.clone() as Calendar
        end.add(Calendar.MONTH, 1)
        return TimeRange(start.timeInMillis, end.timeInMillis)
    }

    private fun trendRange(
        month: SelectedMonth,
        mode: TrendMode
    ): TimeRange {
        return when (mode) {
            TrendMode.DAY -> {
                val monthEnd = calendarFor(
                    month.year,
                    month.month,
                    1
                ).apply {
                    add(Calendar.MONTH, 1)
                }.timeInMillis
                TimeRange(
                    startAt = addDays(monthEnd, -7),
                    endAtExclusive = monthEnd
                )
            }
            TrendMode.MONTH -> monthRange(month)
            TrendMode.YEAR -> {
                val start = calendarFor(
                    month.year,
                    Calendar.JANUARY,
                    1
                )
                val end = start.clone() as Calendar
                end.add(Calendar.YEAR, 1)
                TimeRange(start.timeInMillis, end.timeInMillis)
            }
        }
    }

    private fun addDays(time: Long, days: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = time
            add(Calendar.DAY_OF_MONTH, days)
        }.timeInMillis
    }

    private fun calendarFor(
        year: Int,
        month: Int,
        day: Int
    ): Calendar {
        return Calendar.getInstance().apply {
            clear()
            set(year, month, day, 0, 0, 0)
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(
                modelClass.isAssignableFrom(
                    StatisticsViewModel::class.java
                )
            )
            val billService = ARouter.getInstance()
                .build(BillApiRoutes.BILL_DATA_SERVICE)
                .navigation() as BillDataService

            val budgetService = ARouter.getInstance()
                .build(BudgetApiRoutes.BUDGET_DATA_SERVICE)
                .navigation() as BudgetDataService
            return StatisticsViewModel(
                billDataService = billService,
                budgetService
            ) as T
        }
    }

    companion object {
        private const val OTHER_CATEGORY = "其他"
        private const val UNCATEGORIZED = "未分类"

        private fun currentMonth(): SelectedMonth {
            val calendar = Calendar.getInstance()
            return SelectedMonth(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH)
            )
        }
    }
}
