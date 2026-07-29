package com.boulangerie.pro.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class WeatherResponse(
    val weather: List<WeatherCondition> = emptyList(),
    val main: MainData = MainData(),
    val wind: Wind = Wind(),
    val clouds: Clouds = Clouds(),
    val rain: Rain = Rain(),
    val name: String = "",
)

@Serializable
data class WeatherCondition(
    val id: Int = 0,
    val main: String = "",
    val description: String = "",
    val icon: String = "",
)

@Serializable
data class MainData(
    val temp: Double = 0.0,
    @SerialName("feels_like") val feelsLike: Double = 0.0,
    val humidity: Int = 0,
)

@Serializable
data class Wind(val speed: Double = 0.0)

@Serializable
data class Clouds(val all: Int = 0)

@Serializable
data class Rain(@SerialName("1h") val oneHour: Double = 0.0)

interface WeatherApi {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Float,
        @Query("lon") lon: Float,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "fr",
    ): WeatherResponse
}
