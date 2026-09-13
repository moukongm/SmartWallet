package com.bill.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(bill: BillEntity): Long

    @Query(
        "SELECT * FROM bills " +
            "WHERE userId = :userId " +
            "ORDER BY occurredAt DESC, id DESC"
    )
    fun observeBills(userId: String): Flow<List<BillEntity>>

    @Query(
        """
        SELECT * FROM bills
        WHERE userId = :userId
          AND (:startAt IS NULL OR occurredAt >= :startAt)
          AND (:endAtExclusive IS NULL OR occurredAt < :endAtExclusive)
          AND (:type IS NULL OR type = :type)
          AND (:category IS NULL OR category = :category)
          AND (
              :keyword IS NULL
              OR note LIKE '%' || :keyword || '%'
              OR category LIKE '%' || :keyword || '%'
          )
        ORDER BY occurredAt DESC, id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getBills(
        userId: String,
        startAt: Long?,
        endAtExclusive: Long?,
        type: String?,
        category: String?,
        keyword: String?,
        limit: Int,
        offset: Int
    ): List<BillEntity>

    @Query(
        """
        SELECT * FROM bills
        WHERE userId = :userId
          AND (:startAt IS NULL OR occurredAt >= :startAt)
          AND (:endAtExclusive IS NULL OR occurredAt < :endAtExclusive)
          AND (:type IS NULL OR type = :type)
          AND (:category IS NULL OR category = :category)
          AND (
              :keyword IS NULL
              OR note LIKE '%' || :keyword || '%'
              OR category LIKE '%' || :keyword || '%'
          )
        ORDER BY occurredAt DESC, id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observeBills(
        userId: String,
        startAt: Long?,
        endAtExclusive: Long?,
        type: String?,
        category: String?,
        keyword: String?,
        limit: Int,
        offset: Int
    ): Flow<List<BillEntity>>

    @Query(
        """
        SELECT * FROM bills
        WHERE id = :billId AND userId = :userId
        LIMIT 1
        """
    )
    suspend fun getBillById(
        billId: Long,
        userId: String
    ): BillEntity?

    @Update
    suspend fun update(bill: BillEntity): Int

    @Query(
        """
        DELETE FROM bills
        WHERE id = :billId AND userId = :userId
        """
    )
    suspend fun deleteById(
        billId: Long,
        userId: String
    ): Int

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'income' THEN amountInCents ELSE 0 END), 0)
                AS incomeInCents,
            COALESCE(SUM(CASE WHEN type = 'expense' THEN amountInCents ELSE 0 END), 0)
                AS expenseInCents
        FROM bills
        WHERE userId = :userId
          AND occurredAt >= :startAt
          AND occurredAt < :endAtExclusive
        """
    )
    suspend fun getSummary(
        userId: String,
        startAt: Long,
        endAtExclusive: Long
    ): BillSummaryRow

    @Query(
        """
    UPDATE bills
    SET category = :newName
    WHERE userId = :userId
      AND type = :type
      AND category = :oldName
    """
    )
    suspend fun renameCategoryInBills(
        userId: String,
        type: String,
        oldName: String,
        newName: String
    )
}
