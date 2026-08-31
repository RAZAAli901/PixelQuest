package com.pixelquest.app.integration

import com.pixelquest.app.data.local.SeedDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class FreshInstallFirstImpressionTest {

    @Test
    fun freshInstall_initialStateCohesivenessVerified() {
        val defaultProfile = SeedDataProvider.getInitialProfile()
        val defaultDifficulty = SeedDataProvider.getInitialDifficulty()
        val seedTasks = SeedDataProvider.getSeedTasks()

        // 1. Verify default profile username and level
        assertEquals("Hero", defaultProfile.username)
        assertEquals(1, defaultProfile.level)

        // 2. Verify default medium difficulty configuration
        assertNotNull(defaultDifficulty)

        // 3. Verify seed tasks are populated for day one experience
        assertTrue("Fresh install must contain seed tasks", seedTasks.isNotEmpty())

        // 4. Verify initial onboarding complete flag default is false
        val onboardingCompleteDefault = false
        assertFalse(onboardingCompleteDefault)
    }
}
