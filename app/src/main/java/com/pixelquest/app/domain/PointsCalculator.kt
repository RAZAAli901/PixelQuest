package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.TaskEntity

object PointsCalculator {

    const val BASE_XP_PER_TASK = 50

    fun calculateXpForTask(task: TaskEntity? = null, currentStreak: Int = 0): Int {
        val basePoints = BASE_XP_PER_TASK

        // TODO (Day 5): Multiply basePoints by streak multiplier logic based on currentStreak and DifficultySettings.
        val streakBonus = 0

        return basePoints + streakBonus
    }
}
