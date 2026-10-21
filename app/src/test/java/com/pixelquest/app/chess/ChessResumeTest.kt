package com.pixelquest.app.chess

import android.app.Application
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.activity.ComponentActivity
import com.pixelquest.app.chess.data.fake.FakeChessBackend
import com.pixelquest.app.chess.ui.ChessResumeViewModel
import com.pixelquest.app.chess.ui.LeaveGameGuard
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A game in play opens again after the app is killed; Back asks before leaving one (J163). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChessResumeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theLiveGame_isFound_once() {
        val vm = ChessResumeViewModel(FakeChessBackend())
        composeTestRule.waitUntil(2_000) { vm.game.value != null }
        assertEquals("fake-game-mate", vm.game.value)
        vm.opened()
        assertEquals(null, vm.game.value)
    }

    @Test
    fun back_asksBeforeLeaving() {
        var left = false
        composeTestRule.setContent { PixelQuestTheme { LeaveGameGuard(playing = true) { left = true } } }
        composeTestRule.runOnUiThread { composeTestRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.onNodeWithText("Leave the game?").assertExists()
        assertFalse(left)
    }
}
