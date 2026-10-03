package com.pixelquest.app.ui.theme

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Text on Comic's coloured panels is printed in black ink (inkOnPanel), which must stay readable on
 * every panel colour; Pixel and Light keep their accent colours.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ComicInkContrastTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun contrast(a: Color, b: Color): Double {
        val l1 = a.luminance() + 0.05
        val l2 = b.luminance() + 0.05
        return (maxOf(l1, l2) / minOf(l1, l2)).toDouble()
    }

    @Test
    fun blackInk_isReadableOnEveryComicPanel() {
        val panels = mapOf(
            "white" to ComicTokens.PanelSurface,
            "paper" to ComicTokens.PaperBackground,
            "burnt orange" to ComicTokens.BurntOrange,
            "sky blue" to ComicTokens.SkyBlue,
            "lavender" to ComicTokens.Lavender,
            "gold" to ComicTokens.GoldAccent,
            "coral red" to ComicTokens.CoralRed
        )
        panels.forEach { (name, panel) ->
            assertTrue("Black on $name", contrast(ComicTokens.SolidBlack, panel) >= 4.5)
        }
    }

    @Test
    fun theOldAccentPairs_wereUnreadable() {
        // The pairs Day 27 replaced with black ink.
        assertTrue(contrast(ComicTokens.CoralRed, ComicTokens.BurntOrange) < 2.0)
        assertTrue(contrast(ComicTokens.SkyBlue, ComicTokens.SkyBlue) < 1.1)
        assertTrue(contrast(ComicTokens.Lavender, ComicTokens.SkyBlue) < 2.0)
    }

    private fun inkIn(mode: ThemeMode, accent: Color): Color {
        var result = Color.Unspecified
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) { result = inkOnPanel(accent) }
        }
        composeTestRule.waitForIdle()
        return result
    }

    @Test
    fun comic_printsInBlack() = assertEquals(ComicTokens.SolidBlack, inkIn(ThemeMode.Comic, Color.Red))

    @Test
    fun pixel_keepsItsAccent() = assertEquals(Color.Red, inkIn(ThemeMode.Pixel, Color.Red))

    @Test
    fun light_keepsItsAccent() = assertEquals(Color.Red, inkIn(ThemeMode.Light, Color.Red))
}
