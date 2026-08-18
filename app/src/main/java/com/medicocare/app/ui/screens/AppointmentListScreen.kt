package com.medicocare.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Navigation
import com.medicocare.app.ui.components.GlassCard
import com.medicocare.app.ui.components.ThemedDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.medicocare.app.R
import com.medicocare.app.data.Appointment
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.ads.AdBannerBar
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.SkinArt
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val APPOINTMENT_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentListScreen(
    viewModel: MedicationViewModel,
    onBack: () -> Unit,
    onAddAppointment: () -> Unit,
    onEditAppointment: (Long) -> Unit
) {
    val appointments by viewModel.appointments.collectAsState()
    val hasPremiumAccess by viewModel.hasPremiumAccess.collectAsState()
    var toDelete by remember { mutableStateOf<Appointment?>(null) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    val skin by viewModel.skin.collectAsState()
    val customImagePath by viewModel.customBackgroundImagePath.collectAsState()
    val showsPhotoBackdrop = !customImagePath.isNullOrBlank() && (skin == AppSkin.CUSTOM || skin == AppSkin.CRNA)

    Box(modifier = Modifier.fillMaxSize()) {
    if (!showsPhotoBackdrop) {
        SkinArt(skin = skin, modifier = Modifier.fillMaxSize())
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.appt_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAppointment) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.appt_list_new_desc))
            }
        },
        bottomBar = { AdBannerBar(viewModel) },
    ) { padding ->
        if (appointments.isEmpty()) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.EventNote, contentDescription = null)
                Text(
                    stringResource(R.string.appt_list_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
                items(appointments, key = { it.id }) { appointment ->
                    AppointmentRow(
                        appointment = appointment,
                        hasPremiumAccess = hasPremiumAccess,
                        onClick = { onEditAppointment(appointment.id) },
                        onDelete = { toDelete = appointment },
                        onRequirePremium = { showPremiumDialog = true }
                    )
                }
            }
        }

        toDelete?.let { appointment ->
            ThemedDialog(
                onDismissRequest = { toDelete = null },
                title = { Text(stringResource(R.string.appt_list_delete_title)) },
                text = { Text(stringResource(R.string.appt_list_delete_text, appointment.title)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteAppointment(appointment)
                        toDelete = null
                    }) { Text(stringResource(R.string.common_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }

        if (showPremiumDialog) {
            PremiumRequiredDialog(
                onDismiss = { showPremiumDialog = false },
                onUnlock = { viewModel.setPremiumUnlocked(true); showPremiumDialog = false },
                title = stringResource(R.string.premium_nav_dialog_title),
                text = stringResource(R.string.premium_nav_dialog_text)
            )
        }
    }
    }
}

@Composable
private fun AppointmentRow(
    appointment: Appointment,
    hasPremiumAccess: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRequirePremium: () -> Unit
) {
    val context = LocalContext.current
    val whenText = APPOINTMENT_DATE_FORMAT.format(
        Instant.ofEpochMilli(appointment.dateTimeMillis).atZone(ZoneId.systemDefault())
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        onClick = onClick
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(appointment.title, style = MaterialTheme.typography.titleSmall)
                    val subtitle = listOf(appointment.type, appointment.institution, whenText)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.appt_list_delete_desc))
                }
            }
            if (appointment.address.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        if (!hasPremiumAccess) {
                            onRequirePremium()
                        } else {
                            // Google Maps "Directions" deep link — bez origin parametra, Maps sam
                            // koristi trenutnu lokaciju korisnika kao polaznu tačku i odmah otvara
                            // navigaciju (ne samo pribadaču na mapi kao ranije "geo:" pretraga).
                            val uri = Uri.parse(
                                "https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(appointment.address)
                            )
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.appt_list_navigation_button))
                }
            }
        }
    }
}
