package com.home.ui

import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.business.bill.api.BillRecordType
import com.example.business.bill.api.model.Bill
import com.example.business.home.impl.R
import com.example.business.home.impl.databinding.HomeFragmentBinding
import com.example.common.base.BaseFragment
import com.example.uikit.chart.config.LineChartConfig
import com.example.uikit.chart.model.ChartPoint
import com.example.uikit.chart.model.ChartSeries
import com.example.uikit.chart.model.LineChartModel
import com.home.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar
import java.util.Locale

class HomeFragment : BaseFragment<HomeFragmentBinding>() {

    private val viewModel: HomeViewModel by lazy {
        ViewModelProvider(
            this,
            HomeViewModel.Factory()
        )[HomeViewModel::class.java]
    }
    private lateinit var recentRows: List<BillRowViews>

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): HomeFragmentBinding {
        return HomeFragmentBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        recentRows = listOf(
            BillRowViews(
                root = binding.homeRecentRow1,
                icon = binding.homeRecentIcon1,
                title = binding.homeRecentTitle1,
                category = binding.homeRecentCategory1,
                amount = binding.homeRecentAmount1,
                time = binding.homeRecentTime1
            ),
            BillRowViews(
                root = binding.homeRecentRow2,
                icon = binding.homeRecentIcon2,
                title = binding.homeRecentTitle2,
                category = binding.homeRecentCategory2,
                amount = binding.homeRecentAmount2,
                time = binding.homeRecentTime2
            ),
            BillRowViews(
                root = binding.homeRecentRow3,
                icon = binding.homeRecentIcon3,
                title = binding.homeRecentTitle3,
                category = binding.homeRecentCategory3,
                amount = binding.homeRecentAmount3,
                time = binding.homeRecentTime3
            )
        )
        binding.homeViewAll.setOnClickListener {
            openDeepLink("smartwallet://bill/list")
        }
    }

    override fun initData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ){
                viewModel.uiState.collect(::renderState)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun renderState(
        state: HomeViewModel.HomeUiState
    ) {
        binding.homeGreeting.text = state.greeting
        if (state.avatarUri.isBlank()) {
            binding.homeAvatar.setImageResource(R.drawable.home_ic_avatar)
        } else {
            binding.homeAvatar.setImageURI(Uri.parse(state.avatarUri))
        }
        binding.homeErrorMessage.apply {
            text = state.errorMessage.orEmpty()
            visibility = if (state.errorMessage == null) View.GONE else View.VISIBLE
        }

        if (state.isLoading) {
            renderLoading()
            return
        }
        binding.homeTodayExpenseAmount.text = formatAmount(state.todayExpenseInCents)
        binding.homeTodayIncomeAmount.text = formatAmount(state.todayIncomeInCents)
        binding.homeRemainingBudgetAmount.text =
            state.remainingBudgetInCents
                ?.let(::formatAmount)
                ?: getString(R.string.home_value_unavailable)

        binding.homeMonthBalanceAmount.text =
            formatAmount(state.monthlyBalanceInCents)

        binding.homeMonthIncomeAmount.text =
            formatAmount(state.monthlyIncomeInCents)

        binding.homeMonthExpenseAmount.text =
            formatAmount(state.monthlyExpenseInCents)

        renderTrend(state.dailyTrends)
        renderRecentBills(state.recentBills)
    }

    private fun renderTrend(
        trends: List<HomeViewModel.DailyTrend>
    ) {
        val hasData = trends.any {
            it.incomeInCents != 0L || it.expenseInCents != 0L
        }
        if (!hasData) {
            binding.homeTrendChart.clear()
            return
        }

        val incomeColor = ContextCompat.getColor(
            requireContext(),
            R.color.home_primary
        )

        val expenseColor = ContextCompat.getColor(
            requireContext(),
            R.color.home_expense
        )

        binding.homeTrendChart.render(
            model = LineChartModel(
                series = listOf(
                    ChartSeries(
                        name = getString(
                            R.string.home_trend_income
                        ),
                        color = incomeColor,
                        points = trends.mapIndexed {
                                index, trend ->
                            ChartPoint(
                                x = index.toFloat(),
                                y = trend.incomeInCents / 100f,
                                label = trend.label
                            )
                        }
                    ),
                    ChartSeries(
                        name = getString(
                            R.string.home_trend_expense
                        ),
                        color = expenseColor,
                        points = trends.mapIndexed {
                                index, trend ->
                            ChartPoint(
                                x = index.toFloat(),
                                y = trend.expenseInCents / 100f,
                                label = trend.label
                            )
                        }
                    )
                ),
                xLabels = trends.map { it.label }
            ),
            config = LineChartConfig(
                showLegend = false,
                showGrid = true,
                interactive = false,
                animationDurationMs = 300,
                curvedLines = false,
                drawValues = false,
                drawCircles = true,
                yAxisMinimum = 0f
            )
        )
    }

    private fun renderRecentBills(
        bills: List<Bill>
    ) {
        binding.homeRecentEmpty.visibility =
            if (bills.isEmpty()) View.VISIBLE else View.GONE
        recentRows.forEachIndexed {
            index, row ->
            val bill = bills.getOrNull(index)
            row.root.visibility =
                if (bill == null) View.GONE else View.VISIBLE
            if (bill == null) {
                row.root.setOnClickListener(null)
            } else {
                renderBillRow(row, bill)
                row.root.setOnClickListener {
                    openDeepLink(
                        "smartwallet://bill/edit/${bill.id}"
                    )
                }
            }
        }
        binding.homeRecentDivider1.visibility =
            if (bills.size >= 2) View.VISIBLE else View.GONE

        binding.homeRecentDivider2.visibility =
            if (bills.size >= 3) View.VISIBLE else View.GONE
    }

    private fun renderBillRow(
        row: BillRowViews,
        bill: Bill
    ) {
        row.title.text = bill.note.ifBlank {
            bill.category
        }
        row.category.text = bill.category
        row.amount.text = formatSignedAmount(bill)
        row.time.text = formatTime(bill.occurredAt)
        row.icon.setImageResource(
            iconForCategory(bill.category)
        )

        val amountColor = ContextCompat.getColor(
            requireContext(),
            if (bill.type == BillRecordType.INCOME) {
                R.color.home_primary
            } else {
                R.color.home_expense
            }
        )
        row.amount.setTextColor(amountColor)
    }

    private fun renderLoading() {
        val placeholder = getString(
            R.string.home_value_unavailable
        )
        binding.homeTodayExpenseAmount.text = placeholder
        binding.homeTodayIncomeAmount.text = placeholder
        binding.homeRemainingBudgetAmount.text = placeholder
        binding.homeMonthBalanceAmount.text = placeholder
        binding.homeMonthIncomeAmount.text = placeholder
        binding.homeMonthExpenseAmount.text = placeholder
        binding.homeTrendChart.clear()
        binding.homeRecentEmpty.visibility = View.GONE
        recentRows.forEach { row -> row.root.visibility = View.GONE }
        binding.homeRecentDivider1.visibility = View.GONE
        binding.homeRecentDivider2.visibility = View.GONE
    }

    private fun openDeepLink(uri: String) {
        val request = NavDeepLinkRequest.Builder
            .fromUri(uri.toUri())
            .build()
        val navController = findNavController()
        val options = if (uri == "smartwallet://bill/list") {
            navOptions {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            null
        }
        navController.navigate(request, options)
    }

    private fun formatAmount(
        amountInCents: Long
    ): String {
        val amount = BigDecimal.valueOf(
            amountInCents,
            2
        )

        return "¥${MONEY_FORMAT.format(amount)}"
    }

    private fun formatSignedAmount(
        bill: Bill
    ): String {
        val sign = if (bill.type == BillRecordType.INCOME) {
            "+"
        } else {
            "-"
        }

        return "$sign${formatAmount(bill.amountInCents)}"
    }

    private fun formatTime(time: Long): String {
        val target = Calendar.getInstance().apply {
            timeInMillis = time
        }
        val today = Calendar.getInstance()

        val isToday =
            target.get(Calendar.YEAR) ==
                    today.get(Calendar.YEAR) &&
                    target.get(Calendar.DAY_OF_YEAR) ==
                    today.get(Calendar.DAY_OF_YEAR)

        return SimpleDateFormat(
            if (isToday) {
                "今天 HH:mm"
            } else {
                "M月d日 HH:mm"
            },
            Locale.CHINA
        ).format(time)
    }

    private fun iconForCategory(
        category: String
    ): Int {
        return when (category) {
            "餐饮" -> R.drawable.home_ic_food
            "交通" -> R.drawable.home_ic_subway
            "工资",
            "奖金",
            "理财",
            "转账" -> R.drawable.home_ic_salary

            else -> R.drawable.home_ic_wallet
        }
    }


    private data class BillRowViews(
        val root: LinearLayout,
        val icon: ImageView,
        val title: TextView,
        val category: TextView,
        val amount: TextView,
        val time: TextView
    )

    companion object {
        private val MONEY_FORMAT = DecimalFormat("#,##0.00")
    }
}
