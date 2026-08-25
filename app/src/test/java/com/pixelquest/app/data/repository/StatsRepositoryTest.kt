package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class StatsRepositoryTest {

    private lateinit var statsRepository: StatsRepositoryImpl

    private val startDate = LocalDate.of(2026, 8, 1)
    private val endDate = LocalDate.of(2026, 8, 5)

    private val sampleTask1 = TaskEntity(
        id = 1L,
        name = "Daily Quest 1",
        description = "Test Description",
        scheduledDay = LocalDate.of(2026, 8, 1),
        scheduledTime = LocalTime.of(9, 0),
        recurrenceType = RecurrenceType.DAILY,
        category = TaskCategory.FITNESS,
        isActive = true
    )

    private val sampleTask2 = TaskEntity(
        id = 2L,
        name = "Daily Quest 2",
        description = "Test Description 2",
        scheduledDay = LocalDate.of(2026, 8, 1),
        scheduledTime = LocalTime.of(10, 0),
        recurrenceType = RecurrenceType.DAILY,
        category = TaskCategory.HEALTH,
        isActive = true
    )

    private val fakeTaskRepo = object : TaskRepository {
        var tasksList = listOf(sampleTask1, sampleTask2)
        override fun getAllTasks(): Flow<List<TaskEntity>> = flowOf(tasksList)
        override fun getTaskById(id: Long): Flow<TaskEntity?> = flowOf(tasksList.find { it.id == id })
        override suspend fun insertTask(task: TaskEntity): Long = 1L
        override suspend fun updateTask(task: TaskEntity) {}
        override suspend fun deleteTask(task: TaskEntity) {}
        override fun getTasksForDay(day: LocalDate): Flow<List<TaskEntity>> = flowOf(tasksList)
    }

    private val fakeCompletionLogs = mutableListOf<TaskCompletionLogEntity>()

    private val fakeCompletionRepo = object : TaskCompletionRepository {
        override suspend fun insertLog(log: TaskCompletionLogEntity): Long {
            fakeCompletionLogs.add(log)
            return log.id
        }
        override fun getLogsForDate(date: LocalDate): Flow<List<TaskCompletionLogEntity>> =
            flowOf(fakeCompletionLogs.filter { it.completedDate == date })
        override fun getLogsForTask(taskId: Long): Flow<List<TaskCompletionLogEntity>> =
            flowOf(fakeCompletionLogs.filter { it.taskId == taskId })
        override fun getCompletionHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskCompletionLogEntity>> =
            flowOf(fakeCompletionLogs.filter { !it.completedDate.isBefore(startDate) && !it.completedDate.isAfter(endDate) })
        override fun getAllLogs(): Flow<List<TaskCompletionLogEntity>> =
            flowOf(fakeCompletionLogs)
    }

    private var currentDifficulty = DifficultyLevel.MEDIUM

    private val fakeDifficultyRepo = object : DifficultySettingsRepository {
        override fun getCurrentDifficulty(): Flow<DifficultySettingsEntity?> =
            flowOf(DifficultySettingsEntity(id = 1, difficultyLevel = currentDifficulty))
        override suspend fun updateDifficulty(difficulty: DifficultySettingsEntity) {}
        override suspend fun insertDifficulty(difficulty: DifficultySettingsEntity): Long = 1L
    }

    private val fakeStreakRepo = object : StreakRepository {
        override fun getCurrentStreak(): Flow<StreakEntity?> = flowOf(StreakEntity(id = 1, currentStreak = 3, longestStreak = 5))
        override suspend fun updateStreak(streak: StreakEntity) {}
        override suspend fun insertStreak(streak: StreakEntity): Long = 1L
    }

    private val fakeProfileRepo = object : UserProfileRepository {
        override fun getProfile(): Flow<UserProfileEntity?> = flowOf(UserProfileEntity(id = 1, username = "Hero", totalXp = 100))
        override suspend fun updateProfile(profile: UserProfileEntity) {}
        override suspend fun insertProfile(profile: UserProfileEntity): Long = 1L
    }

    @Before
    fun setup() {
        fakeCompletionLogs.clear()
        currentDifficulty = DifficultyLevel.MEDIUM
        statsRepository = StatsRepositoryImpl(
            taskCompletionRepository = fakeCompletionRepo,
            streakRepository = fakeStreakRepo,
            userProfileRepository = fakeProfileRepo,
            taskRepository = fakeTaskRepo,
            difficultySettingsRepository = fakeDifficultyRepo
        )
    }

    @Test
    fun getCompletionRateOverRange_noLogs_returnsZero() = runTest {
        val rate = statsRepository.getCompletionRateOverRange(startDate, endDate).first()
        assertEquals(0f, rate, 0.001f)
    }

    @Test
    fun getCompletionRateOverRange_partialCompletions_returnsCorrectPercentage() = runTest {
        // 5 days x 2 tasks = 10 scheduled tasks total.
        // Complete 5 tasks.
        fakeCompletionLogs.add(TaskCompletionLogEntity(taskId = 1, completedDate = LocalDate.of(2026, 8, 1), wasCompleted = true, pointsAwarded = 10))
        fakeCompletionLogs.add(TaskCompletionLogEntity(taskId = 2, completedDate = LocalDate.of(2026, 8, 1), wasCompleted = true, pointsAwarded = 10))
        fakeCompletionLogs.add(TaskCompletionLogEntity(taskId = 1, completedDate = LocalDate.of(2026, 8, 2), wasCompleted = true, pointsAwarded = 10))
        fakeCompletionLogs.add(TaskCompletionLogEntity(taskId = 2, completedDate = LocalDate.of(2026, 8, 2), wasCompleted = false, pointsAwarded = 0))

        val rate = statsRepository.getCompletionRateOverRange(startDate, endDate).first()
        // 3 completed / 10 scheduled = 0.30 (30%)
        assertEquals(0.3f, rate, 0.001f)
    }

    @Test
    fun getDailyStatusForRange_evaluatesStatusCorrectly() = runTest {
        // Day 1: 2/2 completed -> PERFECT (Medium threshold = 70%)
        fakeCompletionLogs.add(TaskCompletionLogEntity(taskId = 1, completedDate = LocalDate.of(2026, 8, 1), wasCompleted = true, pointsAwarded = 10))
        fakeCompletionLogs.add(TaskCompletionLogEntity(taskId = 2, completedDate = LocalDate.of(2026, 8, 1), wasCompleted = true, pointsAwarded = 10))

        // Day 2: 1/2 completed -> 50% < 70% -> PARTIAL
        fakeCompletionLogs.add(TaskCompletionLogEntity(taskId = 1, completedDate = LocalDate.of(2026, 8, 2), wasCompleted = true, pointsAwarded = 10))
        fakeCompletionLogs.add(TaskCompletionLogEntity(taskId = 2, completedDate = LocalDate.of(2026, 8, 2), wasCompleted = false, pointsAwarded = 0))

        // Day 3: 0/2 completed -> MISSED
        fakeCompletionLogs.add(TaskCompletionLogEntity(taskId = 1, completedDate = LocalDate.of(2026, 8, 3), wasCompleted = false, pointsAwarded = 0))
        fakeCompletionLogs.add(TaskCompletionLogEntity(taskId = 2, completedDate = LocalDate.of(2026, 8, 3), wasCompleted = false, pointsAwarded = 0))

        val statusMap = statsRepository.getDailyStatusForRange(startDate, endDate).first()

        assertEquals(DailyStatus.PERFECT, statusMap[LocalDate.of(2026, 8, 1)])
        assertEquals(DailyStatus.PARTIAL, statusMap[LocalDate.of(2026, 8, 2)])
        assertEquals(DailyStatus.MISSED, statusMap[LocalDate.of(2026, 8, 3)])
    }

    @Test
    fun getDailyStatusForRange_zeroScheduledTasks_returnsNoTasksScheduled() = runTest {
        fakeTaskRepo.tasksList = emptyList()
        val statusMap = statsRepository.getDailyStatusForRange(startDate, endDate).first()
        assertEquals(DailyStatus.NO_TASKS_SCHEDULED, statusMap[startDate])
    }
}
