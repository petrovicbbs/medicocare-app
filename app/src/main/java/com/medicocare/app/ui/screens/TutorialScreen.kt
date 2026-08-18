package com.medicocare.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import com.medicocare.app.ui.components.GlassCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.medicocare.app.R
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.ads.AdBannerBar
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.SkinArt

private data class TutorialSection(val icon: ImageVector, val titleRes: Int, val bodyRes: Int)

private val TUTORIAL_SECTIONS = listOf(
    TutorialSection(Icons.Filled.MedicalServices, R.string.tutorial_section_main_title, R.string.tutorial_section_main_body),
    TutorialSection(Icons.Filled.NoteAdd, R.string.tutorial_section_addmed_title, R.string.tutorial_section_addmed_body),
    TutorialSection(Icons.Filled.History, R.string.tutorial_section_history_title, R.string.tutorial_section_history_body),
    TutorialSection(Icons.Filled.EventNote, R.string.tutorial_section_appointments_title, R.string.tutorial_section_appointments_body),
    TutorialSection(Icons.Filled.Description, R.string.tutorial_section_documents_title, R.string.tutorial_section_documents_body),
    TutorialSection(Icons.Filled.Bloodtype, R.string.tutorial_section_vitals_title, R.string.tutorial_section_vitals_body),
    TutorialSection(Icons.Filled.CalendarMonth, R.string.tutorial_section_cycle_title, R.string.tutorial_section_cycle_body),
    TutorialSection(Icons.Filled.Call, R.string.tutorial_section_emergency_title, R.string.tutorial_section_emergency_body),
    TutorialSection(Icons.Filled.Settings, R.string.tutorial_section_settings_title, R.string.tutorial_section_settings_body),
    TutorialSection(Icons.Filled.WorkspacePremium, R.string.tutorial_section_premium_title, R.string.tutorial_section_premium_body)
)

/**
 * Uputstvo/Tutorial — kratak vodič kroz sve ekrane i opcije aplikacije, u vidu spiska
 * sekcija koje se šire dodirom (nema stvarne navigacije kroz ekrane, samo objašnjenje).
 * Otvara se preko "?" dugmeta na glavnom ekranu, odmah pored izbora jezika.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(viewModel: MedicationViewModel, onBack: () -> Unit) {
    var expandedIndex by remember { mutableStateOf<Int?>(0) }

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
                title = { Text(stringResource(R.string.tutorial_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        bottomBar = { AdBannerBar(viewModel) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Text(
                    stringResource(R.string.tutorial_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            itemsIndexed(TUTORIAL_SECTIONS) { index, section ->
                val expanded = expandedIndex == index
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    onClick = { expandedIndex = if (expanded) null else index }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    stringResource(section.titleRes),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                            Icon(
                                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null
                            )
                        }
                        if (expanded) {
                            Text(
                                stringResource(section.bodyRes),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    }
}
