package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.domain.model.CrtFilterPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 42: Final verification test:
 * Validates that gated/hidden Comic mode has zero impact on Pixel mode or Light mode behavior,
 * confirms CRT filter exclusivity to Pixel mode, and ensures complete theme orthogonality.
 */
class Day20ComicModeFinalVerificationTest {

    @Test
    fun comicMode_isStrictlyGatedAsUnavailable() {
        // Comic was gated until its screens were restyled, and has been available since Day 23.
        assertTrue("Comic mode is available", ThemeMode.Comic.isAvailable)
        assertTrue("Pixel mode must remain fully available", ThemeMode.Pixel.isAvailable)
        assertTrue("Light mode must remain fully available", ThemeMode.Light.isAvailable)
        assertTrue("System mode must remain fully available", ThemeMode.System.isAvailable)
    }

    @Test
    fun pixelMode_isCompletelyUnaffectedByComicAdditions() {
        val pixelScheme = DefaultPixelColorScheme

        // Classic retro dark arcade tokens must remain pristine
        assertEquals(PixelBackgroundDark, pixelScheme.background)
        assertEquals(PixelSurfaceDark, pixelScheme.surface)
        assertEquals(PixelGold, pixelScheme.primary)
        assertEquals(PixelCyan, pixelScheme.secondary)
        assertEquals(PixelGreen, pixelScheme.tertiary)
        assertEquals(Color(0xFFFFFFFF), pixelScheme.onBackground)
        assertEquals(Color(0xFFFFFFFF), pixelScheme.onSurface)
        assertEquals(PixelSurfaceBorder, pixelScheme.pixelBorder)
        assertTrue("Pixel mode isDark must be true", pixelScheme.isDark)
    }

    @Test
    fun lightMode_isCompletelyUnaffectedByComicAdditions() {
        val lightScheme = DefaultLightColorScheme

        // Retro arcade daylight tokens must remain pristine
        assertEquals(Color(0xFFF8F6F0), lightScheme.background)
        assertEquals(Color(0xFFFFFFFF), lightScheme.surface)
        assertEquals(Color(0xFFB45309), lightScheme.primary)
        assertEquals(Color(0xFF0284C7), lightScheme.secondary)
        assertEquals(Color(0xFF15803D), lightScheme.tertiary)
        assertEquals(Color(0xFF1C1917), lightScheme.onBackground)
        assertEquals(Color(0xFF1C1917), lightScheme.onSurface)
        assertEquals(Color(0xFF292524), lightScheme.pixelBorder)
        assertFalse("Light mode isDark must be false", lightScheme.isDark)
    }

    @Test
    fun crtFilterPolicy_strictlyExcludesComicAndLightModes() {
        // CRT filter is strictly an 8-bit Pixel dark mode feature
        assertTrue("CRT allowed for Pixel mode with user toggle on", CrtFilterPolicy.shouldApplyCrt(isCrtSettingEnabled = true, effectiveThemeMode = ThemeMode.Pixel, isSimpleModeEnabled = false))
        assertFalse("CRT suppressed for Pixel mode when user toggle off", CrtFilterPolicy.shouldApplyCrt(isCrtSettingEnabled = false, effectiveThemeMode = ThemeMode.Pixel, isSimpleModeEnabled = false))
        assertFalse("CRT never allowed for Light mode", CrtFilterPolicy.shouldApplyCrt(isCrtSettingEnabled = true, effectiveThemeMode = ThemeMode.Light, isSimpleModeEnabled = false))
        assertFalse("CRT never allowed for Comic mode", CrtFilterPolicy.shouldApplyCrt(isCrtSettingEnabled = true, effectiveThemeMode = ThemeMode.Comic, isSimpleModeEnabled = false))
    }

    @Test
    fun comicColorScheme_resolvesAllContractRequirements() {
        val comicScheme = DefaultComicColorScheme

        assertEquals(ComicTokens.PaperBackground, comicScheme.background)
        assertEquals(ComicTokens.PanelSurface, comicScheme.surface)
        assertEquals(ComicTokens.CoralRed, comicScheme.primary)
        assertEquals(ComicTokens.BurntOrange, comicScheme.primaryContainer)
        assertEquals(ComicTokens.SkyBlue, comicScheme.secondary)
        assertEquals(ComicTokens.Lavender, comicScheme.tertiary)
        assertEquals(ComicTokens.SolidBlack, comicScheme.onPrimary)
        assertEquals(ComicTokens.SolidBlack, comicScheme.pixelBorder)
        assertFalse(comicScheme.isDark)
    }
}
