package com.bill.data

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.example.business.bill.api.BillApiRoutes
import com.example.business.bill.api.BillRecordType
import com.example.business.bill.api.CategoryDataService
import com.example.business.bill.api.model.BillCategoryInfo
import com.example.common.router.RouterPath
import com.profile.api.AuthService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Route(path = BillApiRoutes.CATEGORY_DATA_SERVICE)
class CategoryDataServiceImpl : CategoryDataService {
    private lateinit var repository: CategoryRepository

    override fun init(context: Context?) {
        val appContext = requireNotNull(context) {
            "CategoryDataService初始化失败：Context为空"
        }.applicationContext

        repository = CategoryRepository(
            BillDatabase.getInstance(appContext)
        )
    }

    override fun observeCategories(
        type: BillRecordType
    ): Flow<List<BillCategoryInfo>> = flow {
        val userId = requireUserId()
        repository.ensureDefaults(userId, type)
        emitAll(
            repository.observeCategories(userId, type)
                .map { categories ->
                    categories.map { entity ->
                        entity.toApiModel()
                    }
                }
        )
    }

    override suspend fun createCategory(
        type: BillRecordType,
        name: String,
        iconKey: String
    ): BillCategoryInfo {
        val normalizedName = validateName(name)
        return repository.create(
            userId = requireUserId(),
            type = type,
            name = normalizedName,
            iconKey = iconKey
        ).toApiModel()
    }

    override suspend fun updateCategory(
        categoryId: Long,
        name: String,
        iconKey: String
    ): BillCategoryInfo {
        require(categoryId > 0) { "分类ID不合法" }
        return repository.update(
            userId = requireUserId(),
            categoryId = categoryId,
            name = validateName(name),
            iconKey = iconKey
        ).toApiModel()
    }

    override suspend fun deleteCategory(
        categoryId: Long
    ): Boolean {
        require(categoryId > 0) {"分类ID不合法"}
        return repository.delete(
            userId = requireUserId(),
            categoryId = categoryId
        )
    }

    override suspend fun reorderCategories(
        type: BillRecordType,
        categoryIds: List<Long>
    ) {
        require(categoryIds.distinct().size == categoryIds.size) {
            "分类排序数据存在重复ID"
        }
        repository.reorder(
            userId = requireUserId(),
            type = type,
            categoryIds = categoryIds
        )
    }

    private fun validateName(name: String): String {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) {
            "分类名称不能为空"
        }
        require(normalized.length <= 10) {
            "分类名称不能超过10个字符"
        }
        return normalized
    }

    private fun requireUserId(): String {
        val authService = ARouter.getInstance()
            .build(RouterPath.USER_AUTH_SERVICE)
            .navigation() as? AuthService
        return authService
            ?.currentUserId()
            ?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("用户未登录")
    }

    private fun CategoryEntity.toApiModel(): BillCategoryInfo {
        return BillCategoryInfo(
            id = id,
            name = name,
            type = BillRecordType.fromValue(type),
            iconKey = iconKey,
            sortOrder = sortOrder
        )
    }
}
