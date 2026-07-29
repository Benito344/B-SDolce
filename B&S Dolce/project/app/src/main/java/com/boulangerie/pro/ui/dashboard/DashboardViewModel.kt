package com.boulangerie.pro.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boulangerie.pro.data.local.entity.ArticleEntity
import com.boulangerie.pro.data.repository.ArticleRepository
import com.boulangerie.pro.data.repository.SaleRepository
import com.boulangerie.pro.data.repository.WeatherRepository
import com.boulangerie.pro.data.preferences.AppPreferencesDataStore
import com.boulangerie.pro.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class PeriodFilter { DAY, WEEK, MONTH, YEAR }

data class ChartDataPoint(val label: String, val value: Float)

data class DashboardUiState(
    val stockValue: Double = 0.0,
    val outOfStockCount: Int = 0,
    val lowStockCount: Int = 0,
    val todayRevenue: Double = 0.0,
    val revenueHistory: List<ChartDataPoint> = emptyList(),
    val categoryBreakdown: List<Pair<String, Float>> = emptyList(),
    val comparedArticles: List<ArticleCompare> = emptyList(),
    val selectedPeriod: PeriodFilter = PeriodFilter.WEEK,
    val selectedChartArticleIds: Set<Long> = emptySet(),
    val allArticles: List<ArticleEntity> = emptyList(),
    val isLoading: Boolean = true,
)

data class ArticleCompare(
    val articleId: Long,
    val name: String,
    val sold: Float,
    val inStock: Float,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val articleRepo: ArticleRepository,
    private val saleRepo: SaleRepository,
    private val weatherRepo: WeatherRepository,
    private val prefs: AppPreferencesDataStore,
) : ViewModel() {

    private val _period = MutableStateFlow(PeriodFilter.WEEK)
    private val _selectedArticles = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<DashboardUiState> = combine(
        articleRepo.observeAll(),
        articleRepo.observeOutOfStock(),
        articleRepo.observeLowStock(),
        articleRepo.observeStockValue(),
        _period,
    ) { articles, outOfStock, lowStock, stockValue, period ->
        DashboardUiState(
            stockValue = stockValue ?: 0.0,
            outOfStockCount = outOfStock.size,
            lowStockCount = lowStock.size,
            allArticles = articles,
            selectedPeriod = period,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    private val _todayRevenue = MutableStateFlow(0.0)
    val todayRevenue: StateFlow<Double> = _todayRevenue.asStateFlow()

    private val _revenueHistory = MutableStateFlow<List<ChartDataPoint>>(emptyList())
    val revenueHistory: StateFlow<List<ChartDataPoint>> = _revenueHistory.asStateFlow()

    private val _comparedArticles = MutableStateFlow<List<ArticleCompare>>(emptyList())
    val comparedArticles: StateFlow<List<ArticleCompare>> = _comparedArticles.asStateFlow()

    private val _selectedArticleIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedArticleIds: StateFlow<Set<Long>> = _selectedArticleIds.asStateFlow()

    init {
        refreshRevenue()
        refreshRevenueHistory()
    }

    fun setPeriod(period: PeriodFilter) {
        _period.value = period
        refreshRevenueHistory()
    }

    fun toggleArticleInChart(id: Long) {
        _selectedArticleIds.value = _selectedArticleIds.value.toMutableSet().apply {
            if (contains(id)) remove(id) else add(id)
        }
        refreshComparedArticles()
    }

    private fun refreshRevenue() = viewModelScope.launch {
        val today = DateUtils.todayEpochDay()
        val (start, end) = DateUtils.dayBounds(today)
        saleRepo.observeRevenueBetween(start, end).collect { _todayRevenue.value = it }
    }

    private fun refreshRevenueHistory() = viewModelScope.launch {
        val now = LocalDate.now()
        val points = when (_period.value) {
            PeriodFilter.DAY -> (0 until 24).map { hour ->
                ChartDataPoint(String.format("%02dh", hour), 0f)
            }
            PeriodFilter.WEEK -> (6 downTo 0).map { daysAgo ->
                val date = now.minusDays(daysAgo.toLong())
                val epochDay = date.toEpochDay()
                val (start, end) = DateUtils.dayBounds(epochDay)
                var rev = 0.0
                saleRepo.observeRevenueBetween(start, end).collect { rev = it }
                val label = date.dayOfWeek.name.take(3).lowercase()
                    .replaceFirstChar { it.uppercase() }
                ChartDataPoint(label, rev.toFloat())
            }
            PeriodFilter.MONTH -> (29 downTo 0 step 7).mapIndexed { i, daysAgo ->
                val date = now.minusDays(daysAgo.toLong())
                val epochDay = date.toEpochDay()
                val (start, end) = DateUtils.dayBounds(epochDay)
                var rev = 0.0
                saleRepo.observeRevenueBetween(start, end).collect { rev = it }
                ChartDataPoint("S${i + 1}", rev.toFloat())
            }
            PeriodFilter.YEAR -> (11 downTo 0).map { monthsAgo ->
                val date = now.minusMonths(monthsAgo.toLong())
                val startDate = date.withDayOfMonth(1)
                val endDate = startDate.plusMonths(1)
                val start = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val end = endDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                var rev = 0.0
                saleRepo.observeRevenueBetween(start, end).collect { rev = it }
                val monthName = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                ChartDataPoint(monthName, rev.toFloat())
            }
        }
        _revenueHistory.value = points
    }

    private fun refreshComparedArticles() = viewModelScope.launch {
        val ids = _selectedArticleIds.value
        if (ids.isEmpty()) { _comparedArticles.value = emptyList(); return@launch }
        val today = DateUtils.todayEpochDay()
        val (start, end) = DateUtils.dayBounds(today)
        val result = ids.mapNotNull { id ->
            val article = articleRepo.getById(id) ?: return@mapNotNull null
            var sold = 0.0
            saleRepo.observeRevenueBetween(start, end).collect { }
            ArticleCompare(
                articleId = id,
                name = article.name,
                sold = sold.toFloat(),
                inStock = article.quantityInStock.toFloat()
            )
        }
        _comparedArticles.value = result
    }

    fun refreshWeather() = viewModelScope.launch {
        prefs.preferencesFlow.collect { p ->
            weatherRepo.fetchAndStore(p.city, p.cityLat, p.cityLon)
        }
    }
}
