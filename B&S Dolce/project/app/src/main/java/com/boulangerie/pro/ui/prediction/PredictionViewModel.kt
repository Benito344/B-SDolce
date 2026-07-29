package com.boulangerie.pro.ui.prediction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boulangerie.pro.ai.PredictionEngine
import com.boulangerie.pro.data.local.dao.LocalEventDao
import com.boulangerie.pro.data.local.dao.PredictionDao
import com.boulangerie.pro.data.local.dao.WeatherDao
import com.boulangerie.pro.data.local.entity.LocalEventEntity
import com.boulangerie.pro.data.local.entity.PredictionEntity
import com.boulangerie.pro.data.repository.ArticleRepository
import com.boulangerie.pro.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class ArticlePrediction(
    val predictionId: Long,
    val articleId: Long,
    val articleName: String,
    val category: String,
    val recommendedQty: Double,
    val confidence: Float,
    val factors: String,
    val manualOverride: Double? = null,
    val actualQty: Double? = null,
)

data class PredictionUiState(
    val predictions: List<ArticlePrediction> = emptyList(),
    val tomorrowEpochDay: Long = LocalDate.now().plusDays(1).toEpochDay(),
    val isLoading: Boolean = false,
    val historyDays: Int = 30,
    val error: String? = null,
)

@HiltViewModel
class PredictionViewModel @Inject constructor(
    private val articleRepo: ArticleRepository,
    private val predictionEngine: PredictionEngine,
    private val predictionDao: PredictionDao,
    private val weatherDao: WeatherDao,
    private val eventDao: LocalEventDao,
) : ViewModel() {

    private val _state = MutableStateFlow(PredictionUiState())
    val state: StateFlow<PredictionUiState> = _state.asStateFlow()

    init { generatePredictions() }

    fun generatePredictions(historyDays: Int = 30) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null, historyDays = historyDays)
        runCatching {
            val tomorrow = LocalDate.now().plusDays(1)
            val tomorrowEpochDay = tomorrow.toEpochDay()
            val tomorrowMillis = tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val articles = articleRepo.observeAll().first()
            val weather = weatherDao.getForDay(tomorrowMillis)
            val events = eventDao.observeForDay(tomorrowMillis).first()
            val eventImpact = if (events.isEmpty()) 1.0f else events.map { it.impact }.average().toFloat()
            val eventNames = events.map { it.name }

            val predictions = articles.map { article ->
                val result = predictionEngine.predict(
                    article = article,
                    targetEpochDay = tomorrowEpochDay,
                    weatherEntity = weather,
                    eventImpact = eventImpact,
                    eventNames = eventNames,
                    historyDays = historyDays,
                )
                val entity = PredictionEntity(
                    articleId = article.id,
                    date = tomorrowMillis,
                    predictedQuantity = result.predictedQuantity,
                    confidence = result.confidence,
                    factors = result.factors,
                )
                val id = predictionDao.insert(entity)
                ArticlePrediction(
                    predictionId = id,
                    articleId = article.id,
                    articleName = article.name,
                    category = article.category,
                    recommendedQty = result.predictedQuantity,
                    confidence = result.confidence,
                    factors = result.factors,
                )
            }
            _state.value = _state.value.copy(
                predictions = predictions,
                tomorrowEpochDay = tomorrowEpochDay,
                isLoading = false
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(isLoading = false, error = e.message)
        }
    }

    fun recordActual(predictionId: Long, actual: Double) = viewModelScope.launch {
        predictionDao.recordActual(predictionId, actual)
        _state.value = _state.value.copy(
            predictions = _state.value.predictions.map {
                if (it.predictionId == predictionId) it.copy(actualQty = actual) else it
            }
        )
    }

    fun addEvent(name: String, type: String, impact: Float, date: Long) = viewModelScope.launch {
        eventDao.insert(LocalEventEntity(date = date, name = name, type = type, impact = impact))
        generatePredictions(_state.value.historyDays)
    }
}
