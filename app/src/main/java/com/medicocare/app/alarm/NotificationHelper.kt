package com.medicocare.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.medicocare.app.MainActivity
import com.medicocare.app.R

object NotificationHelper {

    const val CHANNEL_ID = "medication_reminders"
    const val ACTION_TAKEN = "com.medicocare.app.ACTION_TAKEN"
    const val ACTION_SKIP = "com.medicocare.app.ACTION_SKIP"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_LOG_ID = "extra_log_id"
    const val EXTRA_MEDICATION_NAME = "extra_medication_name"
    const val EXTRA_DOSAGE = "extra_dosage"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_med_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notif_med_channel_desc)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showReminder(
        context: Context,
        notificationId: Int,
        logId: Long,
        medicationName: String,
        dosage: String
    ) {
        createChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = android.app.PendingIntent.getActivity(
            context, notificationId, contentIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_TAKEN
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_LOG_ID, logId)
        }
        val takenPendingIntent = android.app.PendingIntent.getBroadcast(
            context, notificationId * 10 + 1, takenIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SKIP
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_LOG_ID, logId)
        }
        val skipPendingIntent = android.app.PendingIntent.getBroadcast(
            context, notificationId * 10 + 2, skipIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (dosage.isNotBlank()) {
            context.getString(R.string.notif_med_text_with_dose, dosage)
        } else {
            context.getString(R.string.notif_med_text_no_dose)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(medicationName)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, context.getString(R.string.notif_action_taken), takenPendingIntent)
            .addAction(0, context.getString(R.string.notif_action_skip), skipPendingIntent)
            .build()

        NotificationManagerCompat.from(context).apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notify(notificationId, notification)
            }
        }
    }
}
