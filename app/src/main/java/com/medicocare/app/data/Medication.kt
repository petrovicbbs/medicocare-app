package com.medicocare.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Jedan lek u evidenciji korisnika.
 *
 * - stockCount: koliko jedinica (tableta/kapsula/ml...) trenutno ima kod kuće; null = ne prati se zaliha.
 * - lowStockThreshold: kad zaliha padne na ovaj broj ili ispod, prikazuje se upozorenje.
 */
@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,       // npr. "500 mg"
    val form: String,         // tableta, kapsula, sirup, injekcija, kap, mast, ostalo...
    val notes: String = "",
    val active: Boolean = true,
    val stockCount: Double? = null,
    val lowStockThreshold: Double = 5.0,
    val category: MedicationCategory = MedicationCategory.LEK,
    val barcode: String? = null,   // za pretragu/filtriranje na glavnoj listi skeniranjem

    // Periodični podsetnik da je vreme dopuniti/podići lek (nezavisno od praćenja tačne zalihe).
    val refillReminderEnabled: Boolean = false,
    val refillIntervalDays: Int = 30,
    val nextRefillReminderMillis: Long? = null
)
