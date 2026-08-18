package com.medicocare.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.medicocare.app.data.Appointment
import com.medicocare.app.data.CycleEntry
import com.medicocare.app.data.EmergencyNumber
import com.medicocare.app.data.IntakeLog
import com.medicocare.app.data.IntakeLogView
import com.medicocare.app.data.IntakeStatus
import com.medicocare.app.data.LabDocument
import com.medicocare.app.data.Medication
import com.medicocare.app.data.MedicationSchedule
import com.medicocare.app.data.MedicationWithSchedules
import com.medicocare.app.data.SettingsPreferences
import com.medicocare.app.data.VitalReading
import com.medicocare.app.repository.AppointmentRepository
import com.medicocare.app.repository.CycleRepository
import com.medicocare.app.repository.CyclePrediction
import com.medicocare.app.repository.DocumentRepository
import com.medicocare.app.repository.EmergencyNumberRepository
import com.medicocare.app.repository.MedicationRepository
import com.medicocare.app.repository.VitalsRepository
import com.medicocare.app.ui.theme.AnimationsMode
import com.medicocare.app.ui.theme.AppFontFamily
import com.medicocare.app.ui.theme.AppSkin
import com.medicocare.app.ui.theme.CustomFontStyle
import com.medicocare.app.ui.theme.TextSizeOption
import com.medicocare.app.ui.theme.ThemeMode
import com.medicocare.app.ui.theme.TransparencyMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Podaci pokupljeni skeniranjem barkoda, koje treba preneti na ekran za unos leka.
 * `name`/`dosage`/`form` su null ako barkod ranije nije viđen — u tom slučaju korisnik
 * ručno unosi podatke, a oni se pri čuvanju vezuju za ovaj barkod za ubuduće.
 */
data class ScannedMedicationPrefill(
    val barcode: String,
    val name: String?,
    val dosage: String?,
    val form: String?
)

class MedicationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MedicationRepository(application)
    private val appointmentRepository = AppointmentRepository(application)
    private val documentRepository = DocumentRepository(application)
    private val vitalsRepository = VitalsRepository(application)
    private val cycleRepository = CycleRepository(application)
    private val emergencyNumberRepository = EmergencyNumberRepository(application)
    private val settingsPrefs = SettingsPreferences(application)

    private val _skin = MutableStateFlow(settingsPrefs.skin)
    val skin: StateFlow<AppSkin> = _skin.asStateFlow()

    private val _themeMode = MutableStateFlow(settingsPrefs.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _fontFamily = MutableStateFlow(settingsPrefs.fontFamily)
    val fontFamily: StateFlow<AppFontFamily> = _fontFamily.asStateFlow()

    private val _textSize = MutableStateFlow(settingsPrefs.textSize)
    val textSize: StateFlow<TextSizeOption> = _textSize.asStateFlow()

    private val _transparencyMode = MutableStateFlow(settingsPrefs.transparencyMode)
    val transparencyMode: StateFlow<TransparencyMode> = _transparencyMode.asStateFlow()

    private val _animationsMode = MutableStateFlow(settingsPrefs.animationsMode)
    val animationsMode: StateFlow<AnimationsMode> = _animationsMode.asStateFlow()

    private val _premiumUnlocked = MutableStateFlow(settingsPrefs.premiumUnlocked)
    val premiumUnlocked: StateFlow<Boolean> = _premiumUnlocked.asStateFlow()

    fun setSkin(newSkin: AppSkin) {
        settingsPrefs.skin = newSkin
        _skin.value = newSkin
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsPrefs.themeMode = mode
        _themeMode.value = mode
    }

    fun setFontFamily(value: AppFontFamily) {
        settingsPrefs.fontFamily = value
        _fontFamily.value = value
    }

    fun setTextSize(value: TextSizeOption) {
        settingsPrefs.textSize = value
        _textSize.value = value
    }

    fun setTransparencyMode(value: TransparencyMode) {
        settingsPrefs.transparencyMode = value
        _transparencyMode.value = value
    }

    fun setAnimationsMode(value: AnimationsMode) {
        settingsPrefs.animationsMode = value
        _animationsMode.value = value
    }

    /** Privremeni test-prekidač dok se ne uvede pravo plaćanje (Google Play Billing). */
    fun setPremiumUnlocked(unlocked: Boolean) {
        settingsPrefs.premiumUnlocked = unlocked
        _premiumUnlocked.value = unlocked
    }

    private val _premiumPlusUnlocked = MutableStateFlow(settingsPrefs.premiumPlusUnlocked)
    val premiumPlusUnlocked: StateFlow<Boolean> = _premiumPlusUnlocked.asStateFlow()

    /** Poseban, viši nivo pretplate — za sada samo uklanja banner reklamu. Isti test-prekidač princip. */
    fun setPremiumPlusUnlocked(unlocked: Boolean) {
        settingsPrefs.premiumPlusUnlocked = unlocked
        _premiumPlusUnlocked.value = unlocked
    }

    // "Premium+" je zapravo "Premium + još nešto" — ko ima Premium+ (trajno ili privremeno,
    // preko Rewarded reklame) automatski dobija i sve Premium pogodnosti, ne samo uklanjanje
    // reklame. Zato ekrani koji proveravaju pristup Premium sadržaju treba da koriste
    // "hasPremiumAccess" umesto sirovog "premiumUnlocked".

    private val _premiumPlusExpiryMillis = MutableStateFlow(settingsPrefs.premiumPlusExpiryMillis)
    val premiumPlusExpiryMillis: StateFlow<Long?> = _premiumPlusExpiryMillis.asStateFlow()

    /** Otkucava na svakih 30s da bi se "premiumPlusActive"/"hasPremiumAccess" sami osvežili
     *  kad privremeni Premium+ istekne, bez potrebe za bilo kakvom akcijom korisnika. */
    private val premiumTick = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }

    /** Da li je Premium+ trenutno aktivan — trajno otključan ILI privremeno (Rewarded reklama). */
    val premiumPlusActive: StateFlow<Boolean> = combine(
        _premiumPlusUnlocked, _premiumPlusExpiryMillis, premiumTick
    ) { permanent, expiry, now ->
        permanent || (expiry != null && expiry > now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsPrefs.premiumPlusUnlocked ||
            (settingsPrefs.premiumPlusExpiryMillis?.let { it > System.currentTimeMillis() } ?: false)
    )

    /** Efektivni Premium pristup — Premium ILI Premium+ (Premium+ uvek uključuje sve iz Premium-a). */
    val hasPremiumAccess: StateFlow<Boolean> = combine(
        _premiumUnlocked, premiumPlusActive
    ) { premium, plusActive ->
        premium || plusActive
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsPrefs.premiumUnlocked || settingsPrefs.premiumPlusUnlocked ||
            (settingsPrefs.premiumPlusExpiryMillis?.let { it > System.currentTimeMillis() } ?: false)
    )

    /** Poziva se kad korisnik uspešno odgleda Rewarded reklamu do kraja — daje 1h Premium+.
     *  Ako već ima aktivan privremeni Premium+, novih 1h se nadovezuje na postojeći rok. */
    fun grantTemporaryPremiumPlus(durationMillis: Long = ONE_HOUR_MILLIS) {
        val now = System.currentTimeMillis()
        val currentExpiry = settingsPrefs.premiumPlusExpiryMillis
        val base = if (currentExpiry != null && currentExpiry > now) currentExpiry else now
        val newExpiry = base + durationMillis
        settingsPrefs.premiumPlusExpiryMillis = newExpiry
        _premiumPlusExpiryMillis.value = newExpiry
    }

    /** Ručno gašenje Premium+ (i trajnog test-otključavanja i privremenog roka preko reklame) —
     *  da se ponovo vide banner reklama i dugme za Rewarded reklamu. */
    fun clearPremiumPlus() {
        settingsPrefs.premiumPlusUnlocked = false
        _premiumPlusUnlocked.value = false
        settingsPrefs.premiumPlusExpiryMillis = null
        _premiumPlusExpiryMillis.value = null
    }

    // ---------- Prilagođeni (Custom) skin ----------

    private val _customBackgroundImagePath = MutableStateFlow(settingsPrefs.customBackgroundImagePath)
    val customBackgroundImagePath: StateFlow<String?> = _customBackgroundImagePath.asStateFlow()

    private val _customAccentColor = MutableStateFlow(Color(settingsPrefs.customAccentColorArgb))
    val customAccentColor: StateFlow<Color> = _customAccentColor.asStateFlow()

    private val _customBackgroundColor = MutableStateFlow(Color(settingsPrefs.customBackgroundColorArgb))
    val customBackgroundColor: StateFlow<Color> = _customBackgroundColor.asStateFlow()

    private val _customFontScale = MutableStateFlow(settingsPrefs.customFontScale)
    val customFontScale: StateFlow<Float> = _customFontScale.asStateFlow()

    private val _customFontStyle = MutableStateFlow(settingsPrefs.customFontStyle)
    val customFontStyle: StateFlow<CustomFontStyle> = _customFontStyle.asStateFlow()

    fun setCustomBackgroundImagePath(path: String?) {
        settingsPrefs.customBackgroundImagePath = path
        _customBackgroundImagePath.value = path
    }

    fun setCustomAccentColor(color: Color) {
        settingsPrefs.customAccentColorArgb = color.toArgb()
        _customAccentColor.value = color
    }

    fun setCustomBackgroundColor(color: Color) {
        settingsPrefs.customBackgroundColorArgb = color.toArgb()
        _customBackgroundColor.value = color
    }

    fun setCustomFontScale(scale: Float) {
        settingsPrefs.customFontScale = scale
        _customFontScale.value = scale
    }

    fun setCustomFontStyle(style: CustomFontStyle) {
        settingsPrefs.customFontStyle = style
        _customFontStyle.value = style
    }

    private val _scanResult = MutableStateFlow<ScannedMedicationPrefill?>(null)
    val scanResult: StateFlow<ScannedMedicationPrefill?> = _scanResult.asStateFlow()

    fun setScanResult(result: ScannedMedicationPrefill) {
        _scanResult.value = result
    }

    fun clearScanResult() {
        _scanResult.value = null
    }

    suspend fun lookupBarcode(barcode: String) = repository.lookupBarcode(barcode)

    private val _barcodeFilter = MutableStateFlow<String?>(null)
    val barcodeFilter: StateFlow<String?> = _barcodeFilter.asStateFlow()

    fun setBarcodeFilter(barcode: String?) {
        _barcodeFilter.value = barcode
    }

    fun rememberBarcode(barcode: String, name: String, dosage: String, form: String) {
        viewModelScope.launch { repository.rememberBarcode(barcode, name, dosage, form) }
    }

    val medications: StateFlow<List<MedicationWithSchedules>> =
        repository.observeMedications().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun medicationDetail(id: Long): StateFlow<MedicationWithSchedules?> =
        repository.observeMedication(id).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun saveMedication(medication: Medication, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveMedication(medication)
            onSaved(id)
        }
    }

    fun deleteMedication(item: MedicationWithSchedules) {
        viewModelScope.launch { repository.deleteMedication(item) }
    }

    fun saveSchedule(schedule: MedicationSchedule, medicationName: String, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveSchedule(schedule, medicationName)
            onSaved()
        }
    }

    fun deleteSchedule(schedule: MedicationSchedule) {
        viewModelScope.launch { repository.deleteSchedule(schedule) }
    }

    fun setScheduleEnabled(schedule: MedicationSchedule, enabled: Boolean, medicationName: String) {
        viewModelScope.launch { repository.setScheduleEnabled(schedule, enabled, medicationName) }
    }

    val history: StateFlow<List<IntakeLogView>> =
        repository.observeHistory().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun markIntake(logId: Long, status: IntakeStatus) {
        viewModelScope.launch { repository.markIntake(logId, status) }
    }

    /** Ručno dodeljivanje leka na termin (npr. klikom na polje u tabeli na glavnoj stranici). */
    fun logManualIntake(medicationId: Long, scheduledAtMillis: Long, doseLabel: String, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.logScheduledIntake(
                medicationId = medicationId,
                scheduleId = 0L,
                scheduledAtMillis = scheduledAtMillis,
                doseLabel = doseLabel
            )
            onSaved(id)
        }
    }

    fun deleteIntakeLog(log: IntakeLog) {
        viewModelScope.launch { repository.deleteIntakeLog(log) }
    }

    fun restock(medicationId: Long, addAmount: Double) {
        viewModelScope.launch { repository.restock(medicationId, addAmount) }
    }

    val appointments: StateFlow<List<Appointment>> =
        appointmentRepository.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveAppointment(appointment: Appointment, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            appointmentRepository.save(appointment)
            onSaved()
        }
    }

    fun deleteAppointment(appointment: Appointment) {
        viewModelScope.launch { appointmentRepository.delete(appointment) }
    }

    // --- Registar dokumenata/izveštaja (slikane laboratorijske analize i sl.) ---

    val documents: StateFlow<List<LabDocument>> =
        documentRepository.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun newDocumentFile(): File = documentRepository.newDocumentFile()

    fun documentUri(file: File): Uri = documentRepository.uriForFile(file)

    fun saveDocument(document: LabDocument, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            documentRepository.save(document)
            onSaved()
        }
    }

    fun deleteDocument(document: LabDocument) {
        viewModelScope.launch { documentRepository.delete(document) }
    }

    // --- Vitalni znaci: pritisak i šećer u krvi ---

    val vitalReadings: StateFlow<List<VitalReading>> =
        vitalsRepository.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveVitalReading(reading: VitalReading, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            vitalsRepository.save(reading)
            onSaved()
        }
    }

    fun deleteVitalReading(reading: VitalReading) {
        viewModelScope.launch { vitalsRepository.delete(reading) }
    }

    // --- Praćenje menstrualnog ciklusa ---

    val cycleEntries: StateFlow<List<CycleEntry>> =
        cycleRepository.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val cyclePrediction: StateFlow<CyclePrediction> =
        cycleRepository.observeAll()
            .map { entries -> CycleRepository.predict(entries) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = CyclePrediction(null, null, null, null)
            )

    fun saveCycleEntry(entry: CycleEntry, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            cycleRepository.save(entry)
            onSaved()
        }
    }

    fun deleteCycleEntry(entry: CycleEntry) {
        viewModelScope.launch { cycleRepository.delete(entry) }
    }

    // --- Hitni brojevi (policija/hitna pomoć/vatrogasci/pomoć na putu + custom) ---

    val emergencyNumbers: StateFlow<List<EmergencyNumber>> =
        emergencyNumberRepository.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Popunjava osnovna 4 broja prema jeziku aplikacije, samo ako još nisu zavedena. */
    fun seedEmergencyDefaultsIfNeeded(languageTag: String) {
        viewModelScope.launch { emergencyNumberRepository.seedDefaultsIfNeeded(languageTag) }
    }

    fun saveEmergencyNumber(number: EmergencyNumber, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            emergencyNumberRepository.save(number)
            onSaved()
        }
    }

    fun deleteEmergencyNumber(number: EmergencyNumber) {
        viewModelScope.launch { emergencyNumberRepository.delete(number) }
    }

    companion object {
        const val ONE_HOUR_MILLIS = 60 * 60 * 1000L

        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MedicationViewModel(application) as T
                }
            }
    }
}
