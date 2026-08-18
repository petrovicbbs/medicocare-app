package com.medicocare.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medicocare.app.data.AppDatabase
import com.medicocare.app.repository.MedicationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Ponovo zakazuje sve aktivne alarme (lekove i preglede) nakon restarta uređaja
 * (AlarmManager alarmi se brišu pri gašenju telefona).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val medicationRepository = MedicationRepository(context)
                    medicationRepository.rescheduleAllEnabled()
                    medicationRepository.rescheduleAllRefillReminders()

                    val appointmentScheduler = AppointmentAlarmScheduler(context)
                    val appointments = AppDatabase.getInstance(context).appointmentDao().getAllWithReminders()
                    appointments.forEach { appointmentScheduler.schedule(it) }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
