package com.pixelquest.app.darood.ui.chat

import android.app.Application
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The chat's look in each skin (mock 4E). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChatLookTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun lookIn(mode: ThemeMode, simple: Boolean = false): ChatLook {
        var look: ChatLook? = null
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                androidx.compose.runtime.CompositionLocalProvider(LocalDaroodSimpleMode provides simple) { look = chatLook() }
            }
        }
        composeTestRule.waitForIdle()
        return look!!
    }

    @Test
    fun comic_inksEveryPiece_withAFlatShadow_roundedAsItsCards() {
        val look = lookIn(ThemeMode.Comic)
        assertEquals(2.dp, look.outline)
        assertEquals(2.dp, look.shadow)
        // A run's first bubble has a small tail corner on its own side; the rest are round all round.
        assertEquals(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp), look.bubbleShape(mine = true, tail = true))
        assertEquals(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp), look.bubbleShape(mine = false, tail = true))
        assertEquals(RoundedCornerShape(16.dp), look.bubbleShape(mine = false, tail = false))
        assertTrue(look.pillShape is RoundedCornerShape)
    }

    @Test
    fun pixel_isSquare_withTheSameInkAndShadow() {
        val look = lookIn(ThemeMode.Pixel)
        assertEquals(2.dp, look.outline)
        assertEquals(2.dp, look.shadow)
        assertEquals(RoundedCornerShape(0.dp), look.bubbleShape(mine = true, tail = true))
        assertEquals(RectangleShape, look.pillShape)
        assertEquals(RectangleShape, look.fieldShape)
    }

    @Test
    fun simpleMode_keepsTheInk_withoutTheShadows_inComic() {
        val look = lookIn(ThemeMode.Comic, simple = true)
        assertEquals(0.dp, look.shadow)
        assertEquals(2.dp, look.outline)
    }

    @Test
    fun simpleMode_keepsTheInk_withoutTheShadows_inPixel() {
        val look = lookIn(ThemeMode.Pixel, simple = true)
        assertEquals(0.dp, look.shadow)
        assertEquals(2.dp, look.outline)
    }
}
