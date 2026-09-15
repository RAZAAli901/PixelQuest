package com.pixelquest.app.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 16: Verification test that a theme switch triggers state updates
 * and theme-dependent feature gating (e.g. CRT scanline suppression) across screens.
 */
class ThemeRecompositionTest {

    private fun resolveScheme(mode: ThemeMode): AppColorScheme = when (mode) {
        ThemeMode.Pixel -> DefaultPixelColorScheme
        ThemeMode.Light -> DefaultLightColorScheme
        ThemeMode.Comic -> DefaultComicColorScheme
    }

    private fun shouldApplyCrt(isCrtSettingEnabled: Boolean, mode: ThemeMode): Boolean {
        return isCrtSettingEnabled && mode == ThemeMode.Pixel
    }

    @Test
    fun themeSwitch_recomposesColorTokensAccurately() = runTest {
        val themeState = MutableStateFlow(ThemeMode.Pixel)
        val collectedSchemes = mutableListOf<AppColorScheme>()

        // Simulate reactive UI subscription
        themeState.collect { mode ->
            collectedSchemes.add(resolveScheme(mode))
            if (collectedSchemes.size == 3) return@collect
        }

        // Initially in Pixel Mode
        assertEquals(ThemeMode.Pixel, collectedSchemes[0].themeMode)
        assertTrue(collectedSchemes[0].isDark)
        assertEquals(PixelBackgroundDark, collectedSchemes[0].background)

        // Switch to Light Mode
        themeState.value = ThemeMode.Light
        assertEquals(ThemeMode.Light, collectedSchemes[1].themeMode)
        assertFalse(collectedSchemes[1].isDark)
        assertEquals(DefaultLightColorScheme.background, collectedSchemes[1].background)

        // Switch to Comic Mode
        themeState.value = ThemeMode.Comic
        assertEquals(ThemeMode.Comic, collectedSchemes[2].themeMode)
        assertTrue(collectedSchemes[2].isDark)
        assertEquals(DefaultComicColorScheme.background, collectedSchemes[2].background)
    }

    @Test
    fun themeSwitch_disablesCrtOverlayOnNonPixelThemes() {
        val crtUserSetting = true

        // Pixel mode allows CRT overlay
        assertTrue(shouldApplyCrt(crtUserSetting, ThemeMode.Pixel))

        // Light mode bypasses CRT overlay to maintain high contrast
        assertFalse(shouldApplyCrt(crtUserSetting, ThemeMode.Light))

        // Comic mode bypasses CRT overlay to preserve bold pop-art style
        assertFalse(shouldApplyCrt(crtUserSetting, ThemeMode.Comic))

        // When user disables CRT in settings, Pixel mode also disables it
        assertFalse(shouldApplyCrt(isCrtSettingEnabled = false, ThemeMode.Pixel))
    }
}
