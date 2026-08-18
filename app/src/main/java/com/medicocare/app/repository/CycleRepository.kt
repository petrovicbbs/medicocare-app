package com.medicocare.app.repository

import android.content.Context
import com.medicocare.app.data.AppDatabase
import com.medicocare.app.data.CycleEntry
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/** Procena narednog ciklusa i plodnog perioda — samo okvirna procena, ne medicinski savet. */
data class CyclePrediction(
    val averageCycleLengthDays: Int?,
    val nextPeriodStartMillis: Long?,
    val fertileWindowStartMillis: Long?,
    val fertileWindowEndMillis: Long?
)

class CycleRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).cycleEntryDao()

    fun observeAll(): Flow<List<CycleEntry>> = dao.observeAll()

    suspend fun save(entry: CycleEntry): Long {
        return if (entry.id == 0L) {
            dao.insert(entry)
        } else {
            dao.update(entry)
            entry.id
        }
    }

    suspend fun delete(entry: CycleEntry) = dao.delete(entry)

    companion object {
        private val DAY_MS = TimeUnit.DAYS.toMillis(1)

        /**
         * Računa prosečnu dužinu ciklusa iz poslednjih (do 6) razmaka između početaka ciklusa,
         * pa na osnovu toga procenjuje sledeći ciklus i plodni period (ovulacija ~14 dana pre
         * sledećeg ciklusa, plodni prozor ~5 dana pre do 1 dan posle ovulacije).
         * Potrebna su bar 2 zabeležena ciklusa da bi procena bila moguća.
         */
        fun predict(entries: List<CycleEntry>): CyclePrediction {
            val starts = entries.map { it.startDateMillis }.sorted()
            if (starts.size < 2) {
                return CyclePrediction(null, null, null, null)
            }
            val diffsDays = starts.zipWithNext { a, b -> (b - a) / DAY_MS }
            val lastDiffs = diffsDays.takeLast(6)
            val avgDays = (lastDiffs.sum().toDouble() / lastDiffs.size).toInt().coerceAtLeast(21)
            val lastStart = starts.last()
            val nextStart = lastStart + avgDays * DAY_MS
            val ovulation = nextStart - 14 * DAY_MS
            val fertileStart = ovulation - 5 * DAY_MS
            val fertileEnd = ovulation + 1 * DAY_MS
            return CyclePrediction(avgDays, nextStart, fertileStart, fertileEnd)
        }
    }
}
