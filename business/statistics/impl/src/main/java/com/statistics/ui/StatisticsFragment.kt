package com.statistics.ui

import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import com.example.business.budget.api.model.BudgetOverview
import com.example.business.statistics.impl.R
import com.example.business.statistics.impl.databinding.StatisticsFragmentBinding
import com.example.common.base.BaseFragment
import com.example.uikit.chart.config.DonutChartConfig
import com.example.uikit.chart.config.LineChartConfig
import com.example.uikit.chart.formatter.ChartValueFormatter
import com.example.uikit.chart.model.ChartPoint
import com.example.uikit.chart.model.ChartSeries
import com.example.uikit.chart.model.DonutChartModel
import com.example.uikit.chart.model.DonutSlice
import com.example.uikit.chart.model.LineChartModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.statistics.viewmodel.StatisticsViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.Calendar
import java.util.TimeZone

class StatisticsFragment :
    BaseFragment<StatisticsFragmentBinding>() {

    private val viewModel: StatisticsViewModel by lazy {
        ViewModelProvider(
            this,
            StatisticsViewModel.Factory()
        )[StatisticsViewModel::class.java]
    }

    private lateinit var legendRows: List<LegendRow>

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): StatisticsFragmentBinding {
        return StatisticsFragmentBinding.inflate(
            inflater,
            container,
            false
        )
    }

    override fun initView() {
        legendRows = listOf(
            LegendRow(
                binding.statisticsCategoryRow1,
                binding.statisticsCategoryDot1,
                binding.statisticsCategoryName1,
                binding.statisticsCategoryRatio1
            ),
            LegendRow(
                binding.statisticsCategoryRow2,
                binding.statisticsCategoryDot2,
                binding.statisticsCategoryName2,
                binding.statisticsCategoryRatio2
            ),
            LegendRow(
                binding.statisticsCategoryRow3,
                binding.statisticsCategoryDot3,
                binding.statisticsCategoryName3,
                binding.statisticsCategoryRatio3
            ),
            LegendRow(
                binding.statisticsCategoryRow4,
                binding.statisticsCategoryDot4,
                binding.statisticsCategoryName4,
                binding.statisticsCategoryRatio4
            )
        )

        binding.statisticsMonthSelector.setOnClickListener {
            showMonthPicker()
        }
        binding.statisticsTrendDay.setOnClickListener {
            viewModel.selectTrendMode(
                StatisticsViewModel.TrendMode.DAY
            )
        }
        binding.statisticsTrendMonth.setOnClickListener {
            viewModel.selectTrendMode(
                StatisticsViewModel.TrendMode.MONTH
            )
        }
        binding.statisticsTrendYear.setOnClickListener {
            viewModel.selectTrendMode(
                StatisticsViewModel.TrendMode.YEAR
            )
        }

        binding.statisticsBudgetEntry.setOnClickListener {
            val request = NavDeepLinkRequest.Builder
                .fromUri(
                    Uri.parse(
                        "smartwallet://budget/management"
                    )
                )
                .build()
            findNavController().navigate(request)
        }
    }

    override fun initData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                launch {
                    viewModel.uiState.collect(::renderState)
                }

                launch {
                    viewModel.budgetState.collect(::renderBudget)
                }
            }
        }
    }

    private fun renderState(
        state: StatisticsViewModel.StatisticsUiState
    ) {
        binding.statisticsErrorMessage.apply {
            text = state.errorMessage.orEmpty()
            visibility = if (state.errorMessage == null) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
        binding.statisticsSelectedMonth.text = getString(
            R.string.statistics_page_month_format,
            state.selectedYear,
            state.selectedMonth + 1
        )
        renderTrendTabs(state.trendMode)

        if (state.isLoading) {
            renderLoading()
            return
        }

        binding.statisticsIncomeAmount.text =
            formatAmount(state.incomeInCents)
        binding.statisticsExpenseAmount.text =
            formatAmount(state.expenseInCents)
        binding.statisticsBalanceAmount.text =
            formatAmount(state.balanceInCents)

        renderCategoryChart(
            state.categories,
            state.expenseInCents
        )
        renderTrendChart(state.trendPoints)
    }

    private fun renderBudget(
        budget: BudgetOverview
    ) {
        if (budget.remainingInCents >= 0) {
            binding.statisticsBudgetStatus.text =
                getString(
                    R.string.statistics_page_budget_normal
                )
            binding.statisticsBudgetRemaining.text =
                getString(
                    R.string.statistics_page_budget_remaining_format,
                    formatAmount(
                        budget.remainingInCents
                    )
                )
        } else {
            binding.statisticsBudgetStatus.text =
                getString(
                    R.string.statistics_page_budget_over
                )

            binding.statisticsBudgetRemaining.text =
                getString(
                    R.string.statistics_page_budget_over_format,
                    formatAmount(
                        -budget.remainingInCents
                    )
                )
        }
    }

    private fun renderCategoryChart(
        categories: List<StatisticsViewModel.CategoryShare>,
        totalExpenseInCents: Long
    ) {
        val colors = categoryColors()
        if (categories.isEmpty()) {
            binding.statisticsCategoryChart.clear()
            renderLegend(emptyList(), colors)
            return
        }

        binding.statisticsCategoryChart.render(
            model = DonutChartModel(
                slices = categories.mapIndexed { index, category ->
                    DonutSlice(
                        label = category.name,
                        value = category.amountInCents.toFloat(),
                        color = colors[index % colors.size]
                    )
                },
                centerText = formatAmount(totalExpenseInCents)
            ),
            config = DonutChartConfig(
                showLegend = false,
                interactive = false,
                animationDurationMs = 300,
                usePercentValues = true,
                drawValues = true,
                holeRadiusPercent = 62f,
                valueFormatter = ChartValueFormatter { value ->
                    "${value.toInt()}%"
                }
            )
        )
        renderLegend(categories, colors)
    }

    private fun renderLegend(
        categories: List<StatisticsViewModel.CategoryShare>,
        colors: IntArray
    ) {
        legendRows.forEachIndexed { index, row ->
            val category = categories.getOrNull(index)
            row.root.visibility = if (category == null) {
                View.GONE
            } else {
                View.VISIBLE
            }
            if (category != null) {
                row.name.text = category.name
                row.ratio.text = getString(
                    R.string.statistics_page_ratio_format,
                    (category.ratio * 100).toInt()
                )
                row.dot.backgroundTintList =
                    ColorStateList.valueOf(
                        colors[index % colors.size]
                    )
            }
        }
    }

    private fun renderTrendChart(
        points: List<StatisticsViewModel.TrendPoint>
    ) {
        if (points.none { it.amountInCents > 0 }) {
            binding.statisticsTrendChart.clear()
            return
        }

        val primaryColor = ContextCompat.getColor(
            requireContext(),
            R.color.statistics_page_primary
        )
        binding.statisticsTrendChart.render(
            model = LineChartModel(
                series = listOf(
                    ChartSeries(
                        name = getString(
                            R.string.statistics_page_total_expense
                        ),
                        color = primaryColor,
                        drawFilled = true,
                        fillColor = primaryColor,
                        points = points.mapIndexed { index, point ->
                            ChartPoint(
                                x = index.toFloat(),
                                y = point.amountInCents / 100f,
                                label = point.label
                            )
                        }
                    )
                ),
                xLabels = points.map { it.label }
            ),
            config = LineChartConfig(
                showLegend = false,
                showGrid = true,
                interactive = false,
                animationDurationMs = 300,
                drawValues = false,
                drawCircles = true,
                yAxisMinimum = 0f,
                gridColor = ContextCompat.getColor(
                    requireContext(),
                    R.color.statistics_page_grid
                ),
                axisTextColor = ContextCompat.getColor(
                    requireContext(),
                    R.color.statistics_page_text_secondary
                ),
                yAxisFormatter = ChartValueFormatter { value ->
                    AXIS_FORMAT.format(value)
                }
            )
        )
    }

    private fun renderTrendTabs(
        selectedMode: StatisticsViewModel.TrendMode
    ) {
        setTabSelected(
            binding.statisticsTrendDay,
            selectedMode == StatisticsViewModel.TrendMode.DAY
        )
        setTabSelected(
            binding.statisticsTrendMonth,
            selectedMode == StatisticsViewModel.TrendMode.MONTH
        )
        setTabSelected(
            binding.statisticsTrendYear,
            selectedMode == StatisticsViewModel.TrendMode.YEAR
        )
    }

    private fun setTabSelected(view: TextView, selected: Boolean) {
        view.setBackgroundResource(
            if (selected) {
                R.drawable.statistics_page_bg_tab_selected
            } else {
                R.drawable.statistics_page_bg_tab
            }
        )
        view.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (selected) {
                    R.color.statistics_page_primary
                } else {
                    R.color.statistics_page_text_primary
                }
            )
        )
    }

    private fun renderLoading() {
        val placeholder = getString(
            R.string.statistics_page_value_unavailable
        )
        binding.statisticsIncomeAmount.text = placeholder
        binding.statisticsExpenseAmount.text = placeholder
        binding.statisticsBalanceAmount.text = placeholder
        binding.statisticsCategoryChart.clear()
        binding.statisticsTrendChart.clear()
        legendRows.forEach { it.root.visibility = View.GONE }
    }

    private fun showMonthPicker() {
        val state = viewModel.uiState.value
        val utcCalendar = Calendar.getInstance(
            TimeZone.getTimeZone("UTC")
        ).apply {
            clear()
            set(state.selectedYear, state.selectedMonth, 1)
        }
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.statistics_page_select_month)
            .setSelection(utcCalendar.timeInMillis)
            .build()

        picker.addOnPositiveButtonClickListener { selectedTime ->
            val selectedCalendar = Calendar.getInstance(
                TimeZone.getTimeZone("UTC")
            ).apply {
                timeInMillis = selectedTime
            }
            viewModel.selectMonth(
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH)
            )
        }
        picker.show(parentFragmentManager, MONTH_PICKER_TAG)
    }

    private fun categoryColors(): IntArray {
        return intArrayOf(
            ContextCompat.getColor(
                requireContext(),
                R.color.statistics_page_primary
            ),
            ContextCompat.getColor(
                requireContext(),
                R.color.statistics_page_expense
            ),
            ContextCompat.getColor(
                requireContext(),
                R.color.statistics_page_orange
            ),
            ContextCompat.getColor(
                requireContext(),
                R.color.statistics_page_blue
            )
        )
    }

    private fun formatAmount(amountInCents: Long): String {
        return "¥${MONEY_FORMAT.format(
            BigDecimal.valueOf(amountInCents, 2)
        )}"
    }

    private data class LegendRow(
        val root: View,
        val dot: View,
        val name: TextView,
        val ratio: TextView
    )

    companion object {
        private const val MONTH_PICKER_TAG =
            "statistics_month_picker"
        private val MONEY_FORMAT = DecimalFormat("#,##0.00")
        private val AXIS_FORMAT = DecimalFormat("#,##0")
    }
}
