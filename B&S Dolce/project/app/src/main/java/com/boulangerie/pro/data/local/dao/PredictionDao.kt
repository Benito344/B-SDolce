package com.boulangerie.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boulangerie.pro.data.local.entity.PredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionDao {

    @Query("SELECT * FROM predictions WHERE articleId = :articleId ORDER BY date DESC LIMIT :limit")
    fun observeRecentForArticle(articleId: Long, limit: Int = 90): Flow<List<PredictionEntity>>

    @Query("SELECT * FROM predictions WHERE date = :date")
    fun observeForDay(date: Long): Flow<List<PredictionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prediction: PredictionEntity): Long

    @Query("UPDATE predictions SET actualQuantity = :actual WHERE id = :id")
    suspend fun recordActual(id: Long, actual: Double)
}
