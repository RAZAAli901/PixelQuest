package com.pixelquest.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 36: Test verifying "Follow System" dynamically switches effective theme
 * based on OS dark/light setting and maintains strict CRT decoupling.
 */
class FollowSystemThemeSwitchingTest {

    @Test
    fun testFollowSystem_resolvesToLightWhenOsIsLight() {
        val effective = ThemeMode.System.resolveEffective(isSystemInDarkTheme = false)
        assertEquals(ThemeMode.Light, effective)
    }

    @Test
    fun testFollowSystem_resolvesToPixelWhenOsIsDark() {
        val effective = ThemeMode.System.resolveEffective(isSystemInDarkTheme = true)
        assertEquals(ThemeMode.Pixel, effective)
    }

    @Test
    fun testExplicitThemeSelection_overridesOsState() {
        // Light mode explicitly selected remains Light regardless of OS state
        assertEquals(ThemeMode.Light, ThemeMode.Light.resolveEffective(isSystemInDarkTheme = true))
        assertEquals(ThemeMode.Light, ThemeMode.Light.resolveEffective(isSystemInDarkTheme = false))

        // Pixel mode explicitly selected remains Pixel regardless of OS state
        assertEquals(ThemeMode.Pixel, ThemeMode.Pixel.resolveEffective(isSystemInDarkTheme = true))
        assertEquals(ThemeMode.Pixel, ThemeMode.Pixel.resolveEffective(isSystemInDarkTheme = false))
    }

    @Test
    fun testCrtFilter_deactivatesInLightModeEvenWhenFollowSystemActive() {
        val isCrtEnabled = true

        // OS in Light mode -> CRT must NOT apply
        val effectiveLight = ThemeMode.System.resolveEffective(isSystemInDarkTheme = false)
        val shouldApplyCrtInLight = isCrtEnabled && effectiveLight == ThemeMode.Pixel
        assertFalse("CRT overlay must be disabled when Follow System resolves to Light Mode", shouldApplyCrtInLight)

        // OS in Dark mode -> CRT must apply
        val effectiveDark = ThemeMode.System.resolveEffective(isSystemInDarkTheme = true)
        val shouldApplyCrtInDark = isCrtEnabled && effectiveDark == ThemeMode.Pixel
        assertTrue("CRT overlay must be active when Follow System resolves to Pixel Mode", shouldApplyCrtInDark)
    }
}
