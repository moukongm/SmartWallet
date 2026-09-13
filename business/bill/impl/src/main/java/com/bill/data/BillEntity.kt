package com.bill.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val type: String,
    val amountInCents: Long,
    val category: String,
    val note: String,
    val occurredAt: Long,
    val imageUri: String?,
    val createdAt: Long = System.currentTimeMillis()
)
