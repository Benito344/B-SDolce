package com.boulangerie.pro.di

import com.boulangerie.pro.data.repository.ArticleRepository
import com.boulangerie.pro.data.repository.ArticleRepositoryImpl
import com.boulangerie.pro.data.repository.SaleRepository
import com.boulangerie.pro.data.repository.SaleRepositoryImpl
import com.boulangerie.pro.data.repository.WeatherRepository
import com.boulangerie.pro.data.repository.WeatherRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindArticleRepository(impl: ArticleRepositoryImpl): ArticleRepository

    @Binds @Singleton
    abstract fun bindSaleRepository(impl: SaleRepositoryImpl): SaleRepository

    @Binds @Singleton
    abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository
}
