package com.medicocare.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Font "porodica" koju predlaže aktivni skin, sa mogućnošću da korisnik ručno
 * izabere drugu (ili AUTOMATSKI da prati predlog skina). Pošto aplikacija nema
 * pristup internetu da preuzme prava dodatna pisma (Google Fonts i sl.), sve tri
 * imenovane porodice se oslanjaju isključivo na ugrađene, generičke Compose/Android
 * font porodice — ROUNDED je najbliža aproksimacija (SansSerif) jer sistem nema
 * garantovano dostupno "zaobljeno" pismo bez paketovanja .ttf fajlova.
 */
enum class AppFontFamily {
    AUTOMATSKI, MODERAN, ZAOBLJEN, ELEGANTAN;

    fun resolve(fallback: AppFontFamily): FontFamily {
        val effective = if (this == AUTOMATSKI) fallback else this
        return when (effective) {
            ELEGANTAN -> FontFamily.Serif
            ZAOBLJEN -> FontFamily.SansSerif
            MODERAN, AUTOMATSKI -> FontFamily.SansSerif
        }
    }
}

/** Veličina teksta u celoj aplikaciji (globalno, nezavisno od skina). */
enum class TextSizeOption(val scale: Float) {
    MALA(0.9f),
    NORMALNA(1.0f),
    VELIKA(1.15f),
    VRLO_VELIKA(1.3f)
}

/**
 * Nivo providnosti kartica/prozora preko pozadinske ilustracije. VISOKA_CITLJIVOST
 * podiže neprozirnost svih površina radi maksimalne čitljivosti teksta o lekovima —
 * namerno nema "ekstremne" providnosti ni u standardnom režimu.
 */
enum class TransparencyMode {
    STANDARDNA, VISOKA_CITLJIVOST
}

/** Uključuje/isključuje suptilne animacije prelaza (za korisnike osetljive na pokret). */
enum class AnimationsMode {
    UKLJUCENE, ISKLJUCENE
}

/** Zaobljenost uglova po tipu elementa — deo vizuelnog identiteta svakog skina. */
data class SkinShapes(
    val cardRadius: Dp,
    val buttonRadius: Dp,
    val inputRadius: Dp,
    val dialogRadius: Dp
)

/**
 * "Glass" efekti — providnost površina i jačina senke. Namerno bez pravog Gaussian
 * blur-a (Compose to podržava tek od API 31 preko RenderEffect-a, a aplikacija cilja
 * minSdk 26), pa se "staklo" postiže isključivo providnošću + tankom belom ivicom.
 */
data class SkinEffects(
    val cardAlpha: Float,
    val dialogAlpha: Float,
    val inputAlpha: Float,
    val shadowElevation: Dp
)

/** Kompletan vizuelni "paket" jednog skina, van boja (koje već daje AppSkin). */
data class SkinStyle(
    val suggestedFont: AppFontFamily,
    val shapes: SkinShapes,
    val effects: SkinEffects
)

/**
 * Mapiranje AppSkin -> SkinStyle. Namerno odvojeno od AppSkin enuma (koji već nosi
 * boje, lokalizovano ime i premium status) da se ne bi dirala postojeća, već ožičena
 * infrastruktura (i18n, premium gejting, SkinArt pozadine) — ovo je isključivo dodatni,
 * "stilski" sloj preko toga.
 */
object SkinStyles {
    // Podignuto sa ranijih (previše providnih) vrednosti: pozadinska ilustracija se sada
    // prikazuje iza CELE stranice na svim ekranima, pa kartice/dijalozi/polja moraju biti
    // dovoljno neprozirni da tekst uvek ostane čitljiv preko šarene/tamne slike ispod — bez
    // ovoga se boja teksta (koju bira Material3 na osnovu neprozirne "surface" boje) na
    // pojedinim skinovima gubila u pozadini.
    private val GLASS_MEDIUM = SkinEffects(cardAlpha = 0.91f, dialogAlpha = 0.96f, inputAlpha = 0.87f, shadowElevation = 4.dp)

