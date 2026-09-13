package com.bill.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alibaba.android.arouter.launcher.ARouter
import com.example.business.bill.api.BillApiRoutes
import com.example.business.bill.api.BillDataService
import com.example.business.bill.api.BillRecordType
import com.example.business.bill.api.CategoryDataService
import com.example.business.bill.api.model.Bill
import com.example.business.bill.api.model.BillCategoryInfo
import com.example.business.bill.api.model.BillQuery
import com.example.business.bill.api.model.BillSummary
import com.example.common.base.BaseViewModel
import java.util.Calendar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BillViewModel(
    private val billService: BillDataService,
    private val categoryService: CategoryDataService
) : BaseViewModel() {

    data class BillFilter(
        val year: Int,
        val month: Int,
        val keyword: String = "",
        val category: String? = null,
        val type: BillRecordType? = null
    )

    data class BillUiState(
        val filter: BillFilter = currentFilter(),
        val summary: BillSummary = EMPTY_SUMMARY,
        val bills: List<Bill> = emptyList(),
        val categories: List<BillCategoryInfo> = emptyList(),
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(BillUiState())
    val uiState: StateFlow<BillUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        observeCategories()
        refresh()
    }

    fun refresh() = loadPage(reset = true)

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        loadPage(reset = false)
    }

    fun search(keyword: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update {
                it.copy(
                    filter = it.filter.copy(keyword = keyword.trim())
                )
            }
            refresh()
        }
    }

    fun selectMonth(year: Int, month: Int) {
        _uiState.update {
            it.copy(filter = it.filter.copy(year = year, month = month))
        }
        refresh()
    }

    fun selectCategory(category: String?) {
        _uiState.update {
            it.copy(filter = it.filter.copy(category = category))
        }
        refresh()
    }

    fun selectType(type: BillRecordType?) {
        _uiState.update {
            it.copy(
                filter = it.filter.copy(type = type, category = null)
            )
        }
        refresh()
    }

    fun resetFilters() {
        searchJob?.cancel()
        _uiState.update { it.copy(filter = currentFilter()) }
        refresh()
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadPage(reset: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val state = _uiState.value
            val range = monthRange(state.filter)
            val offset = if (reset) 0 else state.bills.size

            _uiState.update {
                it.copy(
                    isLoading = reset,
                    isLoadingMore = !reset,
                    errorMessage = null
                )
            }

            try {
                val page = billService.getBills(
                    BillQuery(
                        startAt = range.first,
                        endAtExclusive = range.second,
                        type = state.filter.type,
                        category = state.filter.category,
                        keyword = state.filter.keyword.takeIf(String::isNotBlank),
                        limit = PAGE_SIZE,
                        offset = offset
                    )
                )

                val summary = if (reset) {
                    billService.getSummary(range.first, range.second)
                } else {
                    _uiState.value.summary
                }

                _uiState.update {
                    it.copy(
                        summary = summary,
                        bills = if (reset) page else it.bills + page,
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = page.size == PAGE_SIZE
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = exception.message ?: "账单加载失败"
                    )
                }
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            combine(
                categoryService.observeCategories(BillRecordType.EXPENSE),
                categoryService.observeCategories(BillRecordType.INCOME)
            ) { expense, income ->
                (expense + income).distinctBy { it.name }
            }.collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    private fun monthRange(filter: BillFilter): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            clear()
            set(filter.year, filter.month, 1)
        }
        val end = start.clone() as Calendar
        end.add(Calendar.MONTH, 1)
        return start.timeInMillis to end.timeInMillis
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(BillViewModel::class.java))
            val billService = ARouter.getInstance()
                .build(BillApiRoutes.BILL_DATA_SERVICE)
                .navigation() as BillDataService
            val categoryService = ARouter.getInstance()
                .build(BillApiRoutes.CATEGORY_DATA_SERVICE)
                .navigation() as CategoryDataService
            return BillViewModel(billService, categoryService) as T
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
        private val EMPTY_SUMMARY = BillSummary(0, 0, 0)

        private fun currentFilter(): BillFilter {
            val calendar = Calendar.getInstance()
            return BillFilter(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH)
            )
        }
    }
}
