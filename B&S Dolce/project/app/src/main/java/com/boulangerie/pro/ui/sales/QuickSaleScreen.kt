package com.boulangerie.pro.ui.sales

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boulangerie.pro.data.local.entity.ArticleEntity
import com.boulangerie.pro.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSaleScreen(viewModel: QuickSaleViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() }
    }

    val totalItems = state.cart.values.sumOf { it }
    val cartArticles = state.articles.filter { state.cart.containsKey(it.id) }
    val totalRevenue = cartArticles.sumOf { (state.cart[it.id] ?: 0.0) * it.salePrice }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Saisie rapide") },
                    actions = {
                        if (state.cart.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearCart() }) { Text("Vider") }
                        }
                    }
                )
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Rechercher un produit…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = state.cart.isNotEmpty()) {
                Surface(tonalElevation = 4.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${totalItems.toInt()} article(s)", style = MaterialTheme.typography.labelLarge)
                            Text(FormatUtils.currency(totalRevenue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showConfirm = true },
                            enabled = !state.isSaving
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Valider les ventes")
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Cart summary at top
            if (state.cart.isNotEmpty()) {
                item {
                    Text("Panier actuel", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                }
                items(cartArticles) { article ->
                    val qty = state.cart[article.id] ?: 0.0
                    SaleItemRow(
                        article = article,
                        quantity = qty,
                        onIncrement = { viewModel.increment(article.id) },
                        onDecrement = { viewModel.decrement(article.id) },
                        onQtyChange = { viewModel.setQuantity(article.id, it) },
                    )
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            }

            val availableArticles = state.articles.filter { !state.cart.containsKey(it.id) }
            if (availableArticles.isNotEmpty()) {
                item {
                    Text("Articles", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                }
                items(availableArticles) { article ->
                    SaleItemRow(
                        article = article,
                        quantity = 0.0,
                        onIncrement = { viewModel.increment(article.id) },
                        onDecrement = { viewModel.decrement(article.id) },
                        onQtyChange = { viewModel.setQuantity(article.id, it) },
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirmer les ventes ?") },
            text = {
                Column {
                    Text("${totalItems.toInt()} article(s) · ${FormatUtils.currency(totalRevenue)}")
                    Spacer(Modifier.height(4.dp))
                    Text("Le stock sera automatiquement mis à jour.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmSales(state.articles); showConfirm = false }) {
                    Text("Valider")
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun SaleItemRow(
    article: ArticleEntity,
    quantity: Double,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onQtyChange: (Double) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (quantity > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(article.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("${FormatUtils.currency(article.salePrice)} / ${article.unit}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Quantity stepper
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                var qtyText by remember(quantity) { mutableStateOf(if (quantity == 0.0) "" else quantity.toInt().toString()) }
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { v ->
                        qtyText = v
                        v.toDoubleOrNull()?.let { onQtyChange(it) }
                    },
                    modifier = Modifier.width(52.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                )
                FilledTonalIconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
