package com.pixelquest.app.notification

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.pixelquest.app.ui.theme.ComicSpec
import com.pixelquest.app.ui.theme.PixelSpec
import org.junit.Assert.assertTrue
import org.junit.Test

/** The notifications' accent (PqNotifications): each skin's red for text, readable on either shade. */
class NotificationStylingTest {

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    @Test
    fun theAccent_readsOnALightShade_andADarkOne_inBothSkins() {
        for (spec in listOf(ComicSpec, PixelSpec)) {
            val accent = spec.palette.primaryText
            val onLight = contrast(accent, Color.White)
            val onDark = contrast(accent, Color(0xFF121212))
            assertTrue("$accent on a white shade is ${"%.2f".format(onLight)}:1", onLight >= 4.5)
            assertTrue("$accent on a #121212 shade is ${"%.2f".format(onDark)}:1", onDark >= 3.0)
        }
    }
}
