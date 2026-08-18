package com.medicocare.app.ui.ads

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Pravi ad-unit ID iz Vladimirovog AdMob naloga (aplikacija "MediCare", banner jedinica).
 * VAŽNO: ovo više NIJE Google-ov test ID — sada se prikazuju STVARNE reklame. Da ne bi došlo
 * do "invalid traffic" upozorenja na nalogu, uređaj na kom se testira treba dodati kao Test
 * device u AdMob-u (Settings → Test devices) pre nego što se sam klikće/gleda reklama više puta.
 */
private const val BANNER_AD_UNIT_ID = "ca-app-pub-2860076775666952/6246701926"

/**
 * Banner reklama (standardna veličina 320x50) na dnu ekrana. Prikazuje se samo kad
 * Premium+ nije otključan — poziva se uslovno sa mesta gde se koristi.
 */
@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BANNER_AD_UNIT_ID
                // PRIVREMENO (dijagnostika): dok se ne potvrdi da baner stvarno stiže sa
                // AdMob naloga, ispisuje Toast sa tačnim razlogom neuspeha (npr. NO_FILL,
                // NETWORK_ERROR) — da se ne nagađa da li je greška u kodu ili u nalogu/mreži.
                // Ukloniti kad se banner potvrdi da radi.
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Toast.makeText(
                            context,
                            "Banner nije učitan: [${error.code}] ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
