package com.medicocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.medicocare.app.R
import com.medicocare.app.data.MedicationCategory
import com.medicocare.app.data.MedicationWithSchedules
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.ads.AdBannerBar
import com.medicocare.app.ui.components.GlassCard
import com.medicocare.app.ui.localizedName
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.SkinArt
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationListScreen(
    viewModel: MedicationViewModel,
    showExactAlarmWarning: Boolean,
    onOpenExactAlarmSettings: () -> Unit,
    onAddMedication: () -> Unit,
    onOpenMedication: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppointments: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenVitals: () -> Unit,
    onOpenCycleTracker: () -> Unit,
    onScanBarcode: () -> Unit,
    onOpenEmergencyNumbers: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val medications by viewModel.medications.collectAsState()
    val barcodeFilter by viewModel.barcodeFilter.collectAsState()
    val skin by viewModel.skin.collectAsState()
    val customImagePath by viewModel.customBackgroundImagePath.collectAsState()
    val hasPremiumAccess by viewModel.hasPremiumAccess.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var categoryFilter by remember { mutableStateOf<MedicationCategory?>(null) }
    var showBarcodePremiumDialog by remember { mutableStateOf(false) }
    val barcodeNotFoundText = stringResource(R.string.med_list_barcode_not_found)

    // Ako skenirani barkod ne odgovara nijednom leku u evidenciji, nema smisla da filter
    // ostane "aktivan" a lista prazna — automatski se uklanja, uz kratku poruku.
    LaunchedEffect(barcodeFilter, medications) {
        if (barcodeFilter != null && medications.isNotEmpty() && medications.none { it.medication.barcode == barcodeFilter }) {
            Toast.makeText(context, barcodeNotFoundText, Toast.LENGTH_SHORT).show()
            viewModel.setBarcodeFilter(null)
        }
    }

    var currentLangTag by remember {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val tag = if (!appLocales.isEmpty) appLocales[0]?.language else Locale.getDefault().language
        mutableStateOf(tag ?: "sr")
    }

    val visibleMedications = remember(medications, barcodeFilter, categoryFilter) {
        medications
            .filter { barcodeFilter == null || it.medication.barcode == barcodeFilter }
            .filter { categoryFilter == null || it.medication.category == categoryFilter }
    }

    // Prilagođena pozadinska slika (postavljena za Prilagođeni skin) se u pozadini glavnog
    // ekrana prikazuje i za Crnu temu (izbledelo), pa je containerColor providan da bi se
    // ta slika (nacrtana iza svega u MainActivity) uopšte videla.
    val showsPhotoBackdrop = !customImagePath.isNullOrBlank() && (skin == AppSkin.CUSTOM || skin == AppSkin.CRNA)

    Box(modifier = Modifier.fillMaxSize()) {
        // Tematska ilustracija sada prekriva CELU glavnu stranicu (a ne samo traku na vrhu),
        // osim kada je aktivna korisnička fotografija (ona se crta iza svega u MainActivity).
        if (!showsPhotoBackdrop) {
            SkinArt(skin = skin, modifier = Modifier.fillMaxSize())
        }
        Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppTitleBar(
                    currentLangTag = currentLangTag,
                    onLangSelected = { tag ->
                        currentLangTag = tag
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    }
                )
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenEmergencyNumbers) {
                            Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.med_list_emergency_desc))
                        }
                        IconButton(onClick = { if (hasPremiumAccess) onScanBarcode() else showBarcodePremiumDialog = true }) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.med_list_scan_filter_desc))
                        }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.med_list_more_options_desc))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.med_list_menu_appointments)) },
                                leadingIcon = { Icon(Icons.Filled.EventNote, contentDescription = null) },
                                onClick = { menuExpanded = false; onOpenAppointments() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.med_list_menu_history)) },
                                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) },
                                onClick = { menuExpanded = false; onOpenHistory() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.med_list_menu_documents)) },
                                leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) },
                                onClick = { menuExpanded = false; onOpenDocuments() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.med_list_menu_vitals)) },
                                leadingIcon = { Icon(Icons.Filled.Bloodtype, contentDescription = null) },
                                onClick = { menuExpanded = false; onOpenVitals() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.med_list_menu_cycle)) },
                                leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                                onClick = { menuExpanded = false; onOpenCycleTracker() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.med_list_menu_settings)) },
                                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                onClick = { menuExpanded = false; onOpenSettings() }
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMedication) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.med_list_add_medication_desc))
            }
        },
        bottomBar = { AdBannerBar(viewModel) }
    ) { padding ->
        // Deo sa spiskovima ima belu matiranu providnu pozadinu — tematska ilustracija
        // (nacrtana iza, preko cele stranice) i dalje se nazire ispod nje.
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.80f))
        ) {
            if (barcodeFilter != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.med_list_filter_active_notice),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(onClick = { viewModel.setBarcodeFilter(null) }) {
                        Text(stringResource(R.string.med_list_remove_filter))
                    }
                }
            }

            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                FilterChip(
                    selected = categoryFilter != null,
                    onClick = { categoryMenuExpanded = true },
                    label = {
                        Text(
                            categoryFilter?.localizedName() ?: stringResource(R.string.common_all),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }
                )
                DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.common_all), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = { categoryFilter = null; categoryMenuExpanded = false }
                    )
                    MedicationCategory.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.localizedName(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = { categoryFilter = option; categoryMenuExpanded = false }
                        )
                    }
                }
            }

            if (showExactAlarmWarning) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null)
                            Text(
                                stringResource(R.string.med_list_exact_alarm_warning),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Button(onClick = onOpenExactAlarmSettings, modifier = Modifier.padding(top = 8.dp)) {
                            Text(stringResource(R.string.med_list_open_permission_settings))
                        }
                    }
                }
            }

            if (medications.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.MedicalServices,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(stringResource(R.string.med_list_empty_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.med_list_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else if (visibleMedications.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.MedicalServices,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        stringResource(R.string.med_list_empty_filtered),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(visibleMedications, key = { it.medication.id }) { item ->
                        MedicationCard(item = item, onClick = { onOpenMedication(item.medication.id) })
                    }
                }
            }
        }
    }
    }

    if (showBarcodePremiumDialog) {
        PremiumRequiredDialog(
            onDismiss = { showBarcodePremiumDialog = false },
            onUnlock = { viewModel.setPremiumUnlocked(true); showBarcodePremiumDialog = false },
            title = stringResource(R.string.premium_barcode_dialog_title),
            text = stringResource(R.string.premium_barcode_dialog_text)
        )
    }
}

@Composable
private fun MedicationCard(item: MedicationWithSchedules, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.medication.name, style = MaterialTheme.typography.titleMedium)
                if (item.medication.category != MedicationCategory.LEK) {
                    Text(
                        "  ${item.medication.category.localizedName()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            val details = listOf(item.medication.dosage, item.medication.form)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
            if (details.isNotBlank()) {
                Text(details, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                medicationScheduleOverview(item.schedules),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            val stock = item.medication.stockCount
            if (stock != null) {
                val low = stock <= item.medication.lowStockThreshold
                Text(
                    stringResource(R.string.med_list_stock_label, formatStock(stock)) +
                        if (low) stringResource(R.string.med_list_stock_low_suffix) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun formatStock(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
