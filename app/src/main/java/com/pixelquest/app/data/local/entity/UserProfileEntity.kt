package com.pixelquest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Long = 1,
    val username: String,
    val avatarId: String,
    val level: Int = 1,
    val totalXp: Int = 0,
    val perfectDaysTowardNextLevel: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
