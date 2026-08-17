package com.pixelquest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pixelquest.app.domain.model.DifficultyLevel

@Entity(tableName = "difficulty_settings")
data class DifficultySettingsEntity(
    @PrimaryKey
    val id: Long = 1,
    val difficultyLevel: DifficultyLevel = DifficultyLevel.MEDIUM,
    val perfectDayThreshold: Float = 0.8f,
    val daysRequiredPerLevel: Int = 7
)
