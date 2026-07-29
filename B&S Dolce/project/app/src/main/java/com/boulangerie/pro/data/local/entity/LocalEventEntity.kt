package com.boulangerie.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_events")
data class LocalEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val name: String,
    val type: String,
    val impact: Float = 1.0f
)
