package com.medicocare.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Jedan slikan/dokumentovan zapis (npr. laboratorijska analiza, nalaz, izveštaj)
 * u hronološkom registru pacijenta.
 *
 * - filePath: putanja do slike sačuvane u internom skladištu aplikacije.
 * - appointmentId: opciona veza sa zakazanim pregledom na koji se izveštaj odnosi.
 */
@Entity(tableName = "lab_documents")
data class LabDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dateMillis: Long,
    val filePath: String,
    val appointmentId: Long? = null,
    val notes: String = ""
)
