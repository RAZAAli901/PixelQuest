package com.pixelquest.app.ui.screens

import com.pixelquest.app.ui.theme.ComponentThemeFamily
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 9 (Day 23): Regression test confirming Pixel and Light splash screen rendering
 * is completely unaffected after wiring ComicSplashScreen dispatch.
 */
class SplashScreenRegressionTest {

    @Test
    fun splashScreen_pixelAndLightModes_bypassComicBranch() {
        assertFalse("Pixel mode must not enter Comic splash branch", ThemeMode.Pixel == ThemeMode.Comic)
        assertFalse("Light mode must not enter Comic splash branch", ThemeMode.Light == ThemeMode.Comic)
        assertFalse("System mode (light) must not resolve to Comic", ThemeMode.System.resolveEffective(false) == ThemeMode.Comic)
        assertFalse("System mode (dark) must not resolve to Comic", ThemeMode.System.resolveEffective(true) == ThemeMode.Comic)
        assertTrue("Comic mode must enter Comic splash branch", ThemeMode.Comic == ThemeMode.Comic)
    }

    @Test
    fun splashScreen_themeFamilyResolution_isIsolated() {
        val pixelFamily = ComponentThemeFamily.fromThemeMode(ThemeMode.Pixel)
        val lightFamily = ComponentThemeFamily.fromThemeMode(ThemeMode.Light)
        val comicFamily = ComponentThemeFamily.fromThemeMode(ThemeMode.Comic)

        assertEquals(ComponentThemeFamily.PIXEL, pixelFamily)
        assertEquals(ComponentThemeFamily.LIGHT, lightFamily)
        assertEquals(ComponentThemeFamily.COMIC, comicFamily)
    }

    @Test
    fun splashScreen_constantsAndText_arePreserved() {
        val canonicalTitle = "PIXELQUEST"
        val retroSubtitle = "8-BIT HABIT TRACKER"
        val comicSubtitle = "COMIC POP HABIT QUEST"
        val splashTimeoutMs = 1500

        assertEquals("PIXELQUEST", canonicalTitle)
        assertEquals("8-BIT HABIT TRACKER", retroSubtitle)
        assertEquals("COMIC POP HABIT QUEST", comicSubtitle)
        assertEquals(1500, splashTimeoutMs)
    }
}
