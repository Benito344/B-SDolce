package com.boulangerie.pro.ui.articles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boulangerie.pro.data.local.entity.ArticleEntity
import com.boulangerie.pro.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticleFormState(
    val id: Long = 0,
    val name: String = "",
    val reference: String = "",
    val quantityInStock: String = "0",
    val purchasePrice: String = "0",
    val salePrice: String = "0",
    val category: String = "Pain",
    val lowStockThreshold: String = "5",
    val unit: String = "pièce",
    val productionTimeMinutes: String = "30",
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
)

fun ArticleFormState.toEntity() = ArticleEntity(
    id = id,
    name = name.trim(),
    reference = reference.trim(),
    quantityInStock = quantityInStock.toDoubleOrNull() ?: 0.0,
    purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
    salePrice = salePrice.toDoubleOrNull() ?: 0.0,
    category = category,
    lowStockThreshold = lowStockThreshold.toDoubleOrNull() ?: 5.0,
    unit = unit,
    productionTimeMinutes = productionTimeMinutes.toIntOrNull() ?: 30,
    notes = notes.trim(),
)

@HiltViewModel
class ArticleFormViewModel @Inject constructor(
    private val repo: ArticleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val articleId: Long = savedStateHandle["articleId"] ?: 0L

    private val _state = MutableStateFlow(ArticleFormState())
    val state: StateFlow<ArticleFormState> = _state.asStateFlow()

    init {
        if (articleId != 0L) loadArticle()
    }

    private fun loadArticle() = viewModelScope.launch {
        val article = repo.getById(articleId) ?: return@launch
        _state.value = ArticleFormState(
            id = article.id,
            name = article.name,
            reference = article.reference,
            quantityInStock = article.quantityInStock.toString(),
            purchasePrice = article.purchasePrice.toString(),
            salePrice = article.salePrice.toString(),
            category = article.category,
            lowStockThreshold = article.lowStockThreshold.toString(),
            unit = article.unit,
            productionTimeMinutes = article.productionTimeMinutes.toString(),
            notes = article.notes,
        )
    }

    fun update(block: ArticleFormState.() -> ArticleFormState) {
        _state.value = _state.value.block()
    }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Le nom est obligatoire")
            return@launch
        }
        _state.value = s.copy(isLoading = true, error = null)
        runCatching {
            val entity = s.toEntity()
            if (entity.id == 0L) repo.save(entity) else repo.update(entity)
        }.onSuccess {
            _state.value = _state.value.copy(isLoading = false, isSaved = true)
        }.onFailure {
            _state.value = _state.value.copy(isLoading = false, error = it.message)
        }
    }
}