    fun styleFor(skin: AppSkin): SkinStyle = when (skin) {
        AppSkin.PODRAZUMEVANA -> SkinStyle(
            suggestedFont = AppFontFamily.MODERAN,
            shapes = SkinShapes(cardRadius = 16.dp, buttonRadius = 14.dp, inputRadius = 12.dp, dialogRadius = 20.dp),
            effects = SkinEffects(cardAlpha = 0.95f, dialogAlpha = 0.98f, inputAlpha = 0.92f, shadowElevation = 2.dp)
        )
        AppSkin.LJUBICASTA -> SkinStyle( // Lavanda
            suggestedFont = AppFontFamily.ZAOBLJEN,
            shapes = SkinShapes(cardRadius = 22.dp, buttonRadius = 18.dp, inputRadius = 16.dp, dialogRadius = 26.dp),
            effects = SkinEffects(cardAlpha = 0.92f, dialogAlpha = 0.96f, inputAlpha = 0.87f, shadowElevation = 5.dp)
        )
        AppSkin.ZUTA -> SkinStyle( // Leto
            suggestedFont = AppFontFamily.ZAOBLJEN,
            shapes = SkinShapes(cardRadius = 20.dp, buttonRadius = 18.dp, inputRadius = 14.dp, dialogRadius = 22.dp),
            effects = SkinEffects(cardAlpha = 0.91f, dialogAlpha = 0.96f, inputAlpha = 0.87f, shadowElevation = 3.dp)
        )
        AppSkin.ZELENA -> SkinStyle( // Šuma
            suggestedFont = AppFontFamily.ELEGANTAN,
            shapes = SkinShapes(cardRadius = 18.dp, buttonRadius = 16.dp, inputRadius = 14.dp, dialogRadius = 22.dp),
            effects = SkinEffects(cardAlpha = 0.92f, dialogAlpha = 0.96f, inputAlpha = 0.88f, shadowElevation = 4.dp)
        )
        AppSkin.CRNA -> SkinStyle( // Starry Night — jedini pravi dark skin
            suggestedFont = AppFontFamily.ELEGANTAN,
            shapes = SkinShapes(cardRadius = 20.dp, buttonRadius = 16.dp, inputRadius = 14.dp, dialogRadius = 24.dp),
            effects = SkinEffects(cardAlpha = 0.93f, dialogAlpha = 0.97f, inputAlpha = 0.90f, shadowElevation = 6.dp)
        )
        AppSkin.MORE -> SkinStyle(
            suggestedFont = AppFontFamily.ELEGANTAN,
            shapes = SkinShapes(cardRadius = 20.dp, buttonRadius = 18.dp, inputRadius = 14.dp, dialogRadius = 24.dp),
            effects = SkinEffects(cardAlpha = 0.91f, dialogAlpha = 0.96f, inputAlpha = 0.87f, shadowElevation = 3.dp)
        )
        AppSkin.ROZE -> SkinStyle( // Lala
            suggestedFont = AppFontFamily.ZAOBLJEN,
            shapes = SkinShapes(cardRadius = 24.dp, buttonRadius = 20.dp, inputRadius = 16.dp, dialogRadius = 28.dp),
            effects = SkinEffects(cardAlpha = 0.92f, dialogAlpha = 0.96f, inputAlpha = 0.88f, shadowElevation = 2.dp)
        )
        AppSkin.PLAVA -> SkinStyle( // Nebo
            suggestedFont = AppFontFamily.MODERAN,
            shapes = SkinShapes(cardRadius = 20.dp, buttonRadius = 16.dp, inputRadius = 14.dp, dialogRadius = 22.dp),
            effects = GLASS_MEDIUM
        )
        AppSkin.NARANDZASTA -> SkinStyle( // Sunce
            suggestedFont = AppFontFamily.MODERAN,
            shapes = SkinShapes(cardRadius = 18.dp, buttonRadius = 16.dp, inputRadius = 14.dp, dialogRadius = 22.dp),
            effects = SkinEffects(cardAlpha = 0.91f, dialogAlpha = 0.96f, inputAlpha = 0.87f, shadowElevation = 3.dp)
        )
        AppSkin.SVITANJE -> SkinStyle(
            suggestedFont = AppFontFamily.MODERAN,
            shapes = SkinShapes(cardRadius = 22.dp, buttonRadius = 18.dp, inputRadius = 16.dp, dialogRadius = 26.dp),
            effects = GLASS_MEDIUM
        )
        AppSkin.CUSTOM -> SkinStyle(
            suggestedFont = AppFontFamily.MODERAN,
            shapes = SkinShapes(cardRadius = 20.dp, buttonRadius = 16.dp, inputRadius = 14.dp, dialogRadius = 24.dp),
            effects = GLASS_MEDIUM
        )
    }

    /** Primenjuje "Visoka čitljivost" tako što podiže sve neprozirnosti blizu neprozirnog. */
    fun applyTransparencyMode(effects: SkinEffects, mode: TransparencyMode): SkinEffects =
        if (mode == TransparencyMode.STANDARDNA) {
            effects
        } else {
            effects.copy(
                cardAlpha = effects.cardAlpha.coerceAtLeast(0.95f),
                dialogAlpha = effects.dialogAlpha.coerceAtLeast(0.98f),
                inputAlpha = effects.inputAlpha.coerceAtLeast(0.92f)
            )
        }
}

val LocalSkinShapes = compositionLocalOf {
    SkinShapes(cardRadius = 16.dp, buttonRadius = 14.dp, inputRadius = 12.dp, dialogRadius = 20.dp)
}

val LocalSkinEffects = compositionLocalOf {
    SkinEffects(cardAlpha = 0.90f, dialogAlpha = 0.96f, inputAlpha = 0.85f, shadowElevation = 3.dp)
}

val LocalAnimationsEnabled = compositionLocalOf { true }
