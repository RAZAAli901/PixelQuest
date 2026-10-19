package com.pixelquest.app.darood.ui.chat

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The chat's backdrop in each look, drawn with Robolectric's native graphics (saved to
 * app/build/outputs/darood-screenshots for review): the theme's background with a pattern that's
 * there, but faint.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h900dp-xhdpi")
class ChatBackgroundTest(private val mode: ThemeMode) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun looks() = listOf(ThemeMode.Pixel, ThemeMode.Comic, ThemeMode.Light).map { arrayOf<Any>(it) }
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theBackdrop_isTheThemesBackground_withAFaintPattern() {
        var background = 0
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode, darkTheme = mode == ThemeMode.Pixel) {
                background = PixelTheme.colors.background.toArgb()
                ChatBackground(modifier = Modifier.size(200.dp).testTag("backdrop")) {}
            }
        }
        composeTestRule.waitForIdle()
        val view = composeTestRule.activity.window.decorView.rootView
        val window = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        composeTestRule.runOnUiThread { view.draw(Canvas(window)) }
        val bounds = composeTestRule.onNodeWithTag("backdrop").fetchSemanticsNode().boundsInWindow
        val shot = Bitmap.createBitmap(window, bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt())
        File("build/outputs/darood-screenshots").apply { mkdirs() }.let { dir ->
            File(dir, "chat-background-${mode.name.lowercase()}.png").outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }

        var patterned = 0
        var strongest = 0
        for (x in 0 until shot.width) for (y in 0 until shot.height) {
            val pixel = shot.getPixel(x, y)
            if (pixel != background) {
                patterned++
                strongest = maxOf(strongest, channelDistance(pixel, background))
            }
        }
        val share = patterned.toDouble() / (shot.width * shot.height)
        assertEquals("the ground is the theme's background", background, shot.getPixel(1, 1))
        assertTrue("$mode: a pattern is there ($share)", share > 0.002)
        assertTrue("$mode: but it's sparse ($share)", share < 0.15)
        assertTrue("$mode: and faint (strongest change $strongest of 255)", strongest <= 40)
        assertEquals(if (mode == ThemeMode.Comic) ChatPattern.HALFTONE else ChatPattern.PIXEL_GRID, chatPatternFor(mode))
    }

    private fun channelDistance(a: Int, b: Int): Int = maxOf(
        kotlin.math.abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)),
        kotlin.math.abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)),
        kotlin.math.abs((a and 0xFF) - (b and 0xFF))
    )
}
