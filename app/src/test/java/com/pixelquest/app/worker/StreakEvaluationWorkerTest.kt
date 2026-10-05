package com.pixelquest.app.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeLevelHistoryRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/** The nightly check scores yesterday against the difficulty's perfect-day threshold. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StreakEvaluationWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val yesterday = LocalDate.now().minusDays(1)

    private val tasks = FakeTaskRepository()
    private val logs = FakeTaskCompletionRepository()
    // Medium: 70% of yesterday's quests make a perfect day.
    private val difficulty = FakeDifficultySettingsRepository(
        DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, perfectDayThreshold = 0.7f, daysRequiredPerLevel = 7)
    )
    private val streaks = FakeStreakRepository(
        StreakEntity(id = 1, currentStreak = 5, longestStreak = 10, lastCompletedDate = yesterday.minusDays(1))
    )

    private fun questsYesterday(count: Int) = (1L..count).forEach { id ->
        runBlocking {
            tasks.insertTask(
                TaskEntity(
                    id = id, name = "Quest $id", description = "", scheduledDay = yesterday.minusDays(7),
                    scheduledTime = LocalTime.of(9, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
                )
            )
        }
    }

    private fun completed(vararg ids: Long) = runBlocking {
        ids.forEach { logs.insertLog(TaskCompletionLogEntity(taskId = it, completedDate = yesterday, wasCompleted = true, pointsAwarded = 50)) }
    }

    private fun runWorker(): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<StreakEvaluationWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    StreakEvaluationWorker(
                        appContext, workerParameters, tasks, logs, streaks, difficulty,
                        FakeUserProfileRepository(), FakeLevelHistoryRepository(), LevelUpSignalManager(appContext)
                    )
            })
            .build()
            .doWork()
    }

    @Test
    fun doWork_allTasksMissed_resetsCurrentStreakToZero() {
        questsYesterday(2)

        assertEquals(ListenableWorker.Result.success(), runWorker())

        val streak = streaks.streak.value!!
        assertEquals(0, streak.currentStreak)
        assertEquals(10, streak.longestStreak)
        assertEquals(yesterday, streak.lastCompletedDate)
    }

    @Test
    fun doWork_exactThreshold_incrementsStreak() {
        questsYesterday(10)
        completed(1, 2, 3, 4, 5, 6, 7) // exactly 70%

        runWorker()

        assertEquals(6, streaks.streak.value!!.currentStreak)
    }

    @Test
    fun doWork_secondCallSameDate_isIdempotent() {
        questsYesterday(1)
        completed(1)

        runWorker()
        runWorker()

        assertEquals(6, streaks.streak.value!!.currentStreak)
    }
}
