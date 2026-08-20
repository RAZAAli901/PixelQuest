package com.pixelquest.app.domain

import com.pixelquest.app.domain.model.DifficultyLevel

object DifficultyMode {

    fun getPerfectDayThreshold(level: DifficultyLevel): Float {
        return when (level) {
            DifficultyLevel.EASY -> 0.50f
            DifficultyLevel.MEDIUM -> 0.70f
            DifficultyLevel.HARD -> 0.90f
            DifficultyLevel.HARDEST -> 1.00f
        }
    }

    fun getDaysRequiredPerLevel(level: DifficultyLevel): Int {
        return when (level) {
            DifficultyLevel.EASY -> 5
            DifficultyLevel.MEDIUM -> 7
            DifficultyLevel.HARD -> 10
            DifficultyLevel.HARDEST -> 14
        }
    }

    fun getDisplayName(level: DifficultyLevel): String {
        return when (level) {
            DifficultyLevel.EASY -> "Easy"
            DifficultyLevel.MEDIUM -> "Medium"
            DifficultyLevel.HARD -> "Hard"
            DifficultyLevel.HARDEST -> "Hardest"
        }
    }

    fun getDescription(level: DifficultyLevel): String {
        return when (level) {
            DifficultyLevel.EASY -> "50% quests completed for a perfect day"
            DifficultyLevel.MEDIUM -> "70% quests completed for a perfect day"
            DifficultyLevel.HARD -> "90% quests completed for a perfect day"
            DifficultyLevel.HARDEST -> "100% quests completed for a perfect day"
        }
    }
}
