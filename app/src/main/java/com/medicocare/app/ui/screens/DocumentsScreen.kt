package com.medicocare.app.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import com.medicocare.app.ui.components.GlassCard
import com.medicocare.app.ui.components.ThemedDialog
import com.medicocare.app.ui.components.ThemedTextField
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.medicocare.app.R
import com.medicocare.app.data.LabDocument
import com.medicocare.app.report.ReportGenerator
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.ads.AdBannerBar
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.SkinArt
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DOC_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.")

/**
 * Hronološki registar slikanih dokumenata (laboratorijske analize, nalazi i sl.),
 * sa pretragom po opsegu datuma, sortiranjem i deljenjem.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    viewModel: MedicationViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsState()
    val premiumUnlocked by viewModel.hasPremiumAccess.collectAsState()
    val shareChooserTitle = stringResource(R.string.documents_share_chooser)
    val shareAllChooserTitle = stringResource(R.string.documents_share_all_chooser)

    var pendingFile by remember { mutableStateOf<File?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var viewingDocument by remember { mutableStateOf<LabDocument?>(null) }
    var toDelete by remember { mutableStateOf<LabDocument?>(null) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    var fromDateMillis by remember { mutableStateOf<Long?>(null) }
    var toDateMillis by remember { mutableStateOf<Long?>(null) }
    var sortAscending by remember { mutableStateOf(false) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            showSaveDialog = true
        } else {
            pendingFile?.delete()
            pendingFile = null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val file = viewModel.newDocumentFile()
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            pendingFile = file
            showSaveDialog = true
        }
    }

    val filteredSorted = remember(documents, fromDateMillis, toDateMillis, sortAscending) {
        val filtered = documents.filter { doc ->
            (fromDateMillis == null || doc.dateMillis >= fromDateMillis!!) &&
                (toDateMillis == null || doc.dateMillis <= toDateMillis!! + 86_399_000L)
        }
        if (sortAscending) filtered.sortedBy { it.dateMillis } else filtered.sortedByDescending { it.dateMillis }
    }

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
                title = { Text(stringResource(R.string.documents_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { sortAscending = !sortAscending }) {
                        Icon(
                            if (sortAscending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                            contentDescription = stringResource(R.string.documents_sort_desc)
                        )
                    }
                    IconButton(onClick = {
                        if (!premiumUnlocked) {
                            showPremiumDialog = true
                        } else {
                            val uri = ReportGenerator.generateDocumentsPdf(context, filteredSorted)
                            shareDocumentReport(context, uri, shareAllChooserTitle)
                        }
                    }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = stringResource(R.string.documents_share_all_pdf_desc))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showSourceDialog = true }) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = stringResource(R.string.documents_add_desc))
            }
        },
        bottomBar = { AdBannerBar(viewModel) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { showFromPicker = true }, modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                    Text(fromDateMillis?.let { DOC_DATE_FORMAT.format(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC)) } ?: stringResource(R.string.documents_from_date))
                }
                OutlinedButton(onClick = { showToPicker = true }, modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(toDateMillis?.let { DOC_DATE_FORMAT.format(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC)) } ?: stringResource(R.string.documents_to_date))
                }
                if (fromDateMillis != null || toDateMillis != null) {
                    IconButton(onClick = { fromDateMillis = null; toDateMillis = null }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.documents_clear_filter_desc))
                    }
                }
            }

            if (filteredSorted.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null)
                    Text(
                        stringResource(R.string.documents_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(filteredSorted, key = { it.id }) { doc ->
                        DocumentRow(
                            document = doc,
                            onClick = { viewingDocument = doc },
                            onDelete = { toDelete = doc },
                            onShare = {
                                if (!premiumUnlocked) {
                                    showPremiumDialog = true
                                } else {
                                    shareDocumentImage(context, viewModel.documentUri(File(doc.filePath)), shareChooserTitle)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSourceDialog) {
        ThemedDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text(stringResource(R.string.documents_add_dialog_title)) },
            text = { Text(stringResource(R.string.documents_add_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    val file = viewModel.newDocumentFile()
                    pendingFile = file
                    cameraLauncher.launch(viewModel.documentUri(file))
                }) { Text(stringResource(R.string.documents_camera)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    galleryLauncher.launch("image/*")
                }) { Text(stringResource(R.string.documents_gallery)) }
            }
        )
    }

    if (showSaveDialog && pendingFile != null) {
        DocumentSaveDialog(
            onDismiss = {
                showSaveDialog = false
                pendingFile?.delete()
                pendingFile = null
            },
            onSave = { title, notes, dateMillis ->
                viewModel.saveDocument(
                    LabDocument(title = title, dateMillis = dateMillis, filePath = pendingFile!!.absolutePath, notes = notes)
                )
                showSaveDialog = false
                pendingFile = null
            }
        )
    }

    viewingDocument?.let { doc ->
        DocumentViewerDialog(document = doc, onDismiss = { viewingDocument = null })
    }

    toDelete?.let { doc ->
        ThemedDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.documents_delete_title)) },
            text = { Text(stringResource(R.string.documents_delete_text, doc.title)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteDocument(doc); toDelete = null }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (showFromPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = fromDateMillis)
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = { fromDateMillis = state.selectedDateMillis; showFromPicker = false }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = { TextButton(onClick = { showFromPicker = false }) { Text(stringResource(R.string.common_cancel)) } }
        ) { DatePicker(state = state) }
    }
    if (showToPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = toDateMillis)
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = { toDateMillis = state.selectedDateMillis; showToPicker = false }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = { TextButton(onClick = { showToPicker = false }) { Text(stringResource(R.string.common_cancel)) } }
        ) { DatePicker(state = state) }
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
private fun DocumentRow(document: LabDocument, onClick: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit) {
    val dateLabel = DOC_DATE_FORMAT.format(Instant.ofEpochMilli(document.dateMillis).atZone(ZoneId.systemDefault()))
    val thumb = remember(document.filePath) {
        runCatching {
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(document.filePath, options)
        }.getOrNull()?.asImageBitmap()
    }
    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (thumb != null) {
                Image(bitmap = thumb, contentDescription = null, modifier = Modifier.size(56.dp))
            } else {
                Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(56.dp))
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(document.title, style = MaterialTheme.typography.titleSmall)
                Text(dateLabel, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onShare) { Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.documents_share_desc)) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.documents_delete_desc)) }
        }
    }
}

@Composable
private fun DocumentViewerDialog(document: LabDocument, onDismiss: () -> Unit) {
    val bitmap = remember(document.filePath) {
        runCatching { BitmapFactory.decodeFile(document.filePath) }.getOrNull()?.asImageBitmap()
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(12.dp)) {
                Text(document.title, style = MaterialTheme.typography.titleMedium)
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
                if (document.notes.isNotBlank()) {
                    Text(document.notes, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) {
                    Text(stringResource(R.string.common_close))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentSaveDialog(onDismiss: () -> Unit, onSave: (String, String, Long) -> Unit) {
    val defaultPrefix = stringResource(R.string.documents_default_title_prefix)
    val fallbackTitle = stringResource(R.string.documents_default_fallback_title)
    var title by remember { mutableStateOf(defaultPrefix + DOC_DATE_FORMAT.format(LocalDate.now())) }
    var notes by remember { mutableStateOf("") }
    val dateMillis = remember { System.currentTimeMillis() }

    ThemedDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.documents_save_dialog_title)) },
        text = {
            Column {
                ThemedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.documents_name_label)) }, modifier = Modifier.fillMaxWidth())
                ThemedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.common_notes_optional)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title.trim().ifBlank { fallbackTitle }, notes.trim(), dateMillis) }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

private fun shareDocumentImage(context: Context, uri: Uri, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

private fun shareDocumentReport(context: Context, uri: Uri, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
