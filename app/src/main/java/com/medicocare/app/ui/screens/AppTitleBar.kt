package com.medicocare.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medicocare.app.R
import com.medicocare.app.ui.SupportedLanguages

/**
 * Deljeni naslovni red — "MediCare" (podebljano "Care") na sredini + izbor jezika desno.
 * Koristi se i na glavnoj (tabela) i na stranici sa spiskom lekova.
 */
@Composable
fun AppTitleBar(currentLangTag: String, onLangSelected: (String) -> Unit) {
    var langMenuExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp)) {
        val titleText = stringResource(R.string.med_list_title)
        val boldStart = titleText.indexOf("Care")
        val annotatedTitle = buildAnnotatedString {
            if (boldStart >= 0) {
                append(titleText.substring(0, boldStart))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                    append(titleText.substring(boldStart, boldStart + 4))
                }
                append(titleText.substring(boldStart + 4))
            } else {
                append(titleText)
            }
        }
        Text(
            annotatedTitle,
            style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 1.sp),
            modifier = Modifier.align(Alignment.Center)
        )
        Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
            TextButton(
                onClick = { langMenuExpanded = true },
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(SupportedLanguages.shortCode(currentLangTag), style = MaterialTheme.typography.labelLarge)
            }
            DropdownMenu(expanded = langMenuExpanded, onDismissRequest = { langMenuExpanded = false }) {
                SupportedLanguages.ALL.forEach { (tag, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            langMenuExpanded = false
                            onLangSelected(tag)
                        }
                    )
                }
            }
        }
    }
}
