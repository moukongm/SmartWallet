package com.example.storage

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

object RoomDatabaseFactory {

    fun <T : RoomDatabase> create(
        context: Context,
        databaseClass: Class<T>,
        databaseName: String
    ): T {
        return Room.databaseBuilder(
            context.applicationContext,
            databaseClass,
            databaseName
        ).build()
    }
}
