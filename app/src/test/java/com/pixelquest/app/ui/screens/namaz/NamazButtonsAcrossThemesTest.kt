package com.pixelquest.app.ui.screens.namaz

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The five buttons, a locked prayer, marking and undoing, in every look: Pixel, Comic, Light and
 * Simple Mode. Lahore at 16:00 on 17 October 2026: Fajr and Dhuhr have passed, Asr is open,
 * Maghrib and Isha are still to come.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class NamazButtonsAcrossThemesTest(private val mode: ThemeMode, private val simpleMode: Boolean) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}, simple={1}")
        fun looks() = listOf(
            arrayOf<Any>(ThemeMode.Pixel, false),
            arrayOf<Any>(ThemeMode.Comic, false),
            arrayOf<Any>(ThemeMode.Light, false),
            arrayOf<Any>(ThemeMode.Comic, true)
        )
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val day = LocalDate.of(2026, 10, 17)
    private val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true))

    private fun show() {
        val viewModel = namazViewModel(
            repository,
            FixedClock(LocalDateTime.of(day, java.time.LocalTime.of(16, 0))),
            settings = FakeSettingsRepository(simpleMode = simpleMode)
        )
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode, darkTheme = mode == ThemeMode.Pixel) { NamazScreen(viewModel = viewModel) }
        }
        composeTestRule.waitForIdle()
    }

    private fun button(prayer: Prayer): String {
        val tag = NamazTestTags.prayer(prayer)
        composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).performScrollToNode(hasTestTag(tag))
        return tag
    }

    @Test
    fun fiveButtons_eachWithItsName() {
        show()
        Prayer.entries.forEach { prayer ->
            composeTestRule.onNodeWithTag(button(prayer)).assertIsDisplayed()
                .assertTextContains(composeTestRule.activity.getString(prayer.displayName), substring = true)
        }
    }

    @Test
    fun aPrayerStillToCome_isLocked_andSaysWhenItStarts() {
        show()
        val isha = button(Prayer.ISHA)
        composeTestRule.onNodeWithTag(isha).assertTextContains("Not yet", substring = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Starts at", substring = true).assertIsDisplayed()
        assertEquals(null, repository.statusOf(day, Prayer.ISHA))
    }

    @Test
    fun markingThenUndoing_aPrayer() {
        show()
        val asr = button(Prayer.ASR)
        composeTestRule.onNodeWithTag(asr).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(asr).assertTextContains("Prayed", substring = true)
        assertEquals(PrayerStatus.PRAYED, repository.statusOf(day, Prayer.ASR))

        composeTestRule.onNodeWithTag(asr).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Undo", substring = true, ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        assertEquals(PrayerStatus.PENDING, repository.statusOf(day, Prayer.ASR))
        composeTestRule.onNodeWithTag(asr).assertTextContains("Tap when prayed", substring = true)
    }
}
