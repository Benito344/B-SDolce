package com.boulangerie.pro.data.repository

import com.boulangerie.pro.BuildConfig
import com.boulangerie.pro.data.local.dao.WeatherDao
import com.boulangerie.pro.data.local.entity.WeatherEntity
import com.boulangerie.pro.data.remote.WeatherApi
import com.boulangerie.pro.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface WeatherRepository {
    fun observeForDay(epochDay: Long): Flow<WeatherEntity?>
    suspend fun fetchAndStore(city: String, lat: Float, lon: Float): Result<WeatherEntity>
}

class WeatherRepositoryImpl @Inject constructor(
    private val api: WeatherApi,
    private val dao: WeatherDao,
) : WeatherRepository {

    override fun observeForDay(epochDay: Long): Flow<WeatherEntity?> {
        val (start, _) = DateUtils.dayBounds(epochDay)
        return dao.observeForDay(start)
    }

    override suspend fun fetchAndStore(city: String, lat: Float, lon: Float): Result<WeatherEntity> =
        runCatching {
            val response = api.getCurrentWeather(lat, lon, BuildConfig.OPEN_WEATHER_API_KEY)
            val today = DateUtils.startOfDay(System.currentTimeMillis())
            val entity = WeatherEntity(
                date = today,
                temperature = response.main.temp,
                feelsLike = response.main.feelsLike,
                humidity = response.main.humidity,
                description = response.weather.firstOrNull()?.description ?: "",
                icon = response.weather.firstOrNull()?.icon ?: "",
                rain = response.rain.oneHour,
                cloudiness = response.clouds.all,
                city = response.name
            )
            dao.insert(entity)
            entity
        }
}
