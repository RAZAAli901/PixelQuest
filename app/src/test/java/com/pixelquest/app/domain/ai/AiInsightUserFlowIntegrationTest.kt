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

    private class FakeInsightCacheRepository(private val now: () -> Long) : InsightCacheRepository {
        var cachedEntity: InsightCacheEntity? = null
        val cacheFlow = MutableStateFlow<InsightCacheEntity?>(null)

        override suspend fun getLatestInsight(): InsightCacheEntity? = cachedEntity

        override fun observeLatestInsight(): Flow<InsightCacheEntity?> = cacheFlow

        override suspend fun saveInsight(response: HabitInsightResponse, dataHash: String) {
            val entity = InsightCacheEntity.fromInsightResponse(response, dataHash).copy(id = 1L, generatedAt = now())
            cachedEntity = entity
            cacheFlow.value = entity
        }

        override suspend fun isCacheValid(currentDataHash: String, maxAgeMillis: Long): Boolean {
            val current = cachedEntity ?: return false
            if (current.dataHash != currentDataHash) return false
            return (now() - current.generatedAt) in 0..maxAgeMillis
        }

        override suspend fun clearExpired(maxAgeMillis: Long): Int = 0

        override suspend fun clearAll(): Int {
            val had = if (cachedEntity != null) 1 else 0
            cachedEntity = null
            cacheFlow.value = null
            return had
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
        val cacheRepo = FakeInsightCacheRepository(timeProvider)
        val usageTracker = InMemoryAiUsageTracker()

        val streakEntity = StreakEntity(id = 1, currentStreak = 7, longestStreak = 14, perfectDaysCount = 10)
        val profileEntity = UserProfileEntity(id = 1, username = "PixelKnight", avatarId = "warrior", level = 3, totalXp = 350)
        val tasks = listOf(
            TaskEntity(id = 1, name = "Morning Run", description = "", scheduledDay = LocalDate.of(2026, 9, 1), scheduledTime = LocalTime.of(7, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS),
            TaskEntity(id = 2, name = "Code Practice", description = "", scheduledDay = LocalDate.of(2026, 9, 1), scheduledTime = LocalTime.of(19, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.LEARNING)
        )
        val today = LocalDate.of(2026, 10, 1)
        val completionLogs = (0..6).map { dayOffset ->
            TaskCompletionLogEntity(
                id = dayOffset.toLong() + 1,
                taskId = 1,
                completedDate = today.minusDays(dayOffset.toLong()),
                wasCompleted = true,
                pointsAwarded = 25
            )
        }

        val habitInsightRepository = HabitInsightRepositoryImpl(
            streakRepository = com.pixelquest.app.testing.FakeStreakRepository(streakEntity),
            userProfileRepository = com.pixelquest.app.testing.FakeUserProfileRepository(profileEntity),
            taskRepository = com.pixelquest.app.testing.FakeTaskRepository(tasks),
            taskCompletionRepository = com.pixelquest.app.testing.FakeTaskCompletionRepository(completionLogs),
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
            taskCompletionRepository = com.pixelquest.app.testing.FakeTaskCompletionRepository(completionLogs),
            insightCacheRepository = cacheRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify screen state is Disabled and Gemini was NEVER called
        assertTrue("State must be Disabled when opt-in is false", viewModel.uiState.value is AiInsightUiState.Disabled)
        assertEquals(0, mockGemini.callCount)

        // Step 2: User enables AI Habit Insights in Settings. The open screen reloads by itself
        // (Day 26), so this is the first, fresh fetch.
        settingsRepo.setAiInsightsEnabled(true)
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
            taskCompletionRepository = com.pixelquest.app.testing.FakeTaskCompletionRepository(completionLogs),
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
