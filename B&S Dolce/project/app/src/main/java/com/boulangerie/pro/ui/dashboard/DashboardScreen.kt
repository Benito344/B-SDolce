package com.boulangerie.pro.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boulangerie.pro.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSales: () -> Unit,
    onNavigateToArticles: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val todayRevenue by viewModel.todayRevenue.collectAsState()
    val revenueHistory by viewModel.revenueHistory.collectAsState()
    val comparedArticles by viewModel.comparedArticles.collectAsState()
    val selectedIds by viewModel.selectedArticleIds.collectAsState()
    var showArticleSelector by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshWeather() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Boulangerie Pro", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Tableau de bord",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshWeather() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // KPI Cards
            item {
                Text(
                    "Indicateurs clés",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        label = "CA du jour",
                        value = FormatUtils.currency(todayRevenue),
                        icon = Icons.Default.Euro,
                        iconColor = MaterialTheme.colorScheme.primary,
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        label = "Valeur stock",
                        value = FormatUtils.currency(state.stockValue),
                        icon = Icons.Default.Inventory,
                        iconColor = Color(0xFF2E7D32),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        label = "Ruptures",
                        value = "${state.outOfStockCount} article(s)",
                        icon = Icons.Default.Warning,
                        iconColor = MaterialTheme.colorScheme.error,
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        label = "Seuil bas",
                        value = "${state.lowStockCount} article(s)",
                        icon = Icons.Default.NotificationsActive,
                        iconColor = Color(0xFFE65100),
                    )
                }
            }

            // Period filter chips
            item {
                Text(
                    "Évolution des ventes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeriodFilter.entries.forEach { period ->
                        FilterChip(
                            selected = state.selectedPeriod == period,
                            onClick = { viewModel.setPeriod(period) },
                            label = {
                                Text(
                                    when (period) {
                                        PeriodFilter.DAY -> "Jour"
                                        PeriodFilter.WEEK -> "Semaine"
                                        PeriodFilter.MONTH -> "Mois"
                                        PeriodFilter.YEAR -> "Année"
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Revenue bar chart
            item {
                RevenueBarChart(points = revenueHistory)
            }

            // Comparison chart header + article selector
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Vendu vs Stock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showArticleSelector = true }) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sélectionner")
                    }
                }

                if (comparedArticles.isNotEmpty()) {
                    CompareBarChart(items = comparedArticles)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sélectionnez des articles à comparer",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showArticleSelector) {
        ArticleSelectorDialog(
            articles = state.allArticles,
            selectedIds = selectedIds,
            onToggle = { viewModel.toggleArticleInChart(it) },
            onDismiss = { showArticleSelector = false }
        )
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RevenueBarChart(points: List<ChartDataPoint>) {
    if (points.isEmpty()) return
    val maxValue = points.maxOfOrNull { it.value } ?: 1f
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEach { point ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        val frac = if (maxValue > 0) point.value / maxValue else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(frac.coerceIn(0.02f, 1f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                points.forEach { point ->
                    Text(
                        point.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareBarChart(items: List<ArticleCompare>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { item ->
                val maxVal = maxOf(item.sold, item.inStock, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Vendu bar
                        Box(
                            modifier = Modifier
                                .weight(item.sold / maxVal)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        // En stock bar
                        Box(
                            modifier = Modifier
                                .weight(item.inStock / maxVal)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.7f))
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(color = MaterialTheme.colorScheme.primary, label = "Vendu: ${item.sold}")
                        LegendDot(color = Color(0xFF2E7D32), label = "Stock: ${item.inStock}")
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ArticleSelectorDialog(
    articles: List<com.boulangerie.pro.data.local.entity.ArticleEntity>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sélectionner les articles") },
        text = {
            LazyColumn(Modifier.height(300.dp)) {
                items(articles) { article ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(article.id) }
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = article.id in selectedIds,
                            onCheckedChange = { onToggle(article.id) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(article.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}
