package com.pixelquest.app.study.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDate

/** The study party's points and rules: the server's values, the early bonus, the daily cap, the week. */
class StudyPointsConfigTest {

    private fun repoFile(path: String) = listOf(File("../$path"), File(path)).first { it.exists() }

    @Test
    fun theValues_areTheServers() {
        val sql = repoFile("supabase/migrations/20261019000042_study_week_scores.sql").readText()
        fun server(name: String) = Regex("""(\d+) AS $name\b""").find(sql)!!.groupValues[1].toInt()
        assertEquals(server("assignment"), StudyPointsConfig.ASSIGNMENT)
        assertEquals(server("quiz"), StudyPointsConfig.QUIZ)
        assertEquals(server("early_bonus"), StudyPointsConfig.EARLY_BONUS)
        assertEquals(server("minutes_per_point"), StudyPointsConfig.MINUTES_PER_POINT)
        assertEquals(server("daily_study_max"), StudyPointsConfig.DAILY_STUDY_MAX)
    }

    @Test
    fun anItemIsWorthItsKind() {
        assertEquals(10, StudyPointsConfig.pointsFor(StudyItemKind.ASSIGNMENT))
        assertEquals(15, StudyPointsConfig.pointsFor(StudyItemKind.QUIZ))
    }

    @Test
    fun theEarlyBonus_isForADayOrMoreBefore_andNeverWithoutADate() {
        val due = Instant.parse("2026-10-22T09:00:00Z")
        assertEquals(5, StudyPointsConfig.earlyBonus(due, Instant.parse("2026-10-21T09:00:00Z")))
        assertEquals(0, StudyPointsConfig.earlyBonus(due, Instant.parse("2026-10-21T09:00:01Z")))
        assertEquals(0, StudyPointsConfig.earlyBonus(null, Instant.parse("2026-10-01T09:00:00Z")))
    }

    @Test
    fun studyTime_is1Per10Minutes_12ADayAtMost() {
        assertEquals(0, StudyPointsConfig.studyPoints(9))
        assertEquals(6, StudyPointsConfig.studyPoints(25 + 40))
        assertEquals(12, StudyPointsConfig.studyPoints(200))
    }

    @Test
    fun aWeek_runsMondayToSunday() {
        assertEquals(LocalDate.of(2026, 10, 19), weekStartOf(LocalDate.of(2026, 10, 19)))
        assertEquals(LocalDate.of(2026, 10, 19), weekStartOf(LocalDate.of(2026, 10, 25)))
        assertEquals(LocalDate.of(2026, 10, 26), weekStartOf(LocalDate.of(2026, 10, 26)))
    }

    @Test
    fun aCompletion_canBeUndone_for24Hours() {
        val done = StudyCompletion(1, "c", "i", "p", "me", Instant.parse("2026-10-20T10:00:00Z"), LocalDate.of(2026, 10, 20), "UTC")
        assertTrue(done.canUndoAt(Instant.parse("2026-10-21T09:59:59Z")))
        assertFalse(done.canUndoAt(Instant.parse("2026-10-21T10:00:00Z")))
    }

    @Test
    fun aKind_isReadFromTheWire() {
        assertEquals(StudyItemKind.QUIZ, StudyItemKind.fromWire("quiz"))
        assertEquals(StudyItemKind.ASSIGNMENT, StudyItemKind.fromWire("assignment"))
    }
}
