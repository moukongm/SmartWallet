package com.bill.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class BillRepository private constructor(
    private val billDao: BillDao
) {

    suspend fun addBill(bill: BillEntity): Long {
        return billDao.insert(bill)
    }

    fun observeBills(userId: String): Flow<List<BillEntity>> {
        return billDao.observeBills(userId)
    }

    suspend fun getBills(
        userId: String,
        startAt: Long?,
        endAtExclusive: Long?,
        type: String?,
        category: String?,
        keyword: String?,
        limit: Int,
        offset: Int
    ): List<BillEntity> {
        return billDao.getBills(
            userId = userId,
            startAt = startAt,
            endAtExclusive = endAtExclusive,
            type = type,
            category = category,
            keyword = keyword,
            limit = limit,
            offset = offset
        )
    }

    fun observeBills(
        userId: String,
        startAt: Long?,
        endAtExclusive: Long?,
        type: String?,
        category: String?,
        keyword: String?,
        limit: Int,
        offset: Int
    ): Flow<List<BillEntity>> {
        return billDao.observeBills(
            userId = userId,
            startAt = startAt,
            endAtExclusive = endAtExclusive,
            type = type,
            category = category,
            keyword = keyword,
            limit = limit,
            offset = offset
        )
    }

    suspend fun getBillById(
        billId: Long,
        userId: String
    ): BillEntity? {
        return billDao.getBillById(billId, userId)
    }

    suspend fun updateBill(bill: BillEntity) {
        val affectedRows = billDao.update(bill)
        check(affectedRows > 0) {
            "账单不存在或已被删除"
        }
    }

    suspend fun deleteBill(
        billId: Long,
        userId: String
    ) {
        val affectedRows = billDao.deleteById(billId, userId)
        check(affectedRows > 0) {
            "账单不存在或已被删除"
        }
    }

    suspend fun deleteBillIfExists(
        billId: Long,
        userId: String
    ): Boolean {
        return billDao.deleteById(billId, userId) > 0
    }

    suspend fun getSummary(
        userId: String,
        startAt: Long,
        endAtExclusive: Long
    ): BillSummaryRow {
        return billDao.getSummary(userId, startAt, endAtExclusive)
    }

    companion object {
        @Volatile
        private var instance: BillRepository? = null

        fun getInstance(context: Context): BillRepository {
            return instance ?: synchronized(this) {
                instance ?: BillRepository(
                    BillDatabase.getInstance(context).billDao()
                ).also { instance = it }
            }
        }
    }
}
