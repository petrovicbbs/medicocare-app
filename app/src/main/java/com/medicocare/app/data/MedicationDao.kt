package com.medicocare.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Insert
    suspend fun insert(medication: Medication): Long

    @Update
    suspend fun update(medication: Medication)

    @Delete
    suspend fun delete(medication: Medication)

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): Medication?

    @Transaction
    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun observeAllWithSchedules(): Flow<List<MedicationWithSchedules>>

    @Transaction
    @Query("SELECT * FROM medications WHERE id = :id")
    fun observeWithSchedules(id: Long): Flow<MedicationWithSchedules?>

    @Query("SELECT * FROM medications WHERE refillReminderEnabled = 1")
    suspend fun getAllWithRefillReminderEnabled(): List<Medication>
}
