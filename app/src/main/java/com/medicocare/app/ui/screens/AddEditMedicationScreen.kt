package com.medicocare.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WorkspacePremium
import com.medicocare.app.ui.components.GlassCard
import com.medicocare.app.ui.components.ThemedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.medicocare.app.R
import com.medicocare.app.data.Medication
import com.medicocare.app.data.MedicationCategory
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.ads.AdBannerBar
import com.medicocare.app.ui.localizedName
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.SkinArt
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val REFILL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.")

/** Razdvaja slobodan tekst doze (npr. "500 mg") na broj i jedinicu mere. */
private fun splitDosage(dosage: String): Pair<String, String> {
    val trimmed = dosage.trim()
    val match = Regex("^([\\d.,]+)\\s*(.*)$").find(trimmed) ?: return "" to trimmed
    return match.groupValues[1] to match.groupValues[2].trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationScreen(
    medicationId: Long,
    viewModel: MedicationViewModel,
    onSaved: (Long) -> Unit,
    onCancel: () -> Unit,
    onScanBarcode: () -> Unit
) {
    val isEditing = medicationId != 0L
    // medicationDetail(0) simply emits null forever (no matching row), so it's safe to
    // always subscribe — this keeps the flow stable across recompositions.
    val detailFlow = remember(medicationId) { viewModel.medicationDetail(medicationId) }
    val existing by detailFlow.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val premiumUnlocked by viewModel.hasPremiumAccess.collectAsState()

    val medicationForms = stringArrayResource(R.array.medication_forms)
    val doseUnits = stringArrayResource(R.array.dose_units)

    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(MedicationCategory.LEK) }
    var doseAmount by rememberSaveable { mutableStateOf("") }
    var doseUnit by rememberSaveable { mutableStateOf("") }
    var form by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var barcode by rememberSaveable { mutableStateOf<String?>(null) }
    var trackStock by rememberSaveable { mutableStateOf(false) }
    var stockCountText by rememberSaveable { mutableStateOf("") }
    var thresholdText by rememberSaveable { mutableStateOf("5") }
    var refillReminderEnabled by rememberSaveable { mutableStateOf(false) }
    var refillIntervalDaysText by rememberSaveable { mutableStateOf("30") }
    // Dok korisnik ručno ne izabere datum (dugme "Promeni"), sledeći podsetnik se prikazuje
    // kao živa procena "danas + interval" koja prati polje intervala. Čim korisnik izabere
    // datum, ili se učita postojeći lek sa već sačuvanim terminom, taj datum se poštuje.
    var refillDateManuallySet by rememberSaveable { mutableStateOf(false) }
    var refillDateMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var showRefillDatePicker by remember { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var formMenuExpanded by remember { mutableStateOf(false) }
    var doseUnitMenuExpanded by remember { mutableStateOf(false) }
    // Brojači kucanja za "jedinicu mere" i "Oblik leka" — meni se NE otvara odmah na svaki
    // taster (to je smetalo pri brisanju više slova zaredom: meni bi se otvorio posle svakog
    // brisanja i morao bi prvo da se zatvori da bi se obrisalo sledeće slovo). Umesto toga,
    // meni se otvara tek kad korisnik napravi kratku pauzu u kucanju (debounce ispod).
    var doseUnitEditTick by remember { mutableStateOf(0) }
    var formEditTick by remember { mutableStateOf(0) }
    var showBarcodePremiumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(doseUnitEditTick) {
        if (doseUnitEditTick == 0) return@LaunchedEffect
        delay(350)
        doseUnitMenuExpanded = true
    }
    LaunchedEffect(formEditTick) {
        if (formEditTick == 0) return@LaunchedEffect
        delay(350)
        formMenuExpanded = true
    }

    fun intervalMillisFromText(): Long =
        (refillIntervalDaysText.toLongOrNull()?.coerceAtLeast(1) ?: 30L) * 24L * 60 * 60 * 1000

    LaunchedEffect(existing) {
        val med = existing?.medication
        if (med != null && !loaded) {
            name = med.name
            category = med.category
            val (amt, unit) = splitDosage(med.dosage)
            doseAmount = amt
            doseUnit = unit
            form = med.form
            notes = med.notes
            if (med.stockCount != null) {
                trackStock = true
                stockCountText = formatQuantity(med.stockCount)
                thresholdText = formatQuantity(med.lowStockThreshold)
            }
            refillReminderEnabled = med.refillReminderEnabled
            refillIntervalDaysText = med.refillIntervalDays.toString()
            if (med.nextRefillReminderMillis != null) {
                refillDateMillis = med.nextRefillReminderMillis
                refillDateManuallySet = true
            }
            loaded = true
        }
    }

    // Kad se korisnik vrati sa skeniranja barkoda: ako je barkod ranije viđen, sva polja
    // se odmah popune; ako nije, samo se zapamti kod da bi se sačuvao uz ručno unet naziv.
    LaunchedEffect(scanResult) {
        scanResult?.let { result ->
            barcode = result.barcode
            if (!result.name.isNullOrBlank()) name = result.name
            if (!result.dosage.isNullOrBlank()) {
                val (amt, unit) = splitDosage(result.dosage)
                doseAmount = amt
                doseUnit = unit
            }
            if (!result.form.isNullOrBlank()) form = result.form
            viewModel.clearScanResult()
        }
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
                title = { Text(if (isEditing) stringResource(R.string.med_form_title_edit) else stringResource(R.string.med_form_title_new)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        bottomBar = { AdBannerBar(viewModel) },
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ThemedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.med_form_name_label)) },
                trailingIcon = {
                    IconButton(onClick = { if (premiumUnlocked) onScanBarcode() else showBarcodePremiumDialog = true }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.med_form_scan_desc))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (barcode != null) {
                Text(
                    stringResource(R.string.med_form_barcode_linked, barcode ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                MedicationCategory.entries.forEach { option ->
                    FilterChip(
                        modifier = Modifier.padding(end = 8.dp),
                        selected = category == option,
                        onClick = { category = option },
                        label = { Text(option.localizedName()) }
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                ThemedTextField(
                    value = doseAmount,
                    onValueChange = { doseAmount = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text(stringResource(R.string.med_form_dose_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                ExposedDropdownMenuBox(
                    expanded = doseUnitMenuExpanded,
                    onExpandedChange = { doseUnitMenuExpanded = it },
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    ThemedTextField(
                        value = doseUnit,
                        onValueChange = { doseUnit = it; doseUnitEditTick++ },
                        label = { Text(stringResource(R.string.med_form_unit_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doseUnitMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    val filteredUnits = doseUnits.filter { it.contains(doseUnit, ignoreCase = true) || doseUnit.isBlank() }
                    if (filteredUnits.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = doseUnitMenuExpanded,
                            onDismissRequest = { doseUnitMenuExpanded = false }
                        ) {
                            filteredUnits.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        doseUnit = option
                                        doseUnitMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = formMenuExpanded,
                onExpandedChange = { formMenuExpanded = it },
                modifier = Modifier.padding(top = 12.dp)
            ) {
                ThemedTextField(
                    value = form,
                    onValueChange = { form = it; formEditTick++ },
                    label = { Text(stringResource(R.string.med_form_form_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                val filteredForms = medicationForms.filter { it.contains(form, ignoreCase = true) || form.isBlank() }
                if (filteredForms.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = formMenuExpanded,
                        onDismissRequest = { formMenuExpanded = false }
                    ) {
                        filteredForms.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    form = option
                                    formMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            ThemedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.common_notes_optional)) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                minLines = 2
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(R.string.med_form_stock_premium_title),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    if (!premiumUnlocked) {
                        Text(
                            stringResource(R.string.premium_not_enabled_test),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        TextButton(
                            onClick = { viewModel.setPremiumUnlocked(true) },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(stringResource(R.string.common_unlock_test))
                        }
                    } else {
                        Switch(
                            checked = trackStock,
                            onCheckedChange = { trackStock = it }
                        )
                        if (trackStock) {
                            ThemedTextField(
                                value = stockCountText,
                                onValueChange = { stockCountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                                label = { Text(stringResource(R.string.med_form_stock_current_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                            ThemedTextField(
                                value = thresholdText,
                                onValueChange = { thresholdText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                                label = { Text(stringResource(R.string.med_form_stock_threshold_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(R.string.med_form_refill_premium_title),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    if (!premiumUnlocked) {
                        Text(
                            stringResource(R.string.premium_not_enabled_test),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        TextButton(
                            onClick = { viewModel.setPremiumUnlocked(true) },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(stringResource(R.string.common_unlock_test))
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Switch(
                                checked = refillReminderEnabled,
                                onCheckedChange = { refillReminderEnabled = it }
                            )
                            Text(
                                stringResource(R.string.med_form_refill_toggle_label),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        if (refillReminderEnabled) {
                            ThemedTextField(
                                value = refillIntervalDaysText,
                                onValueChange = { refillIntervalDaysText = it.filter { c -> c.isDigit() } },
                                label = { Text(stringResource(R.string.med_form_refill_interval_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                            val effectiveRefillMillis = if (refillDateManuallySet) {
                                refillDateMillis
                            } else {
                                System.currentTimeMillis() + intervalMillisFromText()
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(
                                        R.string.med_form_refill_date_label,
                                        REFILL_DATE_FORMAT.format(Instant.ofEpochMilli(effectiveRefillMillis).atZone(ZoneId.systemDefault()))
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedButton(onClick = {
                                    if (!refillDateManuallySet) refillDateMillis = effectiveRefillMillis
                                    showRefillDatePicker = true
                                }) { Text(stringResource(R.string.common_change)) }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val effectiveEnabled = premiumUnlocked && refillReminderEnabled
                    val finalRefillMillis = if (effectiveEnabled) {
                        var millis = if (refillDateManuallySet) refillDateMillis else (System.currentTimeMillis() + intervalMillisFromText())
                        val intervalMillis = intervalMillisFromText()
                        // Sigurnosna mreža: ako je izabran datum u prošlosti, pomeri unapred
                        // za interval dok ne bude budući, da podsetnik sigurno bude zakazan.
                        while (millis <= System.currentTimeMillis()) millis += intervalMillis
                        millis
                    } else {
                        existing?.medication?.nextRefillReminderMillis
                    }
                    val medication = Medication(
                        id = medicationId,
                        name = name.trim(),
                        category = category,
                        dosage = "${doseAmount.trim()} ${doseUnit.trim()}".trim(),
                        form = form.trim(),
                        notes = notes.trim(),
                        stockCount = if (trackStock) {
                            stockCountText.replace(',', '.').toDoubleOrNull() ?: 0.0
                        } else null,
                        lowStockThreshold = thresholdText.replace(',', '.').toDoubleOrNull() ?: 5.0,
                        barcode = barcode ?: existing?.medication?.barcode,
                        refillReminderEnabled = effectiveEnabled,
                        refillIntervalDays = refillIntervalDaysText.toIntOrNull()?.coerceAtLeast(1) ?: 30,
                        nextRefillReminderMillis = finalRefillMillis
                    )
                    val savedBarcode = barcode
                    if (savedBarcode != null) {
                        viewModel.rememberBarcode(savedBarcode, medication.name, medication.dosage, medication.form)
                    }
                    viewModel.saveMedication(medication) { savedId -> onSaved(savedId) }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            ) {
                Text(stringResource(R.string.common_save))
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        }

        if (showRefillDatePicker) {
            val baseMillis = if (refillDateManuallySet) refillDateMillis else (System.currentTimeMillis() + intervalMillisFromText())
            val initialUtcMidnight = Instant.ofEpochMilli(baseMillis).atZone(ZoneOffset.UTC).toLocalDate()
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMidnight)
            DatePickerDialog(
                onDismissRequest = { showRefillDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { pickedUtcMidnight ->
                            val pickedDate = Instant.ofEpochMilli(pickedUtcMidnight).atZone(ZoneOffset.UTC).toLocalDate()
                            val existingTime = Instant.ofEpochMilli(baseMillis).atZone(ZoneId.systemDefault()).toLocalTime()
                            refillDateMillis = pickedDate.atTime(existingTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            refillDateManuallySet = true
                        }
                        showRefillDatePicker = false
                    }) { Text(stringResource(R.string.common_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showRefillDatePicker = false }) { Text(stringResource(R.string.common_cancel)) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
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
    }
}

/** Prikazuje broj bez suvišnih nula (5.0 -> "5", 0.5 -> "0.5"). */
private fun formatQuantity(value: Double): String {
    return if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
