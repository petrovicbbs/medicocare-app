package com.medicocare.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.medicocare.app.MainActivity
import com.medicocare.app.data.SettingsPreferences

/**
 * Obrađuje dodir na dugme "Navigacija" u notifikaciji podsetnika za pregled. Navigacija je
 * Premium funkcija (isto kao dugme na ekranu pregleda) — pošto se ovde ne može prikazati
 * dijalog za otključavanje (nema Activity/Compose konteksta), ako korisnik nema Premium
 * jednostavno se otvara sama aplikacija (gde će, dodirom na isto dugme, videti dijalog).
 */
class AppointmentNavigationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: return
        val hasPremiumAccess = SettingsPreferences(context).hasPremiumAccess()

        val target = if (hasPremiumAccess) {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(address))
            )
        } else {
            Intent(context, MainActivity::class.java)
        }
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(target)
    }

    companion object {
        const val EXTRA_ADDRESS = "extra_address"
    }
}
