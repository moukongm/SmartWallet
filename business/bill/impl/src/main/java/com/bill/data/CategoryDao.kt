package com.bill.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query(
        """
        SELECT * FROM bill_categories
        WHERE userId = :userId AND type = :type
        ORDER BY sortOrder ASC, id ASC
        """
    )
    fun observeCategories(
        userId: String,
        type: String
    ): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT * FROM bill_categories
        WHERE id = :categoryId AND userId = :userId
        LIMIT 1
        """
    )
    suspend fun getById(
        categoryId: Long,
        userId: String
    ): CategoryEntity?

    @Query(
        """
        SELECT * FROM bill_categories
        WHERE userId = :userId
          AND type = :type
          AND name = :name
        LIMIT 1
        """
    )
    suspend fun getByName(
        userId: String,
        type: String,
        name: String
    ): CategoryEntity?

    @Query(
        """
        SELECT COUNT(*) FROM bill_categories
        WHERE userId = :userId AND type = :type
        """
    )
    suspend fun count(
        userId: String,
        type: String
    ): Int

    @Query(
        """
        SELECT MAX(sortOrder) FROM bill_categories
        WHERE userId = :userId AND type = :type
        """
    )
    suspend fun maxSortOrder(
        userId: String,
        type: String
    ): Int?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(categoryEntity: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity): Int

    @Query(
        """
        DELETE FROM bill_categories
        WHERE id = :categoryId AND userId = :userId
        """
    )
    suspend fun delete(
        categoryId: Long,
        userId: String
    ): Int

    @Query(
        """
        UPDATE bill_categories
        SET sortOrder = :sortOrder
        WHERE id = :categoryId AND userId = :userId
        """
    )
    suspend fun updateSortOrder(
        categoryId: Long,
        userId: String,
        sortOrder: Int
    )
}