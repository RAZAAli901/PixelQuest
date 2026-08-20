package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.TaskEntity

object PointsCalculator {

    const val BASE_XP_PER_TASK = 50
    const val BONUS_XP_PER_STREAK_DAY = 10

    /**
     * Calculates XP awarded for completing a task.
     * Streak bonus formula: streakBonus = currentStreak * BONUS_XP_PER_STREAK_DAY.
     * Each active streak day adds +10 XP bonus to the base 50 XP.
     */
    fun calculateXpForTask(task: TaskEntity? = null, currentStreak: Int = 0): Int {
        val basePoints = BASE_XP_PER_TASK
        val streakBonus = currentStreak.coerceAtLeast(0) * BONUS_XP_PER_STREAK_DAY
        return basePoints + streakBonus
    }
}
