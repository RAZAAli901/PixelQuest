package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Step 34: Verification test that PixelThemeAssetFilter produces
 * correct tinting filters for Light mode while preserving null in Pixel mode.
 */
class PixelThemeAssetFilterTest {

    @Test
    fun forTheme_pixelMode_returnsNullToPreserveAuthenticArt() {
        val filter = PixelThemeAssetFilter.forTheme(
            themeMode = ThemeMode.Pixel,
            lightColor = Color.Blue
        )
        assertNull(filter)
    }

    @Test
    fun forTheme_lightMode_returnsNonNullColorFilter() {
        val filter = PixelThemeAssetFilter.forTheme(
            themeMode = ThemeMode.Light,
            lightColor = Color.Yellow
        )
        assertNotNull(filter)
    }

    @Test
    fun forTheme_comicMode_returnsNullForDedicatedArt() {
        val filter = PixelThemeAssetFilter.forTheme(
            themeMode = ThemeMode.Comic,
            lightColor = Color.Red
        )
        assertNull(filter)
    }

    @Test
    fun buttonTint_behaviorMatchesThemeExpectation() {
        assertNull(PixelThemeAssetFilter.buttonTint(ThemeMode.Pixel, Color.Yellow))
        assertNotNull(PixelThemeAssetFilter.buttonTint(ThemeMode.Light, Color.Yellow))
        assertNull(PixelThemeAssetFilter.buttonTint(ThemeMode.Comic, Color.Yellow))
    }

    @Test
    fun matrixFilters_produceNonNullColorFilters() {
        assertNotNull(PixelThemeAssetFilter.grayscale())
        assertNotNull(PixelThemeAssetFilter.invert())
    }
}
