package com.medicocare.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.LocalFireDepartment
import com.medicocare.app.ui.components.GlassCard
import com.medicocare.app.ui.components.ThemedDialog
import com.medicocare.app.ui.components.ThemedTextField
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.medicocare.app.R
import com.medicocare.app.data.EmergencyCategory
import com.medicocare.app.data.EmergencyNumber
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.ads.AdBannerBar
import com.medicocare.app.ui.localizedName
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.SkinArt
import java.util.Locale

/**
 * Ikonice za custom hitne brojeve, da bi se vizuelno lakše razlikovali u listi (npr. "Komšija"
 * vs "Ćerka" vs "Doktor"). `iconKey` se čuva kao naziv enum vrednosti (npr. "HOUSE") u bazi.
 */
enum class EmergencyIconOption(val icon: ImageVector, val labelRes: Int) {
    HOUSE(Icons.Filled.Home, R.string.emergency_icon_house),
    STAR(Icons.Filled.Star, R.string.emergency_icon_star),
    SUN(Icons.Filled.WbSunny, R.string.emergency_icon_sun),
    CAR(Icons.Filled.DirectionsCar, R.string.emergency_icon_car),
    BUILDING(Icons.Filled.Business, R.string.emergency_icon_building),
    BIKE(Icons.Filled.DirectionsBike, R.string.emergency_icon_bike),
    HEART(Icons.Filled.Favorite, R.string.emergency_icon_heart),
    WRENCH(Icons.Filled.Build, R.string.emergency_icon_wrench),
    PAW(Icons.Filled.Pets, R.string.emergency_icon_paw),
    FLOWER(Icons.Filled.LocalFlorist, R.string.emergency_icon_flower),
    PALETTE(Icons.Filled.Palette, R.string.emergency_icon_palette),
    UMBRELLA(Icons.Filled.BeachAccess, R.string.emergency_icon_umbrella),
    CAKE(Icons.Filled.Cake, R.string.emergency_icon_cake),
    BOAT(Icons.Filled.DirectionsBoat, R.string.emergency_icon_boat),
    MUSIC(Icons.Filled.MusicNote, R.string.emergency_icon_music);

    companion object {
        val DEFAULT = CAR
        fun fromKey(key: String?): EmergencyIconOption = entries.find { it.name == key } ?: DEFAULT
    }
}

