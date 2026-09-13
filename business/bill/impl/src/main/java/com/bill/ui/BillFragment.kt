package com.bill.ui

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bill.viewmodel.BillViewModel
import com.example.business.bill.api.BillRecordType
import com.example.business.bill.impl.R
import com.example.business.bill.impl.databinding.BillFragmentBinding
import com.example.common.base.BaseFragment
import java.text.DecimalFormat
import java.util.Calendar
import kotlinx.coroutines.launch

class BillFragment : BaseFragment<BillFragmentBinding>() {

    private val viewModel: BillViewModel by lazy {
        ViewModelProvider(this, BillViewModel.Factory())[BillViewModel::class.java]
    }
    private val billAdapter = BillListAdapter { bill -> openBillEditor(bill.id) }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): BillFragmentBinding {
        val preloadedView = takePreloadedLayout(R.layout.bill_fragment)

        if (preloadedView == null) {
            return BillFragmentBinding.inflate(
                inflater,
                container,
                false
            )
        }

        if (preloadedView.layoutParams == null) {
            preloadedView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        return BillFragmentBinding.bind(
            preloadedView
        )
    }

    override fun initView() {
        binding.billPageList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = billAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy <= 0) return
                    val manager = recyclerView.layoutManager as LinearLayoutManager
                    if (manager.findLastVisibleItemPosition() >= billAdapter.itemCount - 4) {
                        viewModel.loadNextPage()
                    }
                }
            })
        }
        binding.billPageSearchInput.doAfterTextChanged {
            viewModel.search(it?.toString().orEmpty())
        }
        binding.billPageFilterMonth.setOnClickListener { showMonthPicker() }
        binding.billPageFilterCategory.setOnClickListener { showCategoryMenu() }
        binding.billPageFilterType.setOnClickListener { showTypeMenu() }
        binding.billPageMoreButton.setOnClickListener { showMoreMenu() }
    }

    override fun initData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun render(state: BillViewModel.BillUiState) {
        binding.billPageMonthValue.text = "${state.filter.year}年${state.filter.month + 1}月"
        binding.billPageFilterMonth.text = if (isCurrentMonth(state.filter)) {
            getString(R.string.bill_page_filter_month)
        } else {
            "${state.filter.month + 1}月"
        }
        binding.billPageFilterCategory.text = state.filter.category
            ?: getString(R.string.bill_page_filter_category)
        binding.billPageFilterType.text = when (state.filter.type) {
            BillRecordType.EXPENSE -> "支出"
            BillRecordType.INCOME -> "收入"
            null -> getString(R.string.bill_page_filter_type)
        }
        binding.billPageIncomeAmountValue.text = formatAmount(state.summary.incomeInCents)
        binding.billPageExpenseAmountValue.text = formatAmount(state.summary.expenseInCents)
        billAdapter.submitList(state.bills)
        binding.billPageEmpty.visibility =
            if (!state.isLoading && state.bills.isEmpty()) View.VISIBLE else View.GONE
        binding.billPageLoadingMore.visibility =
            if (state.isLoading || state.isLoadingMore) View.VISIBLE else View.GONE
        state.errorMessage?.let {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            viewModel.consumeError()
        }
    }

    private fun showMonthPicker() {
        val filter = viewModel.uiState.value.filter
        DatePickerDialog(
            requireContext(),
            { _, year, month, _ -> viewModel.selectMonth(year, month) },
            filter.year,
            filter.month,
            1
        ).show()
    }

    private fun showCategoryMenu() {
        PopupMenu(requireContext(), binding.billPageFilterCategory).apply {
            menu.add("全部分类").setOnMenuItemClickListener {
                viewModel.selectCategory(null)
                true
            }
            viewModel.uiState.value.categories.forEach { category ->
                menu.add(category.name).setOnMenuItemClickListener {
                    viewModel.selectCategory(category.name)
                    true
                }
            }
            show()
        }
    }

    private fun showTypeMenu() {
        PopupMenu(requireContext(), binding.billPageFilterType).apply {
            menu.add("全部收支").setOnMenuItemClickListener {
                viewModel.selectType(null)
                true
            }
            menu.add("支出").setOnMenuItemClickListener {
                viewModel.selectType(BillRecordType.EXPENSE)
                true
            }
            menu.add("收入").setOnMenuItemClickListener {
                viewModel.selectType(BillRecordType.INCOME)
                true
            }
            show()
        }
    }

    private fun showMoreMenu() {
        PopupMenu(requireContext(), binding.billPageMoreButton).apply {
            menu.add("刷新").setOnMenuItemClickListener {
                viewModel.refresh()
                true
            }
            menu.add("重置筛选").setOnMenuItemClickListener {
                binding.billPageSearchInput.setText("")
                viewModel.resetFilters()
                true
            }
            show()
        }
    }

    private fun openBillEditor(billId: Long) {
        findNavController().navigate(
            NavDeepLinkRequest.Builder
                .fromUri("smartwallet://bill/edit/$billId".toUri())
                .build()
        )
    }

    private fun isCurrentMonth(filter: BillViewModel.BillFilter): Boolean {
        val now = Calendar.getInstance()
        return filter.year == now.get(Calendar.YEAR) &&
            filter.month == now.get(Calendar.MONTH)
    }

    private fun formatAmount(value: Long) = "¥${MONEY_FORMAT.format(value / 100.0)}"

    companion object {
        private val MONEY_FORMAT = DecimalFormat("#,##0.00")
    }
}
