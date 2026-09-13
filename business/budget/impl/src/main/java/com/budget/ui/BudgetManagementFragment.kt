package com.budget.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.budget.viewmodel.BudgetViewModel
import com.example.business.budget.impl.R
import com.example.business.budget.impl.databinding.BudgetFragmentManagementBinding
import com.example.common.base.BaseFragment
import com.example.uikit.chart.model.DonutChartModel
import com.example.uikit.chart.model.DonutSlice
import kotlinx.coroutines.launch
import com.example.business.budget.api.model.BudgetOverview
import com.example.business.budget.impl.databinding.BudgetDialogSetBinding
import com.example.uikit.chart.config.DonutChartConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.math.BigDecimal
import java.text.DecimalFormat

class BudgetManagementFragment : BaseFragment<BudgetFragmentManagementBinding>() {

    private var renderingState = false

    private val viewModel: BudgetViewModel by lazy {
        ViewModelProvider(
            this,
            BudgetViewModel.Factory()
        )[BudgetViewModel::class.java]
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): BudgetFragmentManagementBinding {
        return BudgetFragmentManagementBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        binding.budgetSetButton.setOnClickListener {
            showSetBudgetDialog()
        }
        binding.budgetReminderSwitch.setOnCheckedChangeListener {
            _, checked ->
            if (!renderingState) {
                viewModel.setReminderEnabled(checked)
            }
        }
    }

    override fun initData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                viewModel.uiState.collect(::renderState)
            }
        }
    }

    private fun renderState(
        state: BudgetOverview
    ) {
        binding.budgetTotalAmount.text = formatMoney(state.totalBudgetInCents)
        binding.budgetUsedAmount.text = formatMoney(state.usedInCents)
        binding.budgetRemainingAmount.text = formatMoney(state.remainingInCents)

        binding.budgetStatusText.text =
            if (state.remainingInCents >= 0) {
                "预算使用正常"
            } else {
                "本月预算已超支"
            }

        renderingState = true
        binding.budgetReminderSwitch.isChecked = state.reminderEnabled
        renderingState = false
        renderCategory(
            state,
            "餐饮",
            binding.budgetFoodAmount,
            binding.budgetFoodRatio,
            progresssView = binding.budgetFoodProgress
        )

        renderCategory(
            state,
            "交通",
            binding.budgetTrafficAmount,
            binding.budgetTrafficRatio,
            progresssView = binding.budgetTrafficProgress
        )

        renderCategory(
            state,
            "购物",
            binding.budgetShoppingAmount,
            binding.budgetShoppingRatio,
            progresssView = binding.budgetShoppingProgress
        )
        renderChart(state)
    }

    private fun renderCategory(
        state: BudgetOverview,
        name: String,
        amountView: android.widget.TextView,
        ratioView: android.widget.TextView,
        progresssView: LinearProgressIndicator
    ) {
        val category = state.categories
            .firstOrNull { it.name == name }
            ?: return
        amountView.text = buildString {
            append(formatMoney(category.usedInCents))
            append("/")
            append(formatMoney(category.budgetInCents))
        }

        val progress = category.usagePercent.coerceIn(0, 100)
        ratioView.text = "${category.usagePercent}%"

        progresssView.setProgressCompat(
            progress,
            true
        )
    }

    private fun renderChart(
        state: BudgetOverview
    ) {
        binding.budgetChart.render(
            model = DonutChartModel(
                slices = listOf(
                    DonutSlice(
                        label = "已使用",
                        value = state.usedInCents.toFloat(),
                        color = ContextCompat.getColor(
                            requireContext(),
                            R.color.budget_page_primary
                        )
                    ),
                    DonutSlice(
                        label = "剩余",
                        value = state.remainingInCents
                            .coerceAtLeast(0)
                            .toFloat(),
                        color = ContextCompat.getColor(
                            requireContext(),
                            R.color.budget_page_track
                        )
                    )
                ),
                centerText = "${state.usagePercent}%",
                centerSubText = "预算使用"
            ),
            config = DonutChartConfig(
                showLegend = false,
                interactive = false,
                drawValues = false,
                animationDurationMs = 300
            )
        )
    }

    private fun showSetBudgetDialog() {
        val dialogBinding = BudgetDialogSetBinding.inflate(
            layoutInflater
        )
        val state = viewModel.uiState.value

        dialogBinding.budgetDialogTotal.setText(
            amountText(state.totalBudgetInCents)
        )

        dialogBinding.budgetDialogFood.setText(
            categoryBudget(state, "餐饮")
        )

        dialogBinding.budgetDialogTraffic.setText(
            categoryBudget(state, "交通")
        )

        dialogBinding.budgetDialogShopping.setText(
            categoryBudget(state, "购物")
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("设置预算")
            .setView(dialogBinding.root)
            .setNegativeButton(
                android.R.string.cancel,
                null
            )
            .setPositiveButton("保存") { _,_ ->
                viewModel.saveBudget(
                    totalInCents =
                        dialogBinding.budgetDialogTotal
                            .text.toString()
                            .toCents(),
                    foodInCents =
                        dialogBinding.budgetDialogFood
                            .text.toString()
                            .toCents(),
                    trafficInCents =
                        dialogBinding.budgetDialogTraffic
                            .text.toString()
                            .toCents(),
                    shoppingInCents =
                        dialogBinding.budgetDialogShopping
                            .text.toString()
                            .toCents()
                )
            }
            .show()
    }

    private fun categoryBudget(
        state: BudgetOverview,
        name: String
    ): String {
        return amountText(
            state.categories
                .firstOrNull{it.name == name}
                ?.budgetInCents
                ?: 0L
        )
    }

    private fun String.toCents(): Long {
        return toBigDecimal()
            .movePointRight(2)
            .toLong()
    }

    private fun amountText(
        amountInCents: Long
    ): String {
        return BigDecimal.valueOf(
            amountInCents,
            2
        ).stripTrailingZeros().toPlainString()
    }

    private fun formatMoney(
        amountInCents: Long
    ): String {
        return "¥${MONEY_FORMAT.format(
            BigDecimal.valueOf(
                amountInCents,
                2
            )
        )}"
    }

    companion object {
        private val MONEY_FORMAT = DecimalFormat("#,##0.00")
    }
}
