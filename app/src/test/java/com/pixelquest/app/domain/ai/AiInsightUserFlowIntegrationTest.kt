package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.entity.InsightCacheEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.repository.HabitInsightRepositoryImpl
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.HabitInsightRepository
import com.pixelquest.app.domain.repository.InsightCacheRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.ui.screens.insight.AiInsightUiState
import com.pixelquest.app.ui.screens.insight.AiInsightViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Step 34: Integration test for the full user-facing flow:
 * enable AI insights -> view screen -> see generated insight -> verify caching prevents duplicate Gemini call on re-visit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiInsightUserFlowIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private class RecordingMockGeminiClient(
        var responseToReturn: String
    ) : GeminiClient {
        var callCount: Int = 0

        override suspend fun generateContent(prompt: String, systemInstruction: String?): String {
            callCount++
            return responseToReturn
        }
    }

    private class FakeInsightCacheRepository : InsightCacheRepository {
        var cachedEntity: InsightCacheEntity? = null
        val cacheFlow = MutableStateFlow<InsightCacheEntity?>(null)

        override suspend fun getLatestInsight(): InsightCacheEntity? = cachedEntity

        override fun observeLatestInsight(): Flow<InsightCacheEntity?> = cacheFlow

        override suspend fun saveInsight(insight: HabitInsightResponse, dataHash: String): Long {
            val entity = InsightCacheEntity(
                id = 1L,
                generatedAt = insight.generatedAt,
                summary = insight.summary,
                suggestion = insight.suggestion,
                encouragement = insight.encouragement,
                dataHash = dataHash
            )
            cachedEntity = entity
            cacheFlow.value = entity
            return 1L
        }

        override suspend fun deleteOldInsights(cutoffTimestamp: Long): Int {
            return 0
        }

        override suspend fun clearAll() {
            cachedEntity = null
            cacheFlow.value = null
        }

        override suspend fun isCacheValid(currentDataHash: String, ttlMillis: Long, nowMillis: Long): Boolean {
            val current = cachedEntity ?: return false
            if (current.dataHash != currentDataHash) return false
            return (nowMillis - current.generatedAt) < ttlMillis
        }
    }

    private class FakeSettingsRepository(
        initialAiEnabled: Boolean = false,
        initialSimpleMode: Boolean = false
    ) : SettingsRepository {
        val aiEnabledFlow = MutableStateFlow(initialAiEnabled)
        val simpleModeFlow = MutableStateFlow(initialSimpleMode)
        val lastTimestampFlow = MutableStateFlow(0L)

        override val aiInsightsEnabled: Flow<Boolean> = aiEnabledFlow
        override val simpleModeEnabled: Flow<Boolean> = simpleModeFlow
        override val lastAiInsightTimestamp: Flow<Long> = lastTimestampFlow

        override suspend fun setAiInsightsEnabled(enabled: Boolean) {
            aiEnabledFlow.value = enabled
        }

        override suspend fun setSimpleModeEnabled(enabled: Boolean) {
            simpleModeFlow.value = enabled
        }

        override suspend fun setLastAiInsightTimestamp(timestamp: Long) {
            lastTimestampFlow.value = timestamp
        }

        override val isSoundEnabled: Flow<Boolean> = flowOf(true)
        override val isCrtEnabled: Flow<Boolean> = flowOf(false)
        override val isHapticsEnabled: Flow<Boolean> = flowOf(true)
        override val isReduceMotionEnabled: Flow<Boolean> = flowOf(false)
        override val onboardingComplete: Flow<Boolean> = flowOf(true)
        override val isNotificationsEnabled: Flow<Boolean> = flowOf(true)
        override val isNotificationSoundEnabled: Flow<Boolean> = flowOf(true)
        override val isNotificationVibrationEnabled: Flow<Boolean> = flowOf(true)
        override suspend fun setSoundEnabled(enabled: Boolean) {}
        override suspend fun setCrtEnabled(enabled: Boolean) {}
        override suspend fun setHapticsEnabled(enabled: Boolean) {}
        override suspend fun setReduceMotionEnabled(enabled: Boolean) {}
        override suspend fun setOnboardingComplete(complete: Boolean) {}
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setNotificationSoundEnabled(enabled: Boolean) {}
        override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {}
    }

    private class FakeStreakRepository(private val streak: StreakEntity?) : StreakRepository {
        override fun getCurrentStreak(): Flow<StreakEntity?> = flowOf(streak)
        override suspend fun getStreakOnce(): StreakEntity? = streak
        override suspend fun updateStreak(streak: StreakEntity) {}
        override suspend fun incrementStreak(today: LocalDate) {}
        override suspend fun resetStreak() {}
    }

    private class FakeUserProfileRepository(private val profile: UserProfileEntity?) : UserProfileRepository {
        override fun getProfile(): Flow<UserProfileEntity?> = flowOf(profile)
        override suspend fun getProfileOnce(): UserProfileEntity? = profile
        override suspend fun updateUsername(username: String) {}
        override suspend fun updateAvatar(avatarId: String) {}
        override suspend fun addXp(points: Int) {}
        override suspend fun levelUp() {}
        override suspend fun updateDifficulty(difficulty: com.pixelquest.app.domain.model.DifficultyLevel) {}
    }

    private class FakeTaskRepository(private val tasks: List<TaskEntity>) : TaskRepository {
        override fun getAllTasks(): Flow<List<TaskEntity>> = flowOf(tasks)
        override fun getActiveTasks(): Flow<List<TaskEntity>> = flowOf(tasks)
        override suspend fun getTaskById(taskId: Long): TaskEntity? = tasks.find { it.id == taskId }
        override suspend fun insertTask(task: TaskEntity): Long = task.id
        override suspend fun updateTask(task: TaskEntity) {}
        override suspend fun deleteTask(task: TaskEntity) {}
    }

    private class FakeTaskCompletionRepository(private val logs: List<TaskCompletionLogEntity>) : TaskCompletionRepository {
        override fun getLogsForDate(date: LocalDate): Flow<List<TaskCompletionLogEntity>> = flowOf(logs.filter { it.completedDate == date })
        override fun getLogsForTask(taskId: Long): Flow<List<TaskCompletionLogEntity>> = flowOf(logs.filter { it.taskId == taskId })
        override fun getAllLogs(): Flow<List<TaskCompletionLogEntity>> = flowOf(logs)
        override suspend fun insertCompletionLog(log: TaskCompletionLogEntity): Long = log.id
        override suspend fun getCompletionCountForDate(date: LocalDate): Int = logs.count { it.completedDate == date && it.wasCompleted }
    }

    private var simulatedTime: Long = 1_000_000_000_000L
    private val timeProvider: () -> Long = { simulatedTime }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun fullUserFacingFlow_optIn_generateInsight_andVerifyCachePreventsDuplicateGeminiCall() = runTest {
        // 1. Initial Setup: Opted-out user with realistic habit history
        val mockGemini = RecordingMockGeminiClient(
            responseToReturn = """
                {
                    "summary": "You've crushed 7 days in a row! Momentum is blazing high.",
                    "suggestion": "Schedule your morning meditation right after coffee to lock in consistency.",
                    "encouragement": "Legendary hero status unlocked! The tavern bards will sing of this streak!"
                }
            """.trimIndent()
        )

        val settingsRepo = FakeSettingsRepository(initialAiEnabled = false)
        val cacheRepo = FakeInsightCacheRepository()
        val usageTracker = InMemoryAiUsageTracker()

        val streakEntity = StreakEntity(id = 1, currentStreak = 7, longestStreak = 14, perfectDaysCount = 10)
        val profileEntity = UserProfileEntity(id = 1, username = "PixelKnight", avatarId = "warrior", currentLevel = 3, totalXp = 350)
        val tasks = listOf(
            TaskEntity(id = 1, title = "Morning Run", scheduledTime = LocalTime.of(7, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS),
            TaskEntity(id = 2, title = "Code Practice", scheduledTime = LocalTime.of(19, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.LEARNING)
        )
        val today = LocalDate.of(2026, 10, 1)
        val completionLogs = (0..6).map { dayOffset ->
            TaskCompletionLogEntity(
                id = dayOffset.toLong() + 1,
                taskId = 1,
                completedDate = today.minusDays(dayOffset.toLong()),
                completedTime = LocalTime.of(7, 30),
                wasCompleted = true,
                pointsAwarded = 25
            )
        }

        val habitInsightRepository = HabitInsightRepositoryImpl(
            streakRepository = FakeStreakRepository(streakEntity),
            userProfileRepository = FakeUserProfileRepository(profileEntity),
            taskRepository = FakeTaskRepository(tasks),
            taskCompletionRepository = FakeTaskCompletionRepository(completionLogs),
            settingsRepository = settingsRepo,
            geminiClient = mockGemini,
            insightCacheRepository = cacheRepo,
            usageTracker = usageTracker,
            clock = timeProvider
        )

        // Step 1: User opens AI screen while disabled
        val viewModel = AiInsightViewModel(
            habitInsightRepository = habitInsightRepository,
            settingsRepository = settingsRepo,
            taskCompletionRepository = FakeTaskCompletionRepository(completionLogs),
            insightCacheRepository = cacheRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify screen state is Disabled and Gemini was NEVER called
        assertTrue("State must be Disabled when opt-in is false", viewModel.uiState.value is AiInsightUiState.Disabled)
        assertEquals(0, mockGemini.callCount)

        // Step 2: User enables AI Habit Insights in Settings
        settingsRepo.setAiInsightsEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Step 3: User opens/refreshes AI Insight screen
        viewModel.loadInsight(forceRefresh = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify successful insight generation
        val stateAfterOptIn = viewModel.uiState.value
        assertTrue("State must be Success after opting in", stateAfterOptIn is AiInsightUiState.Success)
        val successState = stateAfterOptIn as AiInsightUiState.Success
        assertFalse("First fetch should be fresh, not cached", successState.isCached)
        assertEquals("You've crushed 7 days in a row! Momentum is blazing high.", successState.insight.summary)
        assertEquals(1, mockGemini.callCount)

        // Verify insight was cached in Room
        assertNotNull(cacheRepo.cachedEntity)
        assertEquals(successState.insight.summary, cacheRepo.cachedEntity!!.summary)

        // Step 4: User immediately re-visits the screen (e.g. Navigates away and returns)
        val secondViewModel = AiInsightViewModel(
            habitInsightRepository = habitInsightRepository,
            settingsRepository = settingsRepo,
            taskCompletionRepository = FakeTaskCompletionRepository(completionLogs),
            insightCacheRepository = cacheRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify caching immediately served the response WITHOUT calling Gemini again!
        val revisitState = secondViewModel.uiState.value
        assertTrue("Revisit state must be Success", revisitState is AiInsightUiState.Success)
        val revisitSuccess = revisitState as AiInsightUiState.Success
        assertTrue("Revisit must be served from cache", revisitSuccess.isCached)
        assertEquals("You've crushed 7 days in a row! Momentum is blazing high.", revisitSuccess.insight.summary)
        assertEquals("Gemini call count must remain 1 on immediate revisit", 1, mockGemini.callCount)

        // Step 5: User attempts manual refresh while cooldown is active
        secondViewModel.refreshInsight()
        testDispatcher.scheduler.advanceUntilIdle()

        val rateLimitedState = secondViewModel.uiState.value
        assertTrue("Manual refresh during cooldown must transition to RateLimited", rateLimitedState is AiInsightUiState.RateLimited)
        val rl = rateLimitedState as AiInsightUiState.RateLimited
        assertTrue("Cooldown message must guide user", rl.message.contains("hours") || rl.message.contains("Check back"))
        assertEquals("Last insight must be preserved during cooldown", "You've crushed 7 days in a row! Momentum is blazing high.", rl.lastInsight?.summary)
        assertEquals("Gemini call count must still be 1 (blocked by cooldown)", 1, mockGemini.callCount)

        // Step 6: Advance time past 6-hour rate-limit window
        simulatedTime += 6 * 3600 * 1000L + 1000L // 6 hours + 1 second
        assertEquals(0L, habitInsightRepository.getRemainingCooldownSeconds())

        // Re-open screen normally: data has not changed and TTL is 12h, so cache hit still occurs!
        secondViewModel.loadInsight(forceRefresh = false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Cache hit still occurs because dataHash has not changed", 1, mockGemini.callCount)

        // Force refresh past cooldown: cache is bypassed, new Gemini call is permitted
        mockGemini.responseToReturn = """
            {
                "summary": "Updated day 8 perspective: fantastic streak!",
                "suggestion": "Stay hydrated before every quest.",
                "encouragement": "Victory awaits tomorrow!"
            }
        """.trimIndent()

        secondViewModel.refreshInsight()
        testDispatcher.scheduler.advanceUntilIdle()

        val refreshedState = secondViewModel.uiState.value
        assertTrue("Refreshed state must be Success", refreshedState is AiInsightUiState.Success)
        val refreshedSuccess = refreshedState as AiInsightUiState.Success
        assertEquals("Updated day 8 perspective: fantastic streak!", refreshedSuccess.insight.summary)
        assertEquals("Gemini call count must now be 2 after explicit refresh past cooldown", 2, mockGemini.callCount)
    }
}
