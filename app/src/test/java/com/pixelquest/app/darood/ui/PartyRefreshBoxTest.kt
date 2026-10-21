package com.pixelquest.app.darood.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The pull-to-refresh circle waits, hidden, just above its box. With something else directly above
 * (a study party's tabs), it must stay inside its box, not draw over that: a 1.5.0 tester saw it as
 * a grey circle over the Scoreboard tab.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PartyRefreshBoxTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test
    fun theIdleCircle_neverDrawsOverWhatsAbove() {
        composeTestRule.setContent {
            PixelQuestTheme {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().height(80.dp).background(Color.Red).testTag("above"))
                    PartyRefreshBox(refreshing = false, onRefresh = {}, modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        // The window as drawn, and the block's place in it.
        val view = composeTestRule.activity.window.decorView.rootView
        val window = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
        composeTestRule.runOnUiThread { view.draw(android.graphics.Canvas(window)) }
        val block = composeTestRule.onNodeWithTag("above").fetchSemanticsNode().boundsInWindow
        val notRed = (block.left.toInt() until block.right.toInt()).sumOf { x ->
            (block.top.toInt() until block.bottom.toInt()).count { y -> window.getPixel(x, y) != Color.Red.toArgb() }
        }
        assertEquals("pixels of the block above drawn over", 0, notRed)
    }
}
