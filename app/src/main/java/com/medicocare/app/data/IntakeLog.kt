package com.medicocare.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Jedan zapis u istoriji: "ovaj lek je trebalo uzeti u ovo vreme".
 * Pravi se čim alarm okine (status = NA_CEKANJU), a zatim se ažurira
 * kad korisnik dodirne "Uzeto"/"Preskoči" na notifikaciji (ili ručno u istoriji).
 *
 * scheduleId nema FK vezu — ako se raspored kasnije izmeni/obriše, istorija ostaje netaknuta
 * (svedoči šta se stvarno dešavalo u tom trenutku).
 */
@Entity(
    tableName = "intake_logs",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class IntakeLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val scheduleId: Long,
    val scheduledAtMillis: Long,
    val doseLabel: String,
    val status: IntakeStatus = IntakeStatus.NA_CEKANJU,
    val actedAtMillis: Long? = null
)

/** IntakeLog + ime leka, za prikaz u istoriji bez dodatnog upita. */
data class IntakeLogView(
    @Embedded val log: IntakeLog,
    val medicationName: String
)
