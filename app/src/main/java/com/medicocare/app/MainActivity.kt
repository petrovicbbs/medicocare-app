package com.medicocare.app

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.ads.MobileAds
import com.medicocare.app.alarm.NotificationHelper
import com.medicocare.app.ui.MedicationViewModel
import com.medicocare.app.ui.screens.AddEditAppointmentScreen
import com.medicocare.app.ui.screens.AddEditMedicationScreen
import com.medicocare.app.ui.screens.AppointmentListScreen
import com.medicocare.app.ui.screens.CycleTrackerScreen
import com.medicocare.app.ui.screens.DocumentsScreen
import com.medicocare.app.ui.screens.EmergencyNumbersScreen
import com.medicocare.app.ui.screens.HistoryScreen
import com.medicocare.app.ui.screens.MedicationDetailScreen
import com.medicocare.app.ui.screens.MedicationListScreen
import com.medicocare.app.ui.screens.MedicationScheduleGridScreen
import com.medicocare.app.ui.screens.ScanBarcodeScreen
import com.medicocare.app.ui.screens.ScanPurpose
import com.medicocare.app.ui.screens.ScheduleFormScreen
import com.medicocare.app.ui.screens.SettingsScreen
import com.medicocare.app.ui.screens.TutorialScreen
import com.medicocare.app.ui.screens.VitalsScreen
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.CustomSkinConfig
import com.medicocare.app.ui.theme.MedicoCareTheme
import com.medicocare.app.ui.theme.ThemeMode

class MainActivity : AppCompatActivity() {

    private val viewModel: MedicationViewModel by viewModels {
        MedicationViewModel.factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)
        MobileAds.initialize(this)

        setContent {
            val skin by viewModel.skin.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            val customImagePath by viewModel.customBackgroundImagePath.collectAsState()
            val customAccent by viewModel.customAccentColor.collectAsState()
            val customBackground by viewModel.customBackgroundColor.collectAsState()
            val customFontScale by viewModel.customFontScale.collectAsState()
            val customFontStyle by viewModel.customFontStyle.collectAsState()
            val appFontFamily by viewModel.fontFamily.collectAsState()
            val textSizeOption by viewModel.textSize.collectAsState()
            val transparencyMode by viewModel.transparencyMode.collectAsState()
            val animationsMode by viewModel.animationsMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SISTEMSKI -> systemDark
                ThemeMode.SVETLA -> false
                ThemeMode.TAMNA -> true
            }
            val customConfig = CustomSkinConfig(
                backgroundImagePath = customImagePath,
                accentColor = customAccent,
                backgroundColor = customBackground,
                fontScale = customFontScale,
                fontStyle = customFontStyle
            )

            MedicoCareTheme(
                skin = skin,
                darkTheme = darkTheme,
                customConfig = customConfig,
                fontFamily = appFontFamily,
                textSize = textSizeOption,
                transparencyMode = transparencyMode,
                animationsMode = animationsMode
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (skin == AppSkin.CRNA && !customImagePath.isNullOrBlank()) {
                        // Ista slika kao kod Prilagođenog skina, ali izbledelo (nizak alpha) preko
                        // tamnoplave podloge — da redovi liste ostanu jasno čitljivi preko nje.
                        val bitmap = remember(customImagePath) {
                            runCatching { BitmapFactory.decodeFile(customImagePath) }.getOrNull()?.asImageBitmap()
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().background(Color(0xFF0D1333)),
                                contentScale = ContentScale.Crop,
                                alpha = 0.2f
                            )
                        }
                    } else if (skin == AppSkin.CUSTOM && !customImagePath.isNullOrBlank()) {
                        val bitmap = remember(customImagePath) {
                            runCatching { BitmapFactory.decodeFile(customImagePath) }.getOrNull()?.asImageBitmap()
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    val surfaceColor = if (!customImagePath.isNullOrBlank() && (skin == AppSkin.CUSTOM || skin == AppSkin.CRNA)) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.background
                    }
                Surface(color = surfaceColor) {
                    val navController = rememberNavController()
                    var notifPermissionGranted by remember { mutableStateOf(hasNotificationPermission()) }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted -> notifPermissionGranted = granted }

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifPermissionGranted) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    val needsExactAlarmPermission = needsExactAlarmSetting()

