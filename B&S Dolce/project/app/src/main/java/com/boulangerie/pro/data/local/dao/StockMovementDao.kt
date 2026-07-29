package com.boulangerie.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boulangerie.pro.data.local.entity.MovementType
import com.boulangerie.pro.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {

    @Query("SELECT * FROM stock_movements WHERE articleId = :articleId ORDER BY date DESC")
    fun observeForArticle(articleId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: StockMovementEntity): Long

    @Query("DELETE FROM stock_movements WHERE type = :type")
    suspend fun deleteByType(type: MovementType)

    @Query("DELETE FROM stock_movements")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM stock_movements")
    suspend fun count(): Int
}
