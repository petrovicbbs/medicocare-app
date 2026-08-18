package com.medicocare.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Skinovi (teme u boji i sitnoj ilustraciji) aplikacije. PODRAZUMEVANA (siva) je
 * besplatna, ostale su označene kao Premium (za sada bez pravog plaćanja — vidi SettingsScreen).
 */
enum class AppSkin(
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val premium: Boolean
) {
    PODRAZUMEVANA("Podrazumevana", Color(0xFF616161), Color(0xFFBDBDBD), Color(0xFF424242), premium = false),
    ZELENA("Šuma", Color(0xFF2E7D5B), Color(0xFF8FD8B0), Color(0xFF4A7C59), premium = true),
    PLAVA("Nebo", Color(0xFF1565C0), Color(0xFF90CAF9), Color(0xFF0D47A1), premium = true),
    LJUBICASTA("Lavanda", Color(0xFF6A1B9A), Color(0xFFCE93D8), Color(0xFF4A148C), premium = true),
    NARANDZASTA("Narandža", Color(0xFFE65100), Color(0xFFFFCC80), Color(0xFFBF360C), premium = true),
    ROZE("Lala", Color(0xFFAD1457), Color(0xFFF8BBD0), Color(0xFF880E4F), premium = true),
    ZUTA("Leto", Color(0xFFF9A825), Color(0xFF4FC3F7), Color(0xFFFFB74D), premium = true),
    CRNA("Crna", Color(0xFF263159), Color(0xFFB0BEC5), Color(0xFFFFD54F), premium = true),
    SVITANJE("Svitanje", Color(0xFF8A6FE8), Color(0xFF6E7FE0), Color(0xFF3FB6C9), premium = true),
    MORE("More", Color(0xFF1E8FB0), Color(0xFF6FCBE0), Color(0xFF0D4F63), premium = true),
    // Stvarne boje za CUSTOM se ne čitaju odavde — biraju se u Podešavanjima i primenjuju
    // preko CustomSkinConfig; ove vrednosti su samo rezervni prikaz dok korisnik ne izabere.
    CUSTOM("Prilagođeno", Color(0xFF616161), Color(0xFFBDBDBD), Color(0xFF424242), premium = true)
}

/** Režim svetla/tamna/sistemski. */
enum class ThemeMode(val displayName: String) {
    SISTEMSKI("Sistemski"),
    SVETLA("Svetla"),
    TAMNA("Tamna")
}
