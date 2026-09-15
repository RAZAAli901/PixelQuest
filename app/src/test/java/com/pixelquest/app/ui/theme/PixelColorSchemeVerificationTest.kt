package com.pixelquest.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 11 Verification: Ensure existing pixel-mode UI renders identically
 * by verifying the PixelColorScheme contract matches canonical tokens bit-for-bit.
 */
class PixelColorSchemeVerificationTest {

    @Test
    fun pixelColorScheme_preservesExactCanonicalTokens() {
        val scheme = DefaultPixelColorScheme

        assertEquals(ThemeMode.Pixel, scheme.themeMode)
        assertTrue(scheme.isDark)

        // Verify all core arcade palette tokens match Day 1 canonical colors
        assertEquals(PixelGold, scheme.primary)
        assertEquals(PixelBlack, scheme.onPrimary)
        assertEquals(PixelGoldDark, scheme.primaryContainer)
        assertEquals(PixelTextWhite, scheme.onPrimaryContainer)
        assertEquals(PixelCyan, scheme.secondary)
        assertEquals(PixelBlack, scheme.onSecondary)
        assertEquals(PixelCyanDark, scheme.secondaryContainer)
        assertEquals(PixelGreen, scheme.tertiary)
        assertEquals(PixelBlack, scheme.onTertiary)
        assertEquals(PixelGreenDark, scheme.tertiaryContainer)
        assertEquals(PixelBackgroundDark, scheme.background)
        assertEquals(PixelTextWhite, scheme.onBackground)
        assertEquals(PixelSurfaceDark, scheme.surface)
        assertEquals(PixelTextWhite, scheme.onSurface)
        assertEquals(PixelSurfaceBorder, scheme.surfaceVariant)
        assertEquals(PixelTextMuted, scheme.onSurfaceVariant)
        assertEquals(PixelRed, scheme.error)
        assertEquals(PixelTextWhite, scheme.onError)
        assertEquals(PixelPurple, scheme.accentPurple)
    }

    @Test
    fun materialColorScheme_matchesOriginalDarkColorSchemeMapping() {
        val matScheme = DefaultPixelColorScheme.toMaterialColorScheme()

        assertEquals(PixelGold, matScheme.primary)
        assertEquals(PixelBlack, matScheme.onPrimary)
        assertEquals(PixelGoldDark, matScheme.primaryContainer)
        assertEquals(PixelTextWhite, matScheme.onPrimaryContainer)
        assertEquals(PixelCyan, matScheme.secondary)
        assertEquals(PixelBlack, matScheme.onSecondary)
        assertEquals(PixelGreen, matScheme.tertiary)
        assertEquals(PixelBlack, matScheme.onTertiary)
        assertEquals(PixelBackgroundDark, matScheme.background)
        assertEquals(PixelTextWhite, matScheme.onBackground)
        assertEquals(PixelSurfaceDark, matScheme.surface)
        assertEquals(PixelTextWhite, matScheme.onSurface)
        assertEquals(PixelSurfaceBorder, matScheme.surfaceVariant)
        assertEquals(PixelTextMuted, matScheme.onSurfaceVariant)
        assertEquals(PixelRed, matScheme.error)
        assertEquals(PixelTextWhite, matScheme.onError)
    }
}
