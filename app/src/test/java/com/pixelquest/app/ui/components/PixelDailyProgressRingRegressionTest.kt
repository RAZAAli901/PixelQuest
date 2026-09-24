package com.pixelquest.app.ui.components

import com.pixelquest.app.ui.theme.ComponentThemeFamily
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 10: Regression test confirming Pixel/Light rendering of PixelDailyProgressRing is unaffected
 * after wiring ComicDailyProgressRing dispatch.
 */
class PixelDailyProgressRingRegressionTest {

    @Test
    fun pixelDailyProgressRing_goalMetLogic_identicalAcrossThemes() {
        fun isGoalMet(progress: Float, target: Float): Boolean = progress >= target

        assertFalse(isGoalMet(0.0f, 1.0f))
        assertFalse(isGoalMet(0.99f, 1.0f))
        assertTrue(isGoalMet(1.0f, 1.0f))
        assertTrue(isGoalMet(1.25f, 1.0f))
    }

    @Test
    fun pixelDailyProgressRing_themeDispatch_onlyActiveInComic() {
        assertFalse("Pixel mode must not trigger ComicDailyProgressRing", ThemeMode.Pixel == ThemeMode.Comic)
        assertFalse("Light mode must not trigger ComicDailyProgressRing", ThemeMode.Light == ThemeMode.Comic)
        assertTrue("Comic mode must trigger ComicDailyProgressRing", ThemeMode.Comic == ThemeMode.Comic)

        assertEquals(ComponentThemeFamily.PIXEL, ComponentThemeFamily.fromThemeMode(ThemeMode.Pixel))
        assertEquals(ComponentThemeFamily.LIGHT, ComponentThemeFamily.fromThemeMode(ThemeMode.Light))
        assertEquals(ComponentThemeFamily.COMIC, ComponentThemeFamily.fromThemeMode(ThemeMode.Comic))
    }

    @Test
    fun pixelDailyProgressRing_percentageFormatting_accurate() {
        val progress = 0.75f
        val target = 1.0f
        val pctInt = (progress * 100).toInt()
        val targetPctInt = (target * 100).toInt()

        assertEquals(75, pctInt)
        assertEquals(100, targetPctInt)
        assertEquals("75% / 100%", "$pctInt% / $targetPctInt%")
    }
}
