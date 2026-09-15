package com.pixelquest.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Step 24: Unit test for system-theme-change detection and resolution
 * while "Follow System" (ThemeMode.System) is active versus explicit overrides.
 */
class SystemThemeResolutionTest {

    @Test
    fun followSystem_resolvesToPixelWhenSystemInDark() {
        val resolved = ThemeMode.System.resolveEffective(isSystemInDark = true)
        assertEquals(ThemeMode.Pixel, resolved)
    }

    @Test
    fun followSystem_resolvesToLightWhenSystemNotInDark() {
        val resolved = ThemeMode.System.resolveEffective(isSystemInDark = false)
        assertEquals(ThemeMode.Light, resolved)
    }

    @Test
    fun explicitPixelTheme_ignoresSystemNightMode() {
        assertEquals(ThemeMode.Pixel, ThemeMode.Pixel.resolveEffective(isSystemInDark = true))
        assertEquals(ThemeMode.Pixel, ThemeMode.Pixel.resolveEffective(isSystemInDark = false))
    }

    @Test
    fun explicitLightTheme_ignoresSystemNightMode() {
        assertEquals(ThemeMode.Light, ThemeMode.Light.resolveEffective(isSystemInDark = true))
        assertEquals(ThemeMode.Light, ThemeMode.Light.resolveEffective(isSystemInDark = false))
    }

    @Test
    fun explicitComicTheme_ignoresSystemNightMode() {
        assertEquals(ThemeMode.Comic, ThemeMode.Comic.resolveEffective(isSystemInDark = true))
        assertEquals(ThemeMode.Comic, ThemeMode.Comic.resolveEffective(isSystemInDark = false))
    }

    @Test
    fun systemNightModeTransition_dynamicallySwitchesResolvedPalette() {
        var isSystemDark = false
        val activeMode = ThemeMode.System

        // Daytime: device in light mode
        val daytimeResolved = activeMode.resolveEffective(isSystemDark)
        assertEquals(ThemeMode.Light, daytimeResolved)

        // Sunset: device switches to dark mode
        isSystemDark = true
        val nighttimeResolved = activeMode.resolveEffective(isSystemDark)
        assertEquals(ThemeMode.Pixel, nighttimeResolved)

        assertNotEquals(daytimeResolved, nighttimeResolved)
    }

    @Test
    fun themeMode_fromId_handlesSystemCorrectly() {
        assertEquals(ThemeMode.System, ThemeMode.fromId("system"))
        assertEquals(ThemeMode.System, ThemeMode.fromId("SYSTEM"))
        assertEquals(ThemeMode.System, ThemeMode.fromId("  system  "))
        assertEquals(ThemeMode.Pixel, ThemeMode.fromId("pixel"))
        assertEquals(ThemeMode.Pixel, ThemeMode.fromId("  pixel\n"))
        assertEquals(ThemeMode.Light, ThemeMode.fromId("light"))
        assertEquals(ThemeMode.Comic, ThemeMode.fromId("comic"))
        assertEquals(ThemeMode.Pixel, ThemeMode.fromId("invalid_mode"))
        assertEquals(ThemeMode.Pixel, ThemeMode.fromId(null))
        assertEquals(ThemeMode.Pixel, ThemeMode.fromId(""))

    }
}
