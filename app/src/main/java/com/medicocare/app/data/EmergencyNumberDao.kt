package com.medicocare.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyNumberDao {

    @Insert
    suspend fun insert(number: EmergencyNumber): Long

    @Insert
    suspend fun insertAll(numbers: List<EmergencyNumber>)

    @Update
    suspend fun update(number: EmergencyNumber)

    @Delete
    suspend fun delete(number: EmergencyNumber)

    @Query("SELECT * FROM emergency_numbers ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<EmergencyNumber>>

    @Query("SELECT COUNT(*) FROM emergency_numbers WHERE category IS NOT NULL")
    suspend fun countDefaults(): Int
}
