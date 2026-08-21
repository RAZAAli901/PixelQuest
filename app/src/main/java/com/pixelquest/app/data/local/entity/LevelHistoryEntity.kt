package com.pixelquest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_history")
data class LevelHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val level: Int,
    val achievedDate: Long = System.currentTimeMillis(),
    val difficultyAtTimeOfLevelUp: String
)
