package com.medicocare.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LabDocumentDao {

    @Insert
    suspend fun insert(document: LabDocument): Long

    @Update
    suspend fun update(document: LabDocument)

    @Delete
    suspend fun delete(document: LabDocument)

    @Query("SELECT * FROM lab_documents ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<LabDocument>>
}
