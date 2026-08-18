package com.medicocare.app.ui.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medicocare.app.R
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.screens.PremiumPlusRequiredDialog

/**
 * Baner reklama + dugmad "Besplatan 1h Premium+" / "Ukloni reklame" — prikazuje se na dnu
 * SVAKOG ekrana (prozora) aplikacije osim modala/dijaloga, sve dok Premium+ nije aktivan.
 * Jasno vizuelno odvojeno od sadržaja iznad (linija + osenčena pozadina). Kad se Premium+
 * otključa (trajno ili privremeno preko reklame) ili istekne, ceo ovaj blok — uključujući
 * granicu — nestaje/vraća se zajedno, jer je sve uslovljeno istim `premiumPlusActive`.
 */
@Composable
fun AdBannerBar(viewModel: MedicationViewModel) {
    val premiumPlusActive by viewModel.premiumPlusActive.collectAsState()
    var showPremiumPlusDialog by remember { mutableStateOf(false) }
    val showRewardedAd = rememberRewardedAdLauncher(onRewardEarned = { viewModel.grantTemporaryPremiumPlus() })

    if (!premiumPlusActive) {
        Column(Modifier.fillMaxWidth()) {
            Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { showRewardedAd() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            stringResource(R.string.rewarded_ad_watch_button),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = { showPremiumPlusDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            stringResource(R.string.ads_remove_button),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                BannerAdView()
            }
        }
    }

    if (showPremiumPlusDialog) {
        PremiumPlusRequiredDialog(
            onDismiss = { showPremiumPlusDialog = false },
            onUnlock = { viewModel.setPremiumPlusUnlocked(true); showPremiumPlusDialog = false }
        )
    }
}
