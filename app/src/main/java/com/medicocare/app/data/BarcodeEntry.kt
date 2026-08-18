package com.medicocare.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lokalna "memorija" barkod → lek. Ne postoji javna baza barkodova za lekove u Srbiji,
 * pa aplikacija sama uči: prvi put kad korisnik ručno unese naziv za skenirani barkod,
 * to se zapamti ovde. Sledeći put kad se isti barkod skenira (npr. nova kutija istog leka),
 * podaci se automatski popune, bez interneta.
 */
@Entity(tableName = "barcode_entries")
data class BarcodeEntry(
    @PrimaryKey val barcode: String,
    val name: String,
    val dosage: String,
    val form: String
)
