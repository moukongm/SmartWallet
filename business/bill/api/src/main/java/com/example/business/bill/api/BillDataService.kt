package com.example.business.bill.api

import com.alibaba.android.arouter.facade.template.IProvider
import com.example.business.bill.api.model.Bill
import com.example.business.bill.api.model.BillQuery
import com.example.business.bill.api.model.BillSaveRequest
import com.example.business.bill.api.model.BillSummary
import kotlinx.coroutines.flow.Flow

interface BillDataService : IProvider {

    suspend fun createBill(request: BillSaveRequest): Bill

    suspend fun getBillById(billId: Long): Bill?

    suspend fun getBills(query: BillQuery = BillQuery()): List<Bill>

    fun observeBills(query: BillQuery = BillQuery()): Flow<List<Bill>>

    suspend fun updateBill(
        billId: Long,
        request: BillSaveRequest
    ): Bill

    suspend fun deleteBill(billId: Long): Boolean

    suspend fun getSummary(
        startAt: Long,
        endAtExclusive: Long
    ): BillSummary
}
