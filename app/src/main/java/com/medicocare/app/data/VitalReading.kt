package com.medicocare.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Jedno merenje pritiska ili šećera u krvi.
 *
 * - valuePrimary: sistolni pritisak (PRITISAK) ili vrednost šećera (SECER).
 * - valueSecondary: dijastolni pritisak — koristi se samo za PRITISAK.
 * - pulse: puls u otkucajima u minuti, opciono (obično se meri uz pritisak).
 * - unit: jedinica mere vrednosti (npr. "mmHg", "mmol/L", "mg/dL").
 */
@Entity(tableName = "vital_readings")
data class VitalReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: VitalType,
    val dateTimeMillis: Long,
    val valuePrimary: Double,
    val valueSecondary: Double? = null,
    val pulse: Int? = null,
    val unit: String = "",
    val notes: String = ""
)
