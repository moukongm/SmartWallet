package com.example.business.bill.api

enum class BillRecordType(
    val value: String
) {
    EXPENSE("expense"),
    INCOME("income");

    companion object {
        fun fromValue(value: String): BillRecordType {
            return entries.firstOrNull { type ->
                type.value == value
            } ?: throw IllegalArgumentException(
                "未知的账单类型：$value"
            )
        }
    }
}
