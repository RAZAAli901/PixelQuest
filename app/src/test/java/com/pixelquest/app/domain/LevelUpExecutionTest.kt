package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class LevelUpExecutionTest {

    @Test
    fun testLevelUpStateTransition() {
        val initialProfile = UserProfileEntity(
            username = "PixelHero",
            avatarId = "hero_1",
            level = 1,
            totalXp = 500,
            perfectDaysTowardNextLevel = 7
        )

        val newLevel = initialProfile.level + 1
        val updatedProfile = initialProfile.copy(
            level = newLevel,
            perfectDaysTowardNextLevel = LevelCalculator.getPostLevelUpProgress()
        )

        assertEquals(2, updatedProfile.level)
        assertEquals(0, updatedProfile.perfectDaysTowardNextLevel)
        assertEquals(500, updatedProfile.totalXp)

        val historyRecord = LevelHistoryEntity(
            level = newLevel,
            achievedDate = 1000L,
            difficultyAtTimeOfLevelUp = "MEDIUM"
        )
        assertEquals(2, historyRecord.level)
        assertEquals("MEDIUM", historyRecord.difficultyAtTimeOfLevelUp)
    }
}
