package com.boulangerie.pro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.boulangerie.pro.data.local.dao.ArticleDao
import com.boulangerie.pro.data.local.dao.LocalEventDao
import com.boulangerie.pro.data.local.dao.PredictionDao
import com.boulangerie.pro.data.local.dao.SaleDao
import com.boulangerie.pro.data.local.dao.StockMovementDao
import com.boulangerie.pro.data.local.dao.WeatherDao
import com.boulangerie.pro.data.local.entity.ArticleEntity
import com.boulangerie.pro.data.local.entity.LocalEventEntity
import com.boulangerie.pro.data.local.entity.MovementType
import com.boulangerie.pro.data.local.entity.PredictionEntity
import com.boulangerie.pro.data.local.entity.SaleEntity
import com.boulangerie.pro.data.local.entity.StockMovementEntity
import com.boulangerie.pro.data.local.entity.WeatherEntity

class Converters {
    @TypeConverter
    fun fromMovementType(type: MovementType): String = type.name

    @TypeConverter
    fun toMovementType(value: String): MovementType = MovementType.valueOf(value)
}

@Database(
    entities = [
        ArticleEntity::class,
        StockMovementEntity::class,
        SaleEntity::class,
        WeatherEntity::class,
        LocalEventEntity::class,
        PredictionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BoulangerieDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun saleDao(): SaleDao
    abstract fun weatherDao(): WeatherDao
    abstract fun localEventDao(): LocalEventDao
    abstract fun predictionDao(): PredictionDao
}
