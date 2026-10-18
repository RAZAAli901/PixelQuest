package com.pixelquest.app.ui.screens.namaz

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.NamazPlace
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.namazViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** The Namaz screen's state in Lahore on 17 October 2026 (Fajr ~04:46, Asr ~15:50, Maghrib ~17:29). */
@OptIn(ExperimentalCoroutinesApi::class)
class NamazViewModelTest {

    private val lahoreZone = ZoneId.of("Asia/Karachi")
    private val day = LocalDate.of(2026, 10, 17)
    private val calculator = PrayerTimesCalculator()

    private val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true))
    private val clock = FixedClock(LocalDateTime.of(day, LocalTime.of(16, 0)))
    private val provider = DefaultPrayerTimeProvider(repository, calculator, clock) { lahoreZone }

    private fun viewModel() = namazViewModel(repository, clock, lahoreZone)

    private fun millisAt(hour: Int) = LocalDateTime.of(day, LocalTime.of(hour, 0)).atZone(lahoreZone).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun todaysFiveLahoreTimes_aLoggedPrayer_andProgress_areShown() = runTest {
        repository.logPrayer(day, Prayer.FAJR, prayedAtMillis = millisAt(5))
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        val expected = calculator.calculate(day, NamazPlace.LAHORE.latitude, NamazPlace.LAHORE.longitude, lahoreZone)
        assertFalse(state.loading)
        assertTrue(state.enabled)
        assertEquals(NamazPlace.LAHORE, state.place)
        assertEquals(expected, state.day!!.times)
        assertEquals(Prayer.entries.map { expected.timeOf(it) }, state.slots.map { it.time })
        assertEquals(
            listOf(PrayerStatus.PRAYED, PrayerStatus.MISSED, PrayerStatus.PENDING, PrayerStatus.PENDING, PrayerStatus.PENDING),
            state.slots.map { it.status }
        )
        assertEquals(millisAt(5), state.slots.first().prayedAt)
        assertEquals(1, state.progress)
        assertEquals(Prayer.ASR, state.next!!.current.prayer)
        assertEquals(Prayer.MAGHRIB, state.next!!.next.prayer)
    }

    @Test
    fun aPrayerLoggedWhileOpen_updatesTheStatusAndProgress() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(0, viewModel.uiState.value.progress)

        repository.logPrayer(day, Prayer.ASR, prayedAtMillis = millisAt(16))
        repository.logPrayer(day, Prayer.DHUHR, prayedAtMillis = millisAt(16))

        val state = viewModel.uiState.value
        assertEquals(2, state.progress)
        assertEquals(PrayerStatus.PRAYED, state.slots.first { it.prayer == Prayer.ASR }.status)
        assertTrue(state.progress in 0..NamazUiState.PRAYERS_PER_DAY)
    }

    @Test
    fun afterMidnight_theScreenStaysOnThePrayerDay_untilFajr() = runTest {
        repository.logPrayer(day, Prayer.ISHA, prayedAtMillis = millisAt(20))
        clock.time = LocalDateTime.of(day.plusDays(1), LocalTime.of(2, 0))
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(day, viewModel.uiState.value.day!!.date)
        assertEquals(1, viewModel.uiState.value.progress)

        clock.time = LocalDateTime.of(day.plusDays(1), LocalTime.of(6, 0))
        viewModel.refresh()

        assertEquals(day.plusDays(1), viewModel.uiState.value.day!!.date)
        assertEquals(0, viewModel.uiState.value.progress)
        assertEquals(Prayer.FAJR, viewModel.uiState.value.next!!.current.prayer)
    }

    @Test
    fun changingThePlace_recalculatesTheTimes() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val lahoreMaghrib = viewModel.uiState.value.day!!.times.maghrib

        // Karachi is about 12 degrees west of Lahore: the sun sets there later.
        repository.saveSettings(repository.getSettings().copy(latitude = 24.8607, longitude = 67.0011, city = "Karachi"))

        val state = viewModel.uiState.value
        assertEquals("Karachi", state.place.name)
        assertTrue(state.day!!.times.maghrib.isAfter(lahoreMaghrib))
    }

    @Test
    fun theTrackerSwitch_isPartOfTheState() = runTest {
        repository.saveSettings(NamazSettingsEntity(enabled = false))
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertFalse(viewModel.uiState.value.enabled)
        repository.saveSettings(repository.getSettings().copy(enabled = true))
        assertTrue(viewModel.uiState.value.enabled)
    }
}
