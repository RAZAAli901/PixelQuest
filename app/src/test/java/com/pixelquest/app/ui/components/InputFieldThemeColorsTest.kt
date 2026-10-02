package com.pixelquest.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.pixelquest.app.ui.theme.DefaultLightColorScheme
import com.pixelquest.app.ui.theme.DefaultPixelColorScheme
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelTextMuted
import com.pixelquest.app.ui.theme.PixelTextWhite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PixelTextField and PixelTimePicker take their colours from the theme roles primary, onSurface,
 * onSurfaceVariant and error. Pixel must keep its original look, and Light must show dark text on
 * its white field (the old fixed PixelTextWhite made typed text invisible in Light).
 */
class InputFieldThemeColorsTest {

    private fun contrast(a: Color, b: Color): Double {
        val l1 = a.luminance() + 0.05
        val l2 = b.luminance() + 0.05
        return (maxOf(l1, l2) / minOf(l1, l2)).toDouble()
    }

    @Test
    fun pixelRoles_matchTheOriginalFieldColours() {
        val pixel = DefaultPixelColorScheme
        assertEquals(PixelGold, pixel.primary)
        assertEquals(PixelTextWhite, pixel.onSurface)
        assertEquals(PixelTextMuted, pixel.onSurfaceVariant)
        assertEquals(PixelRed, pixel.error)
    }

    @Test
    fun lightTypedText_isReadableOnTheField() {
        val light = DefaultLightColorScheme
        assertTrue("Typed text", contrast(light.onSurface, light.surface) >= 4.5)
        assertTrue("Placeholder", contrast(light.onSurfaceVariant, light.surface) >= 4.5)
        assertTrue("Cursor and label", contrast(light.primary, light.surface) >= 3.0)
    }
}
