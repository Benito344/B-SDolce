package com.boulangerie.pro.ui.salesrecap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boulangerie.pro.data.local.dao.SaleDao
import com.boulangerie.pro.data.local.dao.SaleWithArticle
import com.boulangerie.pro.data.local.entity.WeatherEntity
import com.boulangerie.pro.data.repository.SaleRepository
import com.boulangerie.pro.data.repository.WeatherRepository
import com.boulangerie.pro.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class PerformanceIndicator { ABOVE, AVERAGE, BELOW }

data class ArticleDaySummary(
    val articleId: Long,
    val articleName: String,
    val category: String,
    val quantitySold: Double,
    val revenue: Double,
    val averageQty: Double,
    val performance: PerformanceIndicator,
)

data class DayRecapUiState(
    val selectedEpochDay: Long = DateUtils.todayEpochDay(),
    val summaries: List<ArticleDaySummary> = emptyList(),
    val totalRevenue: Double = 0.0,
    val weather: WeatherEntity? = null,
    val eventNames: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val daysWithSales: Set<Long> = emptySet(),
)

@HiltViewModel
class SalesRecapViewModel @Inject constructor(
    private val saleRepo: SaleRepository,
    private val weatherRepo: WeatherRepository,
    private val saleDao: SaleDao,
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(DateUtils.todayEpochDay())
    private val _isLoading = MutableStateFlow(false)
    private val _summaries = MutableStateFlow<List<ArticleDaySummary>>(emptyList())
    private val _weather = MutableStateFlow<WeatherEntity?>(null)
    private val _daysWithSales = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<DayRecapUiState> = combine(
        _selectedDay,
        _summaries,
        _weather,
        _isLoading,
        _daysWithSales,
    ) { day, summaries, weather, loading, days ->
        DayRecapUiState(
            selectedEpochDay = day,
            summaries = summaries,
            totalRevenue = summaries.sumOf { it.revenue },
            weather = weather,
            isLoading = loading,
            daysWithSales = days,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayRecapUiState())

    init {
        loadDaysWithSales()
        selectDay(DateUtils.todayEpochDay())
    }

    private fun loadDaysWithSales() = viewModelScope.launch {
        saleRepo.observeDaysWithSales().collect { milliList ->
            _daysWithSales.value = milliList.map { DateUtils.millisToEpochDay(it) }.toSet()
        }
    }

    fun selectDay(epochDay: Long) {
        _selectedDay.value = epochDay
        loadDayData(epochDay)
    }

    private fun loadDayData(epochDay: Long) = viewModelScope.launch {
        _isLoading.value = true
        runCatching {
            val (start, end) = DateUtils.dayBounds(epochDay)
            // Fetch today's weather from cache
            weatherRepo.observeForDay(epochDay).first()?.let { _weather.value = it }

            // Collect sales for the day
            saleRepo.observeForDay(epochDay).first().let { dayList ->
                // Group by article
                val grouped = dayList.groupBy { it.article.id }
                val summaries = grouped.map { (_, sales) ->
                    val article = sales.first().article
                    val totalQty = sales.sumOf { it.sale.quantity }
                    val totalRev = sales.sumOf { it.sale.total }
                    // Average over last 30 similar days (same day-of-week)
                    val avg = computeAvgForArticle(article.id, epochDay)
                    val perf = when {
                        avg <= 0 -> PerformanceIndicator.AVERAGE
                        totalQty >= avg * 1.15 -> PerformanceIndicator.ABOVE
                        totalQty <= avg * 0.85 -> PerformanceIndicator.BELOW
                        else -> PerformanceIndicator.AVERAGE
                    }
                    ArticleDaySummary(
                        articleId = article.id,
                        articleName = article.name,
                        category = article.category,
                        quantitySold = totalQty,
                        revenue = totalRev,
                        averageQty = avg,
                        performance = perf,
                    )
                }.sortedByDescending { it.revenue }
                _summaries.value = summaries
            }
        }
        _isLoading.value = false
    }

    private suspend fun computeAvgForArticle(articleId: Long, epochDay: Long): Double {
        val targetDow = LocalDate.ofEpochDay(epochDay).dayOfWeek.value
        val quantities = (1..90).mapNotNull { daysBack ->
            val pastDay = epochDay - daysBack
            val pastDow = LocalDate.ofEpochDay(pastDay).dayOfWeek.value
            if (pastDow != targetDow) return@mapNotNull null
            val (s, e) = DateUtils.dayBounds(pastDay)
            val qty = saleDao.quantityForArticleBetween(articleId, s, e)
            if (qty > 0) qty else null
        }
        return if (quantities.isEmpty()) 0.0 else quantities.average()
    }
}
