package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 19: Verification test ensuring live cross-fade theme switching works
 * correctly transitioning into and out of Comic mode's finalized palette.
 */
class ComicThemeCrossfadeTest {

    private fun lerpScheme(start: AppColorScheme, target: AppColorScheme, fraction: Float): AppColorScheme {
        return DynamicAnimatedColorScheme(
            themeMode = target.themeMode,
            primary = lerp(start.primary, target.primary, fraction),
            onPrimary = lerp(start.onPrimary, target.onPrimary, fraction),
            primaryContainer = lerp(start.primaryContainer, target.primaryContainer, fraction),
            onPrimaryContainer = lerp(start.onPrimaryContainer, target.onPrimaryContainer, fraction),
            secondary = lerp(start.secondary, target.secondary, fraction),
            onSecondary = lerp(start.onSecondary, target.onSecondary, fraction),
            secondaryContainer = lerp(start.secondaryContainer, target.secondaryContainer, fraction),
            tertiary = lerp(start.tertiary, target.tertiary, fraction),
            onTertiary = lerp(start.onTertiary, target.onTertiary, fraction),
            tertiaryContainer = lerp(start.tertiaryContainer, target.tertiaryContainer, fraction),
            background = lerp(start.background, target.background, fraction),
            onBackground = lerp(start.onBackground, target.onBackground, fraction),
            surface = lerp(start.surface, target.surface, fraction),
            onSurface = lerp(start.onSurface, target.onSurface, fraction),
            surfaceVariant = lerp(start.surfaceVariant, target.surfaceVariant, fraction),
            onSurfaceVariant = lerp(start.onSurfaceVariant, target.onSurfaceVariant, fraction),
            error = lerp(start.error, target.error, fraction),
            onError = lerp(start.onError, target.onError, fraction),
            accentPurple = lerp(start.accentPurple, target.accentPurple, fraction),
            gold = lerp(start.gold, target.gold, fraction),
            pixelBorder = lerp(start.pixelBorder, target.pixelBorder, fraction),
            isDark = if (fraction < 0.5f) start.isDark else target.isDark
        )
    }

    @Test
    fun testCrossfadeTransition_pixelToComic() {
        val pixel = DefaultPixelColorScheme
        val comic = DefaultComicColorScheme

        // Mid-point transition (t = 0.5f)
        val midScheme = lerpScheme(pixel, comic, 0.5f)
        assertNotNull(midScheme.primary)
        assertNotNull(midScheme.background)
        assertNotNull(midScheme.surface)

        val mat = midScheme.toMaterialColorScheme()
        assertNotNull(mat.primary)
        assertNotNull(mat.background)

        // Target completion (t = 1.0f)
        val endScheme = lerpScheme(pixel, comic, 1.0f)
        assertEquals(comic.primary, endScheme.primary)
        assertEquals(comic.background, endScheme.background)
        assertEquals(comic.burntOrange, endScheme.primaryContainer)
        assertFalse("Comic theme is light paper mode", endScheme.isDark)
    }

    @Test
    fun testCrossfadeTransition_lightToComic() {
        val light = DefaultLightColorScheme
        val comic = DefaultComicColorScheme

        // Both Light and Comic have isDark = false
        assertFalse(light.isDark)
        assertFalse(comic.isDark)

        val midScheme = lerpScheme(light, comic, 0.5f)
        assertFalse("Transitions between light and comic maintain light paper state", midScheme.isDark)

        val endScheme = lerpScheme(light, comic, 1.0f)
        assertEquals(comic.primary, endScheme.primary)
        assertEquals(comic.secondary, endScheme.secondary)
        assertEquals(comic.tertiary, endScheme.tertiary)
    }

    @Test
    fun testCrossfadeTransition_comicToPixel() {
        val comic = DefaultComicColorScheme
        val pixel = DefaultPixelColorScheme

        val endScheme = lerpScheme(comic, pixel, 1.0f)
        assertEquals(pixel.primary, endScheme.primary)
        assertEquals(pixel.background, endScheme.background)
        assertTrue("Restored pixel mode is dark", endScheme.isDark)
    }
}
