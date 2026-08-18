package com.medicocare.app.report

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.medicocare.app.R
import com.medicocare.app.data.CycleEntry
import com.medicocare.app.data.IntakeLogView
import com.medicocare.app.data.IntakeStatus
import com.medicocare.app.data.LabDocument
import com.medicocare.app.data.VitalReading
import com.medicocare.app.data.VitalType
import com.medicocare.app.repository.CyclePrediction
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Generiše izveštaj istorije uzimanja lekova kao PDF ili CSV (za Excel), da bi se poneo
 * lekaru ili podelio sa nekim. Radi u potpunosti na uređaju — bez interneta i bez
 * dodatnih biblioteka (android.graphics.pdf.PdfDocument je ugrađen u Android). Svi tekstualni
 * naslovi/oznake se čitaju iz resursa (context.getString) da bi pratili trenutni jezik aplikacije.
 */
object ReportGenerator {

    private val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm")
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.")

    private fun formatDateLocal(millis: Long): String =
        DATE_FORMAT.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

    private fun formatDateUtc(millis: Long): String =
        DATE_FORMAT.format(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC))

    private fun formatNum(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    private fun statusLabel(context: Context, status: IntakeStatus): String = when (status) {
        IntakeStatus.UZETO -> context.getString(R.string.intake_status_taken)
        IntakeStatus.PRESKOCENO -> context.getString(R.string.intake_status_skipped)
        IntakeStatus.NA_CEKANJU -> context.getString(R.string.intake_status_pending)
    }

    private fun reportsDir(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun generateCsv(context: Context, history: List<IntakeLogView>): Uri {
        val file = File(reportsDir(context), "medicare_istorija.csv")
        FileOutputStream(file).use { out ->
            val sb = StringBuilder()
            sb.append(context.getString(R.string.report_csv_header_history)).append('\n')
            history.forEach { entry ->
                val dateTime = Instant.ofEpochMilli(entry.log.scheduledAtMillis)
                    .atZone(ZoneId.systemDefault())
                    .format(DATE_TIME_FORMAT)
                sb.append(csvEscape(dateTime)).append(',')
                sb.append(csvEscape(entry.medicationName)).append(',')
                sb.append(csvEscape(entry.log.doseLabel)).append(',')
                sb.append(csvEscape(statusLabel(context, entry.log.status))).append('\n')
            }
            // UTF-8 BOM da bi Excel ispravno prikazao dijakritike i druga slova.
            out.write(0xEF)
            out.write(0xBB)
            out.write(0xBF)
            out.write(sb.toString().toByteArray(Charsets.UTF_8))
        }
        return uriFor(context, file)
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    fun generatePdf(context: Context, history: List<IntakeLogView>): Uri {
        val file = File(reportsDir(context), "medicare_istorija.pdf")
        val document = PdfDocument()

        val pageWidth = 595 // A4 @ 72dpi
        val pageHeight = 842
        val marginLeft = 40f
        val marginTop = 50f
        val lineHeight = 20f

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = marginTop

        canvas.drawText(context.getString(R.string.report_med_history_title), marginLeft, y, titlePaint)
        y += lineHeight * 1.5f
        val generatedAt = DATE_TIME_FORMAT.format(java.time.ZonedDateTime.now())
        canvas.drawText(context.getString(R.string.report_generated_at, generatedAt), marginLeft, y, bodyPaint)
        y += lineHeight * 1.5f

        val colDate = marginLeft
        val colMed = marginLeft + 130f
        val colDose = marginLeft + 300f
        val colStatus = marginLeft + 420f

        val colDateHeader = context.getString(R.string.report_col_datetime)
        val colMedHeader = context.getString(R.string.report_col_med)
        val colDoseHeader = context.getString(R.string.report_col_dose)
        val colStatusHeader = context.getString(R.string.report_col_status)

        fun drawHeaderRow() {
            canvas.drawText(colDateHeader, colDate, y, headerPaint)
            canvas.drawText(colMedHeader, colMed, y, headerPaint)
            canvas.drawText(colDoseHeader, colDose, y, headerPaint)
            canvas.drawText(colStatusHeader, colStatus, y, headerPaint)
            y += lineHeight
        }

        drawHeaderRow()

        if (history.isEmpty()) {
            canvas.drawText(context.getString(R.string.report_no_data), marginLeft, y, bodyPaint)
        }

        history.forEach { entry ->
            if (y > pageHeight - marginTop) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = marginTop
                drawHeaderRow()
            }
            val dateTime = Instant.ofEpochMilli(entry.log.scheduledAtMillis)
                .atZone(ZoneId.systemDefault())
                .format(DATE_TIME_FORMAT)
            canvas.drawText(dateTime, colDate, y, bodyPaint)
            canvas.drawText(truncate(entry.medicationName, 22), colMed, y, bodyPaint)
            canvas.drawText(truncate(entry.log.doseLabel, 18), colDose, y, bodyPaint)
            canvas.drawText(statusLabel(context, entry.log.status), colStatus, y, bodyPaint)
            y += lineHeight
        }

        document.finishPage(page)

        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()

        return uriFor(context, file)
    }

    private fun truncate(text: String, maxChars: Int): String =
        if (text.length > maxChars) text.take(maxChars - 1) + "…" else text

    // ---------- Pritisak i šećer u krvi ----------

    private fun vitalTypeLabel(context: Context, type: VitalType): String =
        if (type == VitalType.PRITISAK) context.getString(R.string.vital_type_pressure_label) else context.getString(R.string.vital_type_sugar_label)

    private fun vitalValueLabel(context: Context, reading: VitalReading): String = when (reading.type) {
        VitalType.PRITISAK -> {
            val sys = formatNum(reading.valuePrimary)
            val dia = reading.valueSecondary?.let { formatNum(it) } ?: "?"
            val pulseText = reading.pulse?.let { " (${it})" } ?: ""
            "$sys/$dia ${reading.unit.ifBlank { "mmHg" }}$pulseText"
        }
        VitalType.SECER -> "${formatNum(reading.valuePrimary)} ${reading.unit.ifBlank { "mmol/L" }}"
    }

    fun generateVitalsCsv(context: Context, readings: List<VitalReading>): Uri {
        val file = File(reportsDir(context), "medicare_vitalni_znaci.csv")
        FileOutputStream(file).use { out ->
            val sb = StringBuilder()
            sb.append(context.getString(R.string.report_csv_header_vitals)).append('\n')
            readings.forEach { reading ->
                val dateTime = Instant.ofEpochMilli(reading.dateTimeMillis).atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMAT)
                sb.append(csvEscape(dateTime)).append(',')
                sb.append(csvEscape(vitalTypeLabel(context, reading.type))).append(',')
                sb.append(csvEscape(vitalValueLabel(context, reading))).append(',')
                sb.append(csvEscape(reading.notes)).append('\n')
            }
            out.write(0xEF); out.write(0xBB); out.write(0xBF)
            out.write(sb.toString().toByteArray(Charsets.UTF_8))
        }
        return uriFor(context, file)
    }

    fun generateVitalsPdf(context: Context, readings: List<VitalReading>): Uri {
        val file = File(reportsDir(context), "medicare_vitalni_znaci.pdf")
        val document = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        val marginLeft = 40f
        val marginTop = 50f
        val lineHeight = 20f

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = marginTop

        canvas.drawText(context.getString(R.string.report_vitals_title), marginLeft, y, titlePaint)
        y += lineHeight * 1.5f

        val colDate = marginLeft
        val colType = marginLeft + 150f
        val colValue = marginLeft + 250f
        val colNotes = marginLeft + 400f

        val colDateHeader = context.getString(R.string.report_col_datetime)
        val colTypeHeader = context.getString(R.string.report_col_type)
        val colValueHeader = context.getString(R.string.report_col_value)
        val colNotesHeader = context.getString(R.string.report_col_notes)

        fun drawHeaderRow() {
            canvas.drawText(colDateHeader, colDate, y, headerPaint)
            canvas.drawText(colTypeHeader, colType, y, headerPaint)
            canvas.drawText(colValueHeader, colValue, y, headerPaint)
            canvas.drawText(colNotesHeader, colNotes, y, headerPaint)
            y += lineHeight
        }

        drawHeaderRow()

        if (readings.isEmpty()) {
            canvas.drawText(context.getString(R.string.report_no_data), marginLeft, y, bodyPaint)
        }

        readings.forEach { reading ->
            if (y > pageHeight - marginTop) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = marginTop
                drawHeaderRow()
            }
            val dateTime = Instant.ofEpochMilli(reading.dateTimeMillis).atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMAT)
            canvas.drawText(dateTime, colDate, y, bodyPaint)
            canvas.drawText(vitalTypeLabel(context, reading.type), colType, y, bodyPaint)
            canvas.drawText(vitalValueLabel(context, reading), colValue, y, bodyPaint)
            canvas.drawText(truncate(reading.notes, 20), colNotes, y, bodyPaint)
            y += lineHeight
        }

        document.finishPage(page)
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return uriFor(context, file)
    }

    /** Preuzima CSV evidencije pritiska i šećera u javni folder Preuzimanja. Vraća true ako je uspelo. */
    fun downloadVitalsCsv(context: Context, readings: List<VitalReading>): Boolean {
        val file = File(reportsDir(context), "medicare_vitalni_znaci.csv")
        generateVitalsCsv(context, readings)
        return saveFileToDownloads(context, file, "text/csv")
    }

    /** Preuzima PDF evidencije pritiska i šećera u javni folder Preuzimanja. Vraća true ako je uspelo. */
    fun downloadVitalsPdf(context: Context, readings: List<VitalReading>): Boolean {
        val file = File(reportsDir(context), "medicare_vitalni_znaci.pdf")
        generateVitalsPdf(context, readings)
        return saveFileToDownloads(context, file, "application/pdf")
    }

    // ---------- Ciklus i plodni dani ----------

    fun generateCycleCsv(context: Context, entries: List<CycleEntry>, prediction: CyclePrediction): Uri {
        val file = File(reportsDir(context), "medicare_ciklus.csv")
        FileOutputStream(file).use { out ->
            val sb = StringBuilder()
            sb.append(context.getString(R.string.report_csv_header_cycle)).append('\n')
            entries.sortedByDescending { it.startDateMillis }.forEach { entry ->
                sb.append(csvEscape(formatDateUtc(entry.startDateMillis))).append(',')
                sb.append(csvEscape(entry.endDateMillis?.let { formatDateUtc(it) } ?: "")).append(',')
                sb.append(csvEscape(entry.notes)).append('\n')
            }
            sb.append('\n')
            sb.append(context.getString(R.string.report_cycle_csv_prognosis_header)).append('\n')
            if (prediction.nextPeriodStartMillis != null) {
                sb.append(context.getString(R.string.report_cycle_csv_next)).append(',')
                    .append(csvEscape(formatDateUtc(prediction.nextPeriodStartMillis))).append('\n')
                if (prediction.fertileWindowStartMillis != null && prediction.fertileWindowEndMillis != null) {
                    sb.append(context.getString(R.string.report_cycle_csv_fertile)).append(',')
                        .append(csvEscape("${formatDateUtc(prediction.fertileWindowStartMillis)} - ${formatDateUtc(prediction.fertileWindowEndMillis)}"))
                        .append('\n')
                }
            } else {
                sb.append(context.getString(R.string.report_cycle_csv_insufficient)).append('\n')
            }
            out.write(0xEF); out.write(0xBB); out.write(0xBF)
            out.write(sb.toString().toByteArray(Charsets.UTF_8))
        }
        return uriFor(context, file)
    }

    fun generateCyclePdf(context: Context, entries: List<CycleEntry>, prediction: CyclePrediction): Uri {
        val file = File(reportsDir(context), "medicare_ciklus.pdf")
        val document = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        val marginLeft = 40f
        val marginTop = 50f
        val lineHeight = 20f

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }
        val prognosisPaint = Paint().apply { textSize = 12f; isFakeBoldText = true; color = Color.parseColor("#6A1B9A") }
        val prognosisBgPaint = Paint().apply { color = Color.parseColor("#F3E5F5") }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = marginTop

        canvas.drawText(context.getString(R.string.report_cycle_title), marginLeft, y, titlePaint)
        y += lineHeight * 1.5f

        val colStart = marginLeft
        val colEnd = marginLeft + 150f
        val colNotes = marginLeft + 300f

        canvas.drawText(context.getString(R.string.report_col_start), colStart, y, headerPaint)
        canvas.drawText(context.getString(R.string.report_col_end), colEnd, y, headerPaint)
        canvas.drawText(context.getString(R.string.report_col_notes), colNotes, y, headerPaint)
        y += lineHeight

        val sorted = entries.sortedByDescending { it.startDateMillis }
        if (sorted.isEmpty()) {
            canvas.drawText(context.getString(R.string.report_cycle_no_entries), marginLeft, y, bodyPaint)
            y += lineHeight
        }
        sorted.forEach { entry ->
            if (y > pageHeight - marginTop - 120f) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = marginTop
            }
            canvas.drawText(formatDateUtc(entry.startDateMillis), colStart, y, bodyPaint)
            canvas.drawText(entry.endDateMillis?.let { formatDateUtc(it) } ?: "-", colEnd, y, bodyPaint)
            canvas.drawText(truncate(entry.notes, 28), colNotes, y, bodyPaint)
            y += lineHeight
        }

        y += lineHeight
        val boxTop = y - lineHeight + 4f
        val boxHeight = if (prediction.nextPeriodStartMillis != null) lineHeight * 3.5f else lineHeight * 1.5f
        canvas.drawRect(marginLeft - 8f, boxTop, pageWidth - marginLeft + 8f, boxTop + boxHeight, prognosisBgPaint)
        canvas.drawText(context.getString(R.string.report_cycle_prognosis_header), marginLeft, y, prognosisPaint)
        y += lineHeight
        if (prediction.nextPeriodStartMillis != null) {
            canvas.drawText(
                context.getString(R.string.report_cycle_prognosis_next, formatDateUtc(prediction.nextPeriodStartMillis)),
                marginLeft, y, prognosisPaint
            )
            y += lineHeight
            if (prediction.fertileWindowStartMillis != null && prediction.fertileWindowEndMillis != null) {
                canvas.drawText(
                    context.getString(
                        R.string.report_cycle_prognosis_fertile,
                        formatDateUtc(prediction.fertileWindowStartMillis),
                        formatDateUtc(prediction.fertileWindowEndMillis)
                    ),
                    marginLeft, y, prognosisPaint
                )
                y += lineHeight
            }
        } else {
            canvas.drawText(context.getString(R.string.report_cycle_prognosis_insufficient), marginLeft, y, prognosisPaint)
            y += lineHeight
        }

        document.finishPage(page)
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return uriFor(context, file)
    }

    /**
     * Snima već generisan fajl iz keša direktno u javni folder Preuzimanja uređaja — za razliku
     * od deljenja (ACTION_SEND ka drugoj aplikaciji), fajl ostaje trajno vidljiv korisniku u
     * "Files"/"Preuzimanja" i nakon zatvaranja aplikacije. Na API 29+ koristi MediaStore
     * (scoped storage, bez posebne dozvole); na starijim verzijama piše direktno uz
     * WRITE_EXTERNAL_STORAGE (traži se u UI pre poziva ove funkcije).
     */
    private fun saveFileToDownloads(context: Context, file: File, mimeType: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
                resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: return false
                true
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val target = File(downloadsDir, file.name)
                file.inputStream().use { input -> FileOutputStream(target).use { output -> input.copyTo(output) } }
                MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(mimeType), null)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /** Preuzima CSV izveštaj ciklusa i procene u javni folder Preuzimanja. Vraća true ako je uspelo. */
    fun downloadCycleCsv(context: Context, entries: List<CycleEntry>, prediction: CyclePrediction): Boolean {
        val file = File(reportsDir(context), "medicare_ciklus.csv")
        // Fajl je već napravljen (isti sadržaj) pozivom generateCycleCsv — ponovo ga generišemo
        // ovde da funkcija bude samostalna i ne zavisi od prethodnog poziva.
        generateCycleCsv(context, entries, prediction)
        return saveFileToDownloads(context, file, "text/csv")
    }

    /** Preuzima PDF izveštaj ciklusa i procene u javni folder Preuzimanja. Vraća true ako je uspelo. */
    fun downloadCyclePdf(context: Context, entries: List<CycleEntry>, prediction: CyclePrediction): Boolean {
        val file = File(reportsDir(context), "medicare_ciklus.pdf")
        generateCyclePdf(context, entries, prediction)
        return saveFileToDownloads(context, file, "application/pdf")
    }

    // ---------- Izveštaji i analize (dokumenti) ----------

    fun generateDocumentsPdf(context: Context, documents: List<LabDocument>): Uri {
        val file = File(reportsDir(context), "medicare_izvestaji.pdf")
        val document = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val lineHeight = 18f
        val maxImgWidth = pageWidth - margin * 2
        val maxImgHeight = 260f

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val entryTitlePaint = Paint().apply { textSize = 13f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin

        canvas.drawText(context.getString(R.string.report_documents_title), margin, y, titlePaint)
        y += lineHeight * 2

        if (documents.isEmpty()) {
            canvas.drawText(context.getString(R.string.report_documents_empty), margin, y, bodyPaint)
        }

        documents.sortedByDescending { it.dateMillis }.forEach { doc ->
            val bitmap = runCatching { BitmapFactory.decodeFile(doc.filePath) }.getOrNull()
            var scaledBitmap: Bitmap? = null
            var imgHeight = 0f
            if (bitmap != null) {
                val scale = minOf(maxImgWidth / bitmap.width, maxImgHeight / bitmap.height, 1f)
                val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, w, h, true)
                imgHeight = h.toFloat()
            }
            val neededHeight = lineHeight * 2 + imgHeight + (if (doc.notes.isNotBlank()) lineHeight else 0f) + 16f
            if (y + neededHeight > pageHeight - margin) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = margin
            }
            canvas.drawText("${doc.title} — ${formatDateLocal(doc.dateMillis)}", margin, y, entryTitlePaint)
            y += lineHeight
            scaledBitmap?.let {
                canvas.drawBitmap(it, margin, y, null)
                y += imgHeight + 6f
            }
            if (doc.notes.isNotBlank()) {
                canvas.drawText(truncate(doc.notes, 90), margin, y, bodyPaint)
                y += lineHeight
            }
            y += 16f
        }

        document.finishPage(page)
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return uriFor(context, file)
    }
}
