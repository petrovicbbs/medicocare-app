package com.medicocare.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BarcodeDao {

    @Query("SELECT * FROM barcode_entries WHERE barcode = :barcode")
    suspend fun getByBarcode(barcode: String): BarcodeEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: BarcodeEntry)
}
