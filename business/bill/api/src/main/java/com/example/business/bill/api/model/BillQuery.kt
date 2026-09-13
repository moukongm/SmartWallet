package com.example.business.bill.api.model

import com.example.business.bill.api.BillRecordType

data class BillQuery(
    val startAt: Long? = null,
    val endAtExclusive: Long? = null,
    val type: BillRecordType? = null,
    val category: String? = null,
    val keyword: String? = null,
    val limit: Int? = null,
    val offset: Int = 0
)
