package com.pixelquest.app.ui.screens.namaz

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.NextPrayerUseCase
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.LahoreZone
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

/** Home's Namaz card: only while the tracker is on, with the next prayer and today's count. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class NamazSummaryTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val day = LocalDate.of(2026, 10, 17)
    private val repository = FakeNamazRepository(NamazSettingsEntity(enabled = false))
    private val clock = FixedClock(LocalDateTime.of(day, java.time.LocalTime.of(16, 0)))

    private val hubShown = kotlinx.coroutines.flow.MutableStateFlow(true)

    private fun viewModel(): NamazSummaryViewModel {
        val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), clock) { LahoreZone }
        return NamazSummaryViewModel(repository, NextPrayerUseCase(provider), clock, zone = { LahoreZone }, hubShown = hubShown)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun whileTheTrackerIsOff_thereIsNoCard_andOnceOn_itHasTheNextPrayerAndTheCount() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.summary.collect {} }
        assertNull(viewModel.summary.value)

        repository.saveSettings(NamazSettingsEntity(enabled = true))
        repository.logPrayer(day, Prayer.FAJR, prayedAtMillis = 0)
        repository.logPrayer(day, Prayer.ASR, prayedAtMillis = 0)

        val summary = viewModel.summary.value!!
        assertEquals(Prayer.MAGHRIB, summary.next.next.prayer)
        assertEquals(2, summary.prayed)

        repository.saveSettings(NamazSettingsEntity(enabled = false))
        assertNull(viewModel.summary.value)
    }

    @Test
    fun theCard_goesWhenTheIslamicSectionIsHidden_andComesBack() = runTest {
        repository.saveSettings(NamazSettingsEntity(enabled = true))
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.summary.collect {} }
        assertEquals(true, viewModel.summary.value != null)

        hubShown.value = false
        assertNull(viewModel.summary.value)
        hubShown.value = true
        assertEquals(true, viewModel.summary.value != null)
    }

    @Test
    fun theCard_readsAsOneItem_andOpensTheTracker() {
        runBlocking { repository.saveSettings(NamazSettingsEntity(enabled = true)) }
        var opened = 0
        val viewModel = viewModel()
        composeTestRule.setContent {
            PixelQuestTheme { HomeNamazSection(onOpen = { opened++ }, viewModel = viewModel) }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(NamazTestTags.HOME_CARD)
            .assert(hasContentDescription("Namaz Tracker: Next: Maghrib at", substring = true))
            .assert(hasContentDescription("0 / 5 prayed today", substring = true))
            .performClick()
        assertEquals(1, opened)
    }
}
