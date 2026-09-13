package com.example.business.bill.api

import com.alibaba.android.arouter.facade.template.IProvider
import com.example.business.bill.api.model.BillCategoryInfo
import kotlinx.coroutines.flow.Flow

interface CategoryDataService : IProvider {
    fun observeCategories(
        type: BillRecordType
    ): Flow<List<BillCategoryInfo>>

    suspend fun createCategory(
        type: BillRecordType,
        name: String,
        iconKey: String = CategoryIconKey.OTHER
    ): BillCategoryInfo

    suspend fun updateCategory(
        categoryId: Long,
        name: String,
        iconKey: String
    ): BillCategoryInfo

    suspend fun deleteCategory(
        categoryId: Long
    ): Boolean

    suspend fun reorderCategories(
        type: BillRecordType,
        categoryIds: List<Long>
    )

    object CategoryIconKey {
        const val FOOD = "food"
        const val TRAFFIC = "traffic"
        const val SHOPPING = "shopping"
        const val ENTERTAINMENT = "entertainment"
        const val HOUSING = "housing"
        const val MEDICAL = "medical"
        const val STUDY = "study"
        const val SALARY = "salary"
        const val BONUS = "bonus"
        const val INVESTMENT = "investment"
        const val TRANSFER = "transfer"
        const val OTHER = "other"
    }
}
