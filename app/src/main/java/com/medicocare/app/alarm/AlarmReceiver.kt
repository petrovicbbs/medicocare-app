package com.medicocare.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medicocare.app.data.AppDatabase
import com.medicocare.app.data.IntakeLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Prima okidanje alarma za jedno vreme jednog rasporeda.
 * Upisuje zapis u istoriju (na čekanju), prikazuje notifikaciju
 * (ako je raspored i dalje aktivan) i zakazuje sledeće okidanje.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        val timeIndex = intent.getIntExtra(AlarmScheduler.EXTRA_TIME_INDEX, 0)
        if (scheduleId <= 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val schedule = db.scheduleDao().getById(scheduleId)
                if (schedule == null || !schedule.enabled) {
                    return@launch
                }
                val medication = db.medicationDao().getById(schedule.medicationId)
                if (medication == null || !medication.active) {
                    return@launch
                }

                // Ako je za baš ovo vreme podešena posebna doza (npr. 0.5 tableta uveče
                // umesto uobičajene 1), koristi nju; inače uobičajenu dozu leka.
                val effectiveDose = schedule.doseOverrideFor(timeIndex).ifBlank { medication.dosage }

                val logId = db.intakeLogDao().insert(
                    IntakeLog(
                        medicationId = medication.id,
                        scheduleId = schedule.id,
                        scheduledAtMillis = System.currentTimeMillis(),
                        doseLabel = effectiveDose
                    )
                )

                val notificationId = AlarmScheduler.requestCode(scheduleId, timeIndex)
                NotificationHelper.showReminder(
                    context = context,
                    notificationId = notificationId,
                    logId = logId,
                    medicationName = medication.name,
                    dosage = effectiveDose
                )

                AlarmScheduler(context).rescheduleNext(schedule, timeIndex, medication.name)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
