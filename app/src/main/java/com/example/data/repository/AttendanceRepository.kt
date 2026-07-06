package com.example.data.repository

import com.example.data.local.ScanRecordDao
import com.example.data.local.UserDao
import com.example.data.model.ScanRecord
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AttendanceRepository(
    private val userDao: UserDao,
    private val scanRecordDao: ScanRecordDao
) {
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allScanRecords: Flow<List<ScanRecord>> = scanRecordDao.getAllScanRecords()

    suspend fun seedDefaultUsersIfEmpty() {
        // No default users seeded to keep a blank slate as requested by the user.
    }

    suspend fun authenticate(username: String, password: String): User? {
        val user = userDao.getUserByUsername(username)
        return if (user != null && user.password == password) {
            user
        } else {
            null
        }
    }

    suspend fun insertUser(user: User) = userDao.insertUser(user)

    suspend fun deleteUser(username: String) = userDao.deleteUserByUsername(username)

    suspend fun insertScanRecord(record: ScanRecord) = scanRecordDao.insertScanRecord(record)

    suspend fun getUnsyncedRecords(): List<ScanRecord> = scanRecordDao.getUnsyncedScanRecords()

    suspend fun syncRecords() {
        val unsynced = getUnsyncedRecords()
        if (unsynced.isNotEmpty()) {
            // Simulate API call to upload to server
            val ids = unsynced.map { it.id }
            scanRecordDao.markAsSynced(ids)
        }
    }

    suspend fun clearScanRecords() = scanRecordDao.clearAllScanRecords()
}
