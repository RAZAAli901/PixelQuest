package com.pixelquest.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarTierCalculatorTest {

    @Test
    fun calculateTier_levels1To4_returnsBronze() {
        assertEquals(AvatarTier.BRONZE, AvatarTierCalculator.calculateTier(1))
        assertEquals(AvatarTier.BRONZE, AvatarTierCalculator.calculateTier(2))
        assertEquals(AvatarTier.BRONZE, AvatarTierCalculator.calculateTier(3))
        assertEquals(AvatarTier.BRONZE, AvatarTierCalculator.calculateTier(4))
    }

    @Test
    fun calculateTier_levels5To9_returnsSilver() {
        assertEquals(AvatarTier.SILVER, AvatarTierCalculator.calculateTier(5))
        assertEquals(AvatarTier.SILVER, AvatarTierCalculator.calculateTier(7))
        assertEquals(AvatarTier.SILVER, AvatarTierCalculator.calculateTier(9))
    }

    @Test
    fun calculateTier_levels10AndAbove_returnsGold() {
        assertEquals(AvatarTier.GOLD, AvatarTierCalculator.calculateTier(10))
        assertEquals(AvatarTier.GOLD, AvatarTierCalculator.calculateTier(15))
        assertEquals(AvatarTier.GOLD, AvatarTierCalculator.calculateTier(100))
    }
}
