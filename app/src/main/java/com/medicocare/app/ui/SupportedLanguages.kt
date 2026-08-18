package com.medicocare.app.ui

/**
 * Jezici koje aplikacija podržava (tag, naziv na sopstvenom jeziku). Deljeno između
 * SettingsScreen (puna lista sa nazivima) i brzog izbora jezika na glavnom ekranu
 * (samo skraćena 2-3 slovna oznaka, npr. "SR", "EN", "MK").
 */
object SupportedLanguages {
    val ALL: List<Pair<String, String>> = listOf(
        "sr" to "Srpski",
        "en" to "English",
        "mk" to "Македонски",
        "hr" to "Hrvatski",
        "hu" to "Magyar",
        "ru" to "Русский",
        "de" to "Deutsch",
        "it" to "Italiano",
        "fr" to "Français",
        "es" to "Español",
        "sv" to "Svenska",
        "ro" to "Română",
        "pt" to "Português",
        "ar" to "العربية",
        "tr" to "Türkçe"
    )

    /** Kratka oznaka jezika (2-3 slova) za prikaz u kompaktnom biraču, npr. "SR". */
    fun shortCode(tag: String): String = tag.uppercase()
}
