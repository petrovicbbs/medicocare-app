package com.medicocare.app.repository

import android.content.Context
import com.medicocare.app.data.AppDatabase
import com.medicocare.app.data.VitalReading
import kotlinx.coroutines.flow.Flow

/** Merenja pritiska i šećera u krvi. */
class VitalsRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).vitalReadingDao()

    fun observeAll(): Flow<List<VitalReading>> = dao.observeAll()

    suspend fun save(reading: VitalReading): Long {
        return if (reading.id == 0L) {
            dao.insert(reading)
        } else {
            dao.update(reading)
            reading.id
        }
    }

    suspend fun delete(reading: VitalReading) = dao.delete(reading)
}
