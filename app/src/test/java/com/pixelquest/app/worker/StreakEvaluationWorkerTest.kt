package com.pixelquest.app.worker

import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.ui.FakeTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class FakeStreakRepository : StreakRepository {
    private val streakState = MutableStateFlow<StreakEntity?>(
        StreakEntity(id = 1, currentStreak = 2, longestStreak = 5, perfectDaysCount = 10)
    )

    override fun getCurrentStreak(): Flow<StreakEntity?> = streakState

    override suspend fun insertStreak(streak: StreakEntity): Long {
        streakState.value = streak
        return 1L
    }

    override suspend fun updateStreak(streak: StreakEntity) {
        streakState.value = streak
    }
}

class FakeDifficultyRepository : DifficultySettingsRepository {
    private val diffState = MutableStateFlow<DifficultySettingsEntity?>(
        DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, perfectDayThreshold = 0.7f)
    )

    override fun getCurrentDifficulty(): Flow<DifficultySettingsEntity?> = diffState

    override suspend fun insertDifficultySettings(settings: DifficultySettingsEntity): Long {
        diffState.value = settings
        return 1L
    }

    override suspend fun updateDifficultySettings(settings: DifficultySettingsEntity) {
        diffState.value = settings
    }
}

@RunWith(RobolectricTestRunner::class)
class StreakEvaluationWorkerTest {

    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeCompletionRepo: FakeTaskCompletionRepository
    private lateinit var fakeStreakRepo: FakeStreakRepository
    private lateinit var fakeDiffRepo: FakeDifficultyRepository

    @Before
    fun setUp() {
        fakeTaskRepo = FakeTaskRepository()
        fakeCompletionRepo = FakeTaskCompletionRepository()
        fakeStreakRepo = FakeStreakRepository()
        fakeDiffRepo = FakeDifficultyRepository()
    }

    @Test
    fun doWork_allTasksMissed_resetsCurrentStreakToZero() = runBlocking {
        val yesterday = LocalDate.now().minusDays(1)
        fakeTaskRepo.insertTask(
            TaskEntity(
                id = 1,
                name = "Missed",
                description = "",
                scheduledDay = yesterday,
                scheduledTime = LocalTime.of(9, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.FITNESS
            )
        )
        fakeCompletionRepo.logTaskCompletion(
            TaskCompletionLogEntity(taskId = 1, completedAt = LocalDateTime.of(yesterday, LocalTime.of(10, 0)), wasCompleted = false)
        )

        val params = mock(WorkerParameters::class.java)
        val worker = StreakEvaluationWorker(
            ApplicationProvider.getApplicationContext(),
            params,
            fakeTaskRepo,
            fakeCompletionRepo,
            fakeStreakRepo,
            fakeDiffRepo
        )

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)

        val streak = fakeStreakRepo.getCurrentStreak().first()
        assertEquals(0, streak?.currentStreak)
        assertEquals(5, streak?.longestStreak) // preserved!
    }

    @Test
    fun doWork_exactThreshold_incrementsStreak() = runBlocking {
        val yesterday = LocalDate.now().minusDays(1)
        // 1 task, completed -> 100% >= 70% threshold
        fakeTaskRepo.insertTask(
            TaskEntity(
                id = 1,
                name = "Completed",
                description = "",
                scheduledDay = yesterday,
                scheduledTime = LocalTime.of(9, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.FITNESS
            )
        )
        fakeCompletionRepo.logTaskCompletion(
            TaskCompletionLogEntity(taskId = 1, completedAt = LocalDateTime.of(yesterday, LocalTime.of(10, 0)), wasCompleted = true)
        )

        val params = mock(WorkerParameters::class.java)
        val worker = StreakEvaluationWorker(
            ApplicationProvider.getApplicationContext(),
            params,
            fakeTaskRepo,
            fakeCompletionRepo,
            fakeStreakRepo,
            fakeDiffRepo
        )

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)

        val streak = fakeStreakRepo.getCurrentStreak().first()
        assertEquals(3, streak?.currentStreak) // 2 + 1 = 3
    }

    @Test
    fun doWork_secondCallSameDate_isIdempotent() = runBlocking {
        val yesterday = LocalDate.now().minusDays(1)
        fakeStreakRepo.updateStreak(
            StreakEntity(id = 1, currentStreak = 5, longestStreak = 5, lastCompletedDate = yesterday)
        )

        val params = mock(WorkerParameters::class.java)
        val worker = StreakEvaluationWorker(
            ApplicationProvider.getApplicationContext(),
            params,
            fakeTaskRepo,
            fakeCompletionRepo,
            fakeStreakRepo,
            fakeDiffRepo
        )

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)

        val streak = fakeStreakRepo.getCurrentStreak().first()
        assertEquals(5, streak?.currentStreak) // did not increment again!
    }
}

