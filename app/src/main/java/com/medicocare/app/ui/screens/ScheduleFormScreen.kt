package com.medicocare.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import com.medicocare.app.ui.components.GlassCard
import com.medicocare.app.ui.components.ThemedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.medicocare.app.R
import com.medicocare.app.data.FrequencyType
import com.medicocare.app.data.MedicationSchedule
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.ads.AdBannerBar
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.SkinArt
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleFormScreen(
    medicationId: Long,
    scheduleId: Long,
    viewModel: MedicationViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val detailFlow = remember(medicationId) { viewModel.medicationDetail(medicationId) }
    val detail by detailFlow.collectAsState()
    val medicationName = detail?.medication?.name ?: ""
    val defaultDosage = detail?.medication?.dosage ?: ""
    val existing = detail?.schedules?.firstOrNull { it.id == scheduleId }

    var frequencyType by rememberSaveable { mutableStateOf(FrequencyType.SVAKI_DAN) }
    var times by rememberSaveable { mutableStateOf(listOf<String>()) }
    var doseOverrides by rememberSaveable { mutableStateOf(listOf<String>()) }
    var selectedDays by rememberSaveable { mutableStateOf(setOf<Int>()) }
    var intervalHours by rememberSaveable { mutableStateOf("6") }
    var startTime by rememberSaveable { mutableStateOf("08:00") }
    var intervalDoseOverride by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }

    var showTimePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (existing != null && !loaded) {
            frequencyType = existing.frequencyType
            times = existing.timesList()
            val overrides = existing.doseOverridesList()
            doseOverrides = times.indices.map { i -> overrides.getOrElse(i) { "" } }
            selectedDays = existing.daysOfWeekSet()
            if (existing.intervalHours > 0) intervalHours = existing.intervalHours.toString()
            if (existing.startTime.isNotBlank()) startTime = existing.startTime
            intervalDoseOverride = overrides.getOrElse(0) { "" }
            loaded = true
        }
    }

    fun addTime(formatted: String) {
        if (formatted !in times) {
            times = times + formatted
            doseOverrides = doseOverrides + ""
        }
    }

    fun removeTimeAt(index: Int) {
        times = times.toMutableList().also { it.removeAt(index) }
        doseOverrides = doseOverrides.toMutableList().also { it.removeAt(index) }
    }

    fun updateDoseOverrideAt(index: Int, value: String) {
        doseOverrides = doseOverrides.toMutableList().also { it[index] = value }
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
                title = { Text(if (scheduleId != 0L) stringResource(R.string.schedule_form_title_edit) else stringResource(R.string.schedule_form_title_new)) },
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
            Text(stringResource(R.string.schedule_form_frequency_title), style = MaterialTheme.typography.titleSmall)
            Row(Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                FilterChip(
                    selected = frequencyType == FrequencyType.SVAKI_DAN,
                    onClick = { frequencyType = FrequencyType.SVAKI_DAN },
                    label = { Text(stringResource(R.string.schedule_form_freq_every_day)) }
                )
                FilterChip(
                    modifier = Modifier.padding(start = 8.dp),
                    selected = frequencyType == FrequencyType.ODREDJENI_DANI,
                    onClick = { frequencyType = FrequencyType.ODREDJENI_DANI },
                    label = { Text(stringResource(R.string.schedule_form_freq_specific_days)) }
                )
                FilterChip(
                    modifier = Modifier.padding(start = 8.dp),
                    selected = frequencyType == FrequencyType.NA_SVAKIH_X_SATI,
                    onClick = { frequencyType = FrequencyType.NA_SVAKIH_X_SATI },
                    label = { Text(stringResource(R.string.schedule_form_freq_every_x_hours)) }
                )
            }

            if (frequencyType == FrequencyType.ODREDJENI_DANI) {
                Text(stringResource(R.string.schedule_form_days_title), style = MaterialTheme.typography.titleSmall)
                Row(Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                    dayLabelsMap().forEach { (dayNum, label) ->
                        val selected = dayNum in selectedDays
                        FilterChip(
                            modifier = Modifier.padding(end = 4.dp),
                            selected = selected,
                            onClick = {
                                selectedDays = if (selected) selectedDays - dayNum else selectedDays + dayNum
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (frequencyType == FrequencyType.SVAKI_DAN || frequencyType == FrequencyType.ODREDJENI_DANI) {
                Text(stringResource(R.string.schedule_form_times_title), style = MaterialTheme.typography.titleSmall)
                if (defaultDosage.isNotBlank()) {
                    Text(
                        stringResource(R.string.schedule_form_default_dose_note, defaultDosage),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Column(Modifier.padding(top = 8.dp)) {
                    times.forEachIndexed { index, t ->
                        GlassCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(t, modifier = Modifier.width(64.dp))
                                ThemedTextField(
                                    value = doseOverrides.getOrElse(index) { "" },
                                    onValueChange = { updateDoseOverrideAt(index, it) },
                                    label = { Text(stringResource(R.string.schedule_form_dose_for_time_label, t)) },
                                    placeholder = { Text(stringResource(R.string.schedule_form_dose_placeholder)) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                                )
                                IconButton(onClick = { removeTimeAt(index) }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.schedule_form_remove_time_desc))
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(stringResource(R.string.schedule_form_add_time))
                }
            }

            if (frequencyType == FrequencyType.NA_SVAKIH_X_SATI) {
                ThemedTextField(
                    value = intervalHours,
                    onValueChange = { intervalHours = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.schedule_form_interval_hours_label)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                GlassCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.schedule_form_first_time_label, startTime))
                        OutlinedButton(onClick = { showStartTimePicker = true }) {
                            Text(stringResource(R.string.common_change))
                        }
                    }
                }
                ThemedTextField(
                    value = intervalDoseOverride,
                    onValueChange = { intervalDoseOverride = it },
                    label = { Text(stringResource(R.string.schedule_form_interval_dose_label)) },
                    placeholder = { Text(defaultDosage.ifBlank { stringResource(R.string.schedule_form_dose_placeholder) }) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }

            Button(
                onClick = {
                    val schedule = MedicationSchedule(
                        id = scheduleId,
                        medicationId = medicationId,
                        frequencyType = frequencyType,
                        times = MedicationSchedule.joinTimes(times),
                        daysOfWeek = MedicationSchedule.joinDays(selectedDays),
                        intervalHours = intervalHours.toIntOrNull() ?: 0,
                        startTime = startTime,
                        enabled = existing?.enabled ?: true,
                        doseOverrides = if (frequencyType == FrequencyType.NA_SVAKIH_X_SATI) {
                            intervalDoseOverride.trim()
                        } else {
                            MedicationSchedule.joinDoseOverrides(doseOverrides.map { it.trim() })
                        }
                    )
                    viewModel.saveSchedule(schedule, medicationName) { onSaved() }
                },
                enabled = isFormValid(frequencyType, times, selectedDays, intervalHours),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            ) {
                Text(stringResource(R.string.schedule_form_save))
            }

            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(stringResource(R.string.common_cancel))
            }
        }

        if (showTimePicker) {
            TimePickerDialog(
                initialHour = 8,
                initialMinute = 0,
                onConfirm = { h, m ->
                    addTime(String.format(Locale.getDefault(), "%02d:%02d", h, m))
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false }
            )
        }

        if (showStartTimePicker) {
            val parts = startTime.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            TimePickerDialog(
                initialHour = h,
                initialMinute = m,
                onConfirm = { hh, mm ->
                    startTime = String.format(Locale.getDefault(), "%02d:%02d", hh, mm)
                    showStartTimePicker = false
                },
                onDismiss = { showStartTimePicker = false }
            )
        }
    }
    }
}

private fun isFormValid(
    frequencyType: FrequencyType,
    times: List<String>,
    days: Set<Int>,
    intervalHours: String
): Boolean {
    return when (frequencyType) {
        FrequencyType.SVAKI_DAN -> times.isNotEmpty()
        FrequencyType.ODREDJENI_DANI -> times.isNotEmpty() && days.isNotEmpty()
        FrequencyType.NA_SVAKIH_X_SATI -> (intervalHours.toIntOrNull() ?: 0) > 0
    }
}
