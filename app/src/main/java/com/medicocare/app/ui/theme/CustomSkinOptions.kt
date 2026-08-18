package com.medicocare.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Stil fonta koji korisnik može izabrati za Prilagođeni (Custom) skin. */
enum class CustomFontStyle {
    DEFAULT, SERIF, SANS, MONOSPACE, CURSIVE;

    fun toFontFamily(): FontFamily = when (this) {
        DEFAULT -> FontFamily.Default
        SERIF -> FontFamily.Serif
        SANS -> FontFamily.SansSerif
        MONOSPACE -> FontFamily.Monospace
        CURSIVE -> FontFamily.Cursive
    }
}

/** Ponuđene boje za brzi izbor akcenta/pozadine kod Prilagođenog skina (jednostavne palete umesto punog color-pickera). */
val CUSTOM_SKIN_SWATCHES: List<Color> = listOf(
    Color(0xFF616161), // siva
    Color(0xFF2E7D5B), // zelena
    Color(0xFF1565C0), // plava
    Color(0xFF6A1B9A), // ljubičasta
    Color(0xFFE65100), // narandžasta
    Color(0xFFAD1457), // pink
    Color(0xFFF9A825), // žuta
    Color(0xFF263159), // tamnoplava
    Color(0xFFC62828), // crvena
    Color(0xFF00838F), // tirkizna
    Color(0xFFFFFFFF), // bela
    Color(0xFF212121)  // skoro crna
)

/** Podešavanja Prilagođenog (Custom) skina — čuvaju se u SettingsPreferences. */
data class CustomSkinConfig(
    val backgroundImagePath: String?,
    val accentColor: Color,
    val backgroundColor: Color,
    val fontScale: Float,
    val fontStyle: CustomFontStyle
)
