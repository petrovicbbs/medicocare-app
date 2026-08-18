package com.medicocare.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.medicocare.app.R
import com.medicocare.app.data.EmergencyCategory
import com.medicocare.app.data.MedicationCategory
import com.medicocare.app.ui.theme.AnimationsMode
import com.medicocare.app.ui.theme.AppFontFamily
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.TextSizeOption
import com.medicocare.app.ui.theme.ThemeMode
import com.medicocare.app.ui.theme.TransparencyMode

/**
 * Lokalizovani nazivi za enume koji ne mogu sami da pristupe resursima (nisu Composable).
 * Umesto direktnog čitanja `enum.displayName`, UI kod poziva ove ekstenzije da bi
 * prikazani tekst pratio trenutno izabrani jezik aplikacije.
 */

@Composable
fun AppSkin.localizedName(): String = when (this) {
    AppSkin.PODRAZUMEVANA -> stringResource(R.string.skin_podrazumevana)
    AppSkin.ZELENA -> stringResource(R.string.skin_suma)
    AppSkin.PLAVA -> stringResource(R.string.skin_nebo)
    AppSkin.LJUBICASTA -> stringResource(R.string.skin_lavanda)
    AppSkin.NARANDZASTA -> stringResource(R.string.skin_narandza)
    AppSkin.ROZE -> stringResource(R.string.skin_lala)
    AppSkin.ZUTA -> stringResource(R.string.skin_leto)
    AppSkin.CRNA -> stringResource(R.string.skin_crna)
    AppSkin.SVITANJE -> stringResource(R.string.skin_svitanje)
    AppSkin.MORE -> stringResource(R.string.skin_more)
    AppSkin.CUSTOM -> stringResource(R.string.skin_custom)
}

@Composable
fun ThemeMode.localizedName(): String = when (this) {
    ThemeMode.SISTEMSKI -> stringResource(R.string.theme_mode_sistemski)
    ThemeMode.SVETLA -> stringResource(R.string.theme_mode_svetla)
    ThemeMode.TAMNA -> stringResource(R.string.theme_mode_tamna)
}

@Composable
fun AppFontFamily.localizedName(): String = when (this) {
    AppFontFamily.AUTOMATSKI -> stringResource(R.string.font_family_automatski)
    AppFontFamily.MODERAN -> stringResource(R.string.font_family_moderan)
    AppFontFamily.ZAOBLJEN -> stringResource(R.string.font_family_zaobljen)
    AppFontFamily.ELEGANTAN -> stringResource(R.string.font_family_elegantan)
}

@Composable
fun TextSizeOption.localizedName(): String = when (this) {
    TextSizeOption.MALA -> stringResource(R.string.text_size_mala)
    TextSizeOption.NORMALNA -> stringResource(R.string.text_size_normalna)
    TextSizeOption.VELIKA -> stringResource(R.string.text_size_velika)
    TextSizeOption.VRLO_VELIKA -> stringResource(R.string.text_size_vrlo_velika)
}

@Composable
fun TransparencyMode.localizedName(): String = when (this) {
    TransparencyMode.STANDARDNA -> stringResource(R.string.transparency_standardna)
    TransparencyMode.VISOKA_CITLJIVOST -> stringResource(R.string.transparency_visoka_citljivost)
}

@Composable
fun AnimationsMode.localizedName(): String = when (this) {
    AnimationsMode.UKLJUCENE -> stringResource(R.string.animations_ukljucene)
    AnimationsMode.ISKLJUCENE -> stringResource(R.string.animations_iskljucene)
}

@Composable
fun MedicationCategory.localizedName(): String = when (this) {
    MedicationCategory.LEK -> stringResource(R.string.category_lek)
    MedicationCategory.VITAMIN -> stringResource(R.string.category_vitamin)
    MedicationCategory.SUPLEMENT -> stringResource(R.string.category_suplement)
}

@Composable
fun EmergencyCategory.localizedName(): String = when (this) {
    EmergencyCategory.POLICE -> stringResource(R.string.emergency_category_police)
    EmergencyCategory.AMBULANCE -> stringResource(R.string.emergency_category_ambulance)
    EmergencyCategory.FIRE -> stringResource(R.string.emergency_category_fire)
    EmergencyCategory.ROADSIDE -> stringResource(R.string.emergency_category_roadside)
}
