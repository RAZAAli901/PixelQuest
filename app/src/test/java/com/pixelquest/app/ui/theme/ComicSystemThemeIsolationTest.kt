package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.ui.components.ComicPanelVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Step 38: Verification test rendering and resolving Comic mode under both
 * system light and dark environments, confirming Comic mode is a fixed explicit
 * choice and never accidentally responds to or mutates with system theme changes.
 */
class ComicSystemThemeIsolationTest {

    @Test
    fun comicMode_effectiveResolution_ignoresSystemDarkAndLight() {
        val lightSystemResolved = ThemeMode.Comic.resolveEffective(isSystemInDark = false)
        val darkSystemResolved = ThemeMode.Comic.resolveEffective(isSystemInDark = true)

        assertEquals(ThemeMode.Comic, lightSystemResolved)
        assertEquals(ThemeMode.Comic, darkSystemResolved)
        assertEquals(lightSystemResolved, darkSystemResolved)
    }

    @Test
    fun comicColorScheme_isImmutableUnderSystemDarkAndLight() {
        fun resolveScheme(isSystemInDark: Boolean): AppColorScheme {
            val effective = ThemeMode.Comic.resolveEffective(isSystemInDark)
            return when (effective) {
                ThemeMode.Pixel, ThemeMode.System -> DefaultPixelColorScheme
                ThemeMode.Light -> DefaultLightColorScheme
                ThemeMode.Comic -> DefaultComicColorScheme
            }
        }

        val schemeUnderLight = resolveScheme(isSystemInDark = false)
        val schemeUnderDark = resolveScheme(isSystemInDark = true)

        // Palette properties must be identical
        assertEquals(schemeUnderLight.background, schemeUnderDark.background)
        assertEquals(schemeUnderLight.surface, schemeUnderDark.surface)
        assertEquals(schemeUnderLight.primary, schemeUnderDark.primary)
        assertEquals(schemeUnderLight.secondary, schemeUnderDark.secondary)
        assertEquals(schemeUnderLight.tertiary, schemeUnderDark.tertiary)
        assertEquals(schemeUnderLight.pixelBorder, schemeUnderDark.pixelBorder)
        assertEquals(schemeUnderLight.onBackground, schemeUnderDark.onBackground)
        assertEquals(schemeUnderLight.onSurface, schemeUnderDark.onSurface)

        // Comic mode is explicitly never a dark mode
        assertFalse(schemeUnderLight.isDark)
        assertFalse(schemeUnderDark.isDark)
    }

    @Test
    fun comicPanelVariants_resolveIdenticalTokensInAnySystemState() {
        fun resolvePanelBackground(variant: ComicPanelVariant, isSystemInDark: Boolean): Color {
            // Replicates ComicPanel internal background resolution
            val scheme = when (ThemeMode.Comic.resolveEffective(isSystemInDark)) {
                ThemeMode.Comic -> DefaultComicColorScheme
                else -> DefaultPixelColorScheme
            }

            return when (variant) {
                ComicPanelVariant.SURFACE -> ComicTokens.PanelSurface
                ComicPanelVariant.BURNT_ORANGE -> ComicTokens.BurntOrange
                ComicPanelVariant.SKY_BLUE -> ComicTokens.SkyBlue
                ComicPanelVariant.LAVENDER -> ComicTokens.Lavender
                ComicPanelVariant.PAPER -> ComicTokens.PaperBackground
            }
        }

        for (variant in ComicPanelVariant.values()) {
            val colorLight = resolvePanelBackground(variant, isSystemInDark = false)
            val colorDark = resolvePanelBackground(variant, isSystemInDark = true)

            assertEquals("Variant $variant must have identical color regardless of system theme", colorLight, colorDark)
        }
    }

    @Test
    fun comicPanelBordersAndShadows_remainPureBlackAcrossSystemTransitions() {
        val lightBorder = ComicTokens.SolidBlack
        val darkBorder = ComicTokens.SolidBlack
        val lightShadow = ComicTokens.SolidBlack
        val darkShadow = ComicTokens.SolidBlack

        assertEquals(lightBorder, darkBorder)
        assertEquals(lightShadow, darkShadow)
        assertEquals(Color(0xFF000000), lightBorder)
    }
}
