package com.medicocare.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.medicocare.app.R
import com.medicocare.app.data.CycleEntry
import com.medicocare.app.report.ReportGenerator
import com.medicocare.app.repository.CyclePrediction
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val CYCLE_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.")
private const val DAY_MILLIS: Long = 24L * 60 * 60 * 1000

private fun millisToUtcDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Predstavlja koji dijalog za unos/izmenu ciklusa treba prikazati. */
private sealed class CycleDialogState {
    data class New(val initialStartMillis: Long) : CycleDialogState()
    data class Edit(val entry: CycleEntry) : CycleDialogState()
}

/**
 * Praćenje menstrualnog ciklusa: unos perioda i okvirna procena narednog ciklusa i
 * plodnog perioda na osnovu proseka prethodnih unosa. Ovo je samo procena — nije
 * medicinski savet niti pouzdana metoda kontracepcije.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleTrackerScreen(
    viewModel: MedicationViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val entries by viewModel.cycleEntries.collectAsState()
    val prediction by viewModel.cyclePrediction.collectAsState()
    val premiumUnlocked by viewModel.hasPremiumAccess.collectAsState()
    var dialogState by remember { mutableStateOf<CycleDialogState?>(null) }
    var toDelete by remember { mutableStateOf<CycleEntry?>(null) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    val shareChooserTitle = stringResource(R.string.cycle_share_chooser)
    val downloadSuccessText = stringResource(R.string.cycle_download_success)
    val downloadFailedText = stringResource(R.string.cycle_download_failed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Na API < 29 preuzimanje u javni folder Preuzimanja traži WRITE_EXTERNAL_STORAGE;
    // od API 29 se koristi MediaStore i dozvola nije potrebna.
    var pendingDownloadIsPdf by remember { mutableStateOf(true) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val success = if (pendingDownloadIsPdf) {
                ReportGenerator.downloadCyclePdf(context, entries, prediction)
            } else {
                ReportGenerator.downloadCycleCsv(context, entries, prediction)
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
            ReportGenerator.downloadCyclePdf(context, entries, prediction)
        } else {
            ReportGenerator.downloadCycleCsv(context, entries, prediction)
        }
        scope.launch { snackbarHostState.showSnackbar(if (success) downloadSuccessText else downloadFailedText) }
    }

    val sortedEntries = remember(entries) { entries.sortedByDescending { it.startDateMillis } }

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
                title = { Text(stringResource(R.string.cycle_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.cycle_share_desc))
                    }
                    DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.cycle_export_pdf)) },
                            onClick = {
                                showExportMenu = false
                                if (!premiumUnlocked) {
                                    showPremiumDialog = true
                                } else {
                                    val uri = ReportGenerator.generateCyclePdf(context, entries, prediction)
                                    shareCycleReport(context, uri, "application/pdf", shareChooserTitle)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.cycle_export_csv)) },
                            onClick = {
                                showExportMenu = false
                                if (!premiumUnlocked) {
                                    showPremiumDialog = true
                                } else {
                                    val uri = ReportGenerator.generateCycleCsv(context, entries, prediction)
                                    shareCycleReport(context, uri, "text/csv", shareChooserTitle)
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
            FloatingActionButton(onClick = { dialogState = CycleDialogState.New(LocalDate.now().toUtcMillis()) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cycle_new_desc))
            }
        },
        bottomBar = { AdBannerBar(viewModel) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    CycleCalendarCard(
                        entries = entries,
                        prediction = prediction,
                        onDayClick = { date ->
                            val match = entries.firstOrNull { e ->
                                val start = millisToUtcDate(e.startDateMillis)
                                val end = e.endDateMillis?.let { millisToUtcDate(it) } ?: start
                                !date.isBefore(start) && !date.isAfter(end)
                            }
                            dialogState = if (match != null) CycleDialogState.Edit(match) else CycleDialogState.New(date.toUtcMillis())
                        }
                    )
                    PredictionCard(prediction = prediction, entryCount = entries.size)
                    CycleLengthTrendCard(entries = entries)
                }

                if (sortedEntries.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                            Text(
                                stringResource(R.string.cycle_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                } else {
                    items(sortedEntries, key = { it.id }) { entry ->
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            CycleRow(
                                entry = entry,
                                onClick = { dialogState = CycleDialogState.Edit(entry) },
                                onDelete = { toDelete = entry }
                            )
                        }
                    }
                }
            }
        }
    }

    dialogState?.let { state ->
        CycleEntryDialog(
            existing = (state as? CycleDialogState.Edit)?.entry,
            initialStartMillis = when (state) {
                is CycleDialogState.New -> state.initialStartMillis
                is CycleDialogState.Edit -> state.entry.startDateMillis
            },
            onDismiss = { dialogState = null },
            onSave = { entry ->
                viewModel.saveCycleEntry(entry)
                dialogState = null
            }
        )
    }

    toDelete?.let { entry ->
        ThemedDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.cycle_delete_title)) },
            text = { Text(stringResource(R.string.cycle_delete_text)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCycleEntry(entry); toDelete = null }) { Text(stringResource(R.string.common_delete)) }
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

private fun shareCycleReport(context: Context, uri: Uri, mimeType: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

@Composable
private fun PredictionCard(prediction: CyclePrediction, entryCount: Int) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(12.dp)) {
            val avgDays = prediction.averageCycleLengthDays
            val nextStart = prediction.nextPeriodStartMillis
            if (nextStart == null || avgDays == null) {
                Text(
                    stringResource(R.string.cycle_prediction_hint),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(stringResource(R.string.cycle_prediction_title, avgDays), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.cycle_prediction_next, formatDate(nextStart)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (prediction.fertileWindowStartMillis != null && prediction.fertileWindowEndMillis != null) {
                    Text(
                        stringResource(
                            R.string.cycle_prediction_fertile,
                            formatDate(prediction.fertileWindowStartMillis),
                            formatDate(prediction.fertileWindowEndMillis)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    stringResource(R.string.cycle_prediction_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CycleLengthTrendCard(entries: List<CycleEntry>) {
    val cycleLengths = remember(entries) {
        val starts = entries.map { it.startDateMillis }.sorted()
        starts.zipWithNext { a, b -> ((b - a) / DAY_MILLIS).toInt() }.filter { it > 0 }.takeLast(12)
    }
    if (cycleLengths.size < 2) return

    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.cycle_chart_length_label), style = MaterialTheme.typography.labelMedium)
            LineTrendChart(
                series = listOf(
                    ChartSeries(
                        points = cycleLengths.map { it.toFloat() },
                        color = CYCLE_FERTILE_COLOR,
                        label = ""
                    )
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                yValueFormatter = { v -> v.toInt().toString() }
            )
        }
    }
}

@Composable
private fun CycleRow(entry: CycleEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    val startLabel = formatDate(entry.startDateMillis)
    val endLabel = entry.endDateMillis?.let { formatDate(it) }

    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (endLabel != null) "$startLabel – $endLabel" else stringResource(R.string.cycle_row_start_only, startLabel),
                    style = MaterialTheme.typography.titleSmall
                )
                if (entry.notes.isNotBlank()) {
                    Text(entry.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cycle_delete_desc))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleEntryDialog(
    existing: CycleEntry?,
    initialStartMillis: Long,
    onDismiss: () -> Unit,
    onSave: (CycleEntry) -> Unit
) {
    var startDateMillis by remember { mutableStateOf(existing?.startDateMillis ?: initialStartMillis) }
    var hasEndDate by remember { mutableStateOf(existing?.endDateMillis != null) }
    var endDateMillis by remember { mutableStateOf(existing?.endDateMillis ?: (existing?.startDateMillis ?: initialStartMillis)) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    ThemedDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing != null) R.string.cycle_edit_dialog_title else R.string.cycle_new_dialog_title)) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.cycle_start_label, formatDate(startDateMillis)))
                    OutlinedButton(onClick = { showStartPicker = true }) { Text(stringResource(R.string.common_change)) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.cycle_end_label))
                    Switch(checked = hasEndDate, onCheckedChange = { hasEndDate = it })
                }
                if (hasEndDate) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatDate(endDateMillis))
                        OutlinedButton(onClick = { showEndPicker = true }) { Text(stringResource(R.string.common_change)) }
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
                onSave(
                    CycleEntry(
                        startDateMillis = startDateMillis,
                        endDateMillis = if (hasEndDate) endDateMillis else null,
                        notes = notes.trim()
                    )
                )
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { startDateMillis = it }; showStartPicker = false }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text(stringResource(R.string.common_cancel)) } }
        ) { DatePicker(state = state) }
    }
    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDateMillis)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { endDateMillis = it }; showEndPicker = false }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text(stringResource(R.string.common_cancel)) } }
        ) { DatePicker(state = state) }
    }
}

private fun formatDate(millis: Long): String =
    CYCLE_DATE_FORMAT.format(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC))

private val CYCLE_PERIOD_COLOR = Color(0xFFE91E63)
private val CYCLE_PREDICTED_PERIOD_COLOR = Color(0xFFF48FB1)
private val CYCLE_FERTILE_COLOR = Color(0xFF7C4DFF)

@Composable
private fun CycleCalendarCard(
    entries: List<CycleEntry>,
    prediction: CyclePrediction,
    onDayClick: (LocalDate) -> Unit
) {
    var visibleMonth by remember { mutableStateOf(YearMonth.now()) }

    val periodDays = remember(entries) {
        entries.flatMap { e ->
            val start = millisToUtcDate(e.startDateMillis)
            val end = e.endDateMillis?.let { millisToUtcDate(it) } ?: start
            generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
        }.toSet()
    }

    val fertileDays = remember(prediction) {
        val startMillis = prediction.fertileWindowStartMillis
        val endMillis = prediction.fertileWindowEndMillis
        if (startMillis == null || endMillis == null) {
            emptySet()
        } else {
            val start = millisToUtcDate(startMillis)
            val end = millisToUtcDate(endMillis)
            generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toSet()
        }
    }

    val predictedPeriodDays = remember(prediction, entries) {
        val nextStart = prediction.nextPeriodStartMillis
        if (nextStart == null) {
            emptySet()
        } else {
            val durations = entries.mapNotNull { e ->
                e.endDateMillis?.let { end -> ((end - e.startDateMillis) / DAY_MILLIS).toInt() + 1 }
            }
            val avgDuration = if (durations.isEmpty()) 5 else (durations.sum() / durations.size).coerceIn(1, 10)
            val start = millisToUtcDate(nextStart)
            (0 until avgDuration).map { start.plusDays(it.toLong()) }.toSet()
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.cycle_calendar_prev_month))
                }
                Text(
                    visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        .replaceFirstChar { it.uppercase() } + " " + visibleMonth.year,
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.cycle_calendar_next_month))
                }
            }

            val weekdayLabels = remember {
                listOf(
                    java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY,
                    java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY
                ).map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                weekdayLabels.forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val firstOfMonth = visibleMonth.atDay(1)
            val daysInMonth = visibleMonth.lengthOfMonth()
            val leadingBlanks = firstOfMonth.dayOfWeek.value - 1
            val totalCells = leadingBlanks + daysInMonth
            val trailingBlanks = (7 - totalCells % 7) % 7
            val cells: List<LocalDate?> = List(leadingBlanks) { null } +
                (0 until daysInMonth).map { firstOfMonth.plusDays(it.toLong()) } +
                List(trailingBlanks) { null }

            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (date != null) {
                                val isPeriod = date in periodDays
                                val isFertile = !isPeriod && date in fertileDays
                                val isPredictedPeriod = !isPeriod && !isFertile && date in predictedPeriodDays
                                val bgColor = when {
                                    isPeriod -> CYCLE_PERIOD_COLOR
                                    isFertile -> CYCLE_FERTILE_COLOR
                                    isPredictedPeriod -> CYCLE_PREDICTED_PERIOD_COLOR
                                    else -> Color.Transparent
                                }
                                val textColor = if (isPeriod || isFertile || isPredictedPeriod) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(bgColor)
                                        .clickable { onDayClick(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall, color = textColor)
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(CYCLE_PERIOD_COLOR)
                Text(
                    stringResource(R.string.cycle_legend_period),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                )
                LegendDot(CYCLE_FERTILE_COLOR)
                Text(
                    stringResource(R.string.cycle_legend_fertile),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                )
                LegendDot(CYCLE_PREDICTED_PERIOD_COLOR)
                Text(
                    stringResource(R.string.cycle_legend_predicted),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
}
