package com.pixelquest.app.darood.ui.chat

import android.app.Application
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The chat's look in each theme. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChatLookTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun lookIn(mode: ThemeMode): Pair<ChatLook, com.pixelquest.app.ui.theme.AppColorScheme> {
        var look: ChatLook? = null
        var colors: com.pixelquest.app.ui.theme.AppColorScheme? = null
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode, darkTheme = mode == ThemeMode.Pixel) {
                look = chatLook()
                colors = PixelTheme.colors
            }
        }
        composeTestRule.waitForIdle()
        return look!! to colors!!
    }

    @Test
    fun pixel_cutsItsCorners_andOutlinesEveryPiece() {
        val (look, colors) = lookIn(ThemeMode.Pixel)
        assertTrue(look.cut)
        assertEquals(2.dp, look.outline)
        assertEquals("my bubbles: a gold-dark outline on gold", colors.primaryContainer, look.myOutline)
        assertEquals(colors.pixelBorder, look.theirOutline)
        assertEquals(colors.pixelBorder, look.pillOutline)
        assertTrue(look.bubbleShape(mine = true, tail = true) is CutCornerShape)
        assertTrue(look.pillShape is CutCornerShape)
        assertTrue(look.fieldShape is CutCornerShape)
        // A run's first bubble has a square tail on its own side; the rest are chamfered all round.
        assertEquals(CutCornerShape(topStart = 6.dp, topEnd = 0.dp, bottomEnd = 6.dp, bottomStart = 6.dp), look.bubbleShape(mine = true, tail = true))
        assertEquals(CutCornerShape(topStart = 0.dp, topEnd = 6.dp, bottomEnd = 6.dp, bottomStart = 6.dp), look.bubbleShape(mine = false, tail = true))
        assertEquals(CutCornerShape(6.dp), look.bubbleShape(mine = false, tail = false))
    }

    @Test
    fun light_keepsTheRoundedChat() {
        val (look, _) = lookIn(ThemeMode.Light)
        assertEquals(false, look.cut)
        assertEquals(0.dp, look.outline)
        assertEquals(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp), look.bubbleShape(mine = true, tail = true))
        assertEquals(RoundedCornerShape(16.dp), look.bubbleShape(mine = true, tail = false))
    }
}
