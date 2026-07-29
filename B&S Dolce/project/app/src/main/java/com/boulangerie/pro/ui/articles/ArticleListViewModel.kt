package com.boulangerie.pro.ui.articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boulangerie.pro.data.local.dao.StockMovementDao
import com.boulangerie.pro.data.local.entity.ArticleEntity
import com.boulangerie.pro.data.local.entity.MovementType
import com.boulangerie.pro.data.local.entity.StockMovementEntity
import com.boulangerie.pro.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticleListUiState(
    val articles: List<ArticleEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "Tous",
    val categories: List<String> = listOf("Tous"),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val repo: ArticleRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("Tous")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val _articles = _query
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) repo.observeAll() else repo.search(q)
        }

    val uiState: StateFlow<ArticleListUiState> = combine(
        _articles,
        _query,
        _category,
        repo.observeCategories(),
    ) { articles, q, cat, cats ->
        val filtered = if (cat == "Tous") articles else articles.filter { it.category == cat }
        ArticleListUiState(
            articles = filtered,
            searchQuery = q,
            selectedCategory = cat,
            categories = listOf("Tous") + cats.sorted(),
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArticleListUiState())

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(cat: String) { _category.value = cat }

    fun delete(article: ArticleEntity) = viewModelScope.launch {
        repo.delete(article)
    }
}
