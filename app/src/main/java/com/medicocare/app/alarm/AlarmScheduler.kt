package com.medicocare.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.medicocare.app.data.FrequencyType
import com.medicocare.app.data.MedicationSchedule
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Zakazuje/otkazuje AlarmManager alarme za jedan raspored (MedicationSchedule).
 * Svako pojedinačno vreme u rasporedu (timeIndex) dobija sopstveni PendingIntent
 * request code, tako da se svaki alarm može nezavisno (re)zakazati/otkazati.
 */
class AlarmScheduler(private val context: Context) {

    companion object {
        // Maksimalan broj vremena po rasporedu (dovoljno za realnu upotrebu).
        const val MAX_TIMES_PER_SCHEDULE = 12

        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_TIME_INDEX = "extra_time_index"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_DOSAGE = "extra_dosage"

        fun requestCode(scheduleId: Long, timeIndex: Int): Int =
            (scheduleId * 100 + timeIndex).toInt()
    }

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    /** Otkazuje sve moguće alarme za dati raspored (za sve time indekse). */
    fun cancel(schedule: MedicationSchedule) {
        for (timeIndex in 0 until MAX_TIMES_PER_SCHEDULE) {
            val pi = buildPendingIntent(schedule.id, timeIndex, allowCreate = false)
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
    }

    /** Zakazuje sve alarme za dati raspored, prema tipu učestalosti. */
    fun schedule(schedule: MedicationSchedule, medicationName: String) {
        // Prvo očisti stare alarme (npr. ako je korisnik izmenio vremena).
        cancel(schedule)

        if (!schedule.enabled) return

        when (schedule.frequencyType) {
            FrequencyType.SVAKI_DAN -> {
                schedule.timesList().forEachIndexed { index, timeStr ->
                    val time = runCatching { LocalTime.parse(timeStr) }.getOrNull() ?: return@forEachIndexed
                    val trigger = nextTriggerMillis(time, emptySet())
                    setExactAlarm(schedule, index, trigger, medicationName)
                }
            }
            FrequencyType.ODREDJENI_DANI -> {
                val days = schedule.daysOfWeekSet()
                schedule.timesList().forEachIndexed { index, timeStr ->
                    val time = runCatching { LocalTime.parse(timeStr) }.getOrNull() ?: return@forEachIndexed
                    val trigger = nextTriggerMillis(time, days)
                    setExactAlarm(schedule, index, trigger, medicationName)
                }
            }
            FrequencyType.NA_SVAKIH_X_SATI -> {
                val start = runCatching { LocalTime.parse(schedule.startTime) }.getOrNull()
                    ?: LocalTime.of(8, 0)
                val intervalMillis = schedule.intervalHours.coerceAtLeast(1) * 3600_000L
                // Anchor je startTime na današnji datum (može biti u prošlosti); sledeći termin
                // je prvi anchor + k*interval koji je u budućnosti — ovo tačno radi za bilo koji interval.
                val zone = ZoneId.systemDefault()
                val anchor = LocalDate.now(zone).atTime(start).atZone(zone).toInstant().toEpochMilli()
                val now = System.currentTimeMillis()
                var trigger = anchor
                if (trigger <= now) {
                    val periodsElapsed = (now - trigger) / intervalMillis + 1
                    trigger += periodsElapsed * intervalMillis
                }
                setExactAlarm(schedule, 0, trigger, medicationName)
            }
        }
    }

    /** Zakazuje sledeće okidanje za jedan konkretan timeIndex (poziva ga AlarmReceiver nakon okidanja). */
    fun rescheduleNext(schedule: MedicationSchedule, timeIndex: Int, medicationName: String) {
        if (!schedule.enabled) return
        when (schedule.frequencyType) {
            FrequencyType.SVAKI_DAN -> {
                val timeStr = schedule.timesList().getOrNull(timeIndex) ?: return
                val time = runCatching { LocalTime.parse(timeStr) }.getOrNull() ?: return
                val trigger = nextTriggerMillis(time, emptySet(), forceNextDay = true)
                setExactAlarm(schedule, timeIndex, trigger, medicationName)
            }
            FrequencyType.ODREDJENI_DANI -> {
                val timeStr = schedule.timesList().getOrNull(timeIndex) ?: return
                val time = runCatching { LocalTime.parse(timeStr) }.getOrNull() ?: return
                val trigger = nextTriggerMillis(time, schedule.daysOfWeekSet(), forceNextDay = true)
                setExactAlarm(schedule, timeIndex, trigger, medicationName)
            }
            FrequencyType.NA_SVAKIH_X_SATI -> {
                val intervalMillis = schedule.intervalHours.coerceAtLeast(1) * 3600_000L
                val trigger = System.currentTimeMillis() + intervalMillis
                setExactAlarm(schedule, 0, trigger, medicationName)
            }
        }
    }

    private fun nextTriggerMillis(
        time: LocalTime,
        daysOfWeek: Set<Int>,
        forceNextDay: Boolean = false
    ): Long {
        val zone = ZoneId.systemDefault()
        var date = LocalDate.now(zone)
        var candidate = date.atTime(time).atZone(zone)
        val now = java.time.ZonedDateTime.now(zone)

        if (forceNextDay || !candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        if (daysOfWeek.isNotEmpty()) {
            var guard = 0
            while (candidate.dayOfWeek.value !in daysOfWeek && guard < 8) {
                candidate = candidate.plusDays(1)
                guard++
            }
        }
        return candidate.toInstant().toEpochMilli()
    }

    private fun setExactAlarm(
        schedule: MedicationSchedule,
        timeIndex: Int,
        triggerAtMillis: Long,
        medicationName: String
    ) {
        val pi = buildPendingIntent(schedule.id, timeIndex, allowCreate = true, medicationName = medicationName)
            ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun buildPendingIntent(
        scheduleId: Long,
        timeIndex: Int,
        allowCreate: Boolean,
        medicationName: String = ""
    ): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_TIME_INDEX, timeIndex)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
        }
        val flags = if (allowCreate) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, requestCode(scheduleId, timeIndex), intent, flags)
    }
}
