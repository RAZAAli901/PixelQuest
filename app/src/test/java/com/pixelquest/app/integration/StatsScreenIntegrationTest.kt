package com.pixelquest.app.integration

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.repository.StatsRepositoryImpl
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.ui.screens.stats.StatsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class StatsScreenIntegrationTest {

    // StatsViewModel computes in viewModelScope (Dispatchers.Main) while its state is collected.
    @org.junit.Before
    fun setUp() = kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.UnconfinedTestDispatcher())

    @org.junit.After
    fun tearDown() = kotlinx.coroutines.Dispatchers.resetMain()

    @Test
    fun statsScreen_endToEndMultiWeekDataset_reconcilesMetricsWithRawLogs() = runTest {
        val today = LocalDate.now()
        val startDate = today.minusDays(21)

        val task1 = TaskEntity(
            id = 100L,
            name = "Quest 1",
            description = "Desc",
            scheduledDay = startDate,
            scheduledTime = LocalTime.of(8, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS
        )

        val fakeLogs = mutableListOf<TaskCompletionLogEntity>()
        // 22 days total. Complete 15 days, miss 7 days.
        (0..21).forEach { dayOffset ->
            val date = startDate.plusDays(dayOffset.toLong())
            val wasCompleted = dayOffset % 3 != 0
            fakeLogs.add(
                TaskCompletionLogEntity(
                    id = (dayOffset + 1).toLong(),
                    taskId = 100L,
                    completedDate = date,
                    wasCompleted = wasCompleted,
                    pointsAwarded = if (wasCompleted) 10 else 0
                )
            )
        }

        val fakeTaskRepo = object : TaskRepository {
            override fun getAllTasks(): Flow<List<TaskEntity>> = flowOf(listOf(task1))
            override fun getTaskById(id: Long): Flow<TaskEntity?> = flowOf(task1)
            override suspend fun insertTask(task: TaskEntity): Long = 1L
            override suspend fun updateTask(task: TaskEntity) {}
            override suspend fun deleteTask(task: TaskEntity) {}
            override fun getTasksForDay(day: LocalDate): Flow<List<TaskEntity>> = flowOf(listOf(task1))
        }

        val fakeCompletionRepo = object : TaskCompletionRepository {
            override suspend fun insertLog(log: TaskCompletionLogEntity): Long = 1L
            override fun getLogsForDate(date: LocalDate): Flow<List<TaskCompletionLogEntity>> =
                flowOf(fakeLogs.filter { it.completedDate == date })
            override fun getLogsForTask(taskId: Long): Flow<List<TaskCompletionLogEntity>> =
                flowOf(fakeLogs.filter { it.taskId == taskId })
            override fun getCompletionHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskCompletionLogEntity>> =
                flowOf(fakeLogs.filter { !it.completedDate.isBefore(startDate) && !it.completedDate.isAfter(endDate) })
            override fun getAllLogs(): Flow<List<TaskCompletionLogEntity>> = flowOf(fakeLogs)
            override suspend fun updateLog(log: TaskCompletionLogEntity) {}
            override suspend fun getLogForTaskOnDate(taskId: Long, date: LocalDate): TaskCompletionLogEntity? = null
        }

        val fakeDiffRepo = object : DifficultySettingsRepository {
            override fun getCurrentDifficulty(): Flow<DifficultySettingsEntity?> =
                flowOf(DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.EASY))
            override suspend fun updateSettings(difficulty: DifficultySettingsEntity) {}
            override suspend fun insertSettings(difficulty: DifficultySettingsEntity) {}
        }

        val fakeStreakRepo = object : StreakRepository {
            override fun getCurrentStreak(): Flow<StreakEntity?> = flowOf(StreakEntity(id = 1, currentStreak = 5, longestStreak = 12))
            override suspend fun updateStreak(streak: StreakEntity) {}
            override suspend fun insertStreak(streak: StreakEntity) {}
        }

        val fakeProfileRepo = object : UserProfileRepository {
            override fun getProfile(): Flow<UserProfileEntity?> = flowOf(UserProfileEntity(avatarId = "avatar_hero", id = 1, username = "Hero", totalXp = 850))
            override suspend fun updateProfile(profile: UserProfileEntity) {}
            override suspend fun insertProfile(profile: UserProfileEntity) {}
            override suspend fun performLevelUp(): UserProfileEntity? = null
            override suspend fun updateSupabaseUserId(userId: String?) {}
            override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) {}
            override suspend fun updateLeaderboardOptIn(optIn: Boolean) {}
            override suspend fun clearCloudData() {}
        }

        val statsRepo = StatsRepositoryImpl(
            taskCompletionRepository = fakeCompletionRepo,
            streakRepository = fakeStreakRepo,
            userProfileRepository = fakeProfileRepo,
            taskRepository = fakeTaskRepo,
            difficultySettingsRepository = fakeDiffRepo
        )

        val viewModel = StatsViewModel(
            statsRepository = statsRepo,
            streakRepository = fakeStreakRepo,
            userProfileRepository = fakeProfileRepo,
            difficultySettingsRepository = fakeDiffRepo,
            appClock = com.pixelquest.app.testing.FixedClock(today.atTime(12, 0))
        )

        // The first value is the empty placeholder; wait for the computed stats.
        val state = viewModel.uiState.first { it.heatmapStatusMap.isNotEmpty() }

        assertEquals(5, state.currentStreak)
        assertEquals(12, state.longestStreak)
        assertEquals(850, state.totalPoints)
        assertEquals(DifficultyLevel.EASY, state.difficultyLevel)
        assertNotNull(state.heatmapStatusMap[today])
        assertEquals(4, state.weeklyTrend.size)
    }
}
