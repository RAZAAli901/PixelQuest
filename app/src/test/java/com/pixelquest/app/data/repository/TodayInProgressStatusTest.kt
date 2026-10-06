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
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.ui.screens.stats.StatsDataBucketer
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
 * At 8:35 am the heatmap showed today as a red "missed" day and its dialog said MISSED QUESTS. Today
 * with nothing done yet is now "in progress", and the weekly rate leaves it out until something is done.
 */
class TodayInProgressStatusTest {

    private val today = LocalDate.of(2026, 10, 6)
    private val morning = LocalDateTime.of(today, LocalTime.of(8, 35))

    private fun daily(id: Long) = TaskEntity(
        id = id, name = "Task $id", description = "", scheduledDay = today.minusDays(10), scheduledTime = LocalTime.of(9, 0),
        recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
    )

    private fun done(taskId: Long, date: LocalDate) =
        TaskCompletionLogEntity(taskId = taskId, completedDate = date, wasCompleted = true, pointsAwarded = 50)

    private fun statusesWith(logs: List<TaskCompletionLogEntity>): Map<LocalDate, DailyStatus> = runBlocking {
        val taskRepo = mockk<TaskRepository> { every { getAllTasks() } returns flowOf(listOf(daily(1), daily(2))) }
        val logRepo = mockk<TaskCompletionRepository> { every { getAllLogs() } returns flowOf(logs) }
        val difficulty = mockk<DifficultySettingsRepository> { every { getCurrentDifficulty() } returns flowOf(DifficultySettingsEntity()) }
        StatsRepositoryImpl(logRepo, mockk(), mockk(), taskRepo, difficulty, FixedClock(morning))
            .getDailyStatusForRange(today.minusDays(2), today).first()
    }

    @Test
    fun todayWithNothingDone_isInProgress_butYesterdayWasMissed() {
        val status = statusesWith(emptyList())

        assertEquals(DailyStatus.IN_PROGRESS, status[today])
        assertEquals(DailyStatus.MISSED, status[today.minusDays(1)])
    }

    @Test
    fun todayWithSomethingDone_isPartlyDone_orPerfect() {
        assertEquals(DailyStatus.PARTIAL, statusesWith(listOf(done(1, today)))[today])
        assertEquals(DailyStatus.PERFECT, statusesWith(listOf(done(1, today), done(2, today)))[today])
    }

    private fun rateWith(logs: List<TaskCompletionLogEntity>): Float = runBlocking {
        val taskRepo = mockk<TaskRepository> { every { getAllTasks() } returns flowOf(listOf(daily(1), daily(2))) }
        val logRepo = mockk<TaskCompletionRepository> { every { getAllLogs() } returns flowOf(logs) }
        StatsRepositoryImpl(logRepo, mockk(), mockk(), taskRepo, mockk(), FixedClock(morning))
            .getCompletionRateOverRange(today.minusDays(1), today).first()
    }

    @Test
    fun theCompletionRate_countsTodayOnlyForQuestsWithAResult() {
        // Yesterday 2 of 2 done; this morning nothing yet: still 100%, not 50%.
        val yesterdayDone = listOf(done(1, today.minusDays(1)), done(2, today.minusDays(1)))
        assertEquals(1f, rateWith(yesterdayDone), 0.0001f)

        // One of today's quests answered "not done": that one counts (2 of 3).
        val notDoneToday = TaskCompletionLogEntity(taskId = 1, completedDate = today, wasCompleted = false, pointsAwarded = 0)
        assertEquals(2f / 3f, rateWith(yesterdayDone + notDoneToday), 0.0001f)
    }

    @Test
    fun theWeeklyRate_leavesTodayOutWhileInProgress() {
        // Six earlier days all partly done, today not started: 6 of 6, not 6 of 7.
        val statuses = (1L..6L).associate { today.minusDays(it) to DailyStatus.PARTIAL } + (today to DailyStatus.IN_PROGRESS)

        val now = StatsDataBucketer.calculateWeeklyBuckets(statuses, today = today, weeksCount = 1).single()

        assertEquals(1f, now.second, 0.0001f)
    }
}
