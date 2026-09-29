package com.pixelquest.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 23 (Day 23): Verification that "Follow System" mode strictly never resolves
 * to Comic mode.
 *
 * Per Day 16's original design and Day 23 specification:
 * - System mode in dark -> ThemeMode.Pixel
 * - System mode in light -> ThemeMode.Light
 * - System mode NEVER resolves to ThemeMode.Comic under any condition
 * - Comic mode is strictly an explicit user choice
 */
class Day23FollowSystemResolutionTest {

    @Test
    fun followSystem_neverResolvesToComic_whenDark() {
        val resolved = ThemeMode.System.resolveEffective(isSystemInDark = true)
        assertEquals(ThemeMode.Pixel, resolved)
        assertNotEquals(ThemeMode.Comic, resolved)
    }

    @Test
    fun followSystem_neverResolvesToComic_whenLight() {
        val resolved = ThemeMode.System.resolveEffective(isSystemInDark = false)
        assertEquals(ThemeMode.Light, resolved)
        assertNotEquals(ThemeMode.Comic, resolved)
    }

    @Test
    fun exhaustiveSystemDarkStates_neverProduceComicMode() {
        val booleanStates = listOf(true, false)
        for (isDark in booleanStates) {
            val resolved = ThemeMode.System.resolveEffective(isDark)
            assertTrue("System mode must never resolve to Comic", resolved != ThemeMode.Comic)
            assertTrue(
                "System mode must only resolve to Pixel or Light, got $resolved",
                resolved == ThemeMode.Pixel || resolved == ThemeMode.Light
            )
        }
    }

    @Test
    fun comicMode_isOnlyEverResolved_byExplicitComicSelection() {
        // Only ThemeMode.Comic resolves to Comic
        for (mode in ThemeMode.values()) {
            for (isDark in listOf(true, false)) {
                val resolved = mode.resolveEffective(isDark)
                if (mode == ThemeMode.Comic) {
                    assertEquals(ThemeMode.Comic, resolved)
                } else {
                    assertNotEquals(
                        "Mode $mode must never resolve to Comic (isDark=$isDark)",
                        ThemeMode.Comic,
                        resolved
                    )
                }
            }
        }
    }

    @Test
    fun followSystem_rawColorScheme_neverUsesComicTokens() {
        fun getRawScheme(mode: ThemeMode, isDark: Boolean): AppColorScheme {
            val effective = mode.resolveEffective(isDark)
            return when (effective) {
                ThemeMode.Pixel, ThemeMode.System -> DefaultPixelColorScheme
                ThemeMode.Light -> DefaultLightColorScheme
                ThemeMode.Comic -> DefaultComicColorScheme
            }
        }

        val darkScheme = getRawScheme(ThemeMode.System, isDark = true)
        val lightScheme = getRawScheme(ThemeMode.System, isDark = false)

        assertSame(DefaultPixelColorScheme, darkScheme)
        assertSame(DefaultLightColorScheme, lightScheme)

        assertNotEquals(DefaultComicColorScheme.background, darkScheme.background)
        assertNotEquals(DefaultComicColorScheme.background, lightScheme.background)
        assertNotEquals(DefaultComicColorScheme.themeMode, darkScheme.themeMode)
        assertNotEquals(DefaultComicColorScheme.themeMode, lightScheme.themeMode)
    }
}
