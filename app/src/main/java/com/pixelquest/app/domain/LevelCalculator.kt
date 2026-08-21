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

    /**
     * POST-LEVEL-UP RESET BEHAVIOR:
     * When a level-up triggers, perfectDaysTowardNextLevel resets strictly to 0.
     * There is no partial carryover or fractional rollover to the next level tier,
     * maintaining consistency with Day 5's "no fractional state" design rule.
     */
    fun getPostLevelUpProgress(): Int = 0
}
