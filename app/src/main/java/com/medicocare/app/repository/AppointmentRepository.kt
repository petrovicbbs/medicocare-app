package com.medicocare.app.repository

import android.content.Context
import com.medicocare.app.alarm.AppointmentAlarmScheduler
import com.medicocare.app.data.AppDatabase
import com.medicocare.app.data.Appointment
import kotlinx.coroutines.flow.Flow

/** Pristup podacima za preglede + (za)kazivanje njihovih podsetnika. */
class AppointmentRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val appointmentDao = db.appointmentDao()
    private val alarmScheduler = AppointmentAlarmScheduler(context)

    fun observeAll(): Flow<List<Appointment>> = appointmentDao.observeAll()

    suspend fun save(appointment: Appointment): Long {
        val id = if (appointment.id == 0L) {
            appointmentDao.insert(appointment)
        } else {
            appointmentDao.update(appointment)
            appointment.id
        }
        val saved = appointment.copy(id = id)
        alarmScheduler.schedule(saved)
        return id
    }

    suspend fun delete(appointment: Appointment) {
        alarmScheduler.cancel(appointment.id)
        appointmentDao.delete(appointment)
    }
}
