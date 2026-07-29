package com.boulangerie.pro.ui.prediction

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boulangerie.pro.utils.DateUtils
import com.boulangerie.pro.utils.FormatUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionScreen(viewModel: PredictionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var showEventDialog by remember { mutableStateOf(false) }
    var actualQtyDialog by remember { mutableStateOf<ArticlePrediction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Prédictions IA", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Pour le ${DateUtils.formatDate(state.tomorrowEpochDay)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEventDialog = true }) {
                        Icon(Icons.Default.Event, contentDescription = "Ajouter un événement")
                    }
                    IconButton(onClick = { viewModel.generatePredictions(state.historyDays) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recalculer")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Calcul des prédictions…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // History filter chips
            item {
                Text("Historique utilisé", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 90).forEach { days ->
                        FilterChip(
                            selected = state.historyDays == days,
                            onClick = { viewModel.generatePredictions(days) },
                            label = { Text("$days j") }
                        )
                    }
                }
            }

            if (state.error != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(state.error!!, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            items(state.predictions, key = { it.predictionId }) { pred ->
                PredictionCard(
                    prediction = pred,
                    onRecordActual = { actualQtyDialog = pred }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showEventDialog) {
        AddEventDialog(
            tomorrowMillis = DateUtils.dayBounds(state.tomorrowEpochDay).first,
            onAdd = { name, type, impact, date ->
                viewModel.addEvent(name, type, impact, date)
            },
            onDismiss = { showEventDialog = false }
        )
    }

    actualQtyDialog?.let { pred ->
        RecordActualDialog(
            articleName = pred.articleName,
            recommended = pred.recommendedQty,
            onConfirm = { actual ->
                viewModel.recordActual(pred.predictionId, actual)
                actualQtyDialog = null
            },
            onDismiss = { actualQtyDialog = null }
        )
    }
}

@Composable
private fun PredictionCard(
    prediction: ArticlePrediction,
    onRecordActual: () -> Unit,
) {
    val confidenceColor = when {
        prediction.confidence >= 0.7f -> Color(0xFF2E7D32)
        prediction.confidence >= 0.4f -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(prediction.articleName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(prediction.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${FormatUtils.number(prediction.recommendedQty)} unités",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "à produire",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Confidence bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.SpaceBetween) {
                    Text("Confiance", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${(prediction.confidence * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = confidenceColor
                    )
                }
                LinearProgressIndicator(
                    progress = { prediction.confidence },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = confidenceColor,
                    trackColor = confidenceColor.copy(alpha = 0.2f),
                )
            }

            // Factors
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(prediction.factors, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            prediction.actualQty?.let { actual ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32))
                        Text("Réel : ${FormatUtils.number(actual)} unités", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (prediction.actualQty == null) {
                OutlinedButton(
                    onClick = onRecordActual,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enregistrer la production réelle")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEventDialog(
    tomorrowMillis: Long,
    onAdd: (String, String, Float, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Marché") }
    var impact by remember { mutableStateOf("1.3") }
    val types = listOf("Marché", "Festival", "Jour férié", "Match", "Vacances", "Autre")
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un événement local") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de l'événement") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        types.forEach { t ->
                            DropdownMenuItem(text = { Text(t) }, onClick = { type = t; typeExpanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = impact,
                    onValueChange = { impact = it },
                    label = { Text("Impact (ex: 1.3 = +30%)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onAdd(name, type, impact.toFloatOrNull() ?: 1.0f, tomorrowMillis)
                    onDismiss()
                }
            }) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun RecordActualDialog(
    articleName: String,
    recommended: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(recommended.roundToInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Production réelle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Article : $articleName")
                Text("Préconisation : ${FormatUtils.number(recommended)} unités", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Quantité réellement produite") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                value.toDoubleOrNull()?.let { onConfirm(it) }
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
