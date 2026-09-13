package com.bill.data

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.example.business.bill.api.BillApiRoutes
import com.example.business.bill.api.BillDataService
import com.example.business.bill.api.model.Bill
import com.example.business.bill.api.model.BillQuery
import com.example.business.bill.api.model.BillSaveRequest
import com.example.business.bill.api.model.BillSummary
import com.example.common.router.RouterPath
import com.profile.api.AuthService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Route(path = BillApiRoutes.BILL_DATA_SERVICE)
class BillDataServiceImpl : BillDataService {

    private lateinit var repository: BillRepository

    override fun init(context: Context?) {
        repository = BillRepository.getInstance(
            requireNotNull(context) { "BillDataService 初始化失败：Context 为空" }
                .applicationContext
        )
    }

    override suspend fun createBill(request: BillSaveRequest): Bill {
        validateRequest(request)
        val userId = requireUserId()
        val billId = repository.addBill(request.toEntity(userId))
        return requireNotNull(repository.getBillById(billId, userId)) {
            "账单创建成功，但读取账单失败"
        }.toApiModel()
    }

    override suspend fun getBillById(billId: Long): Bill? {
        require(billId > 0) { "账单ID必须大于0" }
        return repository.getBillById(billId, requireUserId())?.toApiModel()
    }

    override suspend fun getBills(query: BillQuery): List<Bill> {
        val normalized = query.normalized()
        return repository.getBills(
            userId = requireUserId(),
            startAt = normalized.startAt,
            endAtExclusive = normalized.endAtExclusive,
            type = normalized.type?.value,
            category = normalized.category,
            keyword = normalized.keyword,
            limit = normalized.limit ?: Int.MAX_VALUE,
            offset = normalized.offset
        ).map(BillEntity::toApiModel)
    }

    override fun observeBills(query: BillQuery): Flow<List<Bill>> {
        val normalized = query.normalized()
        return repository.observeBills(
            userId = requireUserId(),
            startAt = normalized.startAt,
            endAtExclusive = normalized.endAtExclusive,
            type = normalized.type?.value,
            category = normalized.category,
            keyword = normalized.keyword,
            limit = normalized.limit ?: Int.MAX_VALUE,
            offset = normalized.offset
        ).map { bills -> bills.map(BillEntity::toApiModel) }
    }

    override suspend fun updateBill(
        billId: Long,
        request: BillSaveRequest
    ): Bill {
        require(billId > 0) { "账单ID必须大于0" }
        validateRequest(request)
        val userId = requireUserId()
        val original = repository.getBillById(billId, userId)
            ?: throw NoSuchElementException("账单不存在或已被删除")
        val updated = original.copy(
            type = request.type.value,
            amountInCents = request.amountInCents,
            category = request.category.trim(),
            note = request.note.trim(),
            occurredAt = request.occurredAt,
            imageUri = request.imageUri
        )
        repository.updateBill(updated)
        return updated.toApiModel()
    }

    override suspend fun deleteBill(billId: Long): Boolean {
        require(billId > 0) { "账单ID必须大于0" }
        return repository.deleteBillIfExists(billId, requireUserId())
    }

    override suspend fun getSummary(
        startAt: Long,
        endAtExclusive: Long
    ): BillSummary {
        require(startAt < endAtExclusive) { "结束时间必须晚于开始时间" }
        val row = repository.getSummary(
            userId = requireUserId(),
            startAt = startAt,
            endAtExclusive = endAtExclusive
        )
        return BillSummary(
            incomeInCents = row.incomeInCents,
            expenseInCents = row.expenseInCents,
            balanceInCents = row.incomeInCents - row.expenseInCents
        )
    }

    private fun requireUserId(): String {
        val authService = ARouter.getInstance()
            .build(RouterPath.USER_AUTH_SERVICE)
            .navigation() as? AuthService
        return authService?.currentUserId()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("用户未登录")
    }

    private fun validateRequest(request: BillSaveRequest) {
        require(request.amountInCents > 0) { "账单金额必须大于0" }
        require(request.category.isNotBlank()) { "账单分类不能为空" }
        require(request.occurredAt >= 0) { "账单时间不合法" }
    }

    private fun BillQuery.normalized(): BillQuery {
        val queryLimit = limit
        val queryStartAt = startAt
        val queryEndAtExclusive = endAtExclusive
        require(offset >= 0) { "offset不能小于0" }
        require(queryLimit == null || queryLimit > 0) { "limit必须大于0" }
        if (queryStartAt != null && queryEndAtExclusive != null) {
            require(queryStartAt < queryEndAtExclusive) {
                "结束时间必须晚于开始时间"
            }
        }
        return copy(
            category = category?.trim()?.takeIf(String::isNotEmpty),
            keyword = keyword?.trim()?.takeIf(String::isNotEmpty)
        )
    }
}
