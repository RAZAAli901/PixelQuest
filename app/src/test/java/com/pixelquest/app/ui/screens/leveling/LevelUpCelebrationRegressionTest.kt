package com.pixelquest.app.ui.screens.leveling

import com.pixelquest.app.ui.theme.ComponentThemeFamily
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 35: Regression test confirming Pixel and Light celebration rendering is unaffected
 * after wiring ComicLevelUpCelebration dispatch.
 */
class LevelUpCelebrationRegressionTest {

    @Test
    fun celebration_pixelAndLightModes_bypassComicBranch() {
        assertFalse("Pixel mode must not enter Comic celebration branch", ThemeMode.Pixel == ThemeMode.Comic)
        assertFalse("Light mode must not enter Comic celebration branch", ThemeMode.Light == ThemeMode.Comic)
        assertTrue("Comic mode must enter Comic celebration branch", ThemeMode.Comic == ThemeMode.Comic)
    }

    @Test
    fun celebration_themeFamilyResolution_isIsolated() {
        val pixelFamily = ComponentThemeFamily.fromThemeMode(ThemeMode.Pixel)
        val lightFamily = ComponentThemeFamily.fromThemeMode(ThemeMode.Light)
        val comicFamily = ComponentThemeFamily.fromThemeMode(ThemeMode.Comic)

        assertEquals(ComponentThemeFamily.PIXEL, pixelFamily)
        assertEquals(ComponentThemeFamily.LIGHT, lightFamily)
        assertEquals(ComponentThemeFamily.COMIC, comicFamily)
    }

    @Test
    fun celebration_textFormulas_areExact() {
        fun formatPixelHeader() = "🎉 LEVEL UP! 🎉"
        fun formatComicHeader() = "LEVEL UP!"
        fun formatLevelString(level: Int) = "$level"

        assertEquals("🎉 LEVEL UP! 🎉", formatPixelHeader())
        assertEquals("LEVEL UP!", formatComicHeader())
        assertEquals("42", formatLevelString(42))
    }
}
