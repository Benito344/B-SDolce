package com.boulangerie.pro.data.repository

import com.boulangerie.pro.data.local.dao.ArticleDao
import com.boulangerie.pro.data.local.dao.StockMovementDao
import com.boulangerie.pro.data.local.entity.ArticleEntity
import com.boulangerie.pro.data.local.entity.MovementType
import com.boulangerie.pro.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ArticleRepository {
    fun observeAll(): Flow<List<ArticleEntity>>
    fun search(query: String): Flow<List<ArticleEntity>>
    fun observeById(id: Long): Flow<ArticleEntity?>
    fun observeLowStock(): Flow<List<ArticleEntity>>
    fun observeOutOfStock(): Flow<List<ArticleEntity>>
    fun observeStockValue(): Flow<Double?>
    fun observeCategories(): Flow<List<String>>
    suspend fun getById(id: Long): ArticleEntity?
    suspend fun save(article: ArticleEntity): Long
    suspend fun update(article: ArticleEntity)
    suspend fun delete(article: ArticleEntity)
    suspend fun adjustStock(articleId: Long, delta: Double, type: MovementType, reason: String)
}

class ArticleRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val movementDao: StockMovementDao,
) : ArticleRepository {

    override fun observeAll() = articleDao.observeAll()
    override fun search(query: String) = articleDao.search(query)
    override fun observeById(id: Long) = articleDao.observeById(id)
    override fun observeLowStock() = articleDao.observeLowStock()
    override fun observeOutOfStock() = articleDao.observeOutOfStock()
    override fun observeStockValue() = articleDao.observeStockValue()
    override fun observeCategories() = articleDao.observeCategories()
    override suspend fun getById(id: Long) = articleDao.getById(id)

    override suspend fun save(article: ArticleEntity): Long {
        val id = articleDao.insert(article)
        movementDao.insert(
            StockMovementEntity(
                articleId = id,
                type = MovementType.ENTRY,
                quantity = article.quantityInStock,
                reason = "Stock initial"
            )
        )
        return id
    }

    override suspend fun update(article: ArticleEntity) {
        articleDao.update(article.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun delete(article: ArticleEntity) = articleDao.delete(article)

    override suspend fun adjustStock(
        articleId: Long,
        delta: Double,
        type: MovementType,
        reason: String,
    ) {
        articleDao.adjustStock(articleId, delta)
        movementDao.insert(
            StockMovementEntity(
                articleId = articleId,
                type = type,
                quantity = delta,
                reason = reason
            )
        )
    }
}
