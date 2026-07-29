package com.boulangerie.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.boulangerie.pro.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE name LIKE '%' || :query || '%' OR reference LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    fun search(query: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getById(id: Long): ArticleEntity?

    @Query("SELECT * FROM articles WHERE id = :id")
    fun observeById(id: Long): Flow<ArticleEntity?>

    @Query("SELECT * FROM articles WHERE quantityInStock <= lowStockThreshold")
    fun observeLowStock(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE quantityInStock <= 0")
    fun observeOutOfStock(): Flow<List<ArticleEntity>>

    @Query("SELECT COUNT(*) FROM articles")
    fun observeCount(): Flow<Int>

    @Query("SELECT SUM(quantityInStock * purchasePrice) FROM articles")
    fun observeStockValue(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: ArticleEntity): Long

    @Update
    suspend fun update(article: ArticleEntity)

    @Delete
    suspend fun delete(article: ArticleEntity)

    @Query("UPDATE articles SET quantityInStock = quantityInStock + :delta, updatedAt = :now WHERE id = :id")
    suspend fun adjustStock(id: Long, delta: Double, now: Long = System.currentTimeMillis())

    @Query("SELECT DISTINCT category FROM articles")
    fun observeCategories(): Flow<List<String>>
}
