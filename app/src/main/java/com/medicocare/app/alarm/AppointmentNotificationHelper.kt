package com.medicocare.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.medicocare.app.MainActivity
import com.medicocare.app.R
import com.medicocare.app.data.Appointment
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AppointmentNotificationHelper {

    const val CHANNEL_ID = "appointment_reminders"
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM. 'u' HH:mm")

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_appt_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notif_appt_channel_desc)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showReminder(context: Context, appointment: Appointment) {
        createChannel(context)

        val notificationId = (3_000_000L + appointment.id).toInt()
        val whenText = TIME_FORMAT.format(
            Instant.ofEpochMilli(appointment.dateTimeMillis).atZone(ZoneId.systemDefault())
        )
        val subtitle = listOf(appointment.institution, whenText).filter { it.isNotBlank() }.joinToString(" • ")

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, notificationId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(context.getString(R.string.notif_appt_title, appointment.title))
            .setContentText(subtitle.ifBlank { context.getString(R.string.notif_appt_default_text) })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (appointment.address.isNotBlank()) {
            // Navigacija je Premium funkcija (isto kao dugme na ekranu pregleda), pa se dodir
            // na akciju notifikacije prvo šalje na AppointmentNavigationReceiver koji proverava
            // pristup i tek onda otvara mape (ili samu aplikaciju ako Premium nije aktivan).
            val navIntent = Intent(context, AppointmentNavigationReceiver::class.java).apply {
                putExtra(AppointmentNavigationReceiver.EXTRA_ADDRESS, appointment.address)
            }
            val navPendingIntent = PendingIntent.getBroadcast(
                context, notificationId + 1, navIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, context.getString(R.string.notif_appt_navigation_action), navPendingIntent)
        }

        NotificationManagerCompat.from(context).apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notify(notificationId, builder.build())
            }
        }
    }
}
