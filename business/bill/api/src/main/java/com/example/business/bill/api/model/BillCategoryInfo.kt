package com.example.business.bill.api.model

import com.example.business.bill.api.BillRecordType

data class BillCategoryInfo(
    val id: Long,
    val name: String,
    val type: BillRecordType,
    val iconKey: String,
    val sortOrder: Int
)
