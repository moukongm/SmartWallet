package com.bill.data

import androidx.room.withTransaction
import com.example.business.bill.api.BillRecordType
import com.example.business.bill.api.CategoryDataService
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val database: BillDatabase
) {
    private val categoryDao = database.categoryDao()
    private val billDao = database.billDao()

    fun observeCategories(
        userId: String,
        type: BillRecordType
    ): Flow<List<CategoryEntity>> {
        return categoryDao.observeCategories(
            userId = userId,
            type = type.value
        )
    }

    suspend fun ensureDefaults(
        userId: String,
        type: BillRecordType
    ) {
        database.withTransaction {
            if (categoryDao.count(userId, type.value) > 0) {
                return@withTransaction
            }

            val defaults = defaultCategories(type).mapIndexed { index, item ->
                CategoryEntity(
                    userId = userId,
                    type = type.value,
                    name = item.first,
                    iconKey = item.second,
                    sortOrder = index
                )
            }
            categoryDao.insertAll(defaults)
        }
    }

    suspend fun create(
        userId: String,
        type: BillRecordType,
        name: String,
        iconKey: String
    ): CategoryEntity {
        ensureNameAvailable(
            userId = userId,
            type = type,
            name = name,
            ignoredId = null
        )
        val order = (
                categoryDao.maxSortOrder(userId, type.value) ?: -1
                ) + 1
        val id = categoryDao.insert(
            CategoryEntity(
                userId = userId,
                type = type.value,
                name = name,
                iconKey = iconKey,
                sortOrder = order
            )
        )
        return requireNotNull(categoryDao.getById(id, userId))
    }

    suspend fun update(
        userId: String,
        categoryId: Long,
        name: String,
        iconKey: String
    ): CategoryEntity {
        return database.withTransaction {
            val original = categoryDao.getById(categoryId, userId) ?: throw NoSuchElementException("分类不存在")
            val type = BillRecordType.fromValue(original.type)

            ensureNameAvailable(
                userId = userId,
                type = type,
                name = name,
                ignoredId = categoryId
            )

            val updated = original.copy(
                name = name,
                iconKey = iconKey
            )

            check(categoryDao.update(updated) > 0) {
                "分类更新失败"
            }

            if (original.name != updated.name) {
                billDao.renameCategoryInBills(
                    userId = userId,
                    type = original.type,
                    oldName = original.name,
                    newName = updated.name
                )
            }

            updated
        }
    }

    suspend fun delete(
        userId: String,
        categoryId: Long
    ): Boolean {
        return database.withTransaction {
            val category = categoryDao.getById(categoryId, userId)
                ?: return@withTransaction false
            require(
                categoryDao.count(userId, category.type) > 1
            ) {
                "每种账单类型至少保留一个分类"
            }
            categoryDao.delete(categoryId, userId) > 0
        }
    }

    suspend fun reorder(
        userId: String,
        type: BillRecordType,
        categoryIds: List<Long>
    ) {
        database.withTransaction {
            categoryIds.forEachIndexed { index, categoryId ->
                categoryDao.updateSortOrder(
                    categoryId = categoryId,
                    userId = userId,
                    sortOrder = index
                )
            }
        }
    }

    private suspend fun ensureNameAvailable(
        userId: String,
        type: BillRecordType,
        name: String,
        ignoredId: Long?
    ) {
        val existing = categoryDao.getByName(
            userId = userId,
            type = type.value,
            name = name
        )

        require(existing == null || existing.id == ignoredId) {
            "分类“$name”已经存在"
        }
    }

    private fun defaultCategories(
        type: BillRecordType
    ): List<Pair<String, String>> {
        return when (type) {
            BillRecordType.EXPENSE -> listOf(
                "餐饮" to CategoryDataService.CategoryIconKey.FOOD,
                "交通" to CategoryDataService.CategoryIconKey.TRAFFIC,
                "购物" to CategoryDataService.CategoryIconKey.SHOPPING,
                "娱乐" to CategoryDataService.CategoryIconKey.ENTERTAINMENT,
                "住房" to CategoryDataService.CategoryIconKey.HOUSING,
                "医疗" to CategoryDataService.CategoryIconKey.MEDICAL,
                "学习" to CategoryDataService.CategoryIconKey.STUDY,
                "其他" to CategoryDataService.CategoryIconKey.OTHER
            )

            BillRecordType.INCOME -> listOf(
                "工资" to CategoryDataService.CategoryIconKey.SALARY,
                "奖金" to CategoryDataService.CategoryIconKey.BONUS,
                "理财" to CategoryDataService.CategoryIconKey.INVESTMENT,
                "转账" to CategoryDataService.CategoryIconKey.TRANSFER,
                "其他" to CategoryDataService.CategoryIconKey.OTHER
            )
        }
    }
}
