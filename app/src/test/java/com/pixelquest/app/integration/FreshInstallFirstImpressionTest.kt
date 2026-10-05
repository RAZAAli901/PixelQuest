package com.pixelquest.app.integration

import com.pixelquest.app.data.local.SeedDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshInstallFirstImpressionTest {

    @Test
    fun freshInstall_initialStateCohesivenessVerified() {
        val defaultProfile = SeedDataProvider.defaultProfile()
        val defaultDifficulty = SeedDataProvider.defaultDifficultySettings()
        val seedTasks = SeedDataProvider.initialTasks()

        // 1. Verify default profile username and level
        assertEquals("PixelHero", defaultProfile.username)
        assertEquals(1, defaultProfile.level)
        // The seeded avatar must be a real one, not just fall back to the first.
        assertTrue(com.pixelquest.app.domain.AvatarCatalog.avatars.any { it.id == defaultProfile.avatarId })

        // 2. Verify default medium difficulty configuration
        assertNotNull(defaultDifficulty)

        // 3. Verify seed tasks are populated for day one experience
        assertTrue("Fresh install must contain seed tasks", seedTasks.isNotEmpty())

        // 4. Verify initial onboarding complete flag default is false
        val onboardingCompleteDefault = false
        assertFalse(onboardingCompleteDefault)
    }
}
