package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelUpManualQaSimulationTest {

    @Test
    fun simulateReachingPerfectDayThresholdAndSignal() {
        val initialProfile = UserProfileEntity(username = "Hero", avatarId = "hero_1", level = 1, perfectDaysTowardNextLevel = 6)
        val daysRequired = 7

        val newProgress = initialProfile.perfectDaysTowardNextLevel + 1
        assertTrue(LevelCalculator.shouldLevelUp(newProgress, daysRequired))

        val newLevel = initialProfile.level + 1
        val updatedProfile = initialProfile.copy(
            level = newLevel,
            perfectDaysTowardNextLevel = LevelCalculator.getPostLevelUpProgress()
        )

        val historyEntry = LevelHistoryEntity(level = newLevel, achievedDate = System.currentTimeMillis(), difficultyAtTimeOfLevelUp = "MEDIUM")

        assertEquals(2, updatedProfile.level)
        assertEquals(0, updatedProfile.perfectDaysTowardNextLevel)
        assertEquals(2, historyEntry.level)
    }
}
