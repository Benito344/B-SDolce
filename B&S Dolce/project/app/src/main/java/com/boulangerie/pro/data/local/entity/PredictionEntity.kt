package com.boulangerie.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "predictions")
data class PredictionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val articleId: Long,
    val date: Long,
    val predictedQuantity: Double,
    val confidence: Float,
    val factors: String,
    val actualQuantity: Double? = null
)
