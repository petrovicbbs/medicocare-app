package com.medicocare.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.medicocare.app.data.Appointment
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.ads.AdBannerBar
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.SkinArt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy.")

/** Jedinica za prilagođeni podsetnik pre pregleda. */
private enum class ReminderUnit(val minutesPerUnit: Long) {
    MINUTI(1),
    SATI(60),
    DANI(1440),
    NEDELJE(10080),
    MESEC(43200)
}

@Composable
private fun ReminderUnit.localizedLabel(): String = when (this) {
    ReminderUnit.MINUTI -> stringResource(R.string.reminder_unit_min)
    ReminderUnit.SATI -> stringResource(R.string.reminder_unit_sati)
    ReminderUnit.DANI -> stringResource(R.string.reminder_unit_dana)
    ReminderUnit.NEDELJE -> stringResource(R.string.reminder_unit_nedelje)
    ReminderUnit.MESEC -> stringResource(R.string.reminder_unit_mesec)
}

@Composable
private fun reminderOptions(): List<Pair<Int, String>> = listOf(
    30 to stringResource(R.string.reminder_30min),
    60 to stringResource(R.string.reminder_1h),
    180 to stringResource(R.string.reminder_3h),
    1440 to stringResource(R.string.reminder_1day)
)

/** Nalazi najbolju (celobrojnu) jedinicu za prikaz proizvoljnog broja minuta. */
private fun bestFitReminderUnit(totalMinutes: Int): Pair<Long, ReminderUnit> {
    return when {
        totalMinutes >= 43200 && totalMinutes % 43200 == 0 -> (totalMinutes / 43200).toLong() to ReminderUnit.MESEC
        totalMinutes >= 10080 && totalMinutes % 10080 == 0 -> (totalMinutes / 10080).toLong() to ReminderUnit.NEDELJE
        totalMinutes >= 1440 && totalMinutes % 1440 == 0 -> (totalMinutes / 1440).toLong() to ReminderUnit.DANI
        totalMinutes >= 60 && totalMinutes % 60 == 0 -> (totalMinutes / 60).toLong() to ReminderUnit.SATI
        else -> totalMinutes.toLong() to ReminderUnit.MINUTI
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAppointmentScreen(
    appointmentId: Long,
    viewModel: MedicationViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val appointments by viewModel.appointments.collectAsState()
    val existing = appointments.firstOrNull { it.id == appointmentId }
    val appointmentTypes = stringArrayResource(R.array.appointment_types)
    val reminderOptions = reminderOptions()

    var title by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var institution by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var selectedDateMillis by rememberSaveable { mutableStateOf(LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()) }
    var timeText by rememberSaveable { mutableStateOf("09:00") }
    var reminderEnabled by rememberSaveable { mutableStateOf(true) }
    var reminderMinutes by rememberSaveable { mutableStateOf(60) }
    var isCustomReminder by rememberSaveable { mutableStateOf(false) }
    var customReminderAmount by rememberSaveable { mutableStateOf("1") }
    var customReminderUnit by rememberSaveable { mutableStateOf(ReminderUnit.SATI) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (existing != null && !loaded) {
            title = existing.title
            type = existing.type
            institution = existing.institution
            address = existing.address
            notes = existing.notes
            reminderEnabled = existing.reminderEnabled
            val existingMinutes = existing.reminderMinutesBefore
            if (reminderOptions.any { it.first == existingMinutes }) {
                isCustomReminder = false
                reminderMinutes = existingMinutes
            } else {
                isCustomReminder = true
                val (amount, unit) = bestFitReminderUnit(existingMinutes)
                customReminderAmount = amount.toString()
                customReminderUnit = unit
            }
            val zoned = Instant.ofEpochMilli(existing.dateTimeMillis).atZone(ZoneId.systemDefault())
            selectedDateMillis = zoned.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            timeText = String.format(Locale.getDefault(), "%02d:%02d", zoned.hour, zoned.minute)
            loaded = true
        }
    }

    val dateLabel = DATE_LABEL_FORMAT.format(Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneOffset.UTC))

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
                title = { Text(if (appointmentId != 0L) stringResource(R.string.appt_form_title_edit) else stringResource(R.string.appt_form_title_new)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        bottomBar = { AdBannerBar(viewModel) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ThemedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.appt_form_name_label)) },
                placeholder = { Text(stringResource(R.string.appt_form_name_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = it },
                modifier = Modifier.padding(top = 12.dp)
            ) {
                ThemedTextField(
                    value = type,
                    onValueChange = { type = it; typeMenuExpanded = true },
                    label = { Text(stringResource(R.string.appt_form_type_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                val filteredTypes = appointmentTypes.filter { it.contains(type, ignoreCase = true) || type.isBlank() }
                if (filteredTypes.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        filteredTypes.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    type = option
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            ThemedTextField(
                value = institution,
                onValueChange = { institution = it },
                label = { Text(stringResource(R.string.appt_form_institution_label)) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            ThemedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(stringResource(R.string.appt_form_address_label)) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            GlassCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.appt_form_date_label, dateLabel))
                    OutlinedButton(onClick = { showDatePicker = true }) { Text(stringResource(R.string.common_change)) }
                }
            }
            GlassCard(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.appt_form_time_label, timeText))
                    OutlinedButton(onClick = { showTimePicker = true }) { Text(stringResource(R.string.common_change)) }
                }
            }

            ThemedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.common_notes_optional)) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.appt_form_reminder_title), style = MaterialTheme.typography.titleSmall)
                Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
            }
            if (reminderEnabled) {
                Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState())) {
                    reminderOptions.forEach { (minutes, label) ->
                        FilterChip(
                            modifier = Modifier.padding(end = 8.dp),
                            selected = !isCustomReminder && reminderMinutes == minutes,
                            onClick = { isCustomReminder = false; reminderMinutes = minutes },
                            label = { Text(label) }
                        )
                    }
                    FilterChip(
                        selected = isCustomReminder,
                        onClick = { isCustomReminder = true },
                        label = { Text(stringResource(R.string.appt_form_reminder_custom)) }
                    )
                }
                if (isCustomReminder) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemedTextField(
                            value = customReminderAmount,
                            onValueChange = { customReminderAmount = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.appt_form_reminder_number_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(90.dp)
                        )
                        Row(
                            modifier = Modifier.padding(start = 8.dp).horizontalScroll(rememberScrollState())
                        ) {
                            ReminderUnit.entries.forEach { unit ->
                                FilterChip(
                                    modifier = Modifier.padding(end = 4.dp),
                                    selected = customReminderUnit == unit,
                                    onClick = { customReminderUnit = unit },
                                    label = { Text(unit.localizedLabel()) }
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val datePart = Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    val parts = timeText.split(":")
                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val finalMillis = datePart.atTime(hour, minute)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()

                    val effectiveReminderMinutes = if (isCustomReminder) {
                        val amount = (customReminderAmount.toLongOrNull() ?: 1L).coerceAtLeast(1L)
                        (amount * customReminderUnit.minutesPerUnit).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    } else {
                        reminderMinutes
                    }

                    val appointment = Appointment(
                        id = appointmentId,
                        title = title.trim(),
                        type = type.trim(),
                        institution = institution.trim(),
                        address = address.trim(),
                        dateTimeMillis = finalMillis,
                        notes = notes.trim(),
                        reminderEnabled = reminderEnabled,
                        reminderMinutesBefore = effectiveReminderMinutes
                    )
                    viewModel.saveAppointment(appointment) { onSaved() }
                },
                enabled = title.isNotBlank(),
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

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                        showDatePicker = false
                    }) { Text(stringResource(R.string.common_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showTimePicker) {
            val parts = timeText.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            TimePickerDialog(
                initialHour = h,
                initialMinute = m,
                onConfirm = { hh, mm ->
                    timeText = String.format(Locale.getDefault(), "%02d:%02d", hh, mm)
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false }
            )
        }
    }
    }
}
