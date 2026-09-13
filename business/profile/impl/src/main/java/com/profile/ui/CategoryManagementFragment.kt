package com.profile.ui

import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.business.bill.api.BillRecordType
import com.example.business.bill.api.model.BillCategoryInfo
import com.example.business.profile.impl.R
import com.example.business.profile.impl.databinding.ProfileFragmentCategoryManagementBinding
import com.example.common.base.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.profile.viewmodel.CategoryManagementViewModel
import kotlinx.coroutines.launch

class CategoryManagementFragment :
    BaseFragment<ProfileFragmentCategoryManagementBinding>() {

    private val viewModel: CategoryManagementViewModel by lazy {
        ViewModelProvider(this)[CategoryManagementViewModel::class.java]
    }

    private val categoryAdapter = CategoryAdapter(
        onEdit = ::showEditDialog
    )

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): ProfileFragmentCategoryManagementBinding {
        return ProfileFragmentCategoryManagementBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        binding.profileCategoryList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
        binding.profileCategoryExpenseTab.setOnClickListener {
            viewModel.selectType(BillRecordType.EXPENSE)
        }
        binding.profileCategoryIncomeTab.setOnClickListener {
            viewModel.selectType(BillRecordType.INCOME)
        }
        binding.profileCategoryAddButton.setOnClickListener {
            showAddDialog()
        }

        attachDragHelper()
    }

    override fun initData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                launch {
                    viewModel.uiState.collect { state ->
                        categoryAdapter.submitList(state.categories )
                        renderTabs(state.selectedType)
                    }
                }
                launch {
                    viewModel.messages.collect {
                        message ->
                        Toast.makeText(
                            requireContext(),
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun renderTabs(type: BillRecordType) {
        val expenseSeletcted = type == BillRecordType.EXPENSE
        binding.profileCategoryExpenseTab.setBackgroundResource(
            if (expenseSeletcted) {
                R.drawable.profile_category_bg_tab_selected
            } else {
                R.drawable.profile_category_bg_tab
            }
        )

        binding.profileCategoryIncomeTab.setBackgroundResource(
            if (expenseSeletcted) {
                R.drawable.profile_category_bg_tab
            } else {
                R.drawable.profile_category_bg_tab_selected
            }
        )

        val primary = ContextCompat.getColor(
            requireContext(),
            R.color.profile_category_text_primary
        )

        val normal = ContextCompat.getColor(
            requireContext(),
            R.color.profile_category_primary
        )

        binding.profileCategoryExpenseTab.setTextColor(
            if (expenseSeletcted) primary else normal
        )

        binding.profileCategoryIncomeTab.setTextColor(
            if (expenseSeletcted) normal else primary
        )
    }

    private fun showAddDialog() {
        showNameDialog(
            title = "新增分类",
            initialName = "",
            onSave = viewModel::addCategory
        )
    }

    private fun showEditDialog(category: BillCategoryInfo) {
        val input = createNameInput(category.name)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑分类")
            .setView(input)
            .setNegativeButton("取消", null)
            .setNeutralButton("删除") {
                _,_ ->
                showDeleteConfirmation(category)
            }
            .setPositiveButton("保存") {
                _,_ ->
                viewModel.updateCategory(
                    category = category,
                    name = input.text.toString()
                )
            }
            .show()
    }

    private fun showNameDialog(
        title: String,
        initialName: String,
        onSave: (String) -> Unit
    ) {
        val input = createNameInput(initialName)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") {
                _,_ ->
                onSave(input.text.toString())
            }
            .show()
    }

    private fun createNameInput(initialName: String): EditText {
        return EditText(requireContext()).apply {
            hint = "请输入分类名称"
            setText(initialName)
            setSelection(text.length)
            setSingleLine(true)
            val horizontalPadding = (
                    24 * resources.displayMetrics.density
                    ).toInt()
            setPadding(
                horizontalPadding,
                paddingTop,
                horizontalPadding,
                paddingBottom
            )
        }
    }
    private fun showDeleteConfirmation(
        category: BillCategoryInfo
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除分类")
            .setMessage("确定删除“${category.name}”分类吗？已有账单不会被删除。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteCategory(category.id)
            }
            .show()
    }

    private fun attachDragHelper() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                categoryAdapter.moveItem(
                    viewHolder.bindingAdapterPosition,
                    target.bindingAdapterPosition
                )
                return true
            }

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) = Unit

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                viewModel.saveOrder(categoryAdapter.currentIds())
            }

            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                viewHolder.itemView.alpha =
                    if (isCurrentlyActive) 0.85f else 1f
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(
            binding.profileCategoryList
        )
    }

}
