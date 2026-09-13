package com.example.business.bill.api.model

import com.example.business.bill.api.BillRecordType

data class Bill(
    val id: Long,
    val type: BillRecordType,
    val amountInCents: Long,
    val category: String,
    val note: String,
    val occurredAt: Long,
    val imageUri: String?,
    val createdAt: Long
)
