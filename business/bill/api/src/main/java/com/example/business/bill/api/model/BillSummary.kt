package com.example.business.bill.api.model

data class BillSummary(
    val incomeInCents: Long,
    val expenseInCents: Long,
    val balanceInCents: Long
)
