package com.medicocare.app.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.medicocare.app.data.AppDatabase
import com.medicocare.app.data.LabDocument
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Registar slikanih dokumenata (npr. laboratorijske analize) — hronološka evidencija
 * koja se čuva lokalno, u internom skladištu aplikacije.
 */
class DocumentRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).labDocumentDao()

    fun observeAll(): Flow<List<LabDocument>> = dao.observeAll()

    suspend fun save(document: LabDocument): Long {
        return if (document.id == 0L) {
            dao.insert(document)
        } else {
            dao.update(document)
            document.id
        }
    }

    suspend fun delete(document: LabDocument) {
        dao.delete(document)
        runCatching { File(document.filePath).delete() }
    }

    private fun documentsDir(): File {
        val dir = File(context.filesDir, "documents")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Novi prazan fajl u internom skladištu, spreman da ga popuni kamera/galerija. */
    fun newDocumentFile(): File = File(documentsDir(), "doc_${System.currentTimeMillis()}.jpg")

    fun uriForFile(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
