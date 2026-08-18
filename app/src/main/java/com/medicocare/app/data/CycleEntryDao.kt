package com.medicocare.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleEntryDao {

    @Insert
    suspend fun insert(entry: CycleEntry): Long

    @Update
    suspend fun update(entry: CycleEntry)

    @Delete
    suspend fun delete(entry: CycleEntry)

    @Query("SELECT * FROM cycle_entries ORDER BY startDateMillis DESC")
    fun observeAll(): Flow<List<CycleEntry>>
}
