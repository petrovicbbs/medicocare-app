package com.medicocare.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Jedan hitan broj za brzo biranje.
 *
 * - category != null: jedan od 4 osnovna broja (policija/hitna pomoć/vatrogasci/pomoć na putu),
 *   podrazumevano popunjen prema jeziku aplikacije, ali slobodno izmenljiv (npr. ako je korisnik
 *   u drugoj državi od one na koju jezik obično ukazuje). Ovi brojevi su besplatni.
 * - category == null: dodatni, ručno unet broj (npr. "Sused", "Rođak") — Premium funkcija.
 *
 * iconKey: naziv EmergencyIconOption enum vrednosti (npr. "HOUSE") za custom brojeve, da bi se
 * vizuelno lakše razlikovali u listi; null za osnovna 4 broja (ona uvek koriste ikonicu kategorije).
 */
@Entity(tableName = "emergency_numbers")
data class EmergencyNumber(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: EmergencyCategory? = null,
    val label: String = "",
    val phoneNumber: String = "",
    val sortOrder: Int = 0,
    val iconKey: String? = null
)
