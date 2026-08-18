package com.medicocare.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Medication::class, MedicationSchedule::class, BarcodeEntry::class,
        IntakeLog::class, Appointment::class, LabDocument::class,
        VitalReading::class, CycleEntry::class, EmergencyNumber::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun barcodeDao(): BarcodeDao
    abstract fun intakeLogDao(): IntakeLogDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun labDocumentDao(): LabDocumentDao
    abstract fun vitalReadingDao(): VitalReadingDao
    abstract fun cycleEntryDao(): CycleEntryDao
    abstract fun emergencyNumberDao(): EmergencyNumberDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medicocare.db"
                )
                    // Projekat je u ranoj fazi razvoja — umesto pisanja migracija za svaku
                    // izmenu šeme, baza se jednostavno ponovo napravi kad verzija poraste.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
