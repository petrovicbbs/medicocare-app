package com.medicocare.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalReadingDao {

    @Insert
    suspend fun insert(reading: VitalReading): Long

    @Update
    suspend fun update(reading: VitalReading)

    @Delete
    suspend fun delete(reading: VitalReading)

    @Query("SELECT * FROM vital_readings ORDER BY dateTimeMillis DESC")
    fun observeAll(): Flow<List<VitalReading>>

    @Query("SELECT * FROM vital_readings WHERE type = :type ORDER BY dateTimeMillis DESC")
    fun observeByType(type: VitalType): Flow<List<VitalReading>>
}