/**
 * Hitni brojevi: 4 osnovna (policija/hitna pomoć/vatrogasci/pomoć na putu), podrazumevano
 * popunjena prema jeziku aplikacije ali slobodno izmenljiva, plus dodatni custom brojevi
 * (Premium). Dodirom na broj otvara se birač (ACTION_DIAL) radi brzog pozivanja.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyNumbersScreen(
    viewModel: MedicationViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val numbers by viewModel.emergencyNumbers.collectAsState()
    val premiumUnlocked by viewModel.hasPremiumAccess.collectAsState()

    val languageTag = remember {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        (if (!appLocales.isEmpty) appLocales[0]?.language else Locale.getDefault().language) ?: "sr"
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.seedEmergencyDefaultsIfNeeded(languageTag)
    }

    val defaults = numbers.filter { it.category != null }.sortedBy { it.sortOrder }
    val customNumbers = numbers.filter { it.category == null }

    var editingDefault by remember { mutableStateOf<EmergencyNumber?>(null) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var toDeleteCustom by remember { mutableStateOf<EmergencyNumber?>(null) }
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
                title = { Text(stringResource(R.string.emergency_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (!premiumUnlocked) showPremiumDialog = true else showAddCustomDialog = true
            }) {
                Icon(Icons.Filled.WorkspacePremium, contentDescription = stringResource(R.string.emergency_add_custom_desc))
            }
        },
        bottomBar = { AdBannerBar(viewModel) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxWidth()) {
            Text(
                stringResource(R.string.emergency_defaults_note),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                items(defaults, key = { it.id }) { number ->
                    EmergencyNumberRow(
                        icon = { iconForCategory(number.category) },
                        title = number.category?.localizedName() ?: "",
                        phoneNumber = number.phoneNumber,
                        onCall = { dialNumber(context, number.phoneNumber) },
                        onEdit = { editingDefault = number }
                    )
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)) {
                        Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            stringResource(R.string.emergency_custom_title),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (!premiumUnlocked) {
                    item {
                        GlassCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    stringResource(R.string.premium_not_enabled_test),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                TextButton(
                                    onClick = { viewModel.setPremiumUnlocked(true) },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(stringResource(R.string.common_unlock_test))
                                }
                            }
                        }
                    }
                } else if (customNumbers.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.emergency_custom_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                } else {
                    items(customNumbers, key = { it.id }) { number ->
                        val iconOption = EmergencyIconOption.fromKey(number.iconKey)
                        EmergencyNumberRow(
                            icon = { Icon(iconOption.icon, contentDescription = stringResource(iconOption.labelRes)) },
                            title = number.label,
                            phoneNumber = number.phoneNumber,
                            onCall = { dialNumber(context, number.phoneNumber) },
                            onEdit = null,
                            onDelete = { toDeleteCustom = number }
                        )
                    }
                }
            }
        }
    }

    editingDefault?.let { number ->
        EmergencyEditDialog(
            titleSuffix = number.category?.localizedName() ?: "",
            initialValue = number.phoneNumber,
            onDismiss = { editingDefault = null },
            onSave = { newValue ->
                viewModel.saveEmergencyNumber(number.copy(phoneNumber = newValue.trim()))
                editingDefault = null
            }
        )
    }

    if (showAddCustomDialog) {
        EmergencyAddCustomDialog(
            onDismiss = { showAddCustomDialog = false },
            onSave = { label, number, iconOption ->
                viewModel.saveEmergencyNumber(
                    EmergencyNumber(
                        category = null,
                        label = label.trim(),
                        phoneNumber = number.trim(),
                        sortOrder = 100,
                        iconKey = iconOption.name
                    )
                )
                showAddCustomDialog = false
            }
        )
    }

    toDeleteCustom?.let { number ->
        ThemedDialog(
            onDismissRequest = { toDeleteCustom = null },
            title = { Text(stringResource(R.string.emergency_delete_custom_title)) },
            text = { Text(stringResource(R.string.emergency_delete_custom_text, number.label)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEmergencyNumber(number)
                    toDeleteCustom = null
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { toDeleteCustom = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (showPremiumDialog) {
        PremiumRequiredDialog(
            onDismiss = { showPremiumDialog = false },
            onUnlock = { viewModel.setPremiumUnlocked(true); showPremiumDialog = false }
        )
    }
    }
}

@Composable
private fun iconForCategory(category: EmergencyCategory?) {
    when (category) {
        EmergencyCategory.POLICE -> Icon(Icons.Filled.LocalPolice, contentDescription = null)
        EmergencyCategory.AMBULANCE -> Icon(Icons.Filled.LocalHospital, contentDescription = null)
        EmergencyCategory.FIRE -> Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null)
        EmergencyCategory.ROADSIDE -> Icon(Icons.Filled.LocalShipping, contentDescription = null)
        null -> Icon(Icons.Filled.Call, contentDescription = null)
    }
}

@Composable
private fun EmergencyNumberRow(
    icon: @Composable () -> Unit,
    title: String,
    phoneNumber: String,
    onCall: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)? = null
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        onClick = onCall,
        colors = CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                icon()
                Column(Modifier.padding(start = 12.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        phoneNumber.ifBlank { stringResource(R.string.emergency_number_empty_hint) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (phoneNumber.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onCall) {
                Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.emergency_call_desc))
            }
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.emergency_edit_number_desc))
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_delete))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmergencyEditDialog(
    titleSuffix: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.emergency_edit_dialog_title, titleSuffix)) },
        text = {
            ThemedTextField(
                value = value,
                onValueChange = { value = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } },
                label = { Text(stringResource(R.string.emergency_number_label)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmergencyAddCustomDialog(
    onDismiss: () -> Unit,
    onSave: (label: String, number: String, icon: EmergencyIconOption) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(EmergencyIconOption.DEFAULT) }
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.emergency_add_custom_dialog_title)) },
        text = {
            Column {
                ThemedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.emergency_custom_label_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ThemedTextField(
                    value = number,
                    onValueChange = { number = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } },
                    label = { Text(stringResource(R.string.emergency_custom_number_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Text(
                    stringResource(R.string.emergency_icon_picker_label),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
                EmergencyIconRow(selected = selectedIcon, onSelect = { selectedIcon = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(label, number, selectedIcon) },
                enabled = label.isNotBlank() && number.isNotBlank()
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun EmergencyIconRow(selected: EmergencyIconOption, onSelect: (EmergencyIconOption) -> Unit) {
    Row(Modifier.padding(top = 4.dp).horizontalScroll(rememberScrollState())) {
        EmergencyIconOption.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    option.icon,
                    contentDescription = stringResource(option.labelRes),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun dialNumber(context: android.content.Context, number: String) {
    if (number.isBlank()) return
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)))
    context.startActivity(intent)
}
