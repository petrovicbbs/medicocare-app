package com.medicocare.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.medicocare.app.data.IntakeStatus
import com.medicocare.app.repository.MedicationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Obrađuje dugmad "Uzeto" / "Preskoči" na notifikaciji podsetnika —
 * ažurira odgovarajući zapis u istoriji i uklanja notifikaciju.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)
        val logId = intent.getLongExtra(NotificationHelper.EXTRA_LOG_ID, -1L)

        val status = when (intent.action) {
            NotificationHelper.ACTION_TAKEN -> IntakeStatus.UZETO
            NotificationHelper.ACTION_SKIP -> IntakeStatus.PRESKOCENO
            else -> null
        }

        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        if (status != null && logId != -1L) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    MedicationRepository(context).markIntake(logId, status)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
