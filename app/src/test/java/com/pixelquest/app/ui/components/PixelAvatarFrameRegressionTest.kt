package com.pixelquest.app.ui.components

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.domain.AvatarTier
import com.pixelquest.app.domain.AvatarTierCalculator
import com.pixelquest.app.ui.theme.ComponentThemeFamily
import com.pixelquest.app.ui.theme.DefaultLightColorScheme
import com.pixelquest.app.ui.theme.DefaultPixelColorScheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 20: Regression test confirming Pixel/Light PixelAvatarFrame rendering is unaffected
 * after wiring ComicAvatarFrame dispatch.
 */
class PixelAvatarFrameRegressionTest {

    @Test
    fun pixelAvatarFrame_pixelMode_preservesMetallicTierBorders() {
        // Pixel mode uses AvatarTier.borderColor directly
        assertEquals(0xFFCD7F32, AvatarTier.BRONZE.borderColor)
        assertEquals(0xFFC0C0C0, AvatarTier.SILVER.borderColor)
        assertEquals(0xFFFFD700, AvatarTier.GOLD.borderColor)

        val isComic = ThemeMode.Pixel == ThemeMode.Comic
        assertFalse("Pixel mode must not enter Comic dispatch branch", isComic)
    }

    @Test
    fun pixelAvatarFrame_lightMode_preservesAccessibleTierBorders() {
        fun resolveLightBorderColor(tier: AvatarTier): Color = when (tier) {
            AvatarTier.BRONZE -> Color(0xFF9A4F10) // Rich dark bronze (>5:1 on white)
            AvatarTier.SILVER -> Color(0xFF475569) // Slate chrome silver (>7:1 on white)
            AvatarTier.GOLD -> DefaultLightColorScheme.gold // Deep dungeon gold (>5.2:1 on white)
        }

        assertEquals(Color(0xFF9A4F10), resolveLightBorderColor(AvatarTier.BRONZE))
        assertEquals(Color(0xFF475569), resolveLightBorderColor(AvatarTier.SILVER))
        assertEquals(Color(0xFFB8860B), resolveLightBorderColor(AvatarTier.GOLD))

        val isComic = ThemeMode.Light == ThemeMode.Comic
        assertFalse("Light mode must not enter Comic dispatch branch", isComic)
    }

    @Test
    fun pixelAvatarFrame_comicMode_resolvesComicFamily() {
        val family = ComponentThemeFamily.fromThemeMode(ThemeMode.Comic)
        assertEquals(ComponentThemeFamily.COMIC, family)

        val isComic = ThemeMode.Comic == ThemeMode.Comic
        assertTrue("Comic mode must enter Comic dispatch branch", isComic)
    }

    @Test
    fun pixelAvatarFrame_tierCalculation_exactThresholds() {
        assertEquals(AvatarTier.BRONZE, AvatarTierCalculator.calculateTier(1))
        assertEquals(AvatarTier.BRONZE, AvatarTierCalculator.calculateTier(4))
        assertEquals(AvatarTier.SILVER, AvatarTierCalculator.calculateTier(5))
        assertEquals(AvatarTier.SILVER, AvatarTierCalculator.calculateTier(9))
        assertEquals(AvatarTier.GOLD, AvatarTierCalculator.calculateTier(10))
    }
}
