package com.boulangerie.pro.ai

import com.boulangerie.pro.data.local.dao.SaleDao
import com.boulangerie.pro.data.local.dao.WeatherDao
import com.boulangerie.pro.data.local.entity.ArticleEntity
import com.boulangerie.pro.data.local.entity.PredictionEntity
import com.boulangerie.pro.data.local.entity.WeatherEntity
import com.boulangerie.pro.utils.DateUtils
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Moteur de prédiction léger :
 * 1. Calcule une moyenne glissante pondérée sur 30/60/90 j d'historique de ventes.
 * 2. Applique des coefficients météo (température, pluie) et de jour de semaine.
 * 3. Applique les impacts des événements locaux enregistrés.
 * 4. Calcule un niveau de confiance basé sur la richesse de l'historique.
 */
@Singleton
class PredictionEngine @Inject constructor(
    private val saleDao: SaleDao,
    private val weatherDao: WeatherDao,
) {

    data class PredictionResult(
        val predictedQuantity: Double,
        val confidence: Float,
        val factors: String,
    )

    suspend fun predict(
        article: ArticleEntity,
        targetEpochDay: Long,
        weatherEntity: WeatherEntity?,
        eventImpact: Float = 1.0f,
        eventNames: List<String> = emptyList(),
        historyDays: Int = 30,
    ): PredictionResult {
        val today = LocalDate.now()
        val targetDate = DateUtils.epochDayToLocalDate(targetEpochDay)
        val targetDow = targetDate.dayOfWeek.value

        // Collect historical daily quantities for this article over the last N days
        val endMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val startMillis = today.minusDays(historyDays.toLong())
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val dailySales = mutableListOf<DailySale>()
        (0 until historyDays).forEach { daysBack ->
            val day = today.minusDays(daysBack.toLong())
            val epochDay = day.toEpochDay()
            val (start, end) = DateUtils.dayBounds(epochDay)
            val qty = saleDao.quantityForArticleBetween(article.id, start, end)
            val weather = weatherDao.getForDay(start)
            dailySales.add(DailySale(day.dayOfWeek.value, qty, weather))
        }

        if (dailySales.isEmpty() || dailySales.all { it.quantity == 0.0 }) {
            return PredictionResult(0.0, 0.1f, "Aucun historique de ventes disponible")
        }

        val nonZero = dailySales.count { it.quantity > 0 }
        val baseAvg = dailySales.map { it.quantity }.average()

        // Day-of-week coefficient (ratio of same-DoW average vs global average)
        val sameDowSales = dailySales.filter { it.dayOfWeek == targetDow }
        val dowCoeff = if (sameDowSales.isNotEmpty() && baseAvg > 0) {
            sameDowSales.map { it.quantity }.average() / baseAvg
        } else 1.0

        // Weather coefficients
        val weatherCoeff = weatherEntity?.let { computeWeatherCoeff(it) } ?: 1.0
        val weatherDesc = weatherEntity?.let { buildWeatherDescription(it) } ?: ""

        // Event coefficient
        val eventCoeff = eventImpact.toDouble()

        val predicted = baseAvg * dowCoeff * weatherCoeff * eventCoeff
        val confidence = (nonZero.toFloat() / historyDays.toFloat()).coerceIn(0.1f, 0.95f)

        val factorParts = mutableListOf<String>()
        if (dowCoeff > 1.1) factorParts.add("+${((dowCoeff - 1) * 100).roundToInt()}% ${DateUtils.dayOfWeekFr(targetEpochDay)}")
        if (dowCoeff < 0.9) factorParts.add("${((dowCoeff - 1) * 100).roundToInt()}% ${DateUtils.dayOfWeekFr(targetEpochDay)}")
        if (weatherDesc.isNotBlank()) factorParts.add(weatherDesc)
        eventNames.forEach { factorParts.add("Évènement : $it") }

        val factors = if (factorParts.isEmpty()) "Basé sur l'historique moyen" else factorParts.joinToString(" · ")

        return PredictionResult(
            predictedQuantity = predicted.coerceAtLeast(0.0),
            confidence = confidence,
            factors = factors,
        )
    }

    private fun computeWeatherCoeff(weather: WeatherEntity): Double {
        var coeff = 1.0
        // Température idéale pour la boulangerie (matin frais) : ~12-18°C
        when {
            weather.temperature < 5 -> coeff *= 1.15   // froid → plus de pain chaud
            weather.temperature in 5.0..18.0 -> coeff *= 1.05
            weather.temperature > 28 -> coeff *= 0.90  // chaleur → moins de ventes
        }
        if (weather.rain > 0.5) coeff *= 0.85           // pluie forte → moins de clients
        if (weather.cloudiness > 70) coeff *= 0.95
        return coeff
    }

    private fun buildWeatherDescription(weather: WeatherEntity): String {
        val parts = mutableListOf<String>()
        if (weather.temperature > 28) parts.add("chaleur (${weather.temperature.roundToInt()}°C)")
        if (weather.temperature < 5) parts.add("froid (${weather.temperature.roundToInt()}°C)")
        if (weather.rain > 0.5) parts.add("pluie")
        return parts.joinToString(", ")
    }

    private data class DailySale(
        val dayOfWeek: Int,
        val quantity: Double,
        val weather: WeatherEntity? = null,
    )
}
