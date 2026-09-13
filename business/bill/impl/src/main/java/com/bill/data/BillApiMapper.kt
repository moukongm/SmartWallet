package com.bill.data

import com.example.business.bill.api.BillRecordType
import com.example.business.bill.api.model.Bill
import com.example.business.bill.api.model.BillSaveRequest

internal fun BillEntity.toApiModel(): Bill {
    return Bill(
        id = id,
        type = BillRecordType.fromValue(type),
        amountInCents = amountInCents,
        category = category,
        note = note,
        occurredAt = occurredAt,
        imageUri = imageUri,
        createdAt = createdAt
    )
}

internal fun BillSaveRequest.toEntity(userId: String): BillEntity {
    return BillEntity(
        userId = userId,
        type = type.value,
        amountInCents = amountInCents,
        category = category.trim(),
        note = note.trim(),
        occurredAt = occurredAt,
        imageUri = imageUri
    )
}
