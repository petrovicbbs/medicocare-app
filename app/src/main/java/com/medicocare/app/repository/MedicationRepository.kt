package com.medicocare.app.repository

import android.content.Context
import com.medicocare.app.alarm.AlarmScheduler
import com.medicocare.app.alarm.RefillReminderScheduler
import com.medicocare.app.data.AppDatabase
import com.medicocare.app.data.BarcodeEntry
import com.medicocare.app.data.IntakeLog
import com.medicocare.app.data.IntakeLogView
import com.medicocare.app.data.IntakeStatus
import com.medicocare.app.data.Medication
import com.medicocare.app.data.MedicationSchedule
import com.medicocare.app.data.MedicationWithSchedules
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * Jedinstvena tačka za pristup podacima (Room) i za (za)kazivanje alarma.
 */
class MedicationRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val medicationDao = db.medicationDao()
    private val scheduleDao = db.scheduleDao()
    private val barcodeDao = db.barcodeDao()
    private val intakeLogDao = db.intakeLogDao()
    private val alarmScheduler = AlarmScheduler(context)
    private val refillReminderScheduler = RefillReminderScheduler(context)

    fun observeMedications(): Flow<List<MedicationWithSchedules>> =
        medicationDao.observeAllWithSchedules()

    fun observeMedication(id: Long): Flow<MedicationWithSchedules?> =
        medicationDao.observeWithSchedules(id)

    suspend fun saveMedication(medication: Medication): Long {
        val existing = if (medication.id != 0L) medicationDao.getById(medication.id) else null
        val prepared = prepareRefillReminder(medication, existing)

        val id = if (prepared.id == 0L) {
            medicationDao.insert(prepared)
        } else {
            medicationDao.update(prepared)
            prepared.id
        }
        val saved = prepared.copy(id = id)

        if (saved.refillReminderEnabled) {
            refillReminderScheduler.schedule(saved)
        } else {
            refillReminderScheduler.cancel(saved.id)
        }

        return id
    }

    /**
     * Ekran za unos leka (AddEditMedicationScreen) sad sam računa i nudi ručni izbor datuma za
     * "sledeći termin" (uklj. bezbednosnu proveru da nije u prošlosti), pa se ovde taj izbor
     * samo poštuje. Ako iz nekog razloga stigne bez postavljenog termina (npr. programski poziv
     * mimo ekrana), tek onda se izračunava "sad + interval" kao razumna rezerva.
     */
    private fun prepareRefillReminder(medication: Medication, existing: Medication?): Medication {
        if (!medication.refillReminderEnabled) {
            return medication.copy(nextRefillReminderMillis = null)
        }
        if (medication.nextRefillReminderMillis != null) {
            return medication
        }
        val intervalMillis = medication.refillIntervalDays.coerceAtLeast(1) * TimeUnit.DAYS.toMillis(1)
        return medication.copy(nextRefillReminderMillis = System.currentTimeMillis() + intervalMillis)
    }

    suspend fun deleteMedication(medicationWithSchedules: MedicationWithSchedules) {
        medicationWithSchedules.schedules.forEach { alarmScheduler.cancel(it) }
        refillReminderScheduler.cancel(medicationWithSchedules.medication.id)
        medicationDao.delete(medicationWithSchedules.medication)
    }

    /** Ponovo zakazuje sve aktivne podsetnike za dopunu leka (npr. nakon restarta uređaja). */
    suspend fun rescheduleAllRefillReminders() {
        medicationDao.getAllWithRefillReminderEnabled().forEach { medication ->
            refillReminderScheduler.schedule(medication)
        }
    }

    suspend fun saveSchedule(schedule: MedicationSchedule, medicationName: String): Long {
        val id = if (schedule.id == 0L) {
            scheduleDao.insert(schedule)
        } else {
            scheduleDao.update(schedule)
            schedule.id
        }
        val saved = schedule.copy(id = id)
        if (saved.enabled) {
            alarmScheduler.schedule(saved, medicationName)
        } else {
            alarmScheduler.cancel(saved)
        }
        return id
    }

    suspend fun deleteSchedule(schedule: MedicationSchedule) {
        alarmScheduler.cancel(schedule)
        scheduleDao.delete(schedule)
    }

    suspend fun setScheduleEnabled(schedule: MedicationSchedule, enabled: Boolean, medicationName: String) {
        val updated = schedule.copy(enabled = enabled)
        scheduleDao.update(updated)
        if (enabled) {
            alarmScheduler.schedule(updated, medicationName)
        } else {
            alarmScheduler.cancel(updated)
        }
    }

    suspend fun rescheduleAllEnabled() {
        val enabled = scheduleDao.getAllEnabled()
        enabled.forEach { schedule ->
            val medication = medicationDao.getById(schedule.medicationId)
            if (medication != null) {
                alarmScheduler.schedule(schedule, medication.name)
            }
        }
    }

    /** Da li je ovaj barkod već viđen ranije (naučen iz prethodnog ručnog unosa)? */
    suspend fun lookupBarcode(barcode: String): BarcodeEntry? = barcodeDao.getByBarcode(barcode)

    /** Zapamti barkod → lek, da bi se sledeći put isti barkod automatski popunio. */
    suspend fun rememberBarcode(barcode: String, name: String, dosage: String, form: String) {
        barcodeDao.upsert(BarcodeEntry(barcode = barcode, name = name, dosage = dosage, form = form))
    }

    /** Beleži da je alarm okinuo — kreira "na čekanju" zapis u istoriji. Zove ga AlarmReceiver. */
    suspend fun logScheduledIntake(
        medicationId: Long,
        scheduleId: Long,
        scheduledAtMillis: Long,
        doseLabel: String
    ): Long = intakeLogDao.insert(
        IntakeLog(
            medicationId = medicationId,
            scheduleId = scheduleId,
            scheduledAtMillis = scheduledAtMillis,
            doseLabel = doseLabel,
            status = IntakeStatus.NA_CEKANJU
        )
    )

    /**
     * Označava zapis u istoriji kao uzet/preskočen (sa dugmeta na notifikaciji ili ručno).
     * Ako je "Uzeto" i lek prati zalihu, automatski oduzima potrošenu količinu.
     */
    suspend fun markIntake(logId: Long, status: IntakeStatus) {
        val existing = intakeLogDao.getById(logId) ?: return
        intakeLogDao.update(existing.copy(status = status, actedAtMillis = System.currentTimeMillis()))

        if (status == IntakeStatus.UZETO) {
            val medication = medicationDao.getById(existing.medicationId)
            val currentStock = medication?.stockCount
            if (medication != null && currentStock != null) {
                val consumed = parseDoseQuantity(existing.doseLabel)
                val newStock = (currentStock - consumed).coerceAtLeast(0.0)
                medicationDao.update(medication.copy(stockCount = newStock))
            }
        }
    }

    /** Dodaje kupljenu količinu na postojeću zalihu (npr. kupljena nova kutija leka). */
    suspend fun restock(medicationId: Long, addAmount: Double) {
        val medication = medicationDao.getById(medicationId) ?: return
        val newStock = (medication.stockCount ?: 0.0) + addAmount
        medicationDao.update(medication.copy(stockCount = newStock))
    }

    /** Briše ručno dodeljen (ili bilo koji) zapis iz istorije, npr. iz tabele na glavnoj stranici. */
    suspend fun deleteIntakeLog(log: IntakeLog) = intakeLogDao.delete(log)

    fun observeHistory(): Flow<List<IntakeLogView>> = intakeLogDao.observeAllWithMedicationName()

    companion object {
        /**
         * Pokušava da izvuče brojnu količinu sa početka teksta doze (npr. "0.5 tableta" -> 0.5,
         * "2 kapi" -> 2.0, "1 tableta" -> 1.0). Ako ne uspe (prazno/nečitljivo), pretpostavlja 1.
         */
        fun parseDoseQuantity(doseLabel: String): Double {
            val match = Regex("""[\d]+([.,]\d+)?""").find(doseLabel.trim())
                ?: return 1.0
            return match.value.replace(',', '.').toDoubleOrNull() ?: 1.0
        }
    }
}
