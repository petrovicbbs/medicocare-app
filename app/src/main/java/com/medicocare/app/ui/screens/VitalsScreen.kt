package com.medicocare.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.medicocare.app.R
import com.medicocare.app.data.VitalReading
import com.medicocare.app.data.VitalType
import com.medicocare.app.report.ReportGenerator
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.ads.AdBannerBar
import com.medicocare.app.ui.components.ChartSeries
import com.medicocare.app.ui.components.GlassCard
import com.medicocare.app.ui.components.LineTrendChart
import com.medicocare.app.ui.components.ThemedDialog
import com.medicocare.app.ui.components.ThemedTextField
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.SkinArt
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val VITAL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm")
private val CHART_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")

/**
 * Merenja pritiska i šećera u krvi — unos i hronološki pregled.
 * Ovo nije medicinski savet; za tumačenje vrednosti obratiti se lekaru.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsScreen(
    viewModel: MedicationViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val readings by viewModel.vitalReadings.collectAsState()
    val premiumUnlocked by viewModel.hasPremiumAccess.collectAsState()
    var filter by remember { mutableStateOf<VitalType?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<VitalReading?>(null) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    val shareChooserTitle = stringResource(R.string.vitals_share_chooser)
    val downloadSuccessText = stringResource(R.string.cycle_download_success)
    val downloadFailedText = stringResource(R.string.cycle_download_failed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filtered = remember(readings, filter) {
        if (filter == null) readings else readings.filter { it.type == filter }
    }

    // Na API < 29 preuzimanje u javni folder Preuzimanja traži WRITE_EXTERNAL_STORAGE;
    // od API 29 se koristi MediaStore i dozvola nije potrebna.
    var pendingDownloadIsPdf by remember { mutableStateOf(true) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val success = if (pendingDownloadIsPdf) {
                ReportGenerator.downloadVitalsPdf(context, filtered)
            } else {
                ReportGenerator.downloadVitalsCsv(context, filtered)
            }
            scope.launch { snackbarHostState.showSnackbar(if (success) downloadSuccessText else downloadFailedText) }
        } else {
            scope.launch { snackbarHostState.showSnackbar(downloadFailedText) }
        }
    }

    fun downloadReport(isPdf: Boolean) {
        val needsLegacyPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needsLegacyPermission) {
            pendingDownloadIsPdf = isPdf
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        val success = if (isPdf) {
            ReportGenerator.downloadVitalsPdf(context, filtered)
        } else {
            ReportGenerator.downloadVitalsCsv(context, filtered)
        }
        scope.launch { snackbarHostState.showSnackbar(if (success) downloadSuccessText else downloadFailedText) }
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
                title = { Text(stringResource(R.string.vitals_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.vitals_share_desc))
                    }
                    DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vitals_export_pdf)) },
                            onClick = {
                                showExportMenu = false
                                if (!premiumUnlocked) {
                                    showPremiumDialog = true
                                } else {
                                    val uri = ReportGenerator.generateVitalsPdf(context, filtered)
                                    shareVitalsReport(context, uri, "application/pdf", shareChooserTitle)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vitals_export_csv)) },
                            onClick = {
                                showExportMenu = false
                                if (!premiumUnlocked) {
                                    showPremiumDialog = true
                                } else {
                                    val uri = ReportGenerator.generateVitalsCsv(context, filtered)
                                    shareVitalsReport(context, uri, "text/csv", shareChooserTitle)
                                }
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.cycle_download_pdf)) },
                            leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                            onClick = {
                                showExportMenu = false
                                if (!premiumUnlocked) showPremiumDialog = true else downloadReport(isPdf = true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.cycle_download_csv)) },
                            leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                            onClick = {
                                showExportMenu = false
                                if (!premiumUnlocked) showPremiumDialog = true else downloadReport(isPdf = false)
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.vitals_new_desc))
            }
        },
        bottomBar = { AdBannerBar(viewModel) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text(stringResource(R.string.common_all)) }, modifier = Modifier.padding(end = 8.dp))
                FilterChip(selected = filter == VitalType.PRITISAK, onClick = { filter = VitalType.PRITISAK }, label = { Text(stringResource(R.string.vitals_filter_pressure)) }, modifier = Modifier.padding(end = 8.dp))
                FilterChip(selected = filter == VitalType.SECER, onClick = { filter = VitalType.SECER }, label = { Text(stringResource(R.string.vitals_filter_sugar)) })
            }
            Text(
                stringResource(R.string.vitals_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            VitalsCharts(readings = readings, filter = filter)

            if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Bloodtype, contentDescription = null)
                    Text(
                        stringResource(R.string.vitals_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(filtered, key = { it.id }) { reading ->
                        VitalRow(reading = reading, onDelete = { toDelete = reading })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        VitalEntryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { reading ->
                viewModel.saveVitalReading(reading)
                showAddDialog = false
            }
        )
    }

    toDelete?.let { reading ->
        ThemedDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.vitals_delete_title)) },
            text = { Text(stringResource(R.string.vitals_delete_text)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteVitalReading(reading); toDelete = null }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (showPremiumDialog) {
        PremiumRequiredDialog(
            onDismiss = { showPremiumDialog = false },
            onUnlock = { viewModel.setPremiumUnlocked(true); showPremiumDialog = false }
        )
    }
    }
}

@Composable
private fun VitalsCharts(readings: List<VitalReading>, filter: VitalType?) {
    val pressureReadings = remember(readings) {
        readings.filter { it.type == VitalType.PRITISAK }.sortedBy { it.dateTimeMillis }.takeLast(20)
    }
    val sugarReadings = remember(readings) {
        readings.filter { it.type == VitalType.SECER }.sortedBy { it.dateTimeMillis }.takeLast(20)
    }

    val showPressure = (filter == null || filter == VitalType.PRITISAK) && pressureReadings.size >= 2
    val showSugar = (filter == null || filter == VitalType.SECER) && sugarReadings.size >= 2

    if (!showPressure && !showSugar) return

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        if (showPressure) {
            val systolicLabel = stringResource(R.string.vitals_chart_systolic)
            val diastolicLabel = stringResource(R.string.vitals_chart_diastolic)
            val hasDiastolic = pressureReadings.all { it.valueSecondary != null }
            GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.vitals_chart_pressure_label), style = MaterialTheme.typography.labelMedium)
                    LineTrendChart(
                        series = listOfNotNull(
                            ChartSeries(
                                points = pressureReadings.map { it.valuePrimary.toFloat() },
                                color = MaterialTheme.colorScheme.error,
                                label = systolicLabel
                            ),
                            if (hasDiastolic) {
                                ChartSeries(
                                    points = pressureReadings.map { (it.valueSecondary ?: 0.0).toFloat() },
                                    color = MaterialTheme.colorScheme.primary,
                                    label = diastolicLabel
                                )
                            } else {
                                null
                            }
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        xAxisStartLabel = CHART_DATE_FORMAT.format(Instant.ofEpochMilli(pressureReadings.first().dateTimeMillis).atZone(ZoneId.systemDefault())),
                        xAxisEndLabel = CHART_DATE_FORMAT.format(Instant.ofEpochMilli(pressureReadings.last().dateTimeMillis).atZone(ZoneId.systemDefault())),
                        yValueFormatter = { v -> v.toInt().toString() }
                    )
                }
            }
        }
        if (showSugar) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.vitals_chart_sugar_label), style = MaterialTheme.typography.labelMedium)
                    LineTrendChart(
                        series = listOf(
                            ChartSeries(
                                points = sugarReadings.map { it.valuePrimary.toFloat() },
                                color = MaterialTheme.colorScheme.tertiary,
                                label = ""
                            )
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        xAxisStartLabel = CHART_DATE_FORMAT.format(Instant.ofEpochMilli(sugarReadings.first().dateTimeMillis).atZone(ZoneId.systemDefault())),
                        xAxisEndLabel = CHART_DATE_FORMAT.format(Instant.ofEpochMilli(sugarReadings.last().dateTimeMillis).atZone(ZoneId.systemDefault())),
                        yValueFormatter = { v -> String.format(Locale.US, "%.1f", v) }
                    )
                }
            }
        }
    }
}

private fun shareVitalsReport(context: Context, uri: Uri, mimeType: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

@Composable
private fun VitalRow(reading: VitalReading, onDelete: () -> Unit) {
    val dateLabel = VITAL_DATE_FORMAT.format(Instant.ofEpochMilli(reading.dateTimeMillis).atZone(ZoneId.systemDefault()))
    val pulseSuffix = reading.pulse?.let { stringResource(R.string.vitals_pulse_inline, it.toString()) } ?: ""
    val valueLabel = when (reading.type) {
        VitalType.PRITISAK -> {
            val sys = formatVitalNumber(reading.valuePrimary)
            val dia = reading.valueSecondary?.let { formatVitalNumber(it) } ?: "?"
            "$sys/$dia ${reading.unit.ifBlank { "mmHg" }}$pulseSuffix"
        }
        VitalType.SECER -> "${formatVitalNumber(reading.valuePrimary)} ${reading.unit.ifBlank { "mmol/L" }}"
    }
    val typeLabel = if (reading.type == VitalType.PRITISAK) stringResource(R.string.vital_type_pressure_label) else stringResource(R.string.vital_type_sugar_label)

    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("$typeLabel: $valueLabel", style = MaterialTheme.typography.titleSmall)
                Text(dateLabel, style = MaterialTheme.typography.bodySmall)
                if (reading.notes.isNotBlank()) {
                    Text(reading.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.vitals_delete_desc))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VitalEntryDialog(onDismiss: () -> Unit, onSave: (VitalReading) -> Unit) {
    var type by remember { mutableStateOf(VitalType.PRITISAK) }
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var glucose by remember { mutableStateOf("") }
    var glucoseUnit by remember { mutableStateOf("mmol/L") }
    var notes by remember { mutableStateOf("") }

    ThemedDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vitals_new_dialog_title)) },
        text = {
            Column {
                Row {
                    FilterChip(
                        selected = type == VitalType.PRITISAK,
                        onClick = { type = VitalType.PRITISAK },
                        label = { Text(stringResource(R.string.vitals_filter_pressure)) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = type == VitalType.SECER,
                        onClick = { type = VitalType.SECER },
                        label = { Text(stringResource(R.string.vitals_filter_sugar)) }
                    )
                }
                if (type == VitalType.PRITISAK) {
                    Row(Modifier.padding(top = 8.dp)) {
                        ThemedTextField(
                            value = systolic,
                            onValueChange = { systolic = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.vitals_systolic_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        )
                        ThemedTextField(
                            value = diastolic,
                            onValueChange = { diastolic = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.vitals_diastolic_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                    }
                    ThemedTextField(
                        value = pulse,
                        onValueChange = { pulse = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.vitals_pulse_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                } else {
                    ThemedTextField(
                        value = glucose,
                        onValueChange = { glucose = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = { Text(stringResource(R.string.vitals_glucose_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Row(Modifier.padding(top = 8.dp)) {
                        listOf("mmol/L", "mg/dL").forEach { u ->
                            FilterChip(
                                selected = glucoseUnit == u,
                                onClick = { glucoseUnit = u },
                                label = { Text(u) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }
                ThemedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.common_notes_optional)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val reading = if (type == VitalType.PRITISAK) {
                    VitalReading(
                        type = VitalType.PRITISAK,
                        dateTimeMillis = System.currentTimeMillis(),
                        valuePrimary = systolic.toDoubleOrNull() ?: 0.0,
                        valueSecondary = diastolic.toDoubleOrNull(),
                        pulse = pulse.toIntOrNull(),
                        unit = "mmHg",
                        notes = notes.trim()
                    )
                } else {
                    VitalReading(
                        type = VitalType.SECER,
                        dateTimeMillis = System.currentTimeMillis(),
                        valuePrimary = glucose.replace(',', '.').toDoubleOrNull() ?: 0.0,
                        unit = glucoseUnit,
                        notes = notes.trim()
                    )
                }
                onSave(reading)
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

private fun formatVitalNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
