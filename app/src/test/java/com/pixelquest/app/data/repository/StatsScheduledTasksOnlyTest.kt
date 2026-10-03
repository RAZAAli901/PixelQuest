package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Stats count only completions of tasks scheduled that day, each once. */
class StatsScheduledTasksOnlyTest {

    private val start = LocalDate.of(2026, 9, 1)
    private val end = LocalDate.of(2026, 9, 10)

    private fun daily(id: Long) = TaskEntity(
        id = id, name = "Task $id", description = "", scheduledDay = start, scheduledTime = LocalTime.of(9, 0),
        recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
    )

    private fun done(taskId: Long, date: LocalDate) =
        TaskCompletionLogEntity(taskId = taskId, completedDate = date, wasCompleted = true, pointsAwarded = 50)

    private fun repo(tasks: List<TaskEntity>, logs: List<TaskCompletionLogEntity>): StatsRepositoryImpl {
        val taskRepo = mockk<TaskRepository> { every { getAllTasks() } returns flowOf(tasks) }
        val logRepo = mockk<TaskCompletionRepository> { every { getAllLogs() } returns flowOf(logs) }
        val difficulty = mockk<DifficultySettingsRepository> {
            every { getCurrentDifficulty() } returns flowOf(DifficultySettingsEntity())
        }
        return StatsRepositoryImpl(logRepo, mockk(), mockk(), taskRepo, difficulty)
    }

    private val everyDay = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()

    @Test
    fun aDeletedTasksCompletions_dontMakeDaysPerfect() = runBlocking {
        // Task 1 was done every day, then deleted. Task 2 (still there) was never done.
        val stats = repo(tasks = listOf(daily(2)), logs = everyDay.map { done(1, it) })

        val status = stats.getDailyStatusForRange(start, end).first()
        assertEquals(setOf(DailyStatus.MISSED), status.values.toSet())
        assertEquals(0f, stats.getCompletionRateOverRange(start, end).first(), 0.0001f)
    }

    @Test
    fun theRate_isCompletedScheduledOverScheduled() = runBlocking {
        // Two daily tasks over 10 days; task 1 done every day, task 2 never.
        val stats = repo(tasks = listOf(daily(1), daily(2)), logs = everyDay.map { done(1, it) })
        assertEquals(0.5f, stats.getCompletionRateOverRange(start, end).first(), 0.0001f)
        assertEquals(setOf(DailyStatus.PARTIAL), stats.getDailyStatusForRange(start, end).first().values.toSet())
    }

    @Test
    fun completingAllScheduledTasks_isPerfect() = runBlocking {
        val stats = repo(tasks = listOf(daily(1)), logs = everyDay.map { done(1, it) } + done(9, start))
        assertEquals(setOf(DailyStatus.PERFECT), stats.getDailyStatusForRange(start, end).first().values.toSet())
        assertEquals(1f, stats.getCompletionRateOverRange(start, end).first(), 0.0001f)
    }
}
