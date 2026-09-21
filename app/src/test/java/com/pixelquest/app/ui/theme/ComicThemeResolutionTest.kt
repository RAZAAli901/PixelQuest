package com.pixelquest.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Step 21: Unit test verifying ComicColorScheme resolves correctly through the theme system,
 * produces valid Material ColorScheme values, and enforces explicit theme isolation.
 */
class ComicThemeResolutionTest {

    @Test
    fun testComicColorScheme_resolvesCorrectTokens() {
        val scheme = DefaultComicColorScheme

        assertEquals(ThemeMode.Comic, scheme.themeMode)
        assertEquals(ComicTokens.CoralRed, scheme.primary)
        assertEquals(ComicTokens.SolidBlack, scheme.onPrimary)
        assertEquals(ComicTokens.BurntOrange, scheme.burntOrange)
        assertEquals(ComicTokens.SkyBlue, scheme.skyBlue)
        assertEquals(ComicTokens.Lavender, scheme.lavender)
        assertEquals(ComicTokens.PaperBackground, scheme.background)
        assertEquals(ComicTokens.PanelSurface, scheme.surface)
        assertEquals(ComicTokens.SolidBlack, scheme.pixelBorder)
        assertEquals(ComicTokens.SolidBlack, scheme.comicBorder)
        assertEquals(ComicTokens.SolidBlack, scheme.comicShadow)
        assertFalse("Comic theme is daylight paper theme", scheme.isDark)
    }

    @Test
    fun testComicColorScheme_producesValidMaterialLightColorScheme() {
        val scheme = DefaultComicColorScheme
        val mat = scheme.toMaterialColorScheme()

        assertNotNull(mat)
        assertEquals(ComicTokens.CoralRed, mat.primary)
        assertEquals(ComicTokens.SolidBlack, mat.onPrimary)
        assertEquals(ComicTokens.BurntOrange, mat.primaryContainer)
        assertEquals(ComicTokens.SkyBlue, mat.secondary)
        assertEquals(ComicTokens.Lavender, mat.tertiary)
        assertEquals(ComicTokens.PaperBackground, mat.background)
        assertEquals(ComicTokens.PanelSurface, mat.surface)
    }

    @Test
    fun testComicContainerCyclicalResolution() {
        val scheme = DefaultComicColorScheme

        assertEquals(scheme.burntOrange, scheme.containerForIndex(0))
        assertEquals(scheme.skyBlue, scheme.containerForIndex(1))
        assertEquals(scheme.lavender, scheme.containerForIndex(2))
        assertEquals(scheme.burntOrange, scheme.containerForIndex(3))
        assertEquals(scheme.skyBlue, scheme.containerForIndex(4))
        assertEquals(scheme.lavender, scheme.containerForIndex(5))
    }

    @Test
    fun testThemeModeComic_strictExplicitIsolation() {
        // Comic mode must be an explicit choice and never overridden by OS night mode
        assertEquals(ThemeMode.Comic, ThemeMode.Comic.resolveEffective(isSystemInDark = true))
        assertEquals(ThemeMode.Comic, ThemeMode.Comic.resolveEffective(isSystemInDark = false))
    }
}
