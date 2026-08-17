package com.pixelquest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey
    val id: Long = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: LocalDate? = null,
    val perfectDaysCount: Int = 0
)
