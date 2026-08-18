package com.medicocare.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ScheduleDao {

    @Insert
    suspend fun insert(schedule: MedicationSchedule): Long

    @Update
    suspend fun update(schedule: MedicationSchedule)

    @Delete
    suspend fun delete(schedule: MedicationSchedule)

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: Long): MedicationSchedule?

    @Query("SELECT * FROM schedules WHERE enabled = 1")
    suspend fun getAllEnabled(): List<MedicationSchedule>

    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId")
    suspend fun getForMedication(medicationId: Long): List<MedicationSchedule>
}
