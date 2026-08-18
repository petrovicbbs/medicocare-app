package com.medicocare.app.data

/** Vrsta stavke u evidenciji — lek, vitamin ili dodatak ishrani. */
enum class MedicationCategory(val displayName: String) {
    LEK("Lek"),
    VITAMIN("Vitamin"),
    SUPLEMENT("Suplement")
}
