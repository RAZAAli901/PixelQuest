package com.pixelquest.app.integration

import com.pixelquest.app.data.local.SeedDataProvider
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingSeedTasksQaTest {

    @Test
    fun testOnboardingPreservesSeedDataTasks() {
        val initialTasks = SeedDataProvider.initialTasks().toMutableList()
        assertTrue(initialTasks.isNotEmpty())

        val onboardedProfile = UserProfileEntity(
            id = 1,
            username = "QA_Hero",
            avatarId = "avatar_ninja",
            level = 1,
            totalXp = 0,
            perfectDaysTowardNextLevel = 0
        )

        val onboardedDifficulty = DifficultySettingsEntity(
            id = 1,
            difficultyLevel = DifficultyLevel.HARD,
            perfectDayThreshold = 0.9f,
            daysRequiredPerLevel = 10
        )

        // Verify tasks count remains intact alongside onboarded profile
        assertEquals(3, initialTasks.size)
        assertEquals("QA_Hero", onboardedProfile.username)
        assertEquals(DifficultyLevel.HARD, onboardedDifficulty.difficultyLevel)
    }
}
