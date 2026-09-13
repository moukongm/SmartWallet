package com.bill.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.storage.RoomDatabaseFactory

@Database(
    entities = [
        BillEntity::class,
        CategoryEntity::class       ],
    version = 2,
    exportSchema = false
)
abstract class BillDatabase : RoomDatabase() {

    abstract fun billDao(): BillDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var instance: BillDatabase? = null
        private val MIGRATION_1_2 = object : Migration(1,2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bill_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        name TEXT NOT NULL,
                        iconKey TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_bill_categories_userId_type_name
                    ON bill_categories(userId, type, name)
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): BillDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BillDatabase::class.java,
                    "smart_wallet.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
