package com.boulangerie.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import com.boulangerie.pro.data.local.entity.ArticleEntity
import com.boulangerie.pro.data.local.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

data class SaleWithArticle(
    @Embedded val sale: SaleEntity,
    @Relation(parentColumn = "articleId", entityColumn = "id")
    val article: ArticleEntity
)

@Dao
interface SaleDao {

    @Query("""
        SELECT sales.*, articles.* FROM sales
        INNER JOIN articles ON articles.id = sales.articleId
        WHERE sales.date BETWEEN :start AND :end
        ORDER BY sales.date DESC
    """)
    fun observeBetween(start: Long, end: Long): Flow<List<SaleWithArticle>>

    @Query("""
        SELECT sales.*, articles.* FROM sales
        INNER JOIN articles ON articles.id = sales.articleId
        WHERE sales.date >= :startDay AND sales.date < :endDay
        ORDER BY sales.date DESC
    """)
    fun observeForDay(startDay: Long, endDay: Long): Flow<List<SaleWithArticle>>

    @Query("SELECT COALESCE(SUM(quantity * unitPrice), 0) FROM sales WHERE date >= :startDay AND date < :endDay")
    fun observeRevenueForDay(startDay: Long, endDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(quantity * unitPrice), 0) FROM sales WHERE date >= :start AND date < :end")
    fun observeRevenueBetween(start: Long, end: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(quantity), 0) FROM sales
        WHERE articleId = :articleId AND date >= :start AND date < :end
    """)
    suspend fun quantityForArticleBetween(articleId: Long, start: Long, end: Long): Double

    @Query("""
        SELECT sales.*, articles.* FROM sales
        INNER JOIN articles ON articles.id = sales.articleId
        WHERE articleId = :articleId AND date >= :start AND date < :end
        ORDER BY date DESC
    """)
    fun observeForArticleBetween(articleId: Long, start: Long, end: Long): Flow<List<SaleWithArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: SaleEntity): Long

    @Query("DELETE FROM sales WHERE date >= :startDay AND date < :endDay")
    suspend fun deleteForDay(startDay: Long, endDay: Long): Int

    @Query("DELETE FROM sales WHERE articleId = :articleId")
    suspend fun deleteForArticle(articleId: Long): Int

    @Query("DELETE FROM sales")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM sales")
    suspend fun count(): Int

    @Query("SELECT DISTINCT date FROM sales ORDER BY date DESC")
    fun observeDaysWithSales(): Flow<List<Long>>
}
