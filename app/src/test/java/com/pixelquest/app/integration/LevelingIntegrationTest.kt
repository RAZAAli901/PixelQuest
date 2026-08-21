package com.pixelquest.app.integration

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelCalculator
import com.pixelquest.app.domain.model.DifficultyLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class LevelingIntegrationTest {

    @Test
    fun testEndToEndLevelUpAndHistoryFlow() {
        // Initial state: Level 1, 0 perfect days, Easy mode (3 days per level)
        var userProfile = UserProfileEntity(id = 1, level = 1, perfectDaysTowardNextLevel = 0)
        val difficulty = DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.EASY, daysRequiredPerLevel = 3)
        val levelHistoryLog = mutableListOf<LevelHistoryEntity>()

        // Day 1 Perfect Day
        userProfile = userProfile.copy(perfectDaysTowardNextLevel = userProfile.perfectDaysTowardNextLevel + 1)
        var shouldLevelUp = LevelCalculator.shouldLevelUp(userProfile.perfectDaysTowardNextLevel, difficulty.daysRequiredPerLevel)
        assertEquals(1, userProfile.perfectDaysTowardNextLevel)
        assertEquals(false, shouldLevelUp)

        // Day 2 Perfect Day
        userProfile = userProfile.copy(perfectDaysTowardNextLevel = userProfile.perfectDaysTowardNextLevel + 1)
        shouldLevelUp = LevelCalculator.shouldLevelUp(userProfile.perfectDaysTowardNextLevel, difficulty.daysRequiredPerLevel)
        assertEquals(2, userProfile.perfectDaysTowardNextLevel)
        assertEquals(false, shouldLevelUp)

        // Day 3 Perfect Day -> Reaches 3 days threshold!
        userProfile = userProfile.copy(perfectDaysTowardNextLevel = userProfile.perfectDaysTowardNextLevel + 1)
        shouldLevelUp = LevelCalculator.shouldLevelUp(userProfile.perfectDaysTowardNextLevel, difficulty.daysRequiredPerLevel)
        assertEquals(3, userProfile.perfectDaysTowardNextLevel)
        assertEquals(true, shouldLevelUp)

        // Execute Level Up
        if (shouldLevelUp) {
            val newLevel = userProfile.level + 1
            userProfile = userProfile.copy(
                level = newLevel,
                perfectDaysTowardNextLevel = LevelCalculator.getPostLevelUpProgress()
            )
            levelHistoryLog.add(
                LevelHistoryEntity(
                    id = 1,
                    level = newLevel,
                    achievedDate = System.currentTimeMillis(),
                    difficultyAtTimeOfLevelUp = difficulty.difficultyLevel.name
                )
            )
        }

        // Assert post-level up state
        assertEquals(2, userProfile.level)
        assertEquals(0, userProfile.perfectDaysTowardNextLevel)
        assertEquals(1, levelHistoryLog.size)
        assertEquals(2, levelHistoryLog[0].level)
        assertEquals("EASY", levelHistoryLog[0].difficultyAtTimeOfLevelUp)
    }
}
