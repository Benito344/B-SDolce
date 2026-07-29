package com.boulangerie.pro.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boulangerie.pro.data.local.entity.ArticleEntity
import com.boulangerie.pro.data.repository.ArticleRepository
import com.boulangerie.pro.data.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaleItem(
    val article: ArticleEntity,
    var quantity: Double = 0.0,
)

data class QuickSaleUiState(
    val articles: List<ArticleEntity> = emptyList(),
    val cart: Map<Long, Double> = emptyMap(),
    val searchQuery: String = "",
    val isSaving: Boolean = false,
    val savedMessage: String? = null,
)

@HiltViewModel
class QuickSaleViewModel @Inject constructor(
    private val articleRepo: ArticleRepository,
    private val saleRepo: SaleRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _cart = MutableStateFlow<Map<Long, Double>>(emptyMap())
    private val _isSaving = MutableStateFlow(false)
    private val _savedMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<QuickSaleUiState> = combine(
        articleRepo.observeAll(),
        _query,
        _cart,
        _isSaving,
        _savedMessage,
    ) { articles, q, cart, saving, msg ->
        val filtered = if (q.isBlank()) articles else articles.filter {
            it.name.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true)
        }
        QuickSaleUiState(
            articles = filtered,
            cart = cart,
            searchQuery = q,
            isSaving = saving,
            savedMessage = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickSaleUiState())

    fun setQuery(q: String) { _query.value = q }

    fun setQuantity(articleId: Long, qty: Double) {
        _cart.value = _cart.value.toMutableMap().apply {
            if (qty <= 0) remove(articleId) else put(articleId, qty)
        }
    }

    fun increment(articleId: Long) {
        val current = _cart.value[articleId] ?: 0.0
        setQuantity(articleId, current + 1)
    }

    fun decrement(articleId: Long) {
        val current = _cart.value[articleId] ?: 0.0
        setQuantity(articleId, (current - 1).coerceAtLeast(0.0))
    }

    fun clearCart() { _cart.value = emptyMap() }

    fun confirmSales(articles: List<ArticleEntity>) = viewModelScope.launch {
        if (_cart.value.isEmpty()) return@launch
        _isSaving.value = true
        var count = 0
        _cart.value.forEach { (id, qty) ->
            if (qty > 0) {
                val article = articles.find { it.id == id } ?: return@forEach
                saleRepo.recordSale(id, qty, article.salePrice)
                count++
            }
        }
        _cart.value = emptyMap()
        _isSaving.value = false
        _savedMessage.value = "$count article(s) enregistré(s)"
    }

    fun clearMessage() { _savedMessage.value = null }
}
