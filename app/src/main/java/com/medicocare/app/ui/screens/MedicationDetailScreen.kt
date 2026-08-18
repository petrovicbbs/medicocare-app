package com.medicocare.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.medicocare.app.ui.components.GlassCard
import com.medicocare.app.ui.components.ThemedDialog
import com.medicocare.app.ui.components.ThemedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.medicocare.app.R
import com.medicocare.app.data.MedicationSchedule
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.ads.AdBannerBar
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.SkinArt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailScreen(
    medicationId: Long,
    viewModel: MedicationViewModel,
    onBack: () -> Unit,
    onEditMedication: () -> Unit,
    onAddSchedule: () -> Unit,
    onEditSchedule: (Long) -> Unit,
    onDeleted: () -> Unit
) {
    val detailFlow = remember(medicationId) { viewModel.medicationDetail(medicationId) }
    val detail by detailFlow.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var scheduleToDelete by remember { mutableStateOf<MedicationSchedule?>(null) }
    var showRestockDialog by remember { mutableStateOf(false) }
    var restockAmountText by remember { mutableStateOf("") }

    val skin by viewModel.skin.collectAsState()
    val customImagePath by viewModel.customBackgroundImagePath.collectAsState()
    val showsPhotoBackdrop = !customImagePath.isNullOrBlank() && (skin == AppSkin.CUSTOM || skin == AppSkin.CRNA)

    Box(modifier = Modifier.fillMaxSize()) {
    if (!showsPhotoBackdrop) {
        SkinArt(skin = skin, modifier = Modifier.fillMaxSize())
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(detail?.medication?.name ?: stringResource(R.string.med_detail_default_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEditMedication) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.med_detail_edit_desc))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.med_detail_delete_desc))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSchedule) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.med_detail_add_schedule_desc))
            }
        },
        bottomBar = { AdBannerBar(viewModel) },
    ) { padding ->
        val item = detail
        if (item == null) {
            Column(Modifier.padding(padding).fillMaxSize()) { }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val details = listOf(item.medication.dosage, item.medication.form)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    if (details.isNotBlank()) {
                        Text(details, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (item.medication.notes.isNotBlank()) {
                        Text(
                            item.medication.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    val stock = item.medication.stockCount
                    if (stock != null) {
                        val low = stock <= item.medication.lowStockThreshold
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.med_detail_stock_label, formatDetailQuantity(stock)) +
                                    if (low) stringResource(R.string.med_detail_stock_low_suffix) else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedButton(onClick = {
                                restockAmountText = ""
                                showRestockDialog = true
                            }) {
                                Text(stringResource(R.string.med_detail_restock_button))
                            }
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.med_detail_frequency_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )

            if (item.schedules.isEmpty()) {
                Text(
                    stringResource(R.string.med_detail_no_schedules),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(item.schedules, key = { it.id }) { schedule ->
                        ScheduleRow(
                            schedule = schedule,
                            onToggle = { enabled ->
                                viewModel.setScheduleEnabled(schedule, enabled, item.medication.name)
                            },
                            onEdit = { onEditSchedule(schedule.id) },
                            onDelete = { scheduleToDelete = schedule }
                        )
                    }
                }
            }
        }

        if (showDeleteDialog) {
            ThemedDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.med_detail_delete_medication_title)) },
                text = { Text(stringResource(R.string.med_detail_delete_medication_text, item.medication.name)) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteMedication(item)
                        onDeleted()
                    }) { Text(stringResource(R.string.common_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }

        scheduleToDelete?.let { schedule ->
            ThemedDialog(
                onDismissRequest = { scheduleToDelete = null },
                title = { Text(stringResource(R.string.med_detail_delete_schedule_title)) },
                text = { Text(stringResource(R.string.med_detail_delete_schedule_text)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteSchedule(schedule)
                        scheduleToDelete = null
                    }) { Text(stringResource(R.string.common_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { scheduleToDelete = null }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }

        if (showRestockDialog) {
            ThemedDialog(
                onDismissRequest = { showRestockDialog = false },
                title = { Text(stringResource(R.string.med_detail_restock_title)) },
                text = {
                    ThemedTextField(
                        value = restockAmountText,
                        onValueChange = { restockAmountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = { Text(stringResource(R.string.med_detail_restock_label)) },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val amount = restockAmountText.replace(',', '.').toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            viewModel.restock(item.medication.id, amount)
                        }
                        showRestockDialog = false
                    }) { Text(stringResource(R.string.common_add)) }
                },
                dismissButton = {
                    TextButton(onClick = { showRestockDialog = false }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }
    }
    }
}

private fun formatDetailQuantity(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

@Composable
private fun ScheduleRow(
    schedule: MedicationSchedule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(scheduleSummary(schedule), style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = schedule.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.med_detail_delete_schedule_desc))
            }
        }
    }
}
