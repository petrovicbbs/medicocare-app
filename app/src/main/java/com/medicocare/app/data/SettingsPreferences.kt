package com.medicocare.app.data

import android.content.Context
import com.medicocare.app.ui.theme.AnimationsMode
import com.medicocare.app.ui.theme.AppFontFamily
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.CustomFontStyle
import com.medicocare.app.ui.theme.TextSizeOption
import com.medicocare.app.ui.theme.ThemeMode
import com.medicocare.app.ui.theme.TransparencyMode

/**
 * Jednostavno lokalno čuvanje podešavanja izgleda (SharedPreferences — nema potrebe za Room-om
 * za par vrednosti). "Premium unlocked" je za sada samo test-prekidač dok se ne uvede pravo
 * plaćanje (Google Play Billing) — vidi napomenu u SettingsScreen.
 */
class SettingsPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("medicocare_settings", Context.MODE_PRIVATE)

    var skin: AppSkin
        get() = runCatching { AppSkin.valueOf(prefs.getString(KEY_SKIN, AppSkin.PODRAZUMEVANA.name)!!) }
            .getOrDefault(AppSkin.PODRAZUMEVANA)
        set(value) = prefs.edit().putString(KEY_SKIN, value.name).apply()

    var themeMode: ThemeMode
        get() = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SISTEMSKI.name)!!) }
            .getOrDefault(ThemeMode.SISTEMSKI)
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    // ---------- Globalni izgled (nezavisno od skina) ----------

    /** Font porodica koju korisnik ručno bira, ili AUTOMATSKI da prati predlog aktivnog skina. */
    var fontFamily: AppFontFamily
        get() = runCatching { AppFontFamily.valueOf(prefs.getString(KEY_FONT_FAMILY, AppFontFamily.AUTOMATSKI.name)!!) }
            .getOrDefault(AppFontFamily.AUTOMATSKI)
        set(value) = prefs.edit().putString(KEY_FONT_FAMILY, value.name).apply()

    var textSize: TextSizeOption
        get() = runCatching { TextSizeOption.valueOf(prefs.getString(KEY_TEXT_SIZE, TextSizeOption.NORMALNA.name)!!) }
            .getOrDefault(TextSizeOption.NORMALNA)
        set(value) = prefs.edit().putString(KEY_TEXT_SIZE, value.name).apply()

    var transparencyMode: TransparencyMode
        get() = runCatching { TransparencyMode.valueOf(prefs.getString(KEY_TRANSPARENCY, TransparencyMode.STANDARDNA.name)!!) }
            .getOrDefault(TransparencyMode.STANDARDNA)
        set(value) = prefs.edit().putString(KEY_TRANSPARENCY, value.name).apply()

    var animationsMode: AnimationsMode
        get() = runCatching { AnimationsMode.valueOf(prefs.getString(KEY_ANIMATIONS, AnimationsMode.UKLJUCENE.name)!!) }
            .getOrDefault(AnimationsMode.UKLJUCENE)
        set(value) = prefs.edit().putString(KEY_ANIMATIONS, value.name).apply()

    var premiumUnlocked: Boolean
        get() = prefs.getBoolean(KEY_PREMIUM, false)
        set(value) = prefs.edit().putBoolean(KEY_PREMIUM, value).apply()

    /** Poseban, viši nivo pretplate (iznad Premium-a, uključuje i sve iz Premium-a) — za sada
     *  dodatno uklanja i banner reklamu. */
    var premiumPlusUnlocked: Boolean
        get() = prefs.getBoolean(KEY_PREMIUM_PLUS, false)
        set(value) = prefs.edit().putBoolean(KEY_PREMIUM_PLUS, value).apply()

    /** Vreme (millis) do kog važi privremeni Premium+ dobijen gledanjem Rewarded reklame.
     *  `null` znači da trenutno nema aktivnog privremenog Premium+. */
    var premiumPlusExpiryMillis: Long?
        get() = prefs.getLong(KEY_PREMIUM_PLUS_EXPIRY, -1L).takeIf { it > 0L }
        set(value) = prefs.edit().putLong(KEY_PREMIUM_PLUS_EXPIRY, value ?: -1L).apply()

    /** Isti "efektivni Premium pristup" kao MedicationViewModel.hasPremiumAccess, ali kao
     *  obična funkcija — za mesta bez Compose/ViewModel konteksta (npr. BroadcastReceiver
     *  koji obrađuje dodir na akciju notifikacije). */
    fun hasPremiumAccess(): Boolean =
        premiumUnlocked || premiumPlusUnlocked || (premiumPlusExpiryMillis?.let { it > System.currentTimeMillis() } ?: false)

    // ---------- Prilagođeni (Custom) skin ----------

    var customBackgroundImagePath: String?
        get() = prefs.getString(KEY_CUSTOM_IMAGE, null)
        set(value) = prefs.edit().putString(KEY_CUSTOM_IMAGE, value).apply()

    var customAccentColorArgb: Int
        get() = prefs.getInt(KEY_CUSTOM_ACCENT, DEFAULT_CUSTOM_ACCENT)
        set(value) = prefs.edit().putInt(KEY_CUSTOM_ACCENT, value).apply()

    var customBackgroundColorArgb: Int
        get() = prefs.getInt(KEY_CUSTOM_BACKGROUND, DEFAULT_CUSTOM_BACKGROUND)
        set(value) = prefs.edit().putInt(KEY_CUSTOM_BACKGROUND, value).apply()

    var customFontScale: Float
        get() = prefs.getFloat(KEY_CUSTOM_FONT_SCALE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_CUSTOM_FONT_SCALE, value).apply()

    var customFontStyle: CustomFontStyle
        get() = runCatching { CustomFontStyle.valueOf(prefs.getString(KEY_CUSTOM_FONT_STYLE, CustomFontStyle.DEFAULT.name)!!) }
            .getOrDefault(CustomFontStyle.DEFAULT)
        set(value) = prefs.edit().putString(KEY_CUSTOM_FONT_STYLE, value.name).apply()

    companion object {
        private const val KEY_SKIN = "skin"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_TRANSPARENCY = "transparency_mode"
        private const val KEY_ANIMATIONS = "animations_mode"
        private const val KEY_PREMIUM = "premium_unlocked"
        private const val KEY_PREMIUM_PLUS = "premium_plus_unlocked"
        private const val KEY_PREMIUM_PLUS_EXPIRY = "premium_plus_expiry_millis"
        private const val KEY_CUSTOM_IMAGE = "custom_skin_image_path"
        private const val KEY_CUSTOM_ACCENT = "custom_skin_accent_argb"
        private const val KEY_CUSTOM_BACKGROUND = "custom_skin_background_argb"
        private const val KEY_CUSTOM_FONT_SCALE = "custom_skin_font_scale"
        private const val KEY_CUSTOM_FONT_STYLE = "custom_skin_font_style"
        private const val DEFAULT_CUSTOM_ACCENT = 0xFF616161.toInt()
        private const val DEFAULT_CUSTOM_BACKGROUND = 0xFFF5F5F5.toInt()
    }
}
