package com.pixelquest.app.domain

object LevelCalculator {
    /**
     * Determines whether a level-up should trigger based on current perfect days count
     * toward the next level and the days required per level for the active difficulty.
     */
    fun shouldLevelUp(perfectDaysTowardNextLevel: Int, daysRequiredPerLevel: Int): Boolean {
        if (daysRequiredPerLevel <= 0) return false
        return perfectDaysTowardNextLevel >= daysRequiredPerLevel
    }
}
