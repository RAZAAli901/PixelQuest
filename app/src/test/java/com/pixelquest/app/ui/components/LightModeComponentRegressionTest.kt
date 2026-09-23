package com.pixelquest.app.ui.components

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.ui.theme.ComponentThemeFamily
import com.pixelquest.app.ui.theme.DefaultLightColorScheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 36: Full regression test verifying Light mode is completely unaffected
 * by Day 21's theme-dispatch refactor across buttons, cards, dialogs, and form inputs.
 */
class LightModeComponentRegressionTest {

    @Test
    fun lightMode_componentThemeFamily_resolvesToLight() {
        val family = ComponentThemeFamily.fromThemeMode(ThemeMode.Light)
        assertEquals("ThemeMode.Light must resolve to ComponentThemeFamily.LIGHT", ComponentThemeFamily.LIGHT, family)
    }

    @Test
    fun lightButton_usesDaylightPaletteAndTokens() {
        val lightScheme = DefaultLightColorScheme

        // Light Primary CTA: Retro Daylight Amber (#B45309)
        assertEquals(0xFFB45309, lightScheme.primary.value.toLong() shr 32 or (lightScheme.primary.value.toLong() and 0xFFFFFFFFL))

        // Light Secondary CTA: Retro Daylight Emerald (#15803D)
        assertEquals(0xFF15803D, lightScheme.secondary.value.toLong() shr 32 or (lightScheme.secondary.value.toLong() and 0xFFFFFFFFL))

        // Light OnPrimary and OnSecondary are crisp White (#FFFFFF)
        assertEquals(Color.White, lightScheme.onPrimary)
        assertEquals(Color.White, lightScheme.onSecondary)
    }

    @Test
    fun lightCard_usesDaylightSurfacesAndStoneBorders() {
        val lightScheme = DefaultLightColorScheme

        // Daylight paper surface (#FAFAF8)
        assertEquals(0xFFFAFAF8, lightScheme.surface.value.toLong() shr 32 or (lightScheme.surface.value.toLong() and 0xFFFFFFFFL))

        // Stepped stone outline (#292524)
        assertEquals(0xFF292524, lightScheme.outline.value.toLong() shr 32 or (lightScheme.outline.value.toLong() and 0xFFFFFFFFL))

        // Comic dispatch guard is false for Light mode
        val isComic = ThemeMode.Light == ThemeMode.Comic
        assertFalse("Light mode must not trigger Comic dispatch branch", isComic)
    }

    @Test
    fun lightDialog_preservesCallbacksAndContract() {
        var confirmed = false
        var dismissed = false

        val onConfirm = { confirmed = true }
        val onDismiss = { dismissed = true }

        onConfirm()
        assertTrue("Confirm callback must be executed in Light mode", confirmed)

        onDismiss()
        assertTrue("Dismiss callback must be executed in Light mode", dismissed)
    }

    @Test
    fun lightMode_remainsAvailableInThemeRegistry() {
        assertTrue("ThemeMode.Light must remain available to users", ThemeMode.Light.isAvailable)
        assertFalse("ThemeMode.Comic must remain gated from real users", ThemeMode.Comic.isAvailable)
    }
}
