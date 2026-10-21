package com.pixelquest.app.ui.theme

import com.pixelquest.app.BuildConfig
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.AvatarTier
import com.pixelquest.app.domain.AvatarTierCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 42: Day 22 Final Verification Test.
 * Confirms that AvatarTierCalculator stays pure across all its tier thresholds.
 */
class Day22FinalVerificationTest {

    @Test
    fun avatarTierCalculator_remainsPristineAndUntouched() {
        // Tiers follow the hero's level: Bronze below 5, Silver below 10, Gold from 10.
        assertEquals(AvatarTier.BRONZE, AvatarTierCalculator.calculateTier(1))
        assertEquals(AvatarTier.BRONZE, AvatarTierCalculator.calculateTier(4))
        assertEquals(AvatarTier.SILVER, AvatarTierCalculator.calculateTier(5))
        assertEquals(AvatarTier.SILVER, AvatarTierCalculator.calculateTier(9))
        assertEquals(AvatarTier.GOLD, AvatarTierCalculator.calculateTier(10))
        assertEquals(AvatarTier.GOLD, AvatarTierCalculator.calculateTier(30))
    }
}
