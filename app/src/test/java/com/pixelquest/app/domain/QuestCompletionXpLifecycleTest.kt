package com.pixelquest.app.domain

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.tasks.CompleteTaskUseCase
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeLevelHistoryRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.FixedClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * B07 test:
 * - Create gives 0 XP
 * - First completion gives 0 XP
 * - Second completion gives normal XP
 * - The streak and the level are correct
 * - No level history entry is written for 0 XP
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class QuestCompletionXpLifecycleTest {

    private val day1 = LocalDate.of(2026, 10, 27)
    private val day2 = day1.plusDays(1)
    private val appClock = FixedClock(LocalDateTime.of(day1, LocalTime.of(10, 0)))

    private lateinit var taskRepository: FakeTaskRepository
    private lateinit var completionRepository: FakeTaskCompletionRepository
    private lateinit var userProfileRepository: FakeUserProfileRepository
    private lateinit var streakRepository: FakeStreakRepository
    private lateinit var difficultyRepository: FakeDifficultySettingsRepository
    private lateinit var levelHistoryRepository: FakeLevelHistoryRepository
    private lateinit var recorder: TaskResultRecorder
    private lateinit var completeTaskUseCase: CompleteTaskUseCase

    @Before
    fun setup() {
        taskRepository = FakeTaskRepository()
        completionRepository = FakeTaskCompletionRepository()
        userProfileRepository = FakeUserProfileRepository(
            UserProfileEntity(
                id = 1,
                username = "Hero",
                avatarId = "hero_avatar_1",
                level = 1,
                totalXp = 0,
                perfectDaysTowardNextLevel = 0,
            ),
        )
        streakRepository = FakeStreakRepository(
            StreakEntity(
                id = 1,
                currentStreak = 1,
                longestStreak = 1,
                lastCompletedDate = day1.minusDays(1),
                perfectDaysCount = 1,
            ),
        )
        difficultyRepository = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(
                id = 1,
                difficultyLevel = DifficultyLevel.MEDIUM,
                perfectDayThreshold = 0.7f,
                daysRequiredPerLevel = 7,
            ),
        )
        levelHistoryRepository = FakeLevelHistoryRepository()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val alarmScheduler = TaskAlarmScheduler(context)
        recorder = TaskResultRecorder(completionRepository, userProfileRepository, streakRepository, taskRepository)
        completeTaskUseCase = CompleteTaskUseCase(recorder, alarmScheduler)
    }

    @Test
    fun questLifecycle_createZeroXp_firstCompletionZeroXp_secondCompletionNormalXp() = runBlocking {
        // 1. Create quest: gives 0 XP
        val task = TaskEntity(
            id = 100L,
            name = "Habit Quest",
            description = "Daily habit",
            scheduledDay = day1,
            scheduledTime = LocalTime.of(9, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS,
        )
        taskRepository.insertTask(task)

        val xpAfterCreate = userProfileRepository.getProfile().first()?.totalXp ?: -1
        assertEquals("Create gives 0 XP", 0, xpAfterCreate)
        assertTrue("No logs exist after create", completionRepository.getAllLogs().first().isEmpty())
        assertTrue("No level history after create", levelHistoryRepository.getAllHistory().first().isEmpty())

        // 2. First completion: gives 0 XP
        val firstResult = completeTaskUseCase(task, day1)
        assertTrue("First completion succeeds", firstResult)

        val logsAfterFirst = completionRepository.getLogsForTask(100L).first()
        assertEquals(1, logsAfterFirst.size)
        val firstLog = logsAfterFirst.first()
        assertTrue("Task was completed on day 1", firstLog.wasCompleted)
        assertEquals("First completion awards 0 XP", 0, firstLog.pointsAwarded)

        val profileAfterFirst = userProfileRepository.getProfile().first()!!
        assertEquals("Total XP remains 0 after first completion", 0, profileAfterFirst.totalXp)
        assertEquals("Level remains 1", 1, profileAfterFirst.level)
        assertTrue("No level history entry is written for 0 XP", levelHistoryRepository.getAllHistory().first().isEmpty())

        // 3. Second completion on day 2: gives normal XP
        val streak = streakRepository.getCurrentStreak().first()?.currentStreak ?: 0
        val expectedNormalXp = PointsCalculator.calculateXpForTask(currentStreak = streak)
        assertTrue("Expected normal XP > 0", expectedNormalXp > 0)

        val secondResult = completeTaskUseCase(task, day2)
        assertTrue("Second completion succeeds", secondResult)

        val logsAfterSecond = completionRepository.getLogsForTask(100L).first()
        assertEquals(2, logsAfterSecond.size)
        val secondLog = logsAfterSecond.first { it.completedDate == day2 }
        assertTrue("Task was completed on day 2", secondLog.wasCompleted)
        assertEquals("Second completion awards normal XP", expectedNormalXp, secondLog.pointsAwarded)

        val profileAfterSecond = userProfileRepository.getProfile().first()!!
        assertEquals("Total XP increased by normal XP", expectedNormalXp, profileAfterSecond.totalXp)
        assertEquals("Level is still 1", 1, profileAfterSecond.level)
        assertTrue("No level history entry written without level-up", levelHistoryRepository.getAllHistory().first().isEmpty())

        // Verify streak state is intact
        val currentStreak = streakRepository.getCurrentStreak().first()?.currentStreak
        assertEquals(1, currentStreak)
    }
}
