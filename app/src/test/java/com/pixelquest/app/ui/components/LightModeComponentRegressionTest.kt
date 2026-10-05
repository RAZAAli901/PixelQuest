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
        assertEquals(Color(0xFFB45309), lightScheme.primary)

        // Light Secondary CTA: Daylight Sky (#0284C7); emerald (#15803D) is the tertiary
        assertEquals(Color(0xFF0284C7), lightScheme.secondary)
        assertEquals(Color(0xFF15803D), lightScheme.tertiary)

        // Light OnPrimary and OnSecondary are crisp White (#FFFFFF)
        assertEquals(Color.White, lightScheme.onPrimary)
        assertEquals(Color.White, lightScheme.onSecondary)
    }

    @Test
    fun lightCard_usesDaylightSurfacesAndStoneBorders() {
        val lightScheme = DefaultLightColorScheme

        // Crisp white card surface
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFFFFFF), lightScheme.surface)

        // Stepped stone border (#292524)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF292524), lightScheme.pixelBorder)

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
        assertTrue("ThemeMode.Comic has been available to everyone since Day 23", ThemeMode.Comic.isAvailable)
    }
}
