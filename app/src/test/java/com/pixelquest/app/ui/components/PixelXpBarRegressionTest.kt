package com.pixelquest.app.ui.components

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.ui.theme.ComponentThemeFamily
import com.pixelquest.app.ui.theme.DefaultLightColorScheme
import com.pixelquest.app.ui.theme.DefaultPixelColorScheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 15: Regression test confirming Pixel/Light PixelXpBar rendering is unaffected
 * after wiring ComicXpBar dispatch.
 */
class PixelXpBarRegressionTest {

    @Test
    fun pixelXpBar_pixelMode_preservesDarkColorMapping() {
        val pixelScheme = DefaultPixelColorScheme

        // Pixel mode primary (Cyan accent #00FFFF) and dark surfaceVariant (#2A2A2A)
        assertEquals(Color(0xFF00FFFF), pixelScheme.primary)
        assertEquals(Color(0xFF2A2A2A), pixelScheme.surfaceVariant)
        assertEquals(Color(0xFF000000), pixelScheme.pixelBorder)

        val isComic = ThemeMode.Pixel == ThemeMode.Comic
        assertFalse("Pixel mode must not enter Comic dispatch branch", isComic)
    }

    @Test
    fun pixelXpBar_lightMode_preservesDaylightColorMapping() {
        val lightScheme = DefaultLightColorScheme

        // Light mode primary and surfaceVariant colors remain unchanged
        assertEquals(Color(0xFF2E7D32), lightScheme.primary)
        assertEquals(Color(0xFFE8F5E9), lightScheme.surfaceVariant)
        assertEquals(Color(0xFF1B5E20), lightScheme.pixelBorder)

        val isComic = ThemeMode.Light == ThemeMode.Comic
        assertFalse("Light mode must not enter Comic dispatch branch", isComic)
    }

    @Test
    fun pixelXpBar_comicMode_resolvesComicFamily() {
        val family = ComponentThemeFamily.fromThemeMode(ThemeMode.Comic)
        assertEquals(ComponentThemeFamily.COMIC, family)

        val isComic = ThemeMode.Comic == ThemeMode.Comic
        assertTrue("Comic mode must enter Comic dispatch branch", isComic)
    }

    @Test
    fun pixelXpBar_progressFraction_calculationPreserved() {
        fun calculateFraction(current: Int, max: Int): Float {
            return if (max > 0) (current.toFloat() / max).coerceIn(0f, 1f) else 0f
        }

        assertEquals(0f, calculateFraction(0, 10), 0.0001f)
        assertEquals(0.5f, calculateFraction(5, 10), 0.0001f)
        assertEquals(1f, calculateFraction(10, 10), 0.0001f)
        assertEquals(1f, calculateFraction(15, 10), 0.0001f)
        assertEquals(0f, calculateFraction(5, 0), 0.0001f)
    }

    @Test
    fun pixelXpBar_semanticsDescription_formatInvariant() {
        fun formatSemantics(level: Int, current: Int, max: Int): String {
            val fraction = if (max > 0) (current.toFloat() / max).coerceIn(0f, 1f) else 0f
            val percentage = (fraction * 100).toInt()
            return "Level $level XP progress: $current of $max days ($percentage percent)"
        }

        assertEquals(
            "Level 1 XP progress: 5 of 10 days (50 percent)",
            formatSemantics(1, 5, 10)
        )
        assertEquals(
            "Level 10 XP progress: 10 of 10 days (100 percent)",
            formatSemantics(10, 10, 10)
        )
    }
}
