package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.system.measureTimeMillis

class StatsPerformanceTest {

    @Test
    fun aggregationPerformance_sixMonthsDataset_executesFast() = runTest {
        val today = LocalDate.now()
        val startDate = today.minusMonths(6)

        val tasksList = (1..5).map { taskId ->
            TaskEntity(
                id = taskId.toLong(),
                name = "Quest #$taskId",
                description = "Simulated quest",
                scheduledDay = startDate,
                scheduledTime = LocalTime.of(8 + taskId, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.FITNESS
            )
        }

        val fakeLogs = mutableListOf<TaskCompletionLogEntity>()
        var curr = startDate
        var logId = 1L
        while (!curr.isAfter(today)) {
            tasksList.forEach { task ->
                fakeLogs.add(
                    TaskCompletionLogEntity(
                        id = logId++,
                        taskId = task.id,
                        completedDate = curr,
                        wasCompleted = (logId % 3L != 0L),
                        pointsAwarded = 10
                    )
                )
            }
            curr = curr.plusDays(1)
        }

        val fakeTaskRepo = object : TaskRepository {
            override fun getAllTasks(): Flow<List<TaskEntity>> = flowOf(tasksList)
            override fun getTaskById(id: Long): Flow<TaskEntity?> = flowOf(tasksList.find { it.id == id })
            override suspend fun insertTask(task: TaskEntity): Long = 1L
            override suspend fun updateTask(task: TaskEntity) {}
            override suspend fun deleteTask(task: TaskEntity) {}
            override fun getTasksForDay(day: LocalDate): Flow<List<TaskEntity>> = flowOf(tasksList)
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
                flowOf(DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM))
            override suspend fun updateSettings(difficulty: DifficultySettingsEntity) {}
            override suspend fun insertSettings(difficulty: DifficultySettingsEntity) {}
        }

        val fakeStreakRepo = object : StreakRepository {
            override fun getCurrentStreak(): Flow<StreakEntity?> = flowOf(StreakEntity(id = 1, currentStreak = 10, longestStreak = 20))
            override suspend fun updateStreak(streak: StreakEntity) {}
            override suspend fun insertStreak(streak: StreakEntity) {}
        }

        val fakeProfileRepo = object : UserProfileRepository {
            override fun getProfile(): Flow<UserProfileEntity?> = flowOf(UserProfileEntity(avatarId = "avatar_hero", id = 1, username = "Hero", totalXp = 5000))
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

        val timeTaken = measureTimeMillis {
            val dailyStatusMap = statsRepo.getDailyStatusForRange(startDate, today).first()
            val rate = statsRepo.getCompletionRateOverRange(startDate, today).first()
            assertTrue(dailyStatusMap.isNotEmpty())
            assertTrue(rate > 0f)
        }

        // Verify aggregation completes within 250ms for 900+ logs
        assertTrue("Aggregation took too long: ${timeTaken}ms", timeTaken < 250)
    }
}
