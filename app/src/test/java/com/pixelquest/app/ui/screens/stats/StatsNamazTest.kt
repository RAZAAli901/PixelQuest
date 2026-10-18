package com.pixelquest.app.ui.screens.stats

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.repository.StatsRepository
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.FixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Stats shows the Namaz Tracker as its own category: XP from prayers, and prayers prayed. */
@OptIn(ExperimentalCoroutinesApi::class)
class StatsNamazTest {

    private val today = LocalDate.of(2026, 10, 17)

    private val emptyStats = object : StatsRepository {
        override fun getCompletionRateOverRange(start: LocalDate, end: LocalDate): Flow<Float> = flowOf(0f)
        override fun getDailyStatusForRange(start: LocalDate, end: LocalDate) =
            flowOf(emptyMap<LocalDate, com.pixelquest.app.domain.model.DailyStatus>())
        override fun getPerTaskStats(taskId: Long) = throw UnsupportedOperationException()
    }


    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(namaz: FakeNamazRepository) = StatsViewModel(
        statsRepository = emptyStats,
        streakRepository = com.pixelquest.app.testing.FakeStreakRepository(),
        userProfileRepository = FakeUserProfileRepository(),
        difficultySettingsRepository = FakeDifficultySettingsRepository(),
        appClock = FixedClock(LocalDateTime.of(today, LocalTime.NOON)),
        namazRepository = namaz
    )

    @Test
    fun theNamazTotals_comeFromItsLog() = runTest {
        val namaz = FakeNamazRepository(NamazSettingsEntity(enabled = true))
        namaz.logPrayer(today, Prayer.FAJR, prayedAtMillis = 0, xpAwarded = 10)
        namaz.logPrayer(today.minusDays(3), Prayer.ISHA, prayedAtMillis = 0, xpAwarded = 10, bonusXp = 30)
        namaz.logPrayer(today.minusDays(20), Prayer.ASR, prayedAtMillis = 0, xpAwarded = 5)
        val viewModel = viewModel(namaz)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(NamazStats(xpTotal = 55, xpLastWeek = 50, prayed = 3), viewModel.uiState.value.namaz)
    }

    @Test
    fun aTrackerNeverUsed_addsNothingToStats() = runTest {
        val viewModel = viewModel(FakeNamazRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertNull(viewModel.uiState.value.namaz)
    }
}