                    NavHost(navController = navController, startDestination = "list") {
                        composable("list") {
                            MedicationScheduleGridScreen(
                                viewModel = viewModel,
                                onOpenMedications = { navController.navigate("medications") },
                                onOpenMedication = { id -> navController.navigate("medication_detail/$id") },
                                onOpenHistory = { navController.navigate("history") },
                                onOpenSettings = { navController.navigate("settings") },
                                onOpenAppointments = { navController.navigate("appointments") },
                                onOpenDocuments = { navController.navigate("documents") },
                                onOpenVitals = { navController.navigate("vitals") },
                                onOpenCycleTracker = { navController.navigate("cycle_tracker") },
                                onScanBarcode = { navController.navigate("scan_barcode_filter") },
                                onOpenEmergencyNumbers = { navController.navigate("emergency_numbers") },
                                onOpenTutorial = { navController.navigate("tutorial") }
                            )
                        }
                        composable("medications") {
                            MedicationListScreen(
                                viewModel = viewModel,
                                showExactAlarmWarning = needsExactAlarmPermission,
                                onOpenExactAlarmSettings = { openExactAlarmSettings() },
                                onAddMedication = { navController.navigate("medication_form/0") },
                                onOpenMedication = { id -> navController.navigate("medication_detail/$id") },
                                onOpenHistory = { navController.navigate("history") },
                                onOpenSettings = { navController.navigate("settings") },
                                onOpenAppointments = { navController.navigate("appointments") },
                                onOpenDocuments = { navController.navigate("documents") },
                                onOpenVitals = { navController.navigate("vitals") },
                                onOpenCycleTracker = { navController.navigate("cycle_tracker") },
                                onScanBarcode = { navController.navigate("scan_barcode_filter") },
                                onOpenEmergencyNumbers = { navController.navigate("emergency_numbers") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("tutorial") {
                            TutorialScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable("emergency_numbers") {
                            EmergencyNumbersScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("scan_barcode_filter") {
                            ScanBarcodeScreen(
                                viewModel = viewModel,
                                onDone = { navController.popBackStack() },
                                onCancel = { navController.popBackStack() },
                                purpose = ScanPurpose.FILTER_LIST
                            )
                        }
                        composable("documents") {
                            DocumentsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("vitals") {
                            VitalsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("cycle_tracker") {
                            CycleTrackerScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("appointments") {
                            AppointmentListScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onAddAppointment = { navController.navigate("appointment_form/0") },
                                onEditAppointment = { id -> navController.navigate("appointment_form/$id") }
                            )
                        }
                        composable(
                            "appointment_form/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getLong("id") ?: 0L
                            AddEditAppointmentScreen(
                                appointmentId = id,
                                viewModel = viewModel,
                                onSaved = { navController.popBackStack() },
                                onCancel = { navController.popBackStack() }
                            )
                        }
                        composable("history") {
                            HistoryScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            "medication_form/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getLong("id") ?: 0L
                            AddEditMedicationScreen(
                                medicationId = id,
                                viewModel = viewModel,
                                onSaved = { savedId ->
                                    navController.popBackStack()
                                    navController.navigate("medication_detail/$savedId")
                                },
                                onCancel = { navController.popBackStack() },
                                onScanBarcode = { navController.navigate("scan_barcode") }
                            )
                        }
                        composable("scan_barcode") {
                            ScanBarcodeScreen(
                                viewModel = viewModel,
                                onDone = { navController.popBackStack() },
                                onCancel = { navController.popBackStack() }
                            )
                        }
                        composable(
                            "medication_detail/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getLong("id") ?: 0L
                            MedicationDetailScreen(
                                medicationId = id,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onEditMedication = { navController.navigate("medication_form/$id") },
                                onAddSchedule = { navController.navigate("schedule_form/$id/0") },
                                onEditSchedule = { scheduleId ->
                                    navController.navigate("schedule_form/$id/$scheduleId")
                                },
                                onDeleted = { navController.popBackStack() }
                            )
                        }
                        composable(
                            "schedule_form/{medicationId}/{scheduleId}",
                            arguments = listOf(
                                navArgument("medicationId") { type = NavType.LongType },
                                navArgument("scheduleId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val medicationId = backStackEntry.arguments?.getLong("medicationId") ?: 0L
                            val scheduleId = backStackEntry.arguments?.getLong("scheduleId") ?: 0L
                            ScheduleFormScreen(
                                medicationId = medicationId,
                                scheduleId = scheduleId,
                                viewModel = viewModel,
                                onSaved = { navController.popBackStack() },
                                onCancel = { navController.popBackStack() }
                            )
                        }
                    }
                }
                }
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun needsExactAlarmSetting(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val alarmManager = getSystemService(AlarmManager::class.java)
        return !alarmManager.canScheduleExactAlarms()
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }
}
