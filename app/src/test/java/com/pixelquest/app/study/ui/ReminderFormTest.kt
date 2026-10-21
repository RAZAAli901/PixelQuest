package com.pixelquest.app.study.ui

import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderProblem
import com.pixelquest.app.study.domain.ReminderRepeat
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.StudyReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** The Create reminder sheet's fields, and what they send, in the player's own zone. */
class ReminderFormTest {

    private val zone = ZoneId.of("Asia/Karachi")
    // A Wednesday.
    private val today = LocalDate.of(2026, 10, 21)

    @Test
    fun aWeek_isItsMonday_withNoMoment() {
        val draft = ReminderForm.new(today).withTitle("Quiz this week").draft(zone)
        assertEquals(ReminderScope.WEEK, draft.scope)
        assertEquals(LocalDate.of(2026, 10, 19), draft.weekStart)
        assertNull(draft.dueAt)
        assertEquals("Asia/Karachi", draft.tz)
        assertNull(ReminderForm.new(today).withTitle("Quiz this week").problem(zone))
    }

    @Test
    fun aDay_isItsStart_andATime_isThatMoment_inThePlayersZone() {
        val form = ReminderForm.new(today).withTitle("Exam").copy(day = LocalDate.of(2026, 10, 22))
        assertEquals(Instant.parse("2026-10-21T19:00:00Z"), form.withScope(ReminderScope.DAY).draft(zone).dueAt)
        val timed = form.withScope(ReminderScope.TIME).copy(time = LocalTime.of(10, 0)).draft(zone)
        assertEquals(Instant.parse("2026-10-22T05:00:00Z"), timed.dueAt)
        assertNull(timed.weekStart)
    }

    @Test
    fun aWeek_neverRepeatsDaily() {
        val daily = ReminderForm.new(today).withScope(ReminderScope.DAY).copy(repeat = ReminderRepeat.DAILY)
        assertEquals(ReminderRepeat.NONE, daily.withScope(ReminderScope.WEEK).repeat)
        assertEquals(ReminderRepeat.WEEKLY, daily.copy(repeat = ReminderRepeat.WEEKLY).withScope(ReminderScope.WEEK).repeat)
    }

    @Test
    fun theTitle_stopsAtItsLimit_andIsChecked() {
        assertEquals(80, ReminderForm.new(today).withTitle("x".repeat(100)).title.length)
        assertEquals(ReminderProblem.TITLE, ReminderForm.new(today).withTitle(" a ").problem(zone))
    }

    @Test
    fun theChoices_areFourWeeks_withAReminderOwnKept() {
        assertEquals(LocalDate.of(2026, 10, 19), ReminderForm.weeks(today).first())
        assertEquals(4, ReminderForm.weeks(today).size)
        val past = LocalDate.of(2026, 10, 5)
        assertEquals(past, ReminderForm.weeks(today, past).first())
        assertEquals(28, ReminderForm.days(today).size)
    }

    @Test
    fun anEdit_startsFromTheReminder_asItIs() {
        val reminder = StudyReminder(
            id = "r1", partyId = "p1", createdBy = "aria", title = "Lab", kind = ReminderKind.ASSIGNMENT, scope = ReminderScope.TIME,
            dueAt = Instant.parse("2026-10-23T05:00:00Z"), weekStart = null, tz = "Asia/Karachi", repeat = ReminderRepeat.WEEKLY,
            audience = ReminderAudience.ME, alertOffsets = listOf(30), createdAt = Instant.EPOCH
        )
        val form = ReminderForm.of(reminder, today, zone)
        assertEquals("r1", form.editingId)
        assertEquals(LocalDate.of(2026, 10, 23), form.day)
        assertEquals(LocalTime.of(10, 0), form.time)
        assertEquals(ReminderAudience.ME, form.audience)
        assertEquals(reminder.dueAt, form.draft(zone).dueAt)
    }
}
