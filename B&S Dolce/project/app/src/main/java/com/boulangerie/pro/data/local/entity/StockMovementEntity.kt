package com.boulangerie.pro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MovementType { ENTRY, EXIT, PRODUCTION, LOSS, SALE }

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("articleId"), Index("date")]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val articleId: Long,
    val type: MovementType,
    val quantity: Double,
    val reason: String = "",
    val date: Long = System.currentTimeMillis()
)
