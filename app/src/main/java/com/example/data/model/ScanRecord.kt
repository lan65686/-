package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: String,
    val employeeName: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val isSynced: Boolean,
    val type: String // "CHECK_IN", "CHECK_OUT"
)
