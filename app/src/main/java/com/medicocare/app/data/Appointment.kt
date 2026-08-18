package com.medicocare.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Zakazan pregled/termin kod lekara ili u nekoj ustanovi.
 *
 * - dateTimeMillis: tačno vreme termina
 * - reminderMinutesBefore: koliko minuta pre termina stiže podsetnik (ako je reminderEnabled)
 * - address: koristi se za dugme "Navigacija" (otvara mape)
 */
@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val type: String = "",    // vrsta pregleda, npr. "Opšti pregled", "Laboratorija"...
    val institution: String = "",
    val address: String = "",
    val dateTimeMillis: Long,
    val notes: String = "",
    val reminderEnabled: Boolean = true,
    val reminderMinutesBefore: Int = 60
)
