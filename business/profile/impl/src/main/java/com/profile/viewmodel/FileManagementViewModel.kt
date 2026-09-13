package com.profile.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alibaba.android.arouter.launcher.ARouter
import com.example.business.bill.api.BillApiRoutes
import com.example.business.bill.api.BillDataService
import com.example.business.bill.api.model.Bill
import com.example.business.bill.api.model.BillQuery
import com.example.business.bill.api.model.BillSaveRequest
import com.example.common.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FileManagementViewModel(
    private val billService: BillDataService
) : BaseViewModel() {

    data class UiState(
        val files: List<Bill> = emptyList(),
        val loading: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val files = billService.getBills(BillQuery())
                .filter { !it.imageUri.isNullOrBlank() }
            _uiState.value = UiState(files = files)
        }
    }

    fun removeAttachment(bill: Bill) {
        viewModelScope.launch {
            billService.updateBill(
                bill.id,
                BillSaveRequest(
                    type = bill.type,
                    amountInCents = bill.amountInCents,
                    category = bill.category,
                    note = bill.note,
                    occurredAt = bill.occurredAt,
                    imageUri = null
                )
            )
            loadFiles()
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(FileManagementViewModel::class.java))
            val service = ARouter.getInstance()
                .build(BillApiRoutes.BILL_DATA_SERVICE)
                .navigation() as? BillDataService
            requireNotNull(service) {
                "无法获取 BillDataService，请检查 bill:impl 是否已打包"
            }
            return FileManagementViewModel(service) as T
        }
    }
}
