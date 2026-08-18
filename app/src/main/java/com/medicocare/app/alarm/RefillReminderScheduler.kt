package com.medicocare.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.medicocare.app.data.Medication

/**
 * Zakazuje/otkazuje periodičan (netačan, "kad-tad") podsetnik da je vreme dopuniti/podići
 * lek — odvojeno od alarma za uzimanje doze. Ne zahteva dozvolu za precizne alarme.
 */
class RefillReminderScheduler(private val context: Context) {

    companion object {
        const val EXTRA_MEDICATION_ID = "extra_refill_medication_id"

        // Ofset da se request kodovi ne poklope sa AlarmScheduler-om (lekovi) ni
        // AppointmentAlarmScheduler-om (pregledi).
        fun requestCode(medicationId: Long): Int = (4_000_000L + medicationId).toInt()
    }

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    fun schedule(medication: Medication) {
        cancel(medication.id)
        if (!medication.refillReminderEnabled) return
        val triggerAt = medication.nextRefillReminderMillis ?: return
        if (triggerAt <= System.currentTimeMillis()) return

        val pi = buildPendingIntent(medication.id, allowCreate = true) ?: return
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    fun cancel(medicationId: Long) {
        val pi = buildPendingIntent(medicationId, allowCreate = false)
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun buildPendingIntent(medicationId: Long, allowCreate: Boolean): PendingIntent? {
        val intent = Intent(context, RefillReminderReceiver::class.java).apply {
            putExtra(EXTRA_MEDICATION_ID, medicationId)
        }
        val flags = if (allowCreate) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, requestCode(medicationId), intent, flags)
    }
}
