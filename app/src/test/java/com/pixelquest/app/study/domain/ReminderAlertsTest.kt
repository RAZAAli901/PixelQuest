package com.pixelquest.app.study.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** When a reminder alerts (docs/study-reminders.md, When it alerts), in the reminder's own zone. */
class ReminderAlertsTest {

    private val karachi = ZoneId.of("Asia/Karachi")
    private fun at(text: String): Instant = LocalDateTime.parse(text).atZone(karachi).toInstant()

    private fun reminder(scope: ReminderScope, dueAt: Instant? = null, weekStart: LocalDate? = null, offsets: List<Int>? = null, repeat: ReminderRepeat = ReminderRepeat.NONE) =
        StudyReminder(
            id = "r", partyId = "p", createdBy = "u", title = "Quiz", kind = ReminderKind.QUIZ, scope = scope, dueAt = dueAt,
            weekStart = weekStart, tz = "Asia/Karachi", repeat = repeat, audience = ReminderAudience.PARTY, alertOffsets = offsets,
            createdAt = Instant.EPOCH
        )

    private fun times(reminder: StudyReminder, now: Instant) = ReminderAlerts.upcoming(reminder, now).map { it.at }

    @Test
    fun aWeek_onItsMondayAt9_andSaturdayAt6pm() {
        val week = reminder(ReminderScope.WEEK, weekStart = LocalDate.of(2026, 10, 26))
        assertEquals(listOf(at("2026-10-26T09:00"), at("2026-10-31T18:00")), times(week, at("2026-10-21T12:00")))
        assertEquals("Monday's passed", listOf(at("2026-10-31T18:00")), times(week, at("2026-10-27T12:00")))
    }

    @Test
    fun aDay_theEveningBefore_andThatMorningAt7() {
        val day = reminder(ReminderScope.DAY, dueAt = at("2026-10-23T00:00"))
        assertEquals(listOf(at("2026-10-22T18:00"), at("2026-10-23T07:00")), times(day, at("2026-10-21T12:00")))
    }

    @Test
    fun aTime_aDayBefore_andHalfAnHourBefore() {
        val time = reminder(ReminderScope.TIME, dueAt = at("2026-10-23T10:00"))
        assertEquals(listOf(at("2026-10-22T10:00"), at("2026-10-23T09:30")), times(time, at("2026-10-21T12:00")))
    }

    @Test
    fun customTimes_replaceTheDefaults_asMinutesBeforeTheStart() {
        val week = reminder(ReminderScope.WEEK, weekStart = LocalDate.of(2026, 10, 26), offsets = listOf(0, 60 * 24))
        assertEquals(listOf(at("2026-10-25T00:00"), at("2026-10-26T00:00")), times(week, at("2026-10-21T12:00")))
    }

    @Test
    fun aRepeat_onceItsAlertsHavePassed_armsTheNextOccurrence() {
        val daily = reminder(ReminderScope.TIME, dueAt = at("2026-10-01T10:00"), repeat = ReminderRepeat.DAILY)
        // 09:45 today: today's 30-minute alert passed, the next day's day-before one is today at 10:00.
        val next = ReminderAlerts.upcoming(daily, at("2026-10-21T09:45"))
        assertEquals(listOf(at("2026-10-21T10:00"), at("2026-10-22T09:30")), next.map { it.at })
        assertTrue(next.all { it.occurrence.start.toInstant() == at("2026-10-22T10:00") })
        assertTrue("once, it's over", ReminderAlerts.upcoming(reminder(ReminderScope.TIME, dueAt = at("2026-10-21T10:00")), at("2026-10-21T09:45")).isEmpty())
    }

    @Test
    fun anAlert_isFoundByItsOccurrenceAndIndex() {
        val time = reminder(ReminderScope.TIME, dueAt = at("2026-10-23T10:00"))
        assertEquals(at("2026-10-23T09:30"), ReminderAlerts.alert(time, at("2026-10-23T10:00"), 1)!!.at)
        assertNull(ReminderAlerts.alert(time, at("2026-10-23T10:00"), 2))
        assertNull(ReminderAlerts.alert(time, at("2026-10-24T10:00"), 0))
    }
}
