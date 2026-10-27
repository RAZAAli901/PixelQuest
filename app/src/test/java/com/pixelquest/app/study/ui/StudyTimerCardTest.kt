package com.pixelquest.app.study.ui

import android.app.Application
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.R
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.RunningTimer
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class StudyTimerCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()

    private val party = Party(
        id = "p1",
        name = "Physics Circle",
        leaderId = "aria",
        maxMembers = 10,
        milestone = null,
        milestoneSetAt = null,
        completedAt = null,
        createdAt = Instant.EPOCH,
        total = 0,
        memberCount = 2,
        myStatus = MemberStatus.ACTIVE,
        type = PartyType.STUDY,
    )

    @Test
    fun idleState_showsStartButton_andCallsOnStart() {
        var started = false
        val state = StudyPartyUiState(party = party, timer = null)
        val actions = TimerActions(onStart = { started = true })

        composeTestRule.setContent {
            PixelQuestTheme {
                StudyTimerCard(state = state, actions = actions)
            }
        }

        composeTestRule.onNodeWithTag(StudyTags.TIMER_START).assertExists().performClick()
        assertTrue(started)
    }

    @Test
    fun runningState_showsRunningElapsed_andPauseAndStopButtons() {
        var paused = false
        var stopped = false
        var cancelled = false
        val t0 = 1_000_000L
        val timer = RunningTimer(partyId = "p1", startedAt = t0, partyName = "Physics Circle")
        val state = StudyPartyUiState(party = party, timer = timer)
        val actions = TimerActions(
            onPause = { paused = true },
            onStop = { stopped = true },
            onCancel = { cancelled = true },
        )

        composeTestRule.setContent {
            PixelQuestTheme {
                StudyTimerCard(state = state, actions = actions, clock = { t0 + 125_000L })
            }
        }

        composeTestRule.onNodeWithTag(StudyTags.TIMER_RUNNING).assertExists()
        composeTestRule.onNodeWithTag(StudyTags.TIMER_PAUSE).assertExists().performClick()
        assertTrue(paused)

        composeTestRule.onNodeWithTag(StudyTags.TIMER_STOP).assertExists().performClick()
        assertTrue(stopped)

        composeTestRule.onNodeWithTag(StudyTags.TIMER_CANCEL).assertExists().performClick()
        assertTrue(cancelled)
    }

    @Test
    fun pausedState_showsPausedText_andResumeButton() {
        var resumed = false
        val t0 = 1_000_000L
        val timer = RunningTimer(
            partyId = "p1",
            startedAt = t0,
            partyName = "Physics Circle",
            pausedAt = t0 + 600_000L, // 10m
        )
        val state = StudyPartyUiState(party = party, timer = timer)
        val actions = TimerActions(onResume = { resumed = true })

        composeTestRule.setContent {
            PixelQuestTheme {
                StudyTimerCard(state = state, actions = actions, clock = { t0 + 900_000L })
            }
        }

        composeTestRule.onNodeWithTag(StudyTags.TIMER_RUNNING).assertTextContains("Paused · 10:00")
        composeTestRule.onNodeWithTag(StudyTags.TIMER_RESUME).assertExists().performClick()
        assertTrue(resumed)
    }

    @Test
    fun otherPartyRunning_showsSwitchDialog_andConfirmsSwitch() {
        var switched = false
        val t0 = 1_000_000L
        val otherTimer = RunningTimer(
            partyId = "other_party",
            startedAt = t0,
            partyName = "Math Circle",
        )
        val state = StudyPartyUiState(party = party, timer = otherTimer)
        val actions = TimerActions(onSwitchTimer = { switched = true })

        composeTestRule.setContent {
            PixelQuestTheme {
                StudyTimerCard(state = state, actions = actions)
            }
        }

        // Timer elsewhere text is shown
        composeTestRule.onNodeWithTag(StudyTags.TIMER_ELSEWHERE).assertExists()

        // Clicking start triggers the switch confirmation dialog
        composeTestRule.onNodeWithTag(StudyTags.TIMER_START).assertExists().performClick()

        // Confirmation dialog appears; click confirm ("Switch timer")
        composeTestRule.onNodeWithText(context.getString(R.string.study_timer_switch_action), ignoreCase = true).assertExists().performClick()
        assertTrue(switched)
    }
}
