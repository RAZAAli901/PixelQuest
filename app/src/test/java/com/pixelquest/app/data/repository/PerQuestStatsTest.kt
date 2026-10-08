package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.testing.FixedClock
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A quest's own stats (Analytics) count its occurrences: every day it was due, and today only once
 * it has a result. They used to count today before it was answered (90% every morning for a quest
 * done 9 days running) and to skip unlogged past days, which then didn't break the quest's streak.
 */
class PerQuestStatsTest {

    private val today = LocalDate.of(2026, 10, 8)
    private val morning = LocalDateTime.of(today, LocalTime.of(8, 0))

    private fun stats(task: TaskEntity, logs: List<TaskCompletionLogEntity>) = runBlocking {
        val tasks = mockk<TaskRepository> { every { getTaskById(task.id) } returns flowOf(task) }
        val completions = mockk<TaskCompletionRepository> { every { getLogsForTask(task.id) } returns flowOf(logs) }
        StatsRepositoryImpl(completions, mockk(), mockk(), tasks, mockk(), FixedClock(morning)).getPerTaskStats(task.id).first()
    }

    private fun daily(startDaysAgo: Long, isActive: Boolean = true) = TaskEntity(
        id = 1, name = "Run", description = "", scheduledDay = today.minusDays(startDaysAgo), scheduledTime = LocalTime.of(7, 0),
        recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS, isActive = isActive
    )

    private fun log(daysAgo: Long, done: Boolean = true) = TaskCompletionLogEntity(
        taskId = 1, completedDate = today.minusDays(daysAgo), wasCompleted = done, pointsAwarded = if (done) 50 else 0
    )

    @Test
    fun todayDoesntCount_untilItHasAResult() {
        val nineDays = (1L..9L).map { log(it) }

        val morningStats = stats(daily(startDaysAgo = 9), nineDays)
        assertEquals(1f, morningStats.completionRate, 0.0001f)
        assertEquals(9, morningStats.totalScheduledCount)
        assertEquals(9, morningStats.currentStreak)

        val afterDoingIt = stats(daily(startDaysAgo = 9), nineDays + log(0))
        assertEquals(10, afterDoingIt.totalScheduledCount)
        assertEquals(10, afterDoingIt.currentStreak)
    }

    @Test
    fun aDueDayWithNoResult_isNotDone_andBreaksTheStreak() {
        // Done 6, 5 and 4 days ago; nothing logged 3 days ago; done 2 and 1 days ago.
        val stats = stats(daily(startDaysAgo = 6), listOf(log(6), log(5), log(4), log(2), log(1)))

        assertEquals(6, stats.totalScheduledCount)
        assertEquals(5, stats.completionCount)
        assertEquals(5f / 6f, stats.completionRate, 0.0001f)
        assertEquals(3, stats.longestStreak)
        assertEquals(2, stats.currentStreak)
        assertEquals(today.minusDays(3) to false, stats.recentHistory[3])
    }

    @Test
    fun aQuestCreatedToday_hasNoRateYet_ratherThanZeroPercent() {
        val stats = stats(daily(startDaysAgo = 0), emptyList())

        assertEquals(0, stats.totalScheduledCount)
    }

    @Test
    fun anArchivedQuest_keepsItsHistory() {
        val stats = stats(daily(startDaysAgo = 3, isActive = false), listOf(log(3), log(2), log(1)))

        assertEquals(3, stats.completionCount)
        assertEquals(1f, stats.completionRate, 0.0001f)
    }
}
