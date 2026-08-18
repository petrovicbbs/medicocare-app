package com.medicocare.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeLogDao {

    @Insert
    suspend fun insert(log: IntakeLog): Long

    @Update
    suspend fun update(log: IntakeLog)

    @Delete
    suspend fun delete(log: IntakeLog)

    @Query("SELECT * FROM intake_logs WHERE id = :id")
    suspend fun getById(id: Long): IntakeLog?

    @Query(
        """
        SELECT intake_logs.*, medications.name AS medicationName
        FROM intake_logs
        INNER JOIN medications ON medications.id = intake_logs.medicationId
        ORDER BY scheduledAtMillis DESC
        """
    )
    fun observeAllWithMedicationName(): Flow<List<IntakeLogView>>
}
