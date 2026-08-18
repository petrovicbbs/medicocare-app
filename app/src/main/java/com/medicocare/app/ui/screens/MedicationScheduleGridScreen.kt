package com.medicocare.app.ui.screens

import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import com.medicocare.app.ui.components.GlassCard
import com.medicocare.app.ui.components.ThemedDialog
import com.medicocare.app.ui.components.ThemedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.medicocare.app.R
import com.medicocare.app.data.FrequencyType
import com.medicocare.app.data.IntakeLog
import com.medicocare.app.data.IntakeLogView
import com.medicocare.app.data.IntakeStatus
import com.medicocare.app.data.MedicationSchedule
import com.medicocare.app.data.MedicationWithSchedules
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.ads.AdBannerBar
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.SkinArt
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val HEADER_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.")
private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private const val ROW_HEIGHT_DP = 34
private const val PAST_DAYS = 14
private const val FUTURE_DAYS = 60
private val ALL_HALF_HOUR_SLOTS: List<LocalTime> = (0 until 48).map { LocalTime.of(it / 2, if (it % 2 == 0) 0 else 30) }

/** Jedno "polje" u tabeli — stvaran zapis (istorija/ručno dodeljen) ili projekcija na osnovu rasporeda. */
private data class GridCell(
    val medicationId: Long,
    val medicationName: String,
    val doseLabel: String,
    val logId: Long?,
    val status: IntakeStatus?
)

