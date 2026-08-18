package com.medicocare.app.data

/**
 * Podrazumevani hitni brojevi po jeziku aplikacije (kao okvirna orijentacija — jezik nije isto
 * što i država, pa su brojevi uvek slobodno izmenljivi u ekranu "Hitni brojevi").
 *
 * Brojevi za Srbiju (192/193/194/1987) i za nekoliko drugih jezika/država gde je potvrđen
 * jedinstven evropski broj 112 (ili poznat nacionalni broj) su provereni pretragom. Za "pomoć na
 * putu" i za arapski (koji pokriva mnogo različitih država sa različitim brojevima) namerno je
 * ostavljeno prazno polje umesto nagađanja — korisnik ga sam popunjava za svoju stvarnu lokaciju.
 */
object DefaultEmergencyNumbers {

    private val DEFAULTS: Map<String, Map<EmergencyCategory, String>> = mapOf(
        "sr" to mapOf(
            EmergencyCategory.POLICE to "192",
            EmergencyCategory.AMBULANCE to "194",
            EmergencyCategory.FIRE to "193",
            EmergencyCategory.ROADSIDE to "1987"
        ),
        "en" to mapOf(
            EmergencyCategory.POLICE to "999",
            EmergencyCategory.AMBULANCE to "999",
            EmergencyCategory.FIRE to "999",
            EmergencyCategory.ROADSIDE to ""
        ),
        "mk" to mapOf(
            EmergencyCategory.POLICE to "192",
            EmergencyCategory.AMBULANCE to "194",
            EmergencyCategory.FIRE to "193",
            EmergencyCategory.ROADSIDE to ""
        ),
        "hr" to mapOf(
            EmergencyCategory.POLICE to "112",
            EmergencyCategory.AMBULANCE to "112",
            EmergencyCategory.FIRE to "112",
            EmergencyCategory.ROADSIDE to ""
        ),
        "hu" to mapOf(
            EmergencyCategory.POLICE to "107",
            EmergencyCategory.AMBULANCE to "104",
            EmergencyCategory.FIRE to "105",
            EmergencyCategory.ROADSIDE to ""
        ),
        "ru" to mapOf(
            EmergencyCategory.POLICE to "102",
            EmergencyCategory.AMBULANCE to "103",
            EmergencyCategory.FIRE to "101",
            EmergencyCategory.ROADSIDE to ""
        ),
        "de" to mapOf(
            EmergencyCategory.POLICE to "110",
            EmergencyCategory.AMBULANCE to "112",
            EmergencyCategory.FIRE to "112",
            EmergencyCategory.ROADSIDE to ""
        ),
        "it" to mapOf(
            EmergencyCategory.POLICE to "112",
            EmergencyCategory.AMBULANCE to "112",
            EmergencyCategory.FIRE to "112",
            EmergencyCategory.ROADSIDE to ""
        ),
        "fr" to mapOf(
            EmergencyCategory.POLICE to "112",
            EmergencyCategory.AMBULANCE to "112",
            EmergencyCategory.FIRE to "112",
            EmergencyCategory.ROADSIDE to ""
        ),
        "es" to mapOf(
            EmergencyCategory.POLICE to "112",
            EmergencyCategory.AMBULANCE to "112",
            EmergencyCategory.FIRE to "112",
            EmergencyCategory.ROADSIDE to ""
        ),
        "sv" to mapOf(
            EmergencyCategory.POLICE to "112",
            EmergencyCategory.AMBULANCE to "112",
            EmergencyCategory.FIRE to "112",
            EmergencyCategory.ROADSIDE to ""
        ),
        "ro" to mapOf(
            EmergencyCategory.POLICE to "112",
            EmergencyCategory.AMBULANCE to "112",
            EmergencyCategory.FIRE to "112",
            EmergencyCategory.ROADSIDE to ""
        ),
        "pt" to mapOf(
            EmergencyCategory.POLICE to "112",
            EmergencyCategory.AMBULANCE to "112",
            EmergencyCategory.FIRE to "112",
            EmergencyCategory.ROADSIDE to ""
        ),
        "ar" to mapOf(
            EmergencyCategory.POLICE to "",
            EmergencyCategory.AMBULANCE to "",
            EmergencyCategory.FIRE to "",
            EmergencyCategory.ROADSIDE to ""
        ),
        "tr" to mapOf(
            EmergencyCategory.POLICE to "112",
            EmergencyCategory.AMBULANCE to "112",
            EmergencyCategory.FIRE to "112",
            EmergencyCategory.ROADSIDE to ""
        )
    )

    private val FALLBACK = mapOf(
        EmergencyCategory.POLICE to "112",
        EmergencyCategory.AMBULANCE to "112",
        EmergencyCategory.FIRE to "112",
        EmergencyCategory.ROADSIDE to ""
    )

    /** [languageTag] npr. "sr", "en"... (samo prva komponenta jezičkog taga, bez regiona). */
    fun forLanguageTag(languageTag: String): Map<EmergencyCategory, String> =
        DEFAULTS[languageTag.lowercase()] ?: FALLBACK
}
