package com.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.android.arouter.launcher.ARouter
import com.example.business.bill.api.BillApiRoutes
import com.example.business.bill.api.BillRecordType
import com.example.business.bill.api.CategoryDataService
import com.example.business.bill.api.model.BillCategoryInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryManagementViewModel : ViewModel() {
    private val service: CategoryDataService =
        ARouter.getInstance()
            .build(BillApiRoutes.CATEGORY_DATA_SERVICE)
            .navigation() as? CategoryDataService
            ?: error("CategoryDataService未注册")

    private val selectedType = MutableStateFlow(BillRecordType.EXPENSE)
    val uiState = selectedType
        .flatMapLatest { type ->
        service.observeCategories(type)
            .map { categories ->
                CategoryUiState(
                    selectedType = type,
                    categories = categories
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = CategoryUiState()
        )

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    fun selectType(type: BillRecordType) {
        selectedType.value = type
    }

    fun addCategory(name: String) {
        launchAction {
            service.createCategory(
                type = selectedType.value,
                name = name,
                iconKey = CategoryDataService.CategoryIconKey.OTHER
            )
        }
    }

    fun updateCategory(
        category: BillCategoryInfo,
        name: String
    ) {
        launchAction {
            service.updateCategory(
                categoryId = category.id,
                name = name,
                iconKey = category.iconKey
            )
        }
    }

    fun deleteCategory(categoryId: Long) {
        launchAction {
            service.deleteCategory(categoryId)
        }
    }

    fun saveOrder(categoryIds: List<Long>) {
        launchAction {
            service.reorderCategories(
                type = selectedType.value,
                categoryIds = categoryIds
            )
        }
    }

    private fun launchAction(
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                action()
            } catch (exception: Exception) {
                _messages.emit(
                    exception.message ?: "操作失败"
                )
            }
        }
    }

    data class CategoryUiState(
        val selectedType: BillRecordType = BillRecordType.EXPENSE,
        val categories: List<BillCategoryInfo> = emptyList()
    )
}
