package com.boulangerie.pro.data.repository

import com.boulangerie.pro.data.local.dao.ArticleDao
import com.boulangerie.pro.data.local.dao.SaleDao
import com.boulangerie.pro.data.local.dao.SaleWithArticle
import com.boulangerie.pro.data.local.dao.StockMovementDao
import com.boulangerie.pro.data.local.entity.MovementType
import com.boulangerie.pro.data.local.entity.SaleEntity
import com.boulangerie.pro.data.local.entity.StockMovementEntity
import com.boulangerie.pro.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface SaleRepository {
    fun observeForDay(epochDay: Long): Flow<List<SaleWithArticle>>
    fun observeRevenueBetween(start: Long, end: Long): Flow<Double>
    fun observeBetween(start: Long, end: Long): Flow<List<SaleWithArticle>>
    fun observeDaysWithSales(): Flow<List<Long>>
    suspend fun recordSale(articleId: Long, quantity: Double, unitPrice: Double)
    suspend fun deleteForDay(startDay: Long, endDay: Long): Int
    suspend fun deleteForArticle(articleId: Long): Int
    suspend fun deleteAll(): Int
}

class SaleRepositoryImpl @Inject constructor(
    private val saleDao: SaleDao,
    private val articleDao: ArticleDao,
    private val movementDao: StockMovementDao,
) : SaleRepository {

    override fun observeForDay(epochDay: Long): Flow<List<SaleWithArticle>> {
        val (start, end) = DateUtils.dayBounds(epochDay)
        return saleDao.observeForDay(start, end)
    }

    override fun observeRevenueBetween(start: Long, end: Long) =
        saleDao.observeRevenueBetween(start, end)

    override fun observeBetween(start: Long, end: Long) =
        saleDao.observeBetween(start, end)

    override fun observeDaysWithSales() = saleDao.observeDaysWithSales()

    override suspend fun recordSale(articleId: Long, quantity: Double, unitPrice: Double) {
        saleDao.insert(SaleEntity(articleId = articleId, quantity = quantity, unitPrice = unitPrice))
        articleDao.adjustStock(articleId, -quantity)
        movementDao.insert(
            StockMovementEntity(
                articleId = articleId,
                type = MovementType.SALE,
                quantity = -quantity,
                reason = "Vente"
            )
        )
    }

    override suspend fun deleteForDay(startDay: Long, endDay: Long) =
        saleDao.deleteForDay(startDay, endDay)

    override suspend fun deleteForArticle(articleId: Long) = saleDao.deleteForArticle(articleId)

    override suspend fun deleteAll() = saleDao.deleteAll()
}
