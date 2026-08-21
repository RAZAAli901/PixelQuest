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

    /**
     * MID-PROGRESS DIFFICULTY-SWITCH BEHAVIOR:
     * When switching difficulty mid-level, perfectDaysTowardNextLevel carries over strictly as a raw count.
     * The target threshold immediately updates to the new difficulty's daysRequiredPerLevel.
     * If current count already equals or exceeds the new target (e.g. 5 days when switching from Medium(7) to Easy(3)),
     * the next daily evaluation immediately triggers a level-up, maintaining continuity without progress erasure.
     */
    fun evaluateLevelProgressOnDifficultySwitch(
        currentProgress: Int,
        newDaysRequired: Int
    ): Boolean {
        return shouldLevelUp(currentProgress, newDaysRequired)
    }
}
