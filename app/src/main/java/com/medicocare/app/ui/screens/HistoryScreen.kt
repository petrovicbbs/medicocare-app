package com.medicocare.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import com.medicocare.app.ui.components.GlassCard
import com.medicocare.app.ui.components.ThemedDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.medicocare.app.R
import com.medicocare.app.data.IntakeLogView
import com.medicocare.app.data.IntakeStatus
import com.medicocare.app.report.ReportGenerator
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.ads.AdBannerBar
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.SkinArt
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm")
private val FILTER_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MedicationViewModel,
    onBack: () -> Unit
) {
    val history by viewModel.history.collectAsState()
    val premiumUnlocked by viewModel.hasPremiumAccess.collectAsState()
    var selected by remember { mutableStateOf<IntakeLogView?>(null) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.history_share_report_chooser)

    var fromDateMillis by remember { mutableStateOf<Long?>(null) }
    var toDateMillis by remember { mutableStateOf<Long?>(null) }
    var sortAscending by remember { mutableStateOf(false) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    val filteredSorted = remember(history, fromDateMillis, toDateMillis, sortAscending) {
        val filtered = history.filter { entry ->
            (fromDateMillis == null || entry.log.scheduledAtMillis >= fromDateMillis!!) &&
                (toDateMillis == null || entry.log.scheduledAtMillis <= toDateMillis!! + 86_399_000L)
        }
        if (sortAscending) filtered.sortedBy { it.log.scheduledAtMillis } else filtered.sortedByDescending { it.log.scheduledAtMillis }
    }

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
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { sortAscending = !sortAscending }) {
                        Icon(
                            if (sortAscending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                            contentDescription = stringResource(R.string.history_sort_desc)
                        )
                    }
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.history_export_desc))
                    }
                    DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history_export_pdf)) },
                            onClick = {
                                showExportMenu = false
                                if (!premiumUnlocked) {
                                    showPremiumDialog = true
                                } else {
                                    val uri = ReportGenerator.generatePdf(context, filteredSorted)
                                    shareReport(context, uri, "application/pdf", shareChooserTitle)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history_export_csv)) },
                            onClick = {
                                showExportMenu = false
                                if (!premiumUnlocked) {
                                    showPremiumDialog = true
                                } else {
                                    val uri = ReportGenerator.generateCsv(context, filteredSorted)
                                    shareReport(context, uri, "text/csv", shareChooserTitle)
                                }
                            }
                        )
                    }
                }
            )
        },
        bottomBar = { AdBannerBar(viewModel) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { showFromPicker = true }, modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                    Text(fromDateMillis?.let { FILTER_DATE_FORMAT.format(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC)) } ?: stringResource(R.string.history_from_date))
                }
                OutlinedButton(onClick = { showToPicker = true }, modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(toDateMillis?.let { FILTER_DATE_FORMAT.format(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC)) } ?: stringResource(R.string.history_to_date))
                }
                if (fromDateMillis != null || toDateMillis != null) {
                    IconButton(onClick = { fromDateMillis = null; toDateMillis = null }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.history_clear_filter_desc))
                    }
                }
            }

            if (filteredSorted.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.History, contentDescription = null)
                    Text(
                        stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                ) {
                    items(filteredSorted, key = { it.log.id }) { entry ->
                        HistoryRow(entry = entry, onClick = { if (entry.log.status == IntakeStatus.NA_CEKANJU) selected = entry })
                    }
                }
            }
        }

        selected?.let { entry ->
            ThemedDialog(
                onDismissRequest = { selected = null },
                title = { Text(entry.medicationName) },
                text = { Text(stringResource(R.string.history_mark_dialog_text, entry.log.doseLabel.ifBlank { "" })) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.markIntake(entry.log.id, IntakeStatus.UZETO)
                        selected = null
                    }) { Text(stringResource(R.string.intake_status_taken)) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.markIntake(entry.log.id, IntakeStatus.PRESKOCENO)
                        selected = null
                    }) { Text(stringResource(R.string.intake_status_skipped)) }
                }
            )
        }

        if (showFromPicker) {
            val state = rememberDatePickerState(initialSelectedDateMillis = fromDateMillis)
            DatePickerDialog(
                onDismissRequest = { showFromPicker = false },
                confirmButton = {
                    TextButton(onClick = { fromDateMillis = state.selectedDateMillis; showFromPicker = false }) { Text(stringResource(R.string.common_confirm)) }
                },
                dismissButton = { TextButton(onClick = { showFromPicker = false }) { Text(stringResource(R.string.common_cancel)) } }
            ) { DatePicker(state = state) }
        }
        if (showToPicker) {
            val state = rememberDatePickerState(initialSelectedDateMillis = toDateMillis)
            DatePickerDialog(
                onDismissRequest = { showToPicker = false },
                confirmButton = {
                    TextButton(onClick = { toDateMillis = state.selectedDateMillis; showToPicker = false }) { Text(stringResource(R.string.common_confirm)) }
                },
                dismissButton = { TextButton(onClick = { showToPicker = false }) { Text(stringResource(R.string.common_cancel)) } }
            ) { DatePicker(state = state) }
        }

        if (showPremiumDialog) {
            PremiumRequiredDialog(
                onDismiss = { showPremiumDialog = false },
                onUnlock = { viewModel.setPremiumUnlocked(true); showPremiumDialog = false }
            )
        }
    }
    }
}

@Composable
private fun HistoryRow(entry: IntakeLogView, onClick: () -> Unit) {
    val log = entry.log
    val dateTime = Instant.ofEpochMilli(log.scheduledAtMillis).atZone(ZoneId.systemDefault())
    val (statusLabel, statusColor) = when (log.status) {
        IntakeStatus.UZETO -> stringResource(R.string.intake_status_taken) to MaterialTheme.colorScheme.primary
        IntakeStatus.PRESKOCENO -> stringResource(R.string.intake_status_skipped) to MaterialTheme.colorScheme.error
        IntakeStatus.NA_CEKANJU -> stringResource(R.string.intake_status_pending) to MaterialTheme.colorScheme.tertiary
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.medicationName, style = MaterialTheme.typography.titleSmall)
                val details = listOf(dateTime.format(DATE_TIME_FORMAT), log.doseLabel)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                Text(details, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                statusLabel,
                style = MaterialTheme.typography.labelLarge,
                color = statusColor
            )
        }
    }
}

private fun shareReport(context: Context, uri: Uri, mimeType: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
