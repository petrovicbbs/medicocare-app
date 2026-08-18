package com.medicocare.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Raspored (učestalost) uzimanja leka + podešavanje alarma.
 * Jedan lek može imati više rasporeda (npr. ujutru drugačije doba nego uveče).
 *
 * - times: lista vremena u formatu "HH:mm" odvojena zarezom, koristi se za
 *          SVAKI_DAN i ODREDJENI_DANI
 * - daysOfWeek: brojevi dana u nedelji (1=Ponedeljak ... 7=Nedelja) odvojeni zarezom,
 *          koristi se za ODREDJENI_DANI
 * - intervalHours: koristi se za NA_SVAKIH_X_SATI
 * - startTime: prvo vreme za NA_SVAKIH_X_SATI, format "HH:mm"
 * - doseOverrides: lista doza po vremenu, poravnata sa `times` po indeksu (zarezom odvojena;
 *          prazan element = koristi se podrazumevana doza leka). Za NA_SVAKIH_X_SATI koristi se
 *          samo prvi element (za jedino vreme, startTime). Omogućava npr. da isti lek ima
 *          1 tabletu ujutru/popodne, a 0.5 tableta uveče, bez pravljenja posebnog leka.
 */
@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MedicationSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val frequencyType: FrequencyType,
    val times: String = "",
    val daysOfWeek: String = "",
    val intervalHours: Int = 0,
    val startTime: String = "",
    val enabled: Boolean = true,
    val doseOverrides: String = ""
) {
    fun timesList(): List<String> = times.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun daysOfWeekSet(): Set<Int> =
        daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

    /** Lista dozvola za override, poravnata po indeksu sa timesList() (može sadržati prazne stringove). */
    fun doseOverridesList(): List<String> =
        if (doseOverrides.isEmpty()) emptyList() else doseOverrides.split(",").map { it.trim() }

    /** Override doze za dati indeks vremena, ili prazan string ako nije podešen. */
    fun doseOverrideFor(timeIndex: Int): String =
        doseOverridesList().getOrElse(timeIndex) { "" }

    companion object {
        fun joinTimes(list: List<String>): String = list.joinToString(",")
        fun joinDays(set: Set<Int>): String = set.sorted().joinToString(",")
        fun joinDoseOverrides(list: List<String>): String = list.joinToString(",")
    }
}
