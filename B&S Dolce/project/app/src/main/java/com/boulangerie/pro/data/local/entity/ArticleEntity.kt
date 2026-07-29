package com.boulangerie.pro.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    indices = [Index("reference", unique = true), Index("category")]
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val reference: String,
    val quantityInStock: Double,
    val purchasePrice: Double,
    val salePrice: Double,
    val category: String,
    val lowStockThreshold: Double,
    val unit: String,
    val productionTimeMinutes: Int,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
