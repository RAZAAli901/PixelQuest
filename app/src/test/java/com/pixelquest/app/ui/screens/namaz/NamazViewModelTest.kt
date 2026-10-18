package com.pixelquest.app.ui.screens.namaz

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.NamazPlace
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FakeLocationProvider
import com.pixelquest.app.testing.FakeSettingsRepository
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

    private val location = FakeLocationProvider()
    private val settings = FakeSettingsRepository()

    private fun viewModel() = namazViewModel(repository, clock, lahoreZone, location, settings)

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

    @Test
    fun theSwitch_savesTheSetting_andKeepsTheLog() = runTest {
        repository.logPrayer(day, Prayer.FAJR, prayedAtMillis = millisAt(5))
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.setEnabled(false)
        assertFalse(repository.getSettings().enabled)
        assertFalse(viewModel.uiState.value.enabled)
        assertEquals(PrayerStatus.PRAYED, repository.statusOf(day, Prayer.FAJR))

        viewModel.setEnabled(true)
        assertTrue(repository.getSettings().enabled)
        assertEquals(1, viewModel.uiState.value.progress)
    }

    @Test
    fun turningTheSwitchOnTheFirstTime_opensTheSetup_once() = runTest {
        repository.saveSettings(NamazSettingsEntity(enabled = false))
        val viewModel = viewModel()

        viewModel.setEnabled(true)
        assertTrue(viewModel.setupVisible.value)

        viewModel.finishSetup(com.pixelquest.app.domain.namaz.NamazMethod.MUSLIM_WORLD_LEAGUE, remind = false)
        assertFalse(viewModel.setupVisible.value)
        val saved = repository.getSettings()
        assertEquals("MUSLIM_WORLD_LEAGUE", saved.method)
        assertFalse(saved.remindFajr || saved.remindDhuhr || saved.remindAsr || saved.remindMaghrib || saved.remindIsha)
        assertTrue(settings.namazSetupShown.value)

        viewModel.setEnabled(false)
        viewModel.setEnabled(true)
        assertFalse("shown once", viewModel.setupVisible.value)
    }

    @Test
    fun closingTheSetup_keepsTheDefaults_andDoesntShowItAgain() = runTest {
        val viewModel = viewModel()
        viewModel.setEnabled(true)

        viewModel.dismissSetup()

        assertFalse(viewModel.setupVisible.value)
        assertTrue(settings.namazSetupShown.value)
        assertEquals("KARACHI", repository.getSettings().method)
    }

    @Test
    fun useMyLocation_movesTheTimes_orSaysItCouldnt() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.useMyLocation()
        assertEquals(LocationLookup.UNAVAILABLE, viewModel.locationLookup.value)
        assertEquals(NamazPlace.LAHORE, viewModel.uiState.value.place)

        // Near Karachi's centre: named after it.
        location.point = com.pixelquest.app.namaz.GeoPoint(24.87, 67.03)
        viewModel.useMyLocation()
        assertEquals(LocationLookup.IDLE, viewModel.locationLookup.value)
        assertEquals("Karachi", viewModel.uiState.value.place.name)
    }

    @Test
    fun theDayStrip_isTheLastWeek_andAnEarlierDayShowsItsOwnPrayers() = runTest {
        val wednesday = day.minusDays(3)
        repository.logPrayer(wednesday, Prayer.MAGHRIB, prayedAtMillis = millisAt(18))
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val strip = viewModel.uiState.value.days
        assertEquals(NamazUiState.STRIP_DAYS, strip.size)
        assertEquals(day, strip.last())
        assertEquals(day.minusDays(6), strip.first())
        assertFalse(viewModel.uiState.value.isPastDay)

        viewModel.selectDay(wednesday)
        val shown = viewModel.uiState.value
        assertTrue(shown.isPastDay)
        assertEquals(wednesday, shown.day!!.date)
        assertEquals(1, shown.progress)
        assertEquals(PrayerStatus.PRAYED, shown.slots.first { it.prayer == Prayer.MAGHRIB }.status)
        // Nothing else was marked that day, and its windows have all passed.
        assertEquals(4, shown.slots.count { it.status == PrayerStatus.MISSED })

        viewModel.selectDay(day)
        assertFalse(viewModel.uiState.value.isPastDay)
    }

    @Test
    fun theWeek_hasFiveStatusesPerDay_forItsDots() = runTest {
        repository.logPrayer(day, Prayer.FAJR, prayedAtMillis = millisAt(5))
        repository.logPrayer(day.minusDays(1), Prayer.ISHA, prayedAtMillis = millisAt(20))
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val week = viewModel.uiState.value.week
        assertEquals(NamazUiState.STRIP_DAYS, week.size)
        assertTrue(week.values.all { it.size == NamazUiState.PRAYERS_PER_DAY })
        // Today at 16:00: Fajr prayed, Dhuhr missed, the rest still to come.
        assertEquals(
            listOf(PrayerStatus.PRAYED, PrayerStatus.MISSED, PrayerStatus.PENDING, PrayerStatus.PENDING, PrayerStatus.PENDING),
            week.getValue(day)
        )
        assertEquals(PrayerStatus.PRAYED, week.getValue(day.minusDays(1)).last())

        // A prayer marked now shows in the week too.
        repository.logPrayer(day, Prayer.ASR, prayedAtMillis = millisAt(16))
        assertEquals(PrayerStatus.PRAYED, viewModel.uiState.value.week.getValue(day)[2])
    }

    @Test
    fun theStreak_countsDaysWithAllFive_andGrowsWhenTodaysFifthIsPrayed() = runTest {
        // Yesterday and the day before: all five.
        listOf(day.minusDays(1), day.minusDays(2)).forEach { d ->
            Prayer.entries.forEach { repository.logPrayer(d, it, prayedAtMillis = millisAt(12)) }
        }
        // Today: all but Isha so far.
        Prayer.entries.dropLast(1).forEach { repository.logPrayer(day, it, prayedAtMillis = millisAt(12)) }
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(2, viewModel.uiState.value.streak)

        repository.logPrayer(day, Prayer.ISHA, prayedAtMillis = millisAt(19))
        assertEquals(3, viewModel.uiState.value.streak)
    }

    @Test
    fun theSettings_changeTheTimesAtOnce() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val before = viewModel.uiState.value.day!!.times

        viewModel.setMadhab(com.pixelquest.app.domain.namaz.AsrMadhab.SHAFI)
        // Shafi's Asr starts earlier than Hanafi's.
        assertTrue(viewModel.uiState.value.day!!.times.asr.isBefore(before.asr))

        viewModel.adjust(Prayer.FAJR, 2)
        assertEquals(before.fajr.plusMinutes(2), viewModel.uiState.value.day!!.times.fajr)

        viewModel.setMethod(com.pixelquest.app.domain.namaz.NamazMethod.UMM_AL_QURA)
        assertEquals("UMM_AL_QURA", repository.getSettings().method)
    }

    @Test
    fun anAdjustment_staysWithinHalfAnHourEitherWay() = runTest {
        val viewModel = viewModel()
        repeat(40) { viewModel.adjust(Prayer.ISHA, 1) }
        assertEquals(NamazViewModel.MAX_ADJUST_MINUTES, repository.getSettings().adjustIsha)
        repeat(80) { viewModel.adjust(Prayer.ISHA, -1) }
        assertEquals(-NamazViewModel.MAX_ADJUST_MINUTES, repository.getSettings().adjustIsha)
    }
}
