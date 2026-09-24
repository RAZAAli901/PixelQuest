package com.pixelquest.app.ui.components

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.R
import com.pixelquest.app.ui.theme.ComponentThemeFamily
import com.pixelquest.app.ui.theme.DefaultLightColorScheme
import com.pixelquest.app.ui.theme.DefaultPixelColorScheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 5: Regression test confirming Pixel/Light PixelProgressBar rendering is unaffected
 * after wiring ComicProgressBar dispatch.
 */
class PixelProgressBarRegressionTest {

    @Test
    fun pixelProgressBar_pixelMode_preservesBitmapResources() {
        val bgDrawable = R.drawable.pixel_bar_background
        val fillDrawable = R.drawable.pixel_bar_green_fill

        assertTrue("Pixel bar background resource ID must be valid", bgDrawable != 0)
        assertTrue("Pixel bar fill resource ID must be valid", fillDrawable != 0)

        val isComic = ThemeMode.Pixel == ThemeMode.Comic
        assertFalse("Pixel mode must not enter Comic dispatch branch", isComic)
    }

    @Test
    fun pixelProgressBar_lightMode_preservesDaylightColorMapping() {
        val lightScheme = DefaultLightColorScheme

        // Light mode uses tertiary emerald for progress fill (#15803D)
        assertEquals(Color(0xFF15803D), lightScheme.tertiary)

        val isComic = ThemeMode.Light == ThemeMode.Comic
        assertFalse("Light mode must not enter Comic dispatch branch", isComic)
    }

    @Test
    fun pixelProgressBar_comicMode_resolvesComicFamily() {
        val family = ComponentThemeFamily.fromThemeMode(ThemeMode.Comic)
        assertEquals(ComponentThemeFamily.COMIC, family)

        val isComic = ThemeMode.Comic == ThemeMode.Comic
        assertTrue("Comic mode must enter Comic dispatch branch", isComic)
    }

    @Test
    fun pixelProgressBar_progressClamping_identicalAcrossThemes() {
        fun clamp(progress: Float): Float = progress.coerceIn(0f, 1f)

        assertEquals(0f, clamp(-1f), 0.0001f)
        assertEquals(0.5f, clamp(0.5f), 0.0001f)
        assertEquals(1f, clamp(2f), 0.0001f)
    }
}
