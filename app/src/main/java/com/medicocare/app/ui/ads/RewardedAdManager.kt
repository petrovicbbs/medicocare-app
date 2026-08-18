package com.medicocare.app.ui.ads

import android.app.Activity
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.medicocare.app.R

/**
 * Pravi ad-unit ID iz Vladimirovog AdMob naloga (aplikacija "MediCare", Rewarded jedinica).
 * Gledanjem ove reklame do kraja korisnik dobija 1h privremenog Premium+ (vidi
 * MedicationViewModel.grantTemporaryPremiumPlus). Isto upozorenje kao za banner: ovo je
 * PRAVI ad unit — ne klikati/gledati više puta zaredom radi testiranja (rizik od "invalid
 * traffic" upozorenja na AdMob nalogu); po potrebi registrovati uređaj kao Test device.
 */
private const val REWARDED_AD_UNIT_ID = "ca-app-pub-2860076775666952/7600146197"

/**
 * Priprema (učitava unapred) Rewarded reklamu i vraća funkciju za njeno prikazivanje.
 * Nagrada [onRewardEarned] se poziva ISKLJUČIVO kad AdMob SDK potvrdi da je korisnik
 * odgledao reklamu do kraja (onUserEarnedReward) — ne pri samom otvaranju ili prekidu.
 */
@Composable
fun rememberRewardedAdLauncher(onRewardEarned: () -> Unit): () -> Unit {
    val context = LocalContext.current
    var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val notReadyText = stringResource(R.string.rewarded_ad_not_ready)

    fun loadAd() {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    // Učitaj unapred čim se ekran prikaže, da reklama bude spremna kad korisnik tapne dugme.
    LaunchedEffect(Unit) { loadAd() }

    return {
        val activity = context as? Activity
        val ad = rewardedAd
        when {
            activity == null -> Unit
            ad == null -> {
                Toast.makeText(context, notReadyText, Toast.LENGTH_SHORT).show()
                loadAd()
            }
            else -> {
                ad.show(activity) { onRewardEarned() }
                rewardedAd = null
                loadAd()
            }
        }
    }
}
