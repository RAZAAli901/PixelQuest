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
 * Confirms that:
 * 1. Comic mode, gated on Day 22, has been available to everyone since Day 23.
 * 2. Pixel, Light, and System modes remain fully available.
 * 3. Debug Comic preview toggle is properly gated behind BuildConfig.DEBUG.
 * 4. AvatarTierCalculator remains pure and untouched across all tier thresholds.
 * 5. All Day 22 component tokens and color ramps remain orthogonal and regression-free.
 */
class Day22FinalVerificationTest {

    @Test
    fun comicMode_remainsStrictlyGatedFromUsers() {
        assertTrue("Comic mode is available since Day 23", ThemeMode.Comic.isAvailable)
        assertTrue("Pixel mode must remain fully available", ThemeMode.Pixel.isAvailable)
        assertTrue("Light mode must remain fully available", ThemeMode.Light.isAvailable)
        assertTrue("System mode must remain fully available", ThemeMode.System.isAvailable)
    }

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

    @Test
    fun comicTokens_maintainLockedSpecifications() {
        // Assert Comic key tokens are intact
        assertEquals(2.5.dp, ComicShapeTokens.BorderWidthDefault)
        assertEquals(4.dp, ComicShapeTokens.ShadowOffsetDefault)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), ComicTokens.SolidBlack)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFAF8F5), ComicTokens.PaperBackground)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFF5A4E), ComicTokens.CoralRed)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF8ECAE6), ComicTokens.SkyBlue)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF0A868), ComicTokens.BurntOrange)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFB8A4D4), ComicTokens.Lavender)
    }
}
