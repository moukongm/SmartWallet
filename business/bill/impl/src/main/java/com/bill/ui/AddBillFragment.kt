package com.bill.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.alibaba.android.arouter.launcher.ARouter
import com.bill.data.BillCategory
import com.bill.data.BillEntity
import com.bill.data.BillType
import com.bill.viewmodel.AddBillViewModel
import com.example.business.bill.api.model.BillCategoryInfo
import com.example.business.bill.impl.R
import com.example.business.bill.impl.databinding.BillFragmentAddBinding
import com.example.common.base.BaseFragment
import com.example.common.router.RouterPath
import com.google.android.material.button.MaterialButton
import com.profile.api.AuthService
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

class AddBillFragment : BaseFragment<BillFragmentAddBinding>() {

    private val viewModel: AddBillViewModel by lazy {
        ViewModelProvider(
            this,
            AddBillViewModel.Factory(requireContext())
        )[AddBillViewModel::class.java]
    }

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        persistImagePermission(uri)
        selectedImageUri = uri
        getBindingSafe()?.billAddImageValue?.apply {
            text = getString(R.string.bill_add_image_selected)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.bill_page_primary))
        }
    }

    private lateinit var categoryButtons: List<MaterialButton>
    private var selectedType = BillType.EXPENSE
    private var selectedCategory = ""
    private var selectedTime = System.currentTimeMillis()
    private var selectedImageUri: Uri? = null

    private val editingBillId: Long?
        get() = arguments
            ?.getLong(ARG_BILL_ID, NO_BILL_ID)
            ?.takeIf { it > 0 }

    private var renderedBillId: Long? = null

    companion object {
        const val ARG_BILL_ID = "billId"
        private const val NO_BILL_ID = -1L
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BillFragmentAddBinding {
        return BillFragmentAddBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        categoryButtons = listOf(
            binding.billAddCategory1,
            binding.billAddCategory2,
            binding.billAddCategory3,
            binding.billAddCategory4,
            binding.billAddCategory5,
            binding.billAddCategory6,
            binding.billAddCategory7,
            binding.billAddCategory8
        )

        binding.billAddBackButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.billAddTypeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedType = if (checkedId == binding.billAddIncomeButton.id) {
                BillType.INCOME
            } else {
                BillType.EXPENSE
            }
            viewModel.selectBillType(selectedType)
        }

        binding.billAddTimeValue.setOnClickListener {
            showDatePicker()
        }
        binding.billAddImageValue.setOnClickListener {
            imagePicker.launch(arrayOf("image/*"))
        }
        binding.billAddSaveButton.setOnClickListener {
            submitBill()
        }

        binding.billAddDeleteButton.visibility =
            if (editingBillId == null) View.GONE else View.VISIBLE

        binding.billAddDeleteButton.setOnClickListener {
            showDeleteConfirmation()
        }

        renderSelectedTime()
    }

    override fun initData() {
        val userId = currentUserId()
        if (editingBillId != null && !userId.isNullOrBlank()) {
            viewModel.loadBill(
                billId = editingBillId!!,
                userId = userId
            )
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.saveState.collect(
                        ::renderEditorState
                    )
                }
                launch {
                    viewModel.categories.collect(
                        ::renderCategories
                    )
                }
            }
        }
    }

    private fun renderCategories(
        categories: List<BillCategoryInfo>
    ) {
        categoryButtons.forEachIndexed { index, button ->
            val category = categories.getOrNull(index)

            button.visibility =
                if (category == null) {
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }

            if (category != null) {
                button.text = category.name

                button.setOnClickListener {
                    selectedCategory = category.name
                    renderCategorySelection(index)
                }
            }
        }

        val selectedIndex = categories
            .indexOfFirst {
                it.name == selectedCategory
            }
            .takeIf {
                it >= 0
            } ?: 0

        selectedCategory =
            categories.getOrNull(selectedIndex)?.name.orEmpty()

        if (categories.isNotEmpty()) {
            renderCategorySelection(selectedIndex)
        }
    }

    private fun renderCategorySelection(selectedIndex: Int) {
        val primary = ContextCompat.getColor(requireContext(), R.color.bill_page_primary)
        val primarySoft = ContextCompat.getColor(
            requireContext(),
            R.color.bill_page_primary_soft
        )
        val normalBackground = ContextCompat.getColor(
            requireContext(),
            R.color.bill_page_background
        )
        val normalText = ContextCompat.getColor(
            requireContext(),
            R.color.bill_page_text_primary
        )
        categoryButtons.forEachIndexed { index, button ->
            val selected = index == selectedIndex
            button.backgroundTintList = ColorStateList.valueOf(
                if (selected) primarySoft else normalBackground
            )
            button.strokeColor = ColorStateList.valueOf(
                if (selected) primary else ContextCompat.getColor(
                    requireContext(),
                    R.color.bill_page_border
                )
            )
            button.strokeWidth = if (selected) dp(2) else dp(1)
            button.setTextColor(if (selected) primary else normalText)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedTime }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                showTimePicker(year, month, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedTime }
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                selectedTime = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, hourOfDay, minute, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                renderSelectedTime()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun renderSelectedTime() {
        val formatted = SimpleDateFormat(
            "yyyy年M月d日 HH:mm",
            Locale.CHINA
        ).format(selectedTime)
        binding.billAddTimeValue.text = getString(
            R.string.bill_add_time_format,
            formatted
        )
    }

    private fun submitBill() {
        val amountInCents = parseAmountInCents(
            binding.billAddAmountInput.text?.toString().orEmpty()
        )
        if (amountInCents == null || amountInCents <= 0) {
            binding.billAddAmountInput.error = getString(
                R.string.bill_add_amount_required
            )
            binding.billAddAmountInput.requestFocus()
            return
        }

        val userId = currentUserId()
        if (userId.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                R.string.bill_add_login_required,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewModel.save(
            billId = editingBillId,
            input = AddBillViewModel.BillInput(
                userId = userId,
                type = selectedType,
                amountInCents = amountInCents,
                category = selectedCategory,
                note = binding.billAddNoteInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty(),
                occurredAt = selectedTime,
                imageUri = selectedImageUri?.toString()
            )
        )
    }

    private fun renderBill(bill: BillEntity) {
        if (renderedBillId == bill.id) return
        renderedBillId = bill.id

        selectedType = bill.type
        binding.billAddTypeGroup.check(
            if (bill.type == BillType.INCOME) {
                binding.billAddIncomeButton.id
            } else {
                binding.billAddExpenseButton.id
            }
        )

        selectedCategory = bill.category
        viewModel.selectBillType(bill.type)
        binding.billAddAmountInput.setText(
            java.math.BigDecimal.valueOf(
                bill.amountInCents,
                2
            ).stripTrailingZeros().toPlainString()
        )

        binding.billAddNoteInput.setText(bill.note)

        selectedTime = bill.occurredAt
        renderSelectedTime()
        selectedImageUri = bill.imageUri?.let(Uri::parse)

        if (selectedImageUri != null) {
            binding.billAddImageValue.text = getString(R.string.bill_add_image_selected)
        }

        binding.billAddTitle.text = getString(R.string.bill_edit_title)
        binding.billAddDeleteButton.visibility = View.VISIBLE
    }

    private fun renderEditorState(
        state: AddBillViewModel.EditorState
    ) {
        when(state) {
            AddBillViewModel.EditorState.Idle -> {
                setSaving(false)
            }
            AddBillViewModel.EditorState.Loading -> {
                setSaving(true)
            }
            is AddBillViewModel.EditorState.Loaded -> {
                setSaving(false)
                renderBill(state.bill)
            }
            AddBillViewModel.EditorState.Saving -> {
                setSaving(true)
            }
            is AddBillViewModel.EditorState.Saved -> {
                setSaving(false)
                viewModel.consumeSaveState()
                Toast.makeText(
                    requireContext(),
                    R.string.bill_add_success,
                    Toast.LENGTH_SHORT
                ).show()

                findNavController().popBackStack()
            }
            AddBillViewModel.EditorState.Deleting -> {
                setSaving(true)
            }
            AddBillViewModel.EditorState.Deleted -> {
                setSaving(false)
                viewModel.consumeSaveState()
                Toast.makeText(
                    requireContext(),
                    R.string.bill_delete_success,
                    Toast.LENGTH_SHORT
                ).show()

                findNavController().popBackStack()
            }

            is AddBillViewModel.EditorState.Error -> {
                setSaving(false)
                viewModel.consumeSaveState()
                Toast.makeText(
                    requireContext(),
                    state.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDeleteConfirmation() {
        val billId = editingBillId ?: return
        val userId = currentUserId() ?: return
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle(R.string.bill_delete_title)
            .setMessage(R.string.bill_delete_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.bill_delete_confirm) { _, _ ->
                viewModel.delete(
                    billId = billId,
                    userId = userId
                )
            }
            .show()
    }



    private fun parseAmountInCents(value: String): Long? {
        return try {
            value.trim()
                .toBigDecimal()
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    private fun currentUserId(): String? {
        val service = ARouter.getInstance()
            .build(RouterPath.USER_AUTH_SERVICE)
            .navigation() as? AuthService
        return service?.currentUserId()
    }

    private fun persistImagePermission(uri: Uri) {
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // 部分文档提供方不支持持久授权，当前会话仍可使用该 Uri。
        }
    }

    private fun setSaving(saving: Boolean) {
        binding.billAddSaveButton.isEnabled = !saving
        binding.billAddSaveButton.setText(
            if (saving) R.string.bill_add_saving else R.string.bill_add_save
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
