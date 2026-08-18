package com.medicocare.app.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.medicocare.app.R

/**
 * Tematska ilustracija po skinu — puna, ilustrovana pozadinska sličica (ne više ručno
 * iscrtana Canvas scena) generisana kao gradijent + vektorska ilustracija i rasterizovana
 * u res/drawable-nodpi. Koristi se i kao mala oznaka u Podešavanjima i kao pozadina cele
 * glavne stranice — javni potpis (skin, modifier) je namerno nepromenjen da pozivi na
 * MedicationListScreen.kt, MedicationScheduleGridScreen.kt i SettingsScreen.kt ne moraju
 * da se menjaju.
 */
@Composable
fun SkinArt(skin: AppSkin, modifier: Modifier = Modifier) {
    val drawableId = when (skin) {
        AppSkin.PODRAZUMEVANA -> R.drawable.skin_bg_podrazumevana
        AppSkin.ROZE -> R.drawable.skin_bg_roze
        AppSkin.PLAVA -> R.drawable.skin_bg_plava
        AppSkin.ZELENA -> R.drawable.skin_bg_zelena
        AppSkin.ZUTA -> R.drawable.skin_bg_zuta
        AppSkin.LJUBICASTA -> R.drawable.skin_bg_ljubicasta
        AppSkin.NARANDZASTA -> R.drawable.skin_bg_narandzasta
        AppSkin.CRNA -> R.drawable.skin_bg_crna
        AppSkin.SVITANJE -> R.drawable.skin_bg_svitanje
        AppSkin.MORE -> R.drawable.skin_bg_more
        AppSkin.CUSTOM -> R.drawable.skin_bg_custom_placeholder
    }
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
    }
}
