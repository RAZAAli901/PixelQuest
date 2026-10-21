package com.pixelquest.app.ui.screens.namaz

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.repository.NamazRepositoryImpl
import com.pixelquest.app.data.repository.SettingsRepositoryImpl
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.namazViewModel
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Phase 4's goal end to end, on the real database and settings: the switch turns the tracker on,
 * a button marks a prayer, and after a restart (a new ViewModel reading the same storage) it's all
 * still there, and the first-time setup isn't shown again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class NamazPersistenceTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val repository = NamazRepositoryImpl(db.namazLogDao(), db.namazSettingsDao())
    private val settings = SettingsRepositoryImpl(context)
    private val day = LocalDate.of(2026, 10, 17)
    private val clock = FixedClock(LocalDateTime.of(day, java.time.LocalTime.of(16, 0)))

    @After
    fun tearDown() {
        db.close()
    }

    private fun newViewModel() = namazViewModel(repository, clock, settings = settings)

    /**
     * Room tells observers about a write from its own threads, through the main looper; waitUntil
     * doesn't run Robolectric's paused looper, so this idles it between checks.
     */
    private fun waitFor(condition: () -> Boolean) {
        repeat(100) {
            composeTestRule.waitForIdle()
            if (condition()) return
            Thread.sleep(50)
        }
        throw AssertionError("Still waiting after 5 seconds")
    }

    @Test
    fun theSwitch_andAMarkedPrayer_surviveARestart() {
        var viewModel by mutableStateOf(newViewModel())
        composeTestRule.setContent { PixelQuestTheme { NamazScreen(viewModel = viewModel) } }

        // Off at first: turn it on, and close the first-time setup with Done.
        waitFor { composeTestRule.onAllNodesWithTagCount(NamazTestTags.EXPLAINER) == 1 }
        composeTestRule.onNodeWithTag(NamazTestTags.SWITCH).performClick()
        waitFor { composeTestRule.onAllNodesWithTagCount(NamazTestTags.SETUP) == 1 }
        composeTestRule.onNodeWithText("DONE").performScrollTo().performClick()
        waitFor { composeTestRule.onAllNodesWithTagCount(NamazTestTags.SETUP) == 0 }

        // Mark Asr.
        val asr = NamazTestTags.prayer(Prayer.ASR)
        waitFor { viewModel.uiState.value.enabled && viewModel.uiState.value.slots.isNotEmpty() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).performScrollToNode(hasTestTag(asr))
        composeTestRule.onNodeWithTag(asr).performClick()
        waitFor { runBlocking { repository.getDay(day) }.any { it.prayer == Prayer.ASR && it.status == PrayerStatus.PRAYED } }

        // Restart: a new ViewModel on the same database and settings.
        viewModel = newViewModel()
        waitFor { viewModel.uiState.value.enabled && viewModel.uiState.value.slots.isNotEmpty() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(NamazTestTags.SWITCH).assertIsOn()
        composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).performScrollToNode(hasTestTag(asr))
        waitFor { runCatching { composeTestRule.onNodeWithTag(asr).assertTextContains("Prayed", substring = true) }.isSuccess }
        composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).performScrollToNode(hasTestTag(NamazTestTags.PROGRESS))
        composeTestRule.onNodeWithText("1 / 5 prayed today").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithTagCount(NamazTestTags.SETUP))
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTagCount(tag: String) =
        onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().size
}
