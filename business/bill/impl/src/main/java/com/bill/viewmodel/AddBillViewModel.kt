package com.bill.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alibaba.android.arouter.launcher.ARouter
import com.bill.data.BillEntity
import com.bill.data.BillRepository
import com.bill.data.BillType
import com.example.business.bill.api.BillApiRoutes
import com.example.business.bill.api.BillRecordType
import com.example.business.bill.api.CategoryDataService
import com.example.business.bill.api.model.BillCategoryInfo
import com.example.common.base.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddBillViewModel(
    private val repository: BillRepository,
    private val categoryService: CategoryDataService
) : BaseViewModel() {

    sealed interface EditorState {
        data object Idle : EditorState
        data object Loading : EditorState
        data class Loaded(val bill: BillEntity) : EditorState
        data object Saving : EditorState
        data class Saved(val billId: Long) : EditorState
        data object Deleting : EditorState
        data object Deleted : EditorState
        data class Error(val message: String) : EditorState
    }

    data class BillInput(
        val userId: String,
        val type: String,
        val amountInCents: Long,
        val category: String,
        val note: String,
        val occurredAt: Long,
        val imageUri: String?
    )

    private val _editorState = MutableStateFlow<EditorState>(EditorState.Idle)
    val saveState: StateFlow<EditorState> = _editorState.asStateFlow()
    private var loadedBill: BillEntity? = null

    private val selectedCategoryType = MutableStateFlow(BillRecordType.EXPENSE)

    val categories: StateFlow<List<BillCategoryInfo>> =
        selectedCategoryType.flatMapLatest {
            type -> categoryService.observeCategories(type)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun selectBillType(type: String) {
        selectedCategoryType.value =
            if (type == BillType.INCOME) {
                BillRecordType.INCOME
            } else {
                BillRecordType.EXPENSE
            }
    }

    fun loadBill(
        billId: Long,
        userId: String
    ) {
        if (loadedBill?.id == billId) return
        viewModelScope.launch {
            _editorState.value = EditorState.Loading

            try {
                val bill = repository.getBillById(
                    billId = billId,
                    userId = userId
                ) ?: throw IllegalStateException("账单不存在或已被删除")
                loadedBill = bill
                _editorState.value = EditorState.Loaded(bill)
            } catch (exception: CancellationException) {
                throw  exception
            } catch (exception: Exception) {
                _editorState.value = EditorState.Error(
                    exception.message ?: "读取账单失败"
                )
            }
        }
    }

    fun save(
        billId: Long?,
        input: BillInput
    ) {
        if (
            _editorState.value == EditorState.Saving ||
            _editorState.value == EditorState.Deleting
        ) {
            return
        }

        viewModelScope.launch {
            _editorState.value = EditorState.Saving
            try {
                val savedId = if (billId == null) {
                    repository.addBill(
                        BillEntity(
                            userId = input.userId,
                            type = input.type,
                            amountInCents = input.amountInCents,
                            category = input.category,
                            note = input.note,
                            occurredAt = input.occurredAt,
                            imageUri =  input.imageUri
                        )
                    )
                } else {
                    val original = loadedBill
                        ?: repository.getBillById(
                            billId = billId,
                            userId =  input.userId
                        )
                        ?: throw IllegalStateException("账单不存在或已被删除")
                    val updatedBill = original.copy(
                        type = input.type,
                        amountInCents = input.amountInCents,
                        category = input.category,
                        note = input.note,
                        occurredAt = input.occurredAt,
                        imageUri = input.imageUri
                    )

                    repository.updateBill(updatedBill)
                    loadedBill = updatedBill
                    billId
                }
                _editorState.value = EditorState.Saved(savedId)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _editorState.value = EditorState.Error(
                    exception.message ?: "保存账单失败，请稍后重试"
                )
            }
        }
    }

    fun delete(
        billId: Long,
        userId: String
    ) {
        if (
            _editorState.value == EditorState.Saving ||
            _editorState.value == EditorState.Deleting
        ) {
            return
        }

        viewModelScope.launch {
            _editorState.value = EditorState.Deleting
            try {
                repository.deleteBill(
                    billId = billId,
                    userId = userId
                )
                loadedBill = null
                _editorState.value = EditorState.Deleted
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _editorState.value = EditorState.Error(
                    exception.message ?: "删除账单失败，请稍后重试"
                )
            }
        }
    }

    fun consumeSaveState() {
        _editorState.value = EditorState.Idle
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val repository = BillRepository.getInstance(context.applicationContext)
        private val categoryService = ARouter.getInstance()
            .build(BillApiRoutes.CATEGORY_DATA_SERVICE)
            .navigation() as CategoryDataService

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AddBillViewModel::class.java))
            return AddBillViewModel(repository = repository,
                categoryService = categoryService
            ) as T
        }
    }
}
