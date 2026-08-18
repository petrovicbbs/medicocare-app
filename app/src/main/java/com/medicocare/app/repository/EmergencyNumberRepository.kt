package com.medicocare.app.repository

import android.content.Context
import com.medicocare.app.data.AppDatabase
import com.medicocare.app.data.DefaultEmergencyNumbers
import com.medicocare.app.data.EmergencyCategory
import com.medicocare.app.data.EmergencyNumber
import kotlinx.coroutines.flow.Flow

/** Hitni brojevi (policija/hitna pomoć/vatrogasci/pomoć na putu + dodatni custom brojevi). */
class EmergencyNumberRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).emergencyNumberDao()

    fun observeAll(): Flow<List<EmergencyNumber>> = dao.observeAll()

    /** Ako osnovna 4 broja još nisu zavedena, popunjava ih prema jeziku aplikacije. */
    suspend fun seedDefaultsIfNeeded(languageTag: String) {
        if (dao.countDefaults() > 0) return
        val defaults = DefaultEmergencyNumbers.forLanguageTag(languageTag)
        val rows = EmergencyCategory.entries.mapIndexed { index, category ->
            EmergencyNumber(
                category = category,
                phoneNumber = defaults[category] ?: "",
                sortOrder = index
            )
        }
        dao.insertAll(rows)
    }

    suspend fun save(number: EmergencyNumber): Long {
        return if (number.id == 0L) {
            dao.insert(number)
        } else {
            dao.update(number)
            number.id
        }
    }

    suspend fun delete(number: EmergencyNumber) = dao.delete(number)
}
