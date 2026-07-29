package com.boulangerie.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boulangerie.pro.data.local.entity.WeatherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather WHERE date = :date")
    fun observeForDay(date: Long): Flow<WeatherEntity?>

    @Query("SELECT * FROM weather WHERE date = :date")
    suspend fun getForDay(date: Long): WeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weather: WeatherEntity)
}
