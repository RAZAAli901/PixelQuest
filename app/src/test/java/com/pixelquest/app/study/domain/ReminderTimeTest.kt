package com.pixelquest.app.study.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** When a reminder's occurrences fall, in its own zone, repeats and changes of clock included. */
class ReminderTimeTest {

    private fun reminder(
        scope: ReminderScope,
        dueAt: Instant? = null,
        weekStart: LocalDate? = null,
        repeat: ReminderRepeat = ReminderRepeat.NONE,
        tz: String = "Asia/Karachi"
    ) = StudyReminder(
        id = "r", partyId = "p", createdBy = "u", title = "Quiz", kind = ReminderKind.QUIZ, scope = scope,
        dueAt = dueAt, weekStart = weekStart, tz = tz, repeat = repeat, audience = ReminderAudience.PARTY,
        alertOffsets = null, createdAt = Instant.EPOCH
    )

    private val karachi = ZoneId.of("Asia/Karachi")
    private fun at(text: String, zone: ZoneId = karachi): Instant = ZonedDateTime.of(java.time.LocalDateTime.parse(text), zone).toInstant()

    @Test
    fun aWeek_runsFromItsMonday_toTheNext() {
        val week = reminder(ReminderScope.WEEK, weekStart = LocalDate.of(2026, 10, 19))
        val only = week.occurrencesIn(at("2026-10-21T00:00"), at("2026-10-22T00:00")).single()
        assertEquals(at("2026-10-19T00:00"), only.start.toInstant())
        assertEquals(at("2026-10-26T00:00"), only.end.toInstant())
        assertTrue(week.occurrencesIn(at("2026-10-26T00:00"), at("2026-11-02T00:00")).isEmpty())
        assertNull("over once its week is", week.nextOccurrence(at("2026-10-26T00:00")))
    }

    @Test
    fun aTime_isItsMoment_andOverWhenItComes() {
        val time = reminder(ReminderScope.TIME, dueAt = at("2026-10-22T10:00"))
        assertEquals(1, time.occurrencesIn(at("2026-10-19T00:00"), at("2026-10-26T00:00")).size)
        assertEquals(at("2026-10-22T10:00"), time.nextOccurrence(at("2026-10-22T09:59"))!!.start.toInstant())
        assertNull(time.nextOccurrence(at("2026-10-22T10:00")))
    }

    @Test
    fun aDailyOne_comesEachDay_andAWeeklyOne_eachWeek() {
        val daily = reminder(ReminderScope.TIME, dueAt = at("2026-10-01T07:30"), repeat = ReminderRepeat.DAILY)
        val week = daily.occurrencesIn(at("2026-10-19T00:00"), at("2026-10-26T00:00"))
        assertEquals(7, week.size)
        assertEquals(at("2026-10-19T07:30"), week.first().start.toInstant())
        assertEquals(at("2026-10-22T07:30"), daily.nextOccurrence(at("2026-10-21T08:00"))!!.start.toInstant())

        val weekly = reminder(ReminderScope.WEEK, weekStart = LocalDate.of(2026, 9, 7), repeat = ReminderRepeat.WEEKLY)
        assertEquals(at("2026-10-19T00:00"), weekly.nextOccurrence(at("2026-10-21T12:00"))!!.start.toInstant())
        assertEquals(1, weekly.occurrencesIn(at("2026-10-19T00:00"), at("2026-10-26T00:00")).size)
    }

    @Test
    fun aRepeat_keepsItsLocalTime_acrossAChangeOfClock() {
        // Berlin goes from summer to winter time on 25 October 2026.
        val berlin = ZoneId.of("Europe/Berlin")
        val daily = reminder(ReminderScope.TIME, dueAt = at("2026-10-20T09:00", berlin), repeat = ReminderRepeat.DAILY, tz = "Europe/Berlin")
        val after = daily.nextOccurrence(at("2026-10-26T00:00", berlin))!!
        assertEquals(at("2026-10-26T09:00", berlin), after.start.toInstant())
        assertEquals(9, after.start.hour)
    }

    @Test
    fun aDay_isWholeInItsZone() {
        val day = reminder(ReminderScope.DAY, dueAt = at("2026-10-22T00:00"))
        val only = day.nextOccurrence(at("2026-10-22T23:00"))!!
        assertEquals(at("2026-10-23T00:00"), only.end.toInstant())
        assertNull(day.nextOccurrence(at("2026-10-23T00:00")))
    }

    @Test
    fun anUnknownZone_isReadAsUtc_andAMissingMoment_hasNoOccurrence() {
        assertEquals(java.time.ZoneOffset.UTC, reminder(ReminderScope.TIME, dueAt = Instant.EPOCH, tz = "Mars/Olympus").zone)
        assertNull(reminder(ReminderScope.WEEK).nextOccurrence(Instant.EPOCH))
        assertTrue(reminder(ReminderScope.TIME).occurrencesIn(Instant.EPOCH, Instant.MAX).isEmpty())
    }
}
