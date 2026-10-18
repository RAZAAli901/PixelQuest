package com.pixelquest.app.ui.screens.namaz

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
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
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.repository.NamazRepositoryImpl
import com.pixelquest.app.data.repository.SettingsRepositoryImpl
import com.pixelquest.app.data.repository.UserProfileRepositoryImpl
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.LahoreZone
import com.pixelquest.app.testing.namazViewModel
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Phase 6 end to end, on the real database, profile and settings: Namaz turned on, the day's five
 * prayers marked as each one's time comes, then the XP, the all-five bonus, the streak and the
 * level checked. Lahore, 17 October 2026.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class NamazEndToEndTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val repository = NamazRepositoryImpl(db.namazLogDao(), db.namazSettingsDao())
    private val profile = UserProfileRepositoryImpl(db.userProfileDao())
    private val settings = SettingsRepositoryImpl(context)
    private val day = LocalDate.of(2026, 10, 17)
    private val clock = FixedClock(LocalDateTime.of(day, LocalTime.of(5, 0)))
    private val times = runBlocking {
        DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), clock) { LahoreZone }.timesFor(day)
    }

    @Before
    fun setUp() = runBlocking {
        profile.insertProfile(UserProfileEntity(username = "Aria", avatarId = "avatar_mage", level = 3, totalXp = 500))
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** Room's writes reach observers through the main looper, which this keeps turning. */
    private fun waitFor(condition: () -> Boolean) {
        repeat(100) {
            composeTestRule.waitForIdle()
            if (condition()) return
            Thread.sleep(50)
        }
        throw AssertionError("Still waiting after 5 seconds")
    }

    private fun xp() = runBlocking { profile.getProfile().first()!!.totalXp }
    private fun level() = runBlocking { profile.getProfile().first()!!.level }
    private fun status(prayer: Prayer) = runBlocking { repository.getDay(day).firstOrNull { it.prayer == prayer }?.status }

    @Test
    fun turningItOn_andMarkingAllFive_earnsTheXpAndTheBonus_andStartsAStreak() {
        val viewModel = namazViewModel(repository, clock, settings = settings, profile = profile)
        composeTestRule.setContent { PixelQuestTheme { NamazScreen(viewModel = viewModel) } }

        // On, and the first-time setup closed.
        waitFor { composeTestRule.onAllNodes(hasTestTag(NamazTestTags.SWITCH)).fetchSemanticsNodes().isNotEmpty() }
        composeTestRule.onNodeWithTag(NamazTestTags.SWITCH).performClick()
        waitFor { composeTestRule.onAllNodes(hasTestTag(NamazTestTags.SETUP)).fetchSemanticsNodes().isNotEmpty() }
        composeTestRule.onNodeWithText("Done").performScrollTo().performClick()
        waitFor { composeTestRule.onAllNodes(hasTestTag(NamazTestTags.SETUP)).fetchSemanticsNodes().isEmpty() }

        // Each prayer marked five minutes after it starts.
        Prayer.entries.forEach { prayer ->
            clock.time = times.timeOf(prayer).plusMinutes(5).toLocalDateTime()
            viewModel.refresh()
            waitFor { viewModel.uiState.value.enabled && viewModel.uiState.value.slots.any { it.prayer == prayer && it.started } }
            val button = NamazTestTags.prayer(prayer)
            composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).performScrollToNode(hasTestTag(button))
            composeTestRule.onNodeWithTag(button).performClick()
            waitFor { status(prayer) == PrayerStatus.PRAYED }
        }

        // 5 × 10 on time, and the all-five bonus.
        waitFor { xp() == 580 }
        composeTestRule.onNodeWithText("All five prayed!").assertIsDisplayed()
        composeTestRule.onNodeWithText("+30 XP bonus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Done").performClick()

        // A one-day streak, and the level as it was: levels come from perfect days, not XP.
        waitFor { viewModel.uiState.value.streak == 1 && viewModel.uiState.value.dayXp == 80 }
        composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).performScrollToNode(hasTestTag(NamazTestTags.STREAK))
        composeTestRule.onNodeWithText("1 day in a row with all five prayed", substring = true).assertIsDisplayed()
        assertEquals(3, level())
    }

    @Test
    fun theSeventhFullDayInARow_addsTheWeeksBonus() {
        runBlocking {
            repository.saveSettings(repository.getSettings().copy(enabled = true))
            (1L..6L).forEach { back -> Prayer.entries.forEach { repository.logPrayer(day.minusDays(back), it, prayedAtMillis = 0, xpAwarded = 10) } }
        }
        val viewModel = namazViewModel(repository, clock, settings = settings, profile = profile)
        composeTestRule.setContent { PixelQuestTheme { NamazScreen(viewModel = viewModel) } }

        Prayer.entries.forEach { prayer ->
            clock.time = times.timeOf(prayer).plusMinutes(5).toLocalDateTime()
            viewModel.refresh()
            viewModel.markPrayed(prayer)
            waitFor { status(prayer) == PrayerStatus.PRAYED }
        }

        // 5 × 10, the all-five 30, and the week's 100.
        waitFor { xp() == 680 }
        waitFor { viewModel.allFive.value != null }
        assertEquals(NamazAllFive(allFiveBonus = 30, weekBonus = 100), viewModel.allFive.value)
        waitFor { viewModel.uiState.value.streak == 7 }
        assertEquals(3, level())
    }
}
