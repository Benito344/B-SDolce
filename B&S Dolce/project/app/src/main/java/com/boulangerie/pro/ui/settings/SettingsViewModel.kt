package com.boulangerie.pro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boulangerie.pro.data.local.dao.StockMovementDao
import com.boulangerie.pro.data.local.entity.ArticleEntity
import com.boulangerie.pro.data.local.entity.MovementType
import com.boulangerie.pro.data.preferences.AppPreferencesDataStore
import com.boulangerie.pro.data.preferences.AppPreferences
import com.boulangerie.pro.data.repository.ArticleRepository
import com.boulangerie.pro.data.repository.SaleRepository
import com.boulangerie.pro.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val articles: List<ArticleEntity> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsStore: AppPreferencesDataStore,
    private val saleRepo: SaleRepository,
    private val articleRepo: ArticleRepository,
    private val movementDao: StockMovementDao,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        prefsStore.preferencesFlow,
        articleRepo.observeAll(),
    ) { prefs, articles ->
        SettingsUiState(preferences = prefs, articles = articles)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // Preferences
    fun saveCity(city: String, lat: Float, lon: Float) = viewModelScope.launch {
        prefsStore.setCity(city, lat, lon)
    }
    fun saveDefaultUnit(unit: String) = viewModelScope.launch { prefsStore.setDefaultUnit(unit) }
    fun saveLowStockAlert(enabled: Boolean) = viewModelScope.launch { prefsStore.setLowStockAlert(enabled) }
    fun saveHistoryDays(days: Int) = viewModelScope.launch { prefsStore.setHistoryDays(days) }

    // Sales deletions
    fun deleteAllSales() = viewModelScope.launch {
        val count = saleRepo.deleteAll()
        _message.value = "$count vente(s) supprimée(s)"
    }

    fun deleteSalesForDay(epochDay: Long) = viewModelScope.launch {
        val (start, end) = DateUtils.dayBounds(epochDay)
        val count = saleRepo.deleteForDay(start, end)
        _message.value = "$count vente(s) du ${DateUtils.formatDate(epochDay)} supprimée(s)"
    }

    fun deleteSalesForArticle(articleId: Long, articleName: String) = viewModelScope.launch {
        val count = saleRepo.deleteForArticle(articleId)
        _message.value = "$count vente(s) de « $articleName » supprimée(s)"
    }

    // Stock movement deletions
    fun deleteAllMovements() = viewModelScope.launch {
        movementDao.deleteAll()
        _message.value = "Tous les mouvements de stock supprimés"
    }

    fun deleteMovementsByType(type: MovementType) = viewModelScope.launch {
        movementDao.deleteByType(type)
        _message.value = "Mouvements « ${type.name} » supprimés"
    }

    fun clearMessage() { _message.value = null }
}
