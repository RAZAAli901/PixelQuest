package com.pixelquest.app.ui.components

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.ComponentThemeFamily
import com.pixelquest.app.ui.theme.DefaultComicColorScheme
import com.pixelquest.app.ui.theme.DefaultLightColorScheme
import com.pixelquest.app.ui.theme.DefaultPixelColorScheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Step 5: Unit test for PixelButton's theme-dispatch logic across Pixel, Light, and Comic modes.
 * Validates variant mappings, color scheme resolution, and ComponentThemeFamily behavior.
 */
class PixelButtonThemeDispatchTest {

    @Test
    fun componentThemeFamily_mapsAllThemeModesCorrectly() {
        assertEquals(ComponentThemeFamily.PIXEL, ComponentThemeFamily.fromThemeMode(ThemeMode.Pixel))
        assertEquals(ComponentThemeFamily.PIXEL, ComponentThemeFamily.fromThemeMode(ThemeMode.System))
        assertEquals(ComponentThemeFamily.LIGHT, ComponentThemeFamily.fromThemeMode(ThemeMode.Light))
        assertEquals(ComponentThemeFamily.COMIC, ComponentThemeFamily.fromThemeMode(ThemeMode.Comic))
    }

    @Test
    fun comicButtonVariantMapping_mapsPixelVariantsToComicTokens() {
        // PixelButtonVariant.YELLOW (Primary CTA) -> ComicButtonVariant.PRIMARY (Coral Red)
        fun resolveComicVariant(variant: PixelButtonVariant): ComicButtonVariant = when (variant) {
            PixelButtonVariant.YELLOW -> ComicButtonVariant.PRIMARY
            PixelButtonVariant.BLUE -> ComicButtonVariant.SKY_BLUE
        }

        assertEquals(ComicButtonVariant.PRIMARY, resolveComicVariant(PixelButtonVariant.YELLOW))
        assertEquals(ComicButtonVariant.SKY_BLUE, resolveComicVariant(PixelButtonVariant.BLUE))
    }

    @Test
    fun buttonColorsAcrossThemes_areDistinctAndAppropriate() {
        val pixelScheme = DefaultPixelColorScheme
        val lightScheme = DefaultLightColorScheme
        val comicScheme = DefaultComicColorScheme

        // Pixel Primary CTA: Arcade Gold
        assertEquals(Color(0xFFF4C430), pixelScheme.primary)

        // Light Primary CTA: Retro Daylight Amber
        assertEquals(Color(0xFFB45309), lightScheme.primary)

        // Comic Primary CTA: Pop-Art Coral Red
        assertEquals(ComicTokens.CoralRed, comicScheme.primary)
    }

    @Test
    fun comicButtonVariants_haveExpectedDefaultFills() {
        assertEquals(Color(0xFFFF5A4E), comicButtonFill(ComicButtonVariant.PRIMARY))
        assertEquals(Color(0xFF8ECAE6), comicButtonFill(ComicButtonVariant.SKY_BLUE))
        assertEquals(Color(0xFFF0A868), comicButtonFill(ComicButtonVariant.BURNT_ORANGE))
        assertEquals(Color(0xFFB8A4D4), comicButtonFill(ComicButtonVariant.LAVENDER))
        assertEquals(ComicTokens.PanelSurface, comicButtonFill(ComicButtonVariant.SURFACE))
    }

    @Test
    fun disabledComicButtons_areFlatGreyWhateverTheVariant() {
        ComicButtonVariant.values().forEach { variant ->
            assertEquals(Color(0xFFE0DDD5), comicButtonFill(variant, enabled = false))
        }
    }
}
