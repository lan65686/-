package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val password: String,
    val name: String,
    val role: String, // "EMPLOYEE", "SUPERVISOR", "ADMIN"
    val shift: String, // "صباحي", "مسائي", "كل الشفتات"
    val deviceBound: String? = null,
    val avatarColor: Int,
    val avatarUri: String? = null
)
