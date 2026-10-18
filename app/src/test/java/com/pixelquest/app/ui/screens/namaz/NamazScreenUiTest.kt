package com.pixelquest.app.ui.screens.namaz

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.namazViewModel
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The Namaz screen with its ViewModel on fakes, in Lahore at 16:00 on 17 October 2026 (after Asr,
 * before Maghrib), unless a test moves the clock.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class NamazScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val day = LocalDate.of(2026, 10, 17)
    private val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true))
    private val clock = FixedClock(LocalDateTime.of(2026, 10, 17, 16, 0))
    private val settings = FakeSettingsRepository(onboardingDone = true)

    private fun show(mode: ThemeMode = ThemeMode.Pixel) {
        val viewModel = namazViewModel(repository, clock, settings = settings)
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) { NamazScreen(viewModel = viewModel) }
        }
        composeTestRule.waitForIdle()
    }

    private fun scrollTo(tag: String) {
        composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun fiveSeparateButtons_inTheDaysOrder() {
        show()

        Prayer.entries.forEach { prayer ->
            val button = NamazTestTags.prayer(prayer)
            scrollTo(button)
            composeTestRule.onNodeWithTag(button).assertIsDisplayed()
                .assertTextContains(composeTestRule.activity.getString(prayer.displayName), substring = true)
        }
        // Each one below the one before it.
        Prayer.entries.zipWithNext().forEach { (earlier, later) ->
            scrollTo(NamazTestTags.prayer(later))
            assertTrue("$later comes after $earlier", top(earlier) < top(later))
        }
    }

    private fun top(prayer: Prayer) =
        composeTestRule.onNodeWithTag(NamazTestTags.prayer(prayer)).fetchSemanticsNode().positionInRoot.y

    @Test
    fun aPrayerNotStartedYet_isLocked_andTappingItSaysWhenItStarts() {
        show()
        val maghrib = NamazTestTags.prayer(Prayer.MAGHRIB)
        scrollTo(maghrib)
        composeTestRule.onNodeWithTag(maghrib).assertTextContains("Not yet", substring = true)

        composeTestRule.onNodeWithTag(maghrib).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(NamazTestTags.LOCKED_TOOLTIP).assertIsDisplayed()
        composeTestRule.onNodeWithText("Starts at", substring = true).assertIsDisplayed()
        // Nothing was marked.
        assertTrue(repository.rows.value.isEmpty())
    }

    @Test
    fun tappingAPrayerThatHasStarted_marksItPrayed() {
        show()
        val asr = NamazTestTags.prayer(Prayer.ASR)
        scrollTo(asr)
        composeTestRule.onNodeWithTag(asr).assertTextContains("Tap when prayed", substring = true)

        composeTestRule.onNodeWithTag(asr).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(asr).assertTextContains("Prayed", substring = true)
        assertEquals(PrayerStatus.PRAYED, repository.statusOf(day, Prayer.ASR))
    }

    @Test
    fun aMissedPrayer_canStillBeMarked_late() {
        show()
        val dhuhr = NamazTestTags.prayer(Prayer.DHUHR)
        scrollTo(dhuhr)
        composeTestRule.onNodeWithTag(dhuhr).assertTextContains("Missed", substring = true)

        composeTestRule.onNodeWithTag(dhuhr).performClick()
        composeTestRule.waitForIdle()

        assertEquals(PrayerStatus.PRAYED, repository.statusOf(day, Prayer.DHUHR))
    }
}
