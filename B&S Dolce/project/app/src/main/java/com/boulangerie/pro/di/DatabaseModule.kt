package com.boulangerie.pro.di

import android.content.Context
import androidx.room.Room
import com.boulangerie.pro.data.local.BoulangerieDatabase
import com.boulangerie.pro.data.local.dao.ArticleDao
import com.boulangerie.pro.data.local.dao.LocalEventDao
import com.boulangerie.pro.data.local.dao.PredictionDao
import com.boulangerie.pro.data.local.dao.SaleDao
import com.boulangerie.pro.data.local.dao.StockMovementDao
import com.boulangerie.pro.data.local.dao.WeatherDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BoulangerieDatabase =
        Room.databaseBuilder(context, BoulangerieDatabase::class.java, "boulangerie_db.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideArticleDao(db: BoulangerieDatabase): ArticleDao = db.articleDao()
    @Provides fun provideStockMovementDao(db: BoulangerieDatabase): StockMovementDao = db.stockMovementDao()
    @Provides fun provideSaleDao(db: BoulangerieDatabase): SaleDao = db.saleDao()
    @Provides fun provideWeatherDao(db: BoulangerieDatabase): WeatherDao = db.weatherDao()
    @Provides fun provideLocalEventDao(db: BoulangerieDatabase): LocalEventDao = db.localEventDao()
    @Provides fun providePredictionDao(db: BoulangerieDatabase): PredictionDao = db.predictionDao()
}
