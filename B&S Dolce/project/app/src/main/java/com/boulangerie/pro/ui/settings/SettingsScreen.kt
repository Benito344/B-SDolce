package com.boulangerie.pro.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boulangerie.pro.data.local.entity.MovementType
import com.boulangerie.pro.domain.model.UNITS
import com.boulangerie.pro.utils.DateUtils
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()

    // Confirmation dialogs
    var confirmDialog by remember { mutableStateOf<ConfirmAction?>(null) }

    // Dialogs
    var showDeleteDayPicker by remember { mutableStateOf(false) }
    var showDeleteArticlePicker by remember { mutableStateOf(false) }
    var showDeleteMovementTypePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Réglages") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // General settings section
            item { SectionHeader(icon = Icons.Default.Settings, title = "Général") }

            item {
                var city by remember(state.preferences.city) { mutableStateOf(state.preferences.city) }
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Ville (météo)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { viewModel.saveCity(city, 48.8566f, 2.3522f) }) { Text("Sauver") }
                    },
                    singleLine = true,
                )
            }

            item {
                SettingSwitch(
                    label = "Alertes stock bas",
                    description = "Notification quand un article dépasse le seuil d'alerte",
                    checked = state.preferences.lowStockAlertEnabled,
                    onCheckedChange = viewModel::saveLowStockAlert
                )
            }

            item {
                var days by remember(state.preferences.salesHistoryDays) { mutableStateOf(state.preferences.salesHistoryDays.toString()) }
                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it },
                    label = { Text("Jours d'historique pour les prédictions") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        TextButton(onClick = { days.toIntOrNull()?.let { viewModel.saveHistoryDays(it) } }) { Text("Sauver") }
                    },
                    singleLine = true,
                )
            }

            // Sales deletion section
            item { SectionHeader(icon = Icons.Default.DeleteSweep, title = "Suppression des ventes") }

            item {
                DangerButton(
                    label = "Supprimer TOUTES les ventes",
                    description = "Efface l'intégralité de l'historique de ventes",
                    onClick = {
                        confirmDialog = ConfirmAction(
                            title = "Supprimer toutes les ventes ?",
                            message = "Cette action est irréversible. Toutes les ventes seront effacées.",
                            onConfirm = { viewModel.deleteAllSales() }
                        )
                    }
                )
            }

            item {
                OutlinedButton(
                    onClick = { showDeleteDayPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Supprimer les ventes d'un jour précis")
                }
            }

            item {
                OutlinedButton(
                    onClick = { showDeleteArticlePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.BakeryDining, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Supprimer les ventes d'un article")
                }
            }

            // Stock movements section
            item { SectionHeader(icon = Icons.Default.SwapVert, title = "Mouvements de stock") }

            item {
                DangerButton(
                    label = "Supprimer TOUS les mouvements",
                    description = "Efface tout l'historique des mouvements de stock",
                    onClick = {
                        confirmDialog = ConfirmAction(
                            title = "Supprimer tous les mouvements ?",
                            message = "L'historique complet des entrées, sorties, productions et pertes sera effacé.",
                            onConfirm = { viewModel.deleteAllMovements() }
                        )
                    }
                )
            }

            item {
                OutlinedButton(
                    onClick = { showDeleteMovementTypePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.FilterAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Supprimer par type de mouvement")
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Confirmation dialog
    confirmDialog?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmDialog = null },
            title = { Text(action.title) },
            text = { Text(action.message) },
            confirmButton = {
                TextButton(
                    onClick = { action.onConfirm(); confirmDialog = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Confirmer la suppression") }
            },
            dismissButton = { TextButton(onClick = { confirmDialog = null }) { Text("Annuler") } }
        )
    }

    // Day picker for sales deletion
    if (showDeleteDayPicker) {
        DayPickerDialog(
            onDateSelected = { epochDay ->
                confirmDialog = ConfirmAction(
                    title = "Supprimer les ventes du ${DateUtils.formatDate(epochDay)} ?",
                    message = "Toutes les ventes de ce jour seront effacées.",
                    onConfirm = { viewModel.deleteSalesForDay(epochDay) }
                )
                showDeleteDayPicker = false
            },
            onDismiss = { showDeleteDayPicker = false }
        )
    }

    // Article picker for sales deletion
    if (showDeleteArticlePicker) {
        ArticlePickerDialog(
            articles = state.articles,
            onArticleSelected = { article ->
                confirmDialog = ConfirmAction(
                    title = "Supprimer les ventes de « ${article.name} » ?",
                    message = "Toutes les ventes de cet article seront effacées.",
                    onConfirm = { viewModel.deleteSalesForArticle(article.id, article.name) }
                )
                showDeleteArticlePicker = false
            },
            onDismiss = { showDeleteArticlePicker = false }
        )
    }

    // Movement type picker
    if (showDeleteMovementTypePicker) {
        MovementTypePickerDialog(
            onTypeSelected = { type ->
                confirmDialog = ConfirmAction(
                    title = "Supprimer les mouvements « ${type.name} » ?",
                    message = "Tous les mouvements de type ${type.name} seront effacés.",
                    onConfirm = { viewModel.deleteMovementsByType(type) }
                )
                showDeleteMovementTypePicker = false
            },
            onDismiss = { showDeleteMovementTypePicker = false }
        )
    }
}

private data class ConfirmAction(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit,
)

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DangerButton(label: String, description: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPickerDialog(onDateSelected: (Long) -> Unit, onDismiss: () -> Unit) {
    var year by remember { mutableStateOf(LocalDate.now().year.toString()) }
    var month by remember { mutableStateOf(LocalDate.now().monthValue.toString()) }
    var day by remember { mutableStateOf(LocalDate.now().dayOfMonth.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir un jour") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("Jour") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("Mois") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Année") }, modifier = Modifier.weight(1.5f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                runCatching {
                    val date = LocalDate.of(year.toInt(), month.toInt(), day.toInt())
                    onDateSelected(date.toEpochDay())
                }
            }) { Text("Confirmer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun ArticlePickerDialog(
    articles: List<com.boulangerie.pro.data.local.entity.ArticleEntity>,
    onArticleSelected: (com.boulangerie.pro.data.local.entity.ArticleEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sélectionner un article") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.height(300.dp)) {
                androidx.compose.foundation.lazy.items(articles) { article ->
                    TextButton(
                        onClick = { onArticleSelected(article) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(article.name, modifier = Modifier.fillMaxWidth()) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun MovementTypePickerDialog(
    onTypeSelected: (MovementType) -> Unit,
    onDismiss: () -> Unit,
) {
    val labels = mapOf(
        MovementType.ENTRY to "Entrée",
        MovementType.EXIT to "Sortie",
        MovementType.PRODUCTION to "Production",
        MovementType.LOSS to "Perte",
        MovementType.SALE to "Vente",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir le type") },
        text = {
            Column {
                MovementType.entries.forEach { type ->
                    TextButton(
                        onClick = { onTypeSelected(type) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(labels[type] ?: type.name, modifier = Modifier.fillMaxWidth()) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
