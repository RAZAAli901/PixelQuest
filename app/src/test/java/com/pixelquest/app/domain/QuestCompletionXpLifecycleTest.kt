package com.pixelquest.app.domain

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
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
import org.junit.Assert.assertFalse
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
 * B06 & B07, V028:
 * - Quest creation gives 0 XP across all categories and repetition types
 * - First completion gives 0 XP
 * - Subsequent completions give streak-scaled XP
 * - Double completion on same day gives 0 extra XP and fails gracefully
 * - Clock moved back handles completions deterministically without duplicate XP
 * - Level boundaries across multiple days and levels (progress resets strictly to 0)
 * - Mid-progress difficulty switch evaluation at level boundary
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

    @Test
    fun creation_acrossMultipleCategoriesAndRecurrences_alwaysAwardsZeroXp() = runBlocking {
        val categories = listOf(
            TaskCategory.FITNESS,
            TaskCategory.HEALTH,
            TaskCategory.LEARNING,
            TaskCategory.CHORES,
            TaskCategory.OTHER,
        )
        val recurrences = listOf(
            RecurrenceType.DAILY,
            RecurrenceType.WEEKLY,
            RecurrenceType.MONTHLY,
            RecurrenceType.ONE_TIME,
        )

        var taskIdCounter = 200L
        for (category in categories) {
            for (recurrence in recurrences) {
                val task = TaskEntity(
                    id = taskIdCounter++,
                    name = "Task $taskIdCounter",
                    description = "Test description",
                    scheduledDay = day1,
                    scheduledTime = LocalTime.of(11, 0),
                    recurrenceType = recurrence,
                    category = category,
                )
                taskRepository.insertTask(task)
            }
        }

        val profile = userProfileRepository.getProfile().first()!!
        assertEquals("Creating tasks across categories and recurrences must award 0 XP", 0, profile.totalXp)
        assertTrue("No completion logs should be generated upon creation", completionRepository.getAllLogs().first().isEmpty())
    }

    @Test
    fun doubleCompletion_sameDay_returnsAlreadyCompleted_noDoubleXp() = runBlocking {
        val task = TaskEntity(
            id = 300L,
            name = "Double Completion Quest",
            description = "",
            scheduledDay = day1,
            scheduledTime = LocalTime.of(8, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.HEALTH,
        )
        taskRepository.insertTask(task)

        // Seed a prior completion on day 1 so subsequent completions award XP
        completionRepository.insertLog(
            TaskCompletionLogEntity(
                taskId = task.id,
                completedDate = day1,
                pointsAwarded = 0,
                wasCompleted = true,
            ),
        )

        val streak = streakRepository.getCurrentStreak().first()?.currentStreak ?: 0
        val normalXp = PointsCalculator.calculateXpForTask(currentStreak = streak)

        // First completion on day 2
        val firstCompletionSuccess = completeTaskUseCase(task, day2)
        assertTrue("First completion on day 2 should succeed", firstCompletionSuccess)
        val profileAfterFirst = userProfileRepository.getProfile().first()!!
        assertEquals(normalXp, profileAfterFirst.totalXp)

        // Duplicate second completion on the same day 2
        val secondCompletionSuccess = completeTaskUseCase(task, day2)
        assertFalse("Duplicate completion on same day must return false", secondCompletionSuccess)

        val profileAfterSecond = userProfileRepository.getProfile().first()!!
        assertEquals("Total XP must NOT double after second completion", normalXp, profileAfterSecond.totalXp)

        val logsForDay2 = completionRepository.getLogsForTask(task.id).first().filter { it.completedDate == day2 }
        assertEquals("Exactly one log must exist for task on day 2", 1, logsForDay2.size)
    }

    @Test
    fun clockMovedBack_handlesCompletionsDeterministically() = runBlocking {
        val task = TaskEntity(
            id = 400L,
            name = "Clock Move Quest",
            description = "",
            scheduledDay = day1,
            scheduledTime = LocalTime.of(12, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.LEARNING,
        )
        taskRepository.insertTask(task)

        // Seed prior completion on an even earlier day so that day 1 and day 2 completions earn normal XP
        val earlierDay = day1.minusDays(2)
        completionRepository.insertLog(
            TaskCompletionLogEntity(
                taskId = task.id,
                completedDate = earlierDay,
                pointsAwarded = 0,
                wasCompleted = true,
            ),
        )

        // Complete on day 2
        val day2Success = completeTaskUseCase(task, day2)
        assertTrue(day2Success)
        val xpDay2 = userProfileRepository.getProfile().first()!!.totalXp
        assertTrue(xpDay2 > 0)

        // Device clock or date is moved backwards to day 1.
        // Task on day 1 was not yet completed. Completing day 1 now should succeed and earn XP.
        val day1Success = completeTaskUseCase(task, day1)
        assertTrue("Completing past uncompleted day should succeed", day1Success)
        val xpDay1After = userProfileRepository.getProfile().first()!!.totalXp
        assertTrue("Total XP should increase when completing past uncompleted day", xpDay1After > xpDay2)

        // Attempting to re-complete day 1 again while clock is in the past fails
        val day1DuplicateSuccess = completeTaskUseCase(task, day1)
        assertFalse("Duplicate completion on past day must fail", day1DuplicateSuccess)
        assertEquals(xpDay1After, userProfileRepository.getProfile().first()!!.totalXp)

        // Day 2 completion log remains intact
        val day2Log = completionRepository.getLogForTaskOnDate(task.id, day2)
        assertTrue(day2Log?.wasCompleted == true)
    }

    @Test
    fun levelBoundaries_acrossMultipleDaysAndLevels_resetsProgressStrictlyToZero() = runBlocking {
        // Difficulty: Medium requires 7 perfect days per level
        val daysRequired = 7
        assertEquals(daysRequired, DifficultyMode.daysRequiredPerLevel(difficultyRepository.getCurrentDifficulty().first()))

        var currentLevel = 1
        var currentProgress = 0

        // Days 1 through 6: perfect days advance progress, no level-up
        for (day in 1..6) {
            currentProgress++
            val shouldLevel = LevelCalculator.shouldLevelUp(currentProgress, daysRequired)
            assertFalse("Progress $currentProgress / $daysRequired should not trigger level-up", shouldLevel)
            userProfileRepository.setLevelProgress(currentLevel, currentProgress)
        }

        var profile = userProfileRepository.getProfile().first()!!
        assertEquals("Level remains 1 before boundary", 1, profile.level)
        assertEquals("Progress is 6 before boundary", 6, profile.perfectDaysTowardNextLevel)
        assertTrue(levelHistoryRepository.getAllHistory().first().isEmpty())

        // Day 7: Hits exact boundary (7/7) -> level up to Level 2!
        currentProgress++
        val shouldLevelUpDay7 = LevelCalculator.shouldLevelUp(currentProgress, daysRequired)
        assertTrue("Progress $currentProgress / $daysRequired must trigger level-up", shouldLevelUpDay7)

        currentLevel++
        currentProgress = LevelCalculator.getPostLevelUpProgress()
        assertEquals("Post-level-up progress must reset strictly to 0", 0, currentProgress)

        userProfileRepository.setLevelProgress(currentLevel, currentProgress)
        levelHistoryRepository.insertLevelHistory(
            LevelHistoryEntity(
                level = currentLevel,
                achievedDate = 1000L,
                difficultyAtTimeOfLevelUp = "MEDIUM",
            ),
        )

        profile = userProfileRepository.getProfile().first()!!
        assertEquals("Level is now 2", 2, profile.level)
        assertEquals("Progress is reset to 0", 0, profile.perfectDaysTowardNextLevel)
        assertEquals("One level history record exists", 1, levelHistoryRepository.getAllHistory().first().size)

        // Days 8 through 13: 6 more perfect days toward Level 3
        for (day in 8..13) {
            currentProgress++
            val shouldLevel = LevelCalculator.shouldLevelUp(currentProgress, daysRequired)
            assertFalse(shouldLevel)
            userProfileRepository.setLevelProgress(currentLevel, currentProgress)
        }

        // Day 14: Hits boundary again (7/7) -> level up to Level 3!
        currentProgress++
        val shouldLevelUpDay14 = LevelCalculator.shouldLevelUp(currentProgress, daysRequired)
        assertTrue(shouldLevelUpDay14)

        currentLevel++
        currentProgress = LevelCalculator.getPostLevelUpProgress()
        userProfileRepository.setLevelProgress(currentLevel, currentProgress)
        levelHistoryRepository.insertLevelHistory(
            LevelHistoryEntity(
                level = currentLevel,
                achievedDate = 2000L,
                difficultyAtTimeOfLevelUp = "MEDIUM",
            ),
        )

        profile = userProfileRepository.getProfile().first()!!
        assertEquals("Level is now 3", 3, profile.level)
        assertEquals("Progress is reset to 0", 0, profile.perfectDaysTowardNextLevel)
        assertEquals("Two level history records exist", 2, levelHistoryRepository.getAllHistory().first().size)
    }

    @Test
    fun levelBoundaries_midProgressDifficultySwitch_evaluatedImmediately() = runBlocking {
        // User starts with 3 perfect days on Medium (which requires 7 days)
        val mediumDaysRequired = 7
        val easyDaysRequired = 3
        val currentProgress = 3

        // Under Medium: 3 / 7 does not level up
        assertFalse(LevelCalculator.shouldLevelUp(currentProgress, mediumDaysRequired))

        // When switching to Easy (target becomes 3 days):
        // 3 days already meets or exceeds the new target
        val shouldLevelUpOnSwitch = LevelCalculator.evaluateLevelProgressOnDifficultySwitch(
            currentProgress = currentProgress,
            newDaysRequired = easyDaysRequired,
        )
        assertTrue("Switching to Easy when having 3 perfect days must trigger level-up", shouldLevelUpOnSwitch)
    }
}
