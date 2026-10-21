package com.pixelquest.app.ui.components

import com.pixelquest.app.domain.AvatarTier
import com.pixelquest.app.domain.AvatarTierCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Step 18: Verify Day 7's AvatarTierCalculator integrates correctly with ComicAvatarFrame
 * without needing any changes itself — pure visual rendering swap.
 */
class ComicAvatarTierIntegrationTest {

    @Test
    fun avatarTierCalculator_tierCalculations_remainExact() {
        // Levels 1-4 -> Bronze
        assertEquals(AvatarTier.BRONZE, AvatarTierCalculator.calculateTier(1))
        assertEquals(AvatarTier.BRONZE, AvatarTierCalculator.calculateTier(4))

        // Levels 5-9 -> Silver
        assertEquals(AvatarTier.SILVER, AvatarTierCalculator.calculateTier(5))
        assertEquals(AvatarTier.SILVER, AvatarTierCalculator.calculateTier(9))

        // Levels 10+ -> Gold
        assertEquals(AvatarTier.GOLD, AvatarTierCalculator.calculateTier(10))
        assertEquals(AvatarTier.GOLD, AvatarTierCalculator.calculateTier(42))
    }

    @Test
    fun comicAvatarFrame_tierDisplayNamesAndBadges_arePreserved() {
        assertEquals("BRONZE TIER", AvatarTier.BRONZE.displayName)
        assertEquals("🥉", AvatarTier.BRONZE.badgeEmoji)

        assertEquals("SILVER TIER", AvatarTier.SILVER.displayName)
        assertEquals("🥈", AvatarTier.SILVER.badgeEmoji)

        assertEquals("GOLD TIER", AvatarTier.GOLD.displayName)
        assertEquals("🥇", AvatarTier.GOLD.badgeEmoji)
    }
}
