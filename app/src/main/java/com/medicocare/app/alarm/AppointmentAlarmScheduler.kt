package com.medicocare.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.medicocare.app.data.Appointment

/**
 * Zakazuje/otkazuje AlarmManager podsetnik za jedan pregled (jedan alarm po pregledu,
 * u trenutku dateTimeMillis - reminderMinutesBefore).
 */
class AppointmentAlarmScheduler(private val context: Context) {

    companion object {
        const val EXTRA_APPOINTMENT_ID = "extra_appointment_id"

        // Ofset da se request kodovi ne poklope sa AlarmScheduler-om za lekove.
        fun requestCode(appointmentId: Long): Int = (2_000_000L + appointmentId).toInt()
    }

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    fun schedule(appointment: Appointment) {
        cancel(appointment.id)
        if (!appointment.reminderEnabled) return

        val triggerAt = appointment.dateTimeMillis - appointment.reminderMinutesBefore * 60_000L
        if (triggerAt <= System.currentTimeMillis()) return

        val pi = buildPendingIntent(appointment.id, allowCreate = true) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(appointmentId: Long) {
        val pi = buildPendingIntent(appointmentId, allowCreate = false)
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun buildPendingIntent(appointmentId: Long, allowCreate: Boolean): PendingIntent? {
        val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
            putExtra(EXTRA_APPOINTMENT_ID, appointmentId)
        }
        val flags = if (allowCreate) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, requestCode(appointmentId), intent, flags)
    }
}
