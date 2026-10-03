package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DayOutcomeTest {

    private val day = LocalDate.of(2026, 10, 2)

    private fun done(taskId: Long) = TaskCompletionLogEntity(taskId = taskId, completedDate = day, wasCompleted = true, pointsAwarded = 50)
    private fun missed(taskId: Long) = TaskCompletionLogEntity(taskId = taskId, completedDate = day, wasCompleted = false, pointsAwarded = 0)

    @Test
    fun aDayWithNothingScheduled_isARestDay() {
        assertEquals(DayOutcome.REST, StreakCalculator.dayOutcome(emptySet(), emptyList(), 0.7f))
        // Even if old logs exist for it (e.g. from a since-deleted task).
        assertEquals(DayOutcome.REST, StreakCalculator.dayOutcome(emptySet(), listOf(done(9)), 0.7f))
    }

    @Test
    fun meetingTheThreshold_isPerfect() {
        val scheduled = setOf(1L, 2L, 3L)
        assertEquals(DayOutcome.PERFECT, StreakCalculator.dayOutcome(scheduled, listOf(done(1), done(2), missed(3)), 0.6f))
        assertEquals(DayOutcome.MISSED, StreakCalculator.dayOutcome(scheduled, listOf(done(1), missed(2)), 0.6f))
    }

    @Test
    fun completionsOfTasksNotScheduledThatDay_dontCount() {
        // Task 1 is scheduled and undone; 7 and 8 were deleted or belong to other days.
        assertEquals(DayOutcome.MISSED, StreakCalculator.dayOutcome(setOf(1L), listOf(done(7), done(8)), 0.5f))
    }

    @Test
    fun aTaskLoggedTwice_countsOnce() {
        assertEquals(DayOutcome.MISSED, StreakCalculator.dayOutcome(setOf(1L, 2L), listOf(done(1), done(1)), 0.7f))
    }
}
