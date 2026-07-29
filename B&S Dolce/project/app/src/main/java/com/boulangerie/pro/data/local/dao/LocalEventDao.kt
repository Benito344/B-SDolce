package com.boulangerie.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boulangerie.pro.data.local.entity.LocalEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalEventDao {

    @Query("SELECT * FROM local_events WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun observeBetween(start: Long, end: Long): Flow<List<LocalEventEntity>>

    @Query("SELECT * FROM local_events WHERE date = :date")
    fun observeForDay(date: Long): Flow<List<LocalEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: LocalEventEntity): Long

    @Delete
    suspend fun delete(event: LocalEventEntity)
}