private fun millisToLocalDate(millis: Long, zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

private fun roundDownToHalfHour(t: LocalTime): LocalTime = LocalTime.of(t.hour, if (t.minute < 30) 0 else 30)

/** Parsira "HH:mm" listu vremena rasporeda, uz indeks (za dose override); preskače nevažeće unose. */
private fun parseScheduleTimes(schedule: MedicationSchedule): List<Pair<LocalTime, Int>> {
    val result = mutableListOf<Pair<LocalTime, Int>>()
    schedule.timesList().forEachIndexed { idx, s ->
        val parsed: LocalTime? = runCatching { LocalTime.parse(s) }.getOrNull()
        if (parsed != null) {
            result.add(parsed to idx)
        }
    }
    return result
}

/** Vremena (i indeks u rasporedu, za dose override) kad se dati raspored javlja na dati datum. */
private fun occurrencesForDate(schedule: MedicationSchedule, date: LocalDate): List<Pair<LocalTime, Int>> {
    if (!schedule.enabled) return emptyList()
    return when (schedule.frequencyType) {
        FrequencyType.SVAKI_DAN -> parseScheduleTimes(schedule)
        FrequencyType.ODREDJENI_DANI -> {
            if (date.dayOfWeek.value !in schedule.daysOfWeekSet()) {
                emptyList()
            } else {
                parseScheduleTimes(schedule)
            }
        }
        FrequencyType.NA_SVAKIH_X_SATI -> {
            val parsedStart: LocalTime? = runCatching { LocalTime.parse(schedule.startTime) }.getOrNull()
            if (parsedStart == null) {
                emptyList()
            } else {
                val start: LocalTime = parsedStart
                val intervalHours = schedule.intervalHours.coerceAtLeast(1).toLong()
                val times = mutableListOf<Pair<LocalTime, Int>>()
                var t: LocalTime = start
                var guard = 0
                while (guard < 24) {
                    times.add(t to 0)
                    val next: LocalTime = t.plusHours(intervalHours)
                    if (!next.isAfter(t)) break
                    t = next
                    guard++
                }
                times
            }
        }
    }
}

/** Spaja stvarnu istoriju (alarmi/ručni unosi) i projekcije iz rasporeda za jedan dan, po vremenskom polju od 30 min. */
private fun cellsForDate(
    date: LocalDate,
    medications: List<MedicationWithSchedules>,
    history: List<IntakeLogView>
): Map<LocalTime, List<GridCell>> {
    val zone = ZoneId.systemDefault()
    val result = linkedMapOf<LocalTime, MutableList<GridCell>>()
    val covered = mutableSetOf<Triple<Long, Long, LocalTime>>()

    history.forEach { entry ->
        val entryDate = millisToLocalDate(entry.log.scheduledAtMillis, zone)
        if (entryDate == date) {
            val time = Instant.ofEpochMilli(entry.log.scheduledAtMillis).atZone(zone).toLocalTime()
            val bucket = roundDownToHalfHour(time)
            result.getOrPut(bucket) { mutableListOf() }.add(
                GridCell(
                    medicationId = entry.log.medicationId,
                    medicationName = entry.medicationName,
                    doseLabel = entry.log.doseLabel,
                    logId = entry.log.id,
                    status = entry.log.status
                )
            )
            covered.add(Triple(entry.log.medicationId, entry.log.scheduleId, bucket))
        }
    }

    medications.forEach { mws ->
        mws.schedules.forEach { schedule ->
            occurrencesForDate(schedule, date).forEach { (time, timeIndex) ->
                val bucket = roundDownToHalfHour(time)
                val key = Triple(mws.medication.id, schedule.id, bucket)
                if (key !in covered) {
                    val doseOverride = schedule.doseOverrideFor(timeIndex)
                    val dose = doseOverride.ifBlank { mws.medication.dosage }
                    result.getOrPut(bucket) { mutableListOf() }.add(
                        GridCell(
                            medicationId = mws.medication.id,
                            medicationName = mws.medication.name,
                            doseLabel = dose,
                            logId = null,
                            status = null
                        )
                    )
                    covered.add(key)
                }
            }
        }
    }
    return result
}

/**
 * Glavna stranica — tabela lekova: redovi su sati (podeok 30 min), kolone su dani (klizeći
 * horizontalno). Klik na prazno polje dodeljuje lek tom terminu; polje sa već dodeljenim lekom
 * (stvarnim ili prognoziranim na osnovu rasporeda) otvara detalje/akcije. Skeniranje barkoda
 * filtrira sve vidljive dane na taj lek; u zaglavlju svakog dana može se ručno filtrirati na
 * jedan od lekova prisutnih tog dana.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScheduleGridScreen(
    viewModel: MedicationViewModel,
    onOpenMedications: () -> Unit,
    onOpenMedication: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppointments: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenVitals: () -> Unit,
    onOpenCycleTracker: () -> Unit,
    onScanBarcode: () -> Unit,
    onOpenEmergencyNumbers: () -> Unit,
    onOpenTutorial: () -> Unit
) {
    val context = LocalContext.current
    val medications by viewModel.medications.collectAsState()
    val history by viewModel.history.collectAsState()
    val barcodeFilter by viewModel.barcodeFilter.collectAsState()
    val skin by viewModel.skin.collectAsState()
    val customImagePath by viewModel.customBackgroundImagePath.collectAsState()
    val hasPremiumAccess by viewModel.hasPremiumAccess.collectAsState()

    var menuExpanded by remember { mutableStateOf(false) }
    var showBarcodePremiumDialog by remember { mutableStateOf(false) }
    var activeSlot by remember { mutableStateOf<Pair<LocalDate, LocalTime>?>(null) }
    var assignForSlot by remember { mutableStateOf<Pair<LocalDate, LocalTime>?>(null) }
    val dayFilters = remember { mutableStateMapOf<LocalDate, Long?>() }
    val barcodeNotFoundText = stringResource(R.string.med_list_barcode_not_found)
    val noMedicationToastText = stringResource(R.string.grid_no_medication_toast)

    LaunchedEffect(barcodeFilter, medications) {
        if (barcodeFilter != null && medications.isNotEmpty() && medications.none { it.medication.barcode == barcodeFilter }) {
            Toast.makeText(context, barcodeNotFoundText, Toast.LENGTH_SHORT).show()
            viewModel.setBarcodeFilter(null)
        }
    }

    var currentLangTag by remember {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val tag = if (!appLocales.isEmpty) appLocales[0]?.language else Locale.getDefault().language
        mutableStateOf(tag ?: "sr")
    }

    val barcodeFilterMedId = remember(barcodeFilter, medications) {
        barcodeFilter?.let { bf -> medications.firstOrNull { it.medication.barcode == bf }?.medication?.id }
    }

    val today = remember { LocalDate.now() }
    val dates = remember(today) { (-PAST_DAYS..FUTURE_DAYS).map { today.plusDays(it.toLong()) } }
    val hState = rememberLazyListState(initialFirstVisibleItemIndex = PAST_DAYS)
    val vState = rememberScrollState()
    val rowHeight = ROW_HEIGHT_DP.dp
    val bodyHeight = (ROW_HEIGHT_DP * 48).dp
    val dayColWidth = 112.dp
    val labelColWidth = 56.dp

    val showsPhotoBackdrop = !customImagePath.isNullOrBlank() && (skin == AppSkin.CUSTOM || skin == AppSkin.CRNA)

    Box(modifier = Modifier.fillMaxSize()) {
        if (!showsPhotoBackdrop) {
            SkinArt(skin = skin, modifier = Modifier.fillMaxSize())
        }
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    AppTitleBar(
                        currentLangTag = currentLangTag,
                        onLangSelected = { tag ->
                            currentLangTag = tag
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                        }
                    )
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(onClick = onOpenTutorial) {
                                Icon(Icons.Filled.HelpOutline, contentDescription = stringResource(R.string.med_list_tutorial_desc))
                            }
                        },
                        actions = {
                            IconButton(onClick = onOpenEmergencyNumbers) {
                                Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.med_list_emergency_desc))
                            }
                            IconButton(onClick = { if (hasPremiumAccess) onScanBarcode() else showBarcodePremiumDialog = true }) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.med_list_scan_filter_desc))
                            }
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.med_list_more_options_desc))
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.med_list_menu_medications)) },
                                    leadingIcon = { Icon(Icons.Filled.MedicalServices, contentDescription = null) },
                                    onClick = { menuExpanded = false; onOpenMedications() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.med_list_menu_appointments)) },
                                    leadingIcon = { Icon(Icons.Filled.EventNote, contentDescription = null) },
                                    onClick = { menuExpanded = false; onOpenAppointments() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.med_list_menu_history)) },
                                    leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) },
                                    onClick = { menuExpanded = false; onOpenHistory() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.med_list_menu_documents)) },
                                    leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) },
                                    onClick = { menuExpanded = false; onOpenDocuments() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.med_list_menu_vitals)) },
                                    leadingIcon = { Icon(Icons.Filled.Bloodtype, contentDescription = null) },
                                    onClick = { menuExpanded = false; onOpenVitals() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.med_list_menu_cycle)) },
                                    leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                                    onClick = { menuExpanded = false; onOpenCycleTracker() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.med_list_menu_settings)) },
                                    leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                    onClick = { menuExpanded = false; onOpenSettings() }
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = { AdBannerBar(viewModel) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.80f))
            ) {
                if (barcodeFilter != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.med_list_filter_active_notice),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(onClick = { viewModel.setBarcodeFilter(null) }) {
                            Text(stringResource(R.string.med_list_remove_filter))
                        }
                    }
                }

                if (medications.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.MedicalServices, contentDescription = null, modifier = Modifier.padding(bottom = 16.dp))
                        Text(stringResource(R.string.grid_no_medications_title), style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = onOpenMedications, modifier = Modifier.padding(top = 12.dp)) {
                            Text(stringResource(R.string.grid_no_medications_button))
                        }
                    }
                } else {
                    // Zaglavlje — nazivi dana, ne pomera se vertikalno; horizontalno je uvek
                    // usklađeno sa telom tabele jer koristi isti LazyListState (bez sopstvenog skrola).
                    Row(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Box(modifier = Modifier.width(labelColWidth))
                        LazyRow(state = hState, userScrollEnabled = false, modifier = Modifier.weight(1f)) {
                            items(dates, key = { it.toEpochDay() }) { date ->
                                val dayCells = remember(date, medications, history) { cellsForDate(date, medications, history) }
                                val dayMedOptions = remember(dayCells) {
                                    dayCells.values.flatten().distinctBy { it.medicationId }.map { it.medicationId to it.medicationName }
                                }
                                DayHeaderCell(
                                    date = date,
                                    isToday = date == today,
                                    medicationOptions = dayMedOptions,
                                    selectedFilter = dayFilters[date],
                                    onFilterChange = { dayFilters[date] = it },
                                    modifier = Modifier.width(dayColWidth).fillMaxHeight()
                                )
                            }
                        }
                    }
                    Divider()
                    // Telo tabele — vertikalno klizi (sati), a kolone dana klize horizontalno.
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(vState)
                    ) {
                        Column(modifier = Modifier.width(labelColWidth).height(bodyHeight)) {
                            ALL_HALF_HOUR_SLOTS.forEach { t ->
                                Box(modifier = Modifier.fillMaxWidth().height(rowHeight), contentAlignment = Alignment.Center) {
                                    Text(t.format(TIME_FMT), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        LazyRow(state = hState, modifier = Modifier.height(bodyHeight)) {
                            items(dates, key = { it.toEpochDay() }) { date ->
                                val dayCells = remember(date, medications, history) { cellsForDate(date, medications, history) }
                                val filterMedId = barcodeFilterMedId ?: dayFilters[date]
                                Column(modifier = Modifier.width(dayColWidth)) {
                                    ALL_HALF_HOUR_SLOTS.forEach { t ->
                                        val cellsHere = (dayCells[t] ?: emptyList())
                                            .filter { filterMedId == null || it.medicationId == filterMedId }
                                        GridCellBox(
                                            cells = cellsHere,
                                            isToday = date == today,
                                            modifier = Modifier.width(dayColWidth).height(rowHeight),
                                            onClick = {
                                                if (cellsHere.isEmpty()) {
                                                    if (medications.isEmpty()) {
                                                        Toast.makeText(context, noMedicationToastText, Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        assignForSlot = date to t
                                                    }
                                                } else {
                                                    activeSlot = date to t
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    activeSlot?.let { (date, time) ->
        val dayCells = remember(date, medications, history) { cellsForDate(date, medications, history) }
        val filterMedId = barcodeFilterMedId ?: dayFilters[date]
        val cellsHere = (dayCells[time] ?: emptyList()).filter { filterMedId == null || it.medicationId == filterMedId }
        if (cellsHere.isEmpty()) {
            LaunchedEffect(date, time) { activeSlot = null }
        } else {
            SlotDetailDialog(
                date = date,
                time = time,
                cells = cellsHere,
                onDismiss = { activeSlot = null },
                onMarkStatus = { logId, status -> viewModel.markIntake(logId, status) },
                onDeleteLog = { cell ->
                    cell.logId?.let {
                        viewModel.deleteIntakeLog(
                            IntakeLog(
                                id = it,
                                medicationId = cell.medicationId,
                                scheduleId = 0L,
                                scheduledAtMillis = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                doseLabel = cell.doseLabel,
                                status = cell.status ?: IntakeStatus.NA_CEKANJU
                            )
                        )
                    }
                },
                onMarkTakenNow = { cell ->
                    val millis = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    viewModel.logManualIntake(cell.medicationId, millis, cell.doseLabel) { id ->
                        viewModel.markIntake(id, IntakeStatus.UZETO)
                    }
                },
                onAddAnother = {
                    activeSlot = null
                    assignForSlot = date to time
                },
                onOpenMedication = onOpenMedication
            )
        }
    }

    assignForSlot?.let { (date, time) ->
        AssignMedicationDialog(
            medications = medications,
            onDismiss = { assignForSlot = null },
            onSave = { medicationId, doseLabel ->
                val millis = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                viewModel.logManualIntake(medicationId, millis, doseLabel)
                assignForSlot = null
            }
        )
    }

    if (showBarcodePremiumDialog) {
        PremiumRequiredDialog(
            onDismiss = { showBarcodePremiumDialog = false },
            onUnlock = { viewModel.setPremiumUnlocked(true); showBarcodePremiumDialog = false },
            title = stringResource(R.string.premium_barcode_dialog_title),
            text = stringResource(R.string.premium_barcode_dialog_text)
        )
    }
}

@Composable
private fun DayHeaderCell(
    date: LocalDate,
    isToday: Boolean,
    medicationOptions: List<Pair<Long, String>>,
    selectedFilter: Long?,
    onFilterChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall
        )
        Text(date.format(HEADER_DATE_FMT), style = MaterialTheme.typography.labelMedium)
        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Filled.FilterAlt,
                    contentDescription = stringResource(R.string.grid_day_filter_desc),
                    tint = if (selectedFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.common_all)) },
                    onClick = { onFilterChange(null); menuExpanded = false }
                )
                medicationOptions.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = { onFilterChange(id); menuExpanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun GridCellBox(
    cells: List<GridCell>,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            .background(if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 1.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (cells.isNotEmpty()) {
            Column {
                cells.take(2).forEach { cell ->
                    val color = when (cell.status) {
                        IntakeStatus.UZETO -> MaterialTheme.colorScheme.primary
                        IntakeStatus.PRESKOCENO -> MaterialTheme.colorScheme.error
                        IntakeStatus.NA_CEKANJU -> MaterialTheme.colorScheme.tertiary
                        null -> MaterialTheme.colorScheme.outline
                    }
                    Text(
                        cell.medicationName,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (cells.size > 2) {
                    Text("+${cells.size - 2}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SlotDetailDialog(
    date: LocalDate,
    time: LocalTime,
    cells: List<GridCell>,
    onDismiss: () -> Unit,
    onMarkStatus: (logId: Long, status: IntakeStatus) -> Unit,
    onDeleteLog: (GridCell) -> Unit,
    onMarkTakenNow: (GridCell) -> Unit,
    onAddAnother: () -> Unit,
    onOpenMedication: (Long) -> Unit
) {
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = { Text("${date.format(HEADER_DATE_FMT)} ${time.format(TIME_FMT)}") },
        text = {
            Column {
                cells.forEach { cell ->
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cell.medicationName, style = MaterialTheme.typography.titleSmall)
                                if (cell.logId != null) {
                                    IconButton(onClick = { onDeleteLog(cell) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_delete))
                                    }
                                }
                            }
                            if (cell.doseLabel.isNotBlank()) {
                                Text(cell.doseLabel, style = MaterialTheme.typography.bodySmall)
                            }
                            val (statusLabel, statusColor) = when (cell.status) {
                                IntakeStatus.UZETO -> stringResource(R.string.intake_status_taken) to MaterialTheme.colorScheme.primary
                                IntakeStatus.PRESKOCENO -> stringResource(R.string.intake_status_skipped) to MaterialTheme.colorScheme.error
                                IntakeStatus.NA_CEKANJU -> stringResource(R.string.intake_status_pending) to MaterialTheme.colorScheme.tertiary
                                null -> stringResource(R.string.grid_projected_label) to MaterialTheme.colorScheme.outline
                            }
                            Text(
                                statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Row(modifier = Modifier.padding(top = 6.dp)) {
                                if (cell.logId != null && cell.status == IntakeStatus.NA_CEKANJU) {
                                    TextButton(onClick = { onMarkStatus(cell.logId, IntakeStatus.UZETO) }) {
                                        Text(stringResource(R.string.intake_status_taken))
                                    }
                                    TextButton(onClick = { onMarkStatus(cell.logId, IntakeStatus.PRESKOCENO) }) {
                                        Text(stringResource(R.string.intake_status_skipped))
                                    }
                                }
                                if (cell.logId == null) {
                                    TextButton(onClick = { onMarkTakenNow(cell) }) {
                                        Text(stringResource(R.string.grid_mark_taken_now))
                                    }
                                }
                                TextButton(onClick = { onOpenMedication(cell.medicationId) }) {
                                    Text(stringResource(R.string.common_details))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAddAnother) { Text(stringResource(R.string.grid_add_another)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        }
    )
}

@Composable
private fun AssignMedicationDialog(
    medications: List<MedicationWithSchedules>,
    onDismiss: () -> Unit,
    onSave: (medicationId: Long, doseLabel: String) -> Unit
) {
    var selectedId by remember { mutableStateOf(medications.firstOrNull()?.medication?.id) }
    var selectedName by remember { mutableStateOf(medications.firstOrNull()?.medication?.name ?: "") }
    var dose by remember { mutableStateOf(medications.firstOrNull()?.medication?.dosage ?: "") }
    var menuExpanded by remember { mutableStateOf(false) }

    ThemedDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.grid_assign_dialog_title)) },
        text = {
            Column {
                Box {
                    OutlinedButton(onClick = { menuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            selectedName.ifBlank { stringResource(R.string.grid_assign_medication_label) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        medications.forEach { mws ->
                            DropdownMenuItem(
                                text = { Text(mws.medication.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = {
                                    selectedId = mws.medication.id
                                    selectedName = mws.medication.name
                                    dose = mws.medication.dosage
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
                ThemedTextField(
                    value = dose,
                    onValueChange = { dose = it },
                    label = { Text(stringResource(R.string.grid_assign_dose_label)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedId?.let { onSave(it, dose.trim()) } },
                enabled = selectedId != null
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
