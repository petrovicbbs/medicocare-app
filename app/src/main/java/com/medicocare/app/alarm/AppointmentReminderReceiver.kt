package com.medicocare.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medicocare.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Prima okidanje podsetnika za zakazan pregled i prikazuje notifikaciju. */
class AppointmentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AppointmentAlarmScheduler.EXTRA_APPOINTMENT_ID, -1L)
        if (id <= 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appointment = AppDatabase.getInstance(context).appointmentDao().getById(id)
                if (appointment != null) {
                    AppointmentNotificationHelper.showReminder(context, appointment)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
