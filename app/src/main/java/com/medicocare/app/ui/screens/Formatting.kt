package com.medicocare.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.medicocare.app.R
import com.medicocare.app.data.FrequencyType
import com.medicocare.app.data.MedicationSchedule

@Composable
private fun dayLabels(): Map<Int, String> = mapOf(
    1 to stringResource(R.string.day_mon),
    2 to stringResource(R.string.day_tue),
    3 to stringResource(R.string.day_wed),
    4 to stringResource(R.string.day_thu),
    5 to stringResource(R.string.day_fri),
    6 to stringResource(R.string.day_sat),
    7 to stringResource(R.string.day_sun)
)

@Composable
fun dayLabelsMap(): Map<Int, String> = dayLabels()

@Composable
private fun formatTimesWithDoses(schedule: MedicationSchedule): String {
    val times = schedule.timesList()
    if (times.isEmpty()) return stringResource(R.string.schedule_no_times)
    return times.mapIndexed { index, t ->
        val override = schedule.doseOverrideFor(index)
        if (override.isNotBlank()) "$t ($override)" else t
    }.joinToString(", ")
}

@Composable
fun scheduleSummary(schedule: MedicationSchedule): String {
    return when (schedule.frequencyType) {
        FrequencyType.SVAKI_DAN -> stringResource(R.string.schedule_every_day, formatTimesWithDoses(schedule))
        FrequencyType.ODREDJENI_DANI -> {
            val labels = dayLabels()
            val days = schedule.daysOfWeekSet().sorted().joinToString(", ") { labels[it] ?: "?" }
            stringResource(
                R.string.schedule_specific_days,
                days.ifBlank { stringResource(R.string.schedule_no_days) },
                formatTimesWithDoses(schedule)
            )
        }
        FrequencyType.NA_SVAKIH_X_SATI -> {
            val override = schedule.doseOverrideFor(0)
            val doseText = if (override.isNotBlank()) " ($override)" else ""
            stringResource(R.string.schedule_every_x_hours, schedule.intervalHours.toString(), schedule.startTime, doseText)
        }
    }
}

@Composable
fun medicationScheduleOverview(schedules: List<MedicationSchedule>): String {
    if (schedules.isEmpty()) return stringResource(R.string.schedule_none_set)
    val active = schedules.filter { it.enabled }
    if (active.isEmpty()) return stringResource(R.string.schedule_alarms_off)
    val summaries = active.map { scheduleSummary(it) }
    return summaries.joinToString(" • ")
}
