package com.pixelquest.app.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.LevelHistoryRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * The nightly streak check scores every day since the last one it evaluated, ignores days with
 * nothing scheduled, and only counts completions of tasks scheduled that day.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StreakCatchUpTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.now()
    private val yesterday = today.minusDays(1)

    private fun task(id: Long) = TaskEntity(
        id = id, name = "Task $id", description = "", scheduledDay = today.minusDays(30),
        scheduledTime = LocalTime.of(9, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
    )

    private fun done(taskId: Long, date: LocalDate) =
        TaskCompletionLogEntity(taskId = taskId, completedDate = date, wasCompleted = true, pointsAwarded = 50)

    /** Runs the worker and returns the saved streak. */
    private fun run(
        start: StreakEntity,
        tasksOn: (LocalDate) -> List<TaskEntity>,
        logsOn: (LocalDate) -> List<TaskCompletionLogEntity>
    ): StreakEntity = runBlocking {
        val streakFlow = MutableStateFlow<StreakEntity?>(start)
        val streaks = mockk<StreakRepository> {
            every { getCurrentStreak() } returns streakFlow
            coEvery { updateStreak(any()) } answers { streakFlow.value = firstArg() }
        }
        val tasks = mockk<TaskRepository> { every { getTasksForDay(any()) } answers { flowOf(tasksOn(firstArg())) } }
        val logs = mockk<TaskCompletionRepository> { every { getLogsForDate(any()) } answers { flowOf(logsOn(firstArg())) } }
        val difficulty = mockk<DifficultySettingsRepository> {
            every { getCurrentDifficulty() } returns flowOf(DifficultySettingsEntity(perfectDayThreshold = 1.0f, daysRequiredPerLevel = 100))
        }
        val profileFlow = MutableStateFlow<UserProfileEntity?>(UserProfileEntity(username = "Hero", avatarId = "avatar_hero"))
        val profiles = mockk<UserProfileRepository> {
            every { getProfile() } returns profileFlow
            coEvery { updateProfile(any()) } answers { profileFlow.value = firstArg() }
            coEvery { setLevelProgress(any(), any()) } answers {
                profileFlow.value = profileFlow.value!!.copy(level = firstArg(), perfectDaysTowardNextLevel = secondArg())
            }
        }
        val history = mockk<LevelHistoryRepository>(relaxed = true)
        val worker = TestListenableWorkerBuilder<StreakEvaluationWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    StreakEvaluationWorker(
                        appContext, workerParameters, tasks, logs, streaks, difficulty, profiles, history,
                        LevelUpSignalManager(appContext)
                    )
            })
            .build()
        worker.doWork()
        streakFlow.value!!
    }

    @Test
    fun missedDaysWhileThePhoneWasOff_breakTheStreak() {
        // Last evaluated 4 days ago with a 5-day streak; nothing done since.
        val result = run(
            StreakEntity(currentStreak = 5, longestStreak = 5, lastCompletedDate = today.minusDays(4)),
            tasksOn = { listOf(task(1)) },
            logsOn = { emptyList() }
        )
        assertEquals(0, result.currentStreak)
        assertEquals(5, result.longestStreak)
        assertEquals(yesterday, result.lastCompletedDate)
    }

    @Test
    fun everyCaughtUpPerfectDay_counts() {
        val result = run(
            StreakEntity(currentStreak = 2, longestStreak = 2, lastCompletedDate = today.minusDays(4), perfectDaysCount = 2),
            tasksOn = { listOf(task(1)) },
            logsOn = { date -> listOf(done(1, date)) }
        )
        assertEquals(5, result.currentStreak) // 3 more days
        assertEquals(5, result.longestStreak)
        assertEquals(5, result.perfectDaysCount)
    }

    @Test
    fun restDays_neitherExtendNorBreakTheStreak() {
        val result = run(
            StreakEntity(currentStreak = 3, longestStreak = 3, lastCompletedDate = today.minusDays(3), perfectDaysCount = 3),
            tasksOn = { emptyList() },
            logsOn = { emptyList() }
        )
        assertEquals(3, result.currentStreak)
        assertEquals(3, result.perfectDaysCount)
        assertEquals(yesterday, result.lastCompletedDate)
    }

    @Test
    fun completionsOfDeletedTasks_dontMakeADayPerfect() {
        val result = run(
            StreakEntity(currentStreak = 1, longestStreak = 1, lastCompletedDate = today.minusDays(2)),
            tasksOn = { listOf(task(1)) },          // only task 1 still exists
            logsOn = { date -> listOf(done(99, date)) } // a deleted task's log
        )
        assertEquals(0, result.currentStreak)
    }

    @Test
    fun anAlreadyEvaluatedYesterday_isNotScoredTwice() {
        val start = StreakEntity(currentStreak = 4, longestStreak = 4, lastCompletedDate = yesterday, perfectDaysCount = 4)
        val result = run(start, tasksOn = { listOf(task(1)) }, logsOn = { date -> listOf(done(1, date)) })
        assertEquals(start, result)
    }
}
