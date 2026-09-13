package com.bill.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bill_categories",
    indices = [
        Index(
            value = ["userId", "type", "name"],
            unique = true
        )
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val type: String,
    val name: String,
    val iconKey: String,
    val sortOrder: Int
)
