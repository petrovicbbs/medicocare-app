package com.medicocare.app.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.medicocare.app.R
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.ads.AdBannerBar
import com.medicocare.app.ui.SupportedLanguages
import com.medicocare.app.ui.localizedName
import com.medicocare.app.ui.components.GlassCard
import com.medicocare.app.ui.components.ThemedDialog
import com.medicocare.app.ui.theme.AnimationsMode
import com.medicocare.app.ui.theme.AppFontFamily
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.CUSTOM_SKIN_SWATCHES
import com.medicocare.app.ui.theme.CustomFontStyle
import com.medicocare.app.ui.theme.SkinArt
import com.medicocare.app.ui.theme.TextSizeOption
import com.medicocare.app.ui.theme.ThemeMode
import com.medicocare.app.ui.theme.TransparencyMode
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MedicationViewModel,
    onBack: () -> Unit
) {
    val currentSkin by viewModel.skin.collectAsState()
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val currentFontFamily by viewModel.fontFamily.collectAsState()
    val currentTextSize by viewModel.textSize.collectAsState()
    val currentTransparencyMode by viewModel.transparencyMode.collectAsState()
    val currentAnimationsMode by viewModel.animationsMode.collectAsState()
    val hasPremiumAccess by viewModel.hasPremiumAccess.collectAsState()
    val premiumPlusUnlocked by viewModel.premiumPlusUnlocked.collectAsState()
    val premiumPlusActive by viewModel.premiumPlusActive.collectAsState()
    val premiumPlusExpiryMillis by viewModel.premiumPlusExpiryMillis.collectAsState()
    val showRewardedAd = com.medicocare.app.ui.ads.rememberRewardedAdLauncher(
        onRewardEarned = { viewModel.grantTemporaryPremiumPlus() }
    )

    var lockedSkinTapped by remember { mutableStateOf<AppSkin?>(null) }
    var langMenuExpanded by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedConfirmationText = stringResource(R.string.settings_saved_confirmation)
    val resetConfirmationText = stringResource(R.string.settings_reset_confirmation)

    val currentLocaleTag = remember {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) appLocales[0]?.language else Locale.getDefault().language
    }

    // Izbori se drže u lokalnom ("pending") stanju i primenjuju tek na dodir dugmeta
    // "Sačuvaj izmene" — dodirivanje opcija samo menja prikaz na ovom ekranu.
    var pendingSkin by remember { mutableStateOf(currentSkin) }
    var pendingThemeMode by remember { mutableStateOf(currentThemeMode) }
    var pendingLangTag by remember { mutableStateOf(currentLocaleTag ?: "sr") }
    var pendingFontFamily by remember { mutableStateOf(currentFontFamily) }
    var pendingTextSize by remember { mutableStateOf(currentTextSize) }
    var pendingTransparencyMode by remember { mutableStateOf(currentTransparencyMode) }
    var pendingAnimationsMode by remember { mutableStateOf(currentAnimationsMode) }

    fun saveChanges() {
        viewModel.setThemeMode(pendingThemeMode)
        viewModel.setSkin(pendingSkin)
        viewModel.setFontFamily(pendingFontFamily)
        viewModel.setTextSize(pendingTextSize)
        viewModel.setTransparencyMode(pendingTransparencyMode)
        viewModel.setAnimationsMode(pendingAnimationsMode)
        if (pendingLangTag != currentLocaleTag) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(pendingLangTag))
        }
        scope.launch { snackbarHostState.showSnackbar(savedConfirmationText) }
    }

    // Resetuje SAMO izgled (jezik/skin/režim/font/veličinu teksta/providnost/animacije) na
    // podrazumevane vrednosti — ne dira lekove, istoriju, preglede ni bilo koje druge podatke.
    fun resetDisplaySettings() {
        pendingSkin = AppSkin.PODRAZUMEVANA
        pendingThemeMode = ThemeMode.SISTEMSKI
        pendingLangTag = "sr"
        pendingFontFamily = AppFontFamily.AUTOMATSKI
        pendingTextSize = TextSizeOption.NORMALNA
        pendingTransparencyMode = TransparencyMode.STANDARDNA
        pendingAnimationsMode = AnimationsMode.UKLJUCENE
        viewModel.setSkin(AppSkin.PODRAZUMEVANA)
        viewModel.setThemeMode(ThemeMode.SISTEMSKI)
        viewModel.setFontFamily(AppFontFamily.AUTOMATSKI)
        viewModel.setTextSize(TextSizeOption.NORMALNA)
        viewModel.setTransparencyMode(TransparencyMode.STANDARDNA)
        viewModel.setAnimationsMode(AnimationsMode.UKLJUCENE)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("sr"))
        scope.launch { snackbarHostState.showSnackbar(resetConfirmationText) }
    }

    val customImagePath by viewModel.customBackgroundImagePath.collectAsState()
    val showsPhotoBackdrop = !customImagePath.isNullOrBlank() && (currentSkin == AppSkin.CUSTOM || currentSkin == AppSkin.CRNA)

    Box(modifier = Modifier.fillMaxSize()) {
    if (!showsPhotoBackdrop) {
        SkinArt(skin = currentSkin, modifier = Modifier.fillMaxSize())
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } },
        bottomBar = {
            Column {
                androidx.compose.material3.Button(
                    onClick = { saveChanges() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.settings_save_button), modifier = Modifier.padding(start = 8.dp))
                }
                AdBannerBar(viewModel)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.settings_screen_mode), style = MaterialTheme.typography.titleSmall)
            Row(Modifier.padding(top = 8.dp, bottom = 24.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        modifier = Modifier.padding(end = 8.dp),
                        selected = pendingThemeMode == mode,
                        onClick = { pendingThemeMode = mode },
                        label = { Text(mode.localizedName()) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.settings_language_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            ExposedDropdownMenuBox(
                expanded = langMenuExpanded,
                onExpandedChange = { langMenuExpanded = it },
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            ) {
                val currentLabel = SupportedLanguages.ALL.firstOrNull { it.first == pendingLangTag }?.second ?: pendingLangTag
                OutlinedTextField(
                    value = currentLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                DropdownMenu(expanded = langMenuExpanded, onDismissRequest = { langMenuExpanded = false }) {
                    SupportedLanguages.ALL.forEach { (tag, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                pendingLangTag = tag
                                langMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.settings_skins_premium),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                stringResource(R.string.settings_skins_desc),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            AppSkin.entries.forEach { skin ->
                // Prilagođeni (Custom) skin je deo Premium+ paketa (ne samo Premium) — sve
                // ostale premium teme otključava običan Premium.
                val requiresPremiumPlus = skin == AppSkin.CUSTOM
                val locked = skin.premium && (if (requiresPremiumPlus) !premiumPlusActive else !hasPremiumAccess)
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    onClick = {
                        if (locked) lockedSkinTapped = skin else pendingSkin = skin
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkinArt(
                                skin = skin,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Text(skin.localizedName(), modifier = Modifier.padding(start = 12.dp))
                            if (skin.premium) {
                                Text(
                                    "  " + stringResource(if (requiresPremiumPlus) R.string.premium_plus_badge else R.string.settings_premium_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (locked) {
                            Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.settings_locked_desc))
                        } else if (pendingSkin == skin) {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.settings_selected_desc), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (skin == AppSkin.CUSTOM && pendingSkin == AppSkin.CUSTOM && !locked) {
                    CustomSkinEditor(viewModel = viewModel)
                }
            }

            AppearanceSection(
                fontFamily = pendingFontFamily,
                onFontFamilyChange = { pendingFontFamily = it },
                textSize = pendingTextSize,
                onTextSizeChange = { pendingTextSize = it },
                transparencyMode = pendingTransparencyMode,
                onTransparencyModeChange = { pendingTransparencyMode = it },
                animationsMode = pendingAnimationsMode,
                onAnimationsModeChange = { pendingAnimationsMode = it }
            )

            GlassCard(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(R.string.settings_premium_plus_title),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Text(
                        stringResource(R.string.settings_premium_plus_desc),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (premiumPlusUnlocked) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                stringResource(R.string.settings_premium_plus_active),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        TextButton(onClick = { viewModel.clearPremiumPlus() }, modifier = Modifier.padding(top = 4.dp)) {
                            Text(stringResource(R.string.common_lock_test))
                        }
                    } else if (premiumPlusActive) {
                        val remaining = (premiumPlusExpiryMillis ?: 0L) - System.currentTimeMillis()
                        val hours = (remaining / (60 * 60 * 1000L)).coerceAtLeast(0L)
                        val minutes = ((remaining / (60 * 1000L)) % 60).coerceAtLeast(0L)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                stringResource(R.string.settings_premium_plus_temporary_active, hours.toInt(), minutes.toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        TextButton(onClick = { viewModel.clearPremiumPlus() }, modifier = Modifier.padding(top = 4.dp)) {
                            Text(stringResource(R.string.common_lock_test))
                        }
                    } else {
                        Text(
                            stringResource(R.string.premium_plus_not_enabled_test),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            TextButton(onClick = { showRewardedAd() }) {
                                Text(stringResource(R.string.rewarded_ad_watch_button))
                            }
                            TextButton(onClick = { viewModel.setPremiumPlusUnlocked(true) }) {
                                Text(stringResource(R.string.common_unlock_test))
                            }
                        }
                    }
                }
            }

            GlassCard(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.settings_reset_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.settings_reset_desc),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    TextButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(stringResource(R.string.settings_reset_button))
                    }
                }
            }
        }

        lockedSkinTapped?.let { skin ->
            val skinNeedsPremiumPlus = skin == AppSkin.CUSTOM
            ThemedDialog(
                onDismissRequest = { lockedSkinTapped = null },
                title = {
                    Text(
                        stringResource(
                            if (skinNeedsPremiumPlus) R.string.settings_skin_premium_plus_title else R.string.settings_skin_premium_title,
                            skin.localizedName()
                        )
                    )
                },
                text = { Text(stringResource(R.string.settings_skin_dialog_text)) },
                confirmButton = {
                    TextButton(onClick = {
                        if (skinNeedsPremiumPlus) viewModel.setPremiumPlusUnlocked(true) else viewModel.setPremiumUnlocked(true)
                        pendingSkin = skin
                        lockedSkinTapped = null
                    }) { Text(stringResource(R.string.common_unlock_test)) }
                },
                dismissButton = {
                    TextButton(onClick = { lockedSkinTapped = null }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }

        if (showResetDialog) {
            ThemedDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
                text = { Text(stringResource(R.string.settings_reset_dialog_text)) },
                confirmButton = {
                    TextButton(onClick = {
                        resetDisplaySettings()
                        showResetDialog = false
                    }) { Text(stringResource(R.string.common_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }
    }
    }
}

/**
 * Sekcija "Izgled" — globalna podešavanja koja važe preko svih skinova: font (ili
 * automatski prati predlog aktivnog skina), veličina teksta, providnost kartica/prozora
 * i animacije. Deo istog "pending + Sačuvaj izmene" toka kao režim/skin/jezik iznad.
 */
@Composable
private fun AppearanceSection(
    fontFamily: AppFontFamily,
    onFontFamilyChange: (AppFontFamily) -> Unit,
    textSize: TextSizeOption,
    onTextSizeChange: (TextSizeOption) -> Unit,
    transparencyMode: TransparencyMode,
    onTransparencyModeChange: (TransparencyMode) -> Unit,
    animationsMode: AnimationsMode,
    onAnimationsModeChange: (AnimationsMode) -> Unit
) {
    Text(
        stringResource(R.string.settings_appearance_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 8.dp)
    )

    Text(
        stringResource(R.string.settings_font_title),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp)
    )
    Row(Modifier.padding(top = 4.dp, bottom = 12.dp).horizontalScroll(rememberScrollState())) {
        AppFontFamily.entries.forEach { option ->
            FilterChip(
                modifier = Modifier.padding(end = 8.dp),
                selected = fontFamily == option,
                onClick = { onFontFamilyChange(option) },
                label = { Text(option.localizedName()) }
            )
        }
    }

    Text(
        stringResource(R.string.settings_text_size_title),
        style = MaterialTheme.typography.bodySmall
    )
    Row(Modifier.padding(top = 4.dp, bottom = 12.dp).horizontalScroll(rememberScrollState())) {
        TextSizeOption.entries.forEach { option ->
            FilterChip(
                modifier = Modifier.padding(end = 8.dp),
                selected = textSize == option,
                onClick = { onTextSizeChange(option) },
                label = { Text(option.localizedName()) }
            )
        }
    }

    Text(
        stringResource(R.string.settings_transparency_title),
        style = MaterialTheme.typography.bodySmall
    )
    Row(Modifier.padding(top = 4.dp, bottom = 12.dp)) {
        TransparencyMode.entries.forEach { option ->
            FilterChip(
                modifier = Modifier.padding(end = 8.dp),
                selected = transparencyMode == option,
                onClick = { onTransparencyModeChange(option) },
                label = { Text(option.localizedName()) }
            )
        }
    }

    Text(
        stringResource(R.string.settings_animations_title),
        style = MaterialTheme.typography.bodySmall
    )
    Row(Modifier.padding(top = 4.dp, bottom = 8.dp)) {
        AnimationsMode.entries.forEach { option ->
            FilterChip(
                modifier = Modifier.padding(end = 8.dp),
                selected = animationsMode == option,
                onClick = { onAnimationsModeChange(option) },
                label = { Text(option.localizedName()) }
            )
        }
    }
}

/**
 * Podešavanja za Prilagođeni (Custom) skin: pozadinska slika (iz galerije), boja akcenta,
 * boja pozadine, veličina i stil fonta. Sve se primenjuje odmah u SettingsPreferences —
 * ovi izbori nisu deo "pending" stanja jer se svaki menja odvojenim, samostalnim kontrolama
 * (izbor slike, boje, slajder), a njihov efekat je vidljiv tek kad je skin CUSTOM izabran i
 * sačuvan preko glavnog dugmeta "Sačuvaj izmene".
 */
@Composable
private fun CustomSkinEditor(viewModel: MedicationViewModel) {
    val context = LocalContext.current
    val imagePath by viewModel.customBackgroundImagePath.collectAsState()
    val accentColor by viewModel.customAccentColor.collectAsState()
    val backgroundColor by viewModel.customBackgroundColor.collectAsState()
    val fontScale by viewModel.customFontScale.collectAsState()
    val fontStyle by viewModel.customFontStyle.collectAsState()

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val file = File(context.filesDir, "custom_skin_background.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            viewModel.setCustomBackgroundImagePath(file.absolutePath)
        }
    }

    GlassCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.settings_custom_skin_section), style = MaterialTheme.typography.titleSmall)

            Text(
                stringResource(R.string.settings_custom_image_label),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp)
            )
            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!imagePath.isNullOrBlank()) {
                    val thumb = remember(imagePath) {
                        runCatching { BitmapFactory.decodeFile(imagePath) }.getOrNull()?.asImageBitmap()
                    }
                    thumb?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                TextButton(onClick = { imageLauncher.launch("image/*") }) {
                    Text(stringResource(R.string.settings_custom_pick_image))
                }
                if (!imagePath.isNullOrBlank()) {
                    TextButton(onClick = { viewModel.setCustomBackgroundImagePath(null) }) {
                        Text(stringResource(R.string.settings_custom_remove_image))
                    }
                }
            }

            Text(
                stringResource(R.string.settings_custom_accent_color),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )
            ColorSwatchRow(selected = accentColor, onSelect = { viewModel.setCustomAccentColor(it) })

            Text(
                stringResource(R.string.settings_custom_background_color),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )
            ColorSwatchRow(selected = backgroundColor, onSelect = { viewModel.setCustomBackgroundColor(it) })

            Text(
                stringResource(R.string.settings_custom_font_size) + ": ${(fontScale * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )
            Slider(
                value = fontScale,
                onValueChange = { viewModel.setCustomFontScale(it) },
                valueRange = 0.85f..1.3f
            )

            Text(
                stringResource(R.string.settings_custom_font_style),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(Modifier.padding(top = 4.dp).horizontalScroll(rememberScrollState())) {
                CustomFontStyle.entries.forEach { style ->
                    FilterChip(
                        modifier = Modifier.padding(end = 8.dp),
                        selected = fontStyle == style,
                        onClick = { viewModel.setCustomFontStyle(style) },
                        label = { Text(style.localizedLabel()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomFontStyle.localizedLabel(): String = when (this) {
    CustomFontStyle.DEFAULT -> stringResource(R.string.font_style_default)
    CustomFontStyle.SERIF -> stringResource(R.string.font_style_serif)
    CustomFontStyle.SANS -> stringResource(R.string.font_style_sans)
    CustomFontStyle.MONOSPACE -> stringResource(R.string.font_style_monospace)
    CustomFontStyle.CURSIVE -> stringResource(R.string.font_style_cursive)
}

@Composable
private fun ColorSwatchRow(selected: Color, onSelect: (Color) -> Unit) {
    Row(Modifier.padding(top = 4.dp).horizontalScroll(rememberScrollState())) {
        CUSTOM_SKIN_SWATCHES.forEach { color ->
            val isSelected = color == selected
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onSelect(color) }
            )
        }
    }
}
