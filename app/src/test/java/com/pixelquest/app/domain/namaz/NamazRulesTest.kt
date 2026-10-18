package com.pixelquest.app.domain.namaz

import com.pixelquest.app.data.local.entity.NamazLogEntity
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.LahoreZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** The anti-abuse rules, on their own and through every way a prayer is marked. */
class NamazRulesTest {

    private val day = LocalDate.of(2026, 10, 17)
    private fun at(date: LocalDate, hour: Int, minute: Int = 0) = LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(LahoreZone)

    @Test
    fun theDaysThatCanBeMarked_andUndone() {
        assertTrue(NamazRules.canMarkDay(day, day))
        assertTrue(NamazRules.canMarkDay(day.minusDays(1), day))
        assertFalse(NamazRules.canMarkDay(day.minusDays(2), day))
        assertFalse(NamazRules.canMarkDay(day.plusDays(1), day))
        assertTrue(NamazRules.canUndoDay(day, day))
        assertFalse(NamazRules.canUndoDay(day.minusDays(1), day))
    }

    @Test
    fun aPrayerCountsAsStarted_fromItsVeryTime() {
        val start = at(day, 15, 50)
        assertFalse(NamazRules.hasStarted(start, start.minusSeconds(1)))
        assertTrue(NamazRules.hasStarted(start, start))
    }

    @Test
    fun aClockBehindAMark_isCaught_butAFewMinutesOfDriftAreFine() {
        val marked = NamazLogEntity(date = day, prayer = Prayer.ISHA, status = PrayerStatus.PRAYED, prayedAt = at(day, 20).toInstant().toEpochMilli())
        assertFalse(NamazRules.clockIsBehind(listOf(marked), at(day, 20, 30)))
        assertFalse("drift", NamazRules.clockIsBehind(listOf(marked), at(day, 19, 55)))
        assertTrue(NamazRules.clockIsBehind(listOf(marked), at(day, 16)))
        assertFalse(NamazRules.clockIsBehind(emptyList(), at(day, 16)))
    }

    @Test
    fun aClockSetAheadToMarkEarly_thenBack_cantMarkAgainUntilTimeCatchesUp() = runBlocking {
        val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true))
        val clock = FixedClock()
        val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), clock) { LahoreZone }
        val profile = FakeUserProfileRepository()
        val mark = MarkPrayerUseCase(provider, repository, NextPrayerUseCase(provider), profile)

        // Set ahead to 20:00: Maghrib and Isha are "started" and marked.
        assertTrue(mark(day, Prayer.ISHA, at(day, 20)) is MarkPrayerResult.Marked)
        // Back to the real 16:00: Asr is refused, and earns nothing.
        assertEquals(MarkPrayerResult.ClockBehind, mark(day, Prayer.ASR, at(day, 16)))
        assertEquals(10, profile.profile.value!!.totalXp)
        // Once the real time passes that mark, marking works again.
        assertTrue(mark(day, Prayer.ASR, at(day, 20, 5)) is MarkPrayerResult.Marked)
    }
}
