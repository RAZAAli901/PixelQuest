package com.pixelquest.app.ui.screens.namaz

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.domain.namaz.NextPrayer
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerMoment
import com.pixelquest.app.testing.LahoreZone
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** The next prayer's card: name and time, and a countdown that moves every second. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class NextPrayerCardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val day = LocalDate.of(2026, 10, 17)
    private fun at(hour: Int, minute: Int) = LocalDateTime.of(day, LocalTime.of(hour, minute)).atZone(LahoreZone)

    private fun next(remaining: Duration) = NextPrayer(
        current = PrayerMoment(Prayer.ASR, day, at(15, 50)),
        next = PrayerMoment(Prayer.MAGHRIB, day, at(17, 29)),
        remaining = remaining,
        prayerDay = day
    )

    @Test
    fun theCard_showsTheNextPrayer_andCountsDownEachSecond() {
        var ended = 0
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            PixelQuestTheme { NextPrayerCard(next = next(Duration.ofSeconds(3_725)), onCountdownEnd = { ended++ }) }
        }

        composeTestRule.onNodeWithText("Maghrib").assertIsDisplayed()
        composeTestRule.onNodeWithText("Now: Asr").assertIsDisplayed()
        composeTestRule.onNodeWithTag(NamazTestTags.COUNTDOWN).assertTextContains("1:02:05", substring = true)

        composeTestRule.mainClock.advanceTimeBy(1_100)
        composeTestRule.onNodeWithTag(NamazTestTags.COUNTDOWN).assertTextContains("1:02:04", substring = true)
        assertEquals(0, ended)
    }

    @Test
    fun atZero_itAsksForTheNextPrayer() {
        var ended = 0
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            PixelQuestTheme { NextPrayerCard(next = next(Duration.ofSeconds(2)), onCountdownEnd = { ended++ }) }
        }

        composeTestRule.mainClock.advanceTimeBy(2_100)
        composeTestRule.onNodeWithTag(NamazTestTags.COUNTDOWN).assertTextContains("0:00", substring = true)
        assertEquals(1, ended)
    }

    @Test
    fun countdowns_readAsHoursMinutesSeconds() {
        assertEquals("1:02:05", formatCountdown(Duration.ofSeconds(3_725)))
        assertEquals("4:09", formatCountdown(Duration.ofSeconds(249)))
        assertEquals("0:00", formatCountdown(Duration.ofSeconds(-5)))
        assertEquals("17:29", prayerTimeFormatter(is24Hour = true).format(at(17, 29)))
        assertEquals("5:29 PM", prayerTimeFormatter(is24Hour = false).format(at(17, 29)).uppercase().replace(' ', ' '))
    }
}
