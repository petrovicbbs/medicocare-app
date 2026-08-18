package com.medicocare.app.ui.screens

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.medicocare.app.R
import com.medicocare.app.ui.components.ThemedDialog

/**
 * Deljenje i preuzimanje izveštaja (istorija, izveštaji/analize, vitalni znaci, ciklus)
 * su Premium funkcije. Ovaj dijalog se prikazuje kad korisnik pokuša da deli/preuzme
 * bez otključanog Premium-a, sa istim test-otključavanjem kao i ostale Premium funkcije.
 */
@Composable
fun PremiumRequiredDialog(
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
    title: String = stringResource(R.string.premium_dialog_title),
    text: String = stringResource(R.string.premium_dialog_text)
) {
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onUnlock) { Text(stringResource(R.string.common_unlock_test)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

/**
 * Poseban, viši nivo pretplate (Premium+) — iznad običnog Premium-a. Za sada jedina razlika
 * je uklanjanje banner reklame na dnu glavnog ekrana. Isti test-otključavanje princip.
 */
@Composable
fun PremiumPlusRequiredDialog(onDismiss: () -> Unit, onUnlock: () -> Unit) {
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.premium_plus_dialog_title)) },
        text = { Text(stringResource(R.string.premium_plus_dialog_text)) },
        confirmButton = {
            TextButton(onClick = onUnlock) { Text(stringResource(R.string.common_unlock_test)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
