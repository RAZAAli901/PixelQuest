package com.pixelquest.app.chess

import android.app.Application
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pixelquest.app.chess.domain.Challenge
import com.pixelquest.app.chess.domain.ChallengeStatus
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.ui.IncomingChallengeScreen
import com.pixelquest.app.chess.ui.IncomingState
import com.pixelquest.app.chess.ui.IncomingTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** The incoming challenge (J151): a countdown to its expiry, Accept and Decline. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class IncomingChallengeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val now = Instant.parse("2026-10-22T10:00:00Z")
    private fun challenge(expires: Instant) = Challenge("c", "p", "sara", "me", TimeControl.THREE, ColourChoice.WHITE, true, ChallengeStatus.OPEN, expires, now)

    @Test
    fun countsDown_andAnswers() {
        val answers = mutableListOf<Boolean>()
        composeTestRule.setContent {
            PixelQuestTheme { IncomingChallengeScreen(IncomingState(challenge(now.plusSeconds(581)), fromName = "sara"), onAnswer = { answers += it }, onNavigateBack = {}, clock = { now }) }
        }
        composeTestRule.onNodeWithTag(IncomingTags.COUNTDOWN).assertTextContains("9:41", substring = true)
        composeTestRule.onNodeWithTag(IncomingTags.ACCEPT).performClick()
        assertEquals(listOf(true), answers)
    }

    @Test
    fun pastItsTime_itCantBeAnswered() {
        composeTestRule.setContent {
            PixelQuestTheme { IncomingChallengeScreen(IncomingState(challenge(now.minusSeconds(1)), fromName = "sara"), onAnswer = {}, onNavigateBack = {}, clock = { now }) }
        }
        composeTestRule.onNodeWithTag(IncomingTags.ACCEPT).assertIsNotEnabled()
    }
}
