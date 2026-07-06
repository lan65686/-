package com.example.data.local

import androidx.room.*
import com.example.data.model.ScanRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRecordDao {
    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    fun getAllScanRecords(): Flow<List<ScanRecord>>

    @Query("SELECT * FROM scan_records WHERE isSynced = 0")
    suspend fun getUnsyncedScanRecords(): List<ScanRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanRecord(record: ScanRecord)

    @Update
    suspend fun updateScanRecord(record: ScanRecord)

    @Query("UPDATE scan_records SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Int>)

    @Query("DELETE FROM scan_records")
    suspend fun clearAllScanRecords()
}
