package com.medicocare.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Jedan zabeležen menstrualni ciklus — početak (obavezno) i kraj (opciono, ako je
 * korisnica zabeležila i kraj perioda). Koristi se za procenu narednog ciklusa i
 * plodnog perioda na osnovu proseka prethodnih ciklusa.
 */
@Entity(tableName = "cycle_entries")
data class CycleEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startDateMillis: Long,
    val endDateMillis: Long? = null,
    val notes: String = ""
)
