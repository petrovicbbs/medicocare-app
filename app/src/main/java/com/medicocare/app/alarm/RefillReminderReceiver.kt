package com.medicocare.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medicocare.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Prima okidanje periodičnog podsetnika za dopunu leka, prikazuje notifikaciju i
 * pomera sledeći termin za refillIntervalDays unapred (ciklično ponavljanje).
 */
class RefillReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(RefillReminderScheduler.EXTRA_MEDICATION_ID, -1L)
        if (id <= 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).medicationDao()
                val medication = dao.getById(id)
                if (medication != null && medication.refillReminderEnabled) {
                    RefillReminderNotificationHelper.showReminder(context, medication)

                    val base = medication.nextRefillReminderMillis ?: System.currentTimeMillis()
                    val intervalMillis = medication.refillIntervalDays.coerceAtLeast(1) * TimeUnit.DAYS.toMillis(1)
                    val updated = medication.copy(nextRefillReminderMillis = base + intervalMillis)
                    dao.update(updated)
                    RefillReminderScheduler(context).schedule(updated)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
