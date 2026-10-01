package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.dao.InsightCacheDao
import com.pixelquest.app.data.local.entity.InsightCacheEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.repository.HabitInsightRepositoryImpl
import com.pixelquest.app.data.repository.InsightCacheRepositoryImpl
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 9: Unit tests verifying rate-limit enforcement across boundary conditions:
 * initial call, rapid re-invocation, mid-cooldown, boundary at 1 second before expiry,
 * boundary at expiry + 1ms, and non-lockout on failures.
 */
class AiRateLimitEnforcementTest {

    private class FakeInsightCacheDao : InsightCacheDao {
        val storage = mutableListOf<InsightCacheEntity>()
        private val _flow = MutableStateFlow<InsightCacheEntity?>(null)

        override suspend fun insertInsight(insight: InsightCacheEntity): Long {
            storage.add(insight)
            _flow.value = insight
            return storage.size.toLong()
        }

        override suspend fun getLatestInsight(): InsightCacheEntity? = storage.maxByOrNull { it.generatedAt }
        override fun observeLatestInsight(): Flow<InsightCacheEntity?> = _flow.asStateFlow()
        override suspend fun deleteOld(expiryTimestamp: Long): Int = 0
        override suspend fun clearCache(): Int = 0
    }

    private class CallTrackingGeminiClient : GeminiClient {
        var callCount = 0
        var shouldThrowNetworkError = false

        override suspend fun generateContent(prompt: String, systemInstruction: String?): String {
            if (shouldThrowNetworkError) {
                throw java.io.IOException("Simulated network failure")
            }
            callCount++
            return """
                {
                  "summary": "Great momentum on your active quests!",
                  "suggestion": "Stay focused on daily discipline.",
                  "encouragement": "Victory awaits you, hero!",
                  "highlightCategory": "FITNESS",
                  "specificTaskCallout": "Morning workout"
                }
            """.trimIndent()
        }
    }

    private class TestSettingsRepository : SettingsRepository {
        val aiEnabledFlow = MutableStateFlow(true)
        val lastTimestampFlow = MutableStateFlow(0L)

        override val aiInsightsEnabled: Flow<Boolean> = aiEnabledFlow
        override val lastAiInsightTimestamp: Flow<Long> = lastTimestampFlow

        override suspend fun setAiInsightsEnabled(enabled: Boolean) {
            aiEnabledFlow.value = enabled
        }

        override suspend fun setLastAiInsightTimestamp(timestamp: Long) {
            lastTimestampFlow.value = timestamp
        }

        override val simpleModeEnabled: Flow<Boolean> = flowOf(false)
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

    @Test
    fun rateLimit_enforcesAcrossAllBoundaryConditions() = runBlocking {
        var simulatedNow = 1_000_000_000L // Baseline epoch time
        val settingsRepo = TestSettingsRepository()
        val fakeDao = FakeInsightCacheDao()
        val cacheRepo = InsightCacheRepositoryImpl(fakeDao, clock = { simulatedNow })
        val client = CallTrackingGeminiClient()

        val streakRepo = object : StreakRepository {
            override suspend fun insertStreak(streak: StreakEntity) {}
            override suspend fun updateStreak(streak: StreakEntity) {}
            override fun getCurrentStreak(): Flow<StreakEntity?> = flowOf(StreakEntity(currentStreak = 3))
        }

        val profileRepo = object : UserProfileRepository {
            override suspend fun insertProfile(profile: UserProfileEntity) {}
            override suspend fun updateProfile(profile: UserProfileEntity) {}
            override fun getProfile(): Flow<UserProfileEntity?> = flowOf(UserProfileEntity(level = 2))
        }

        val taskRepo = object : TaskRepository {
            override suspend fun insertTask(task: com.pixelquest.app.data.local.entity.TaskEntity): Long = 1L
            override suspend fun updateTask(task: com.pixelquest.app.data.local.entity.TaskEntity) {}
            override suspend fun deleteTask(task: com.pixelquest.app.data.local.entity.TaskEntity) {}
            override fun getAllTasks(): Flow<List<com.pixelquest.app.data.local.entity.TaskEntity>> = flowOf(emptyList())
            override fun getTaskById(taskId: Long): Flow<com.pixelquest.app.data.local.entity.TaskEntity?> = flowOf(null)
            override fun getTasksForDay(day: java.time.LocalDate): Flow<List<com.pixelquest.app.data.local.entity.TaskEntity>> = flowOf(emptyList())
        }

        val completionRepo = object : TaskCompletionRepository {
            override suspend fun insertLog(log: com.pixelquest.app.data.local.entity.TaskCompletionLogEntity): Long = 1L
            override fun getLogsForDate(date: java.time.LocalDate): Flow<List<com.pixelquest.app.data.local.entity.TaskCompletionLogEntity>> = flowOf(emptyList())
            override fun getLogsForTask(taskId: Long): Flow<List<com.pixelquest.app.data.local.entity.TaskCompletionLogEntity>> = flowOf(emptyList())
            override fun getCompletionHistory(startDate: java.time.LocalDate, endDate: java.time.LocalDate): Flow<List<com.pixelquest.app.data.local.entity.TaskCompletionLogEntity>> = flowOf(emptyList())
            override fun getAllLogs(): Flow<List<com.pixelquest.app.data.local.entity.TaskCompletionLogEntity>> = flowOf(emptyList())
        }

        val repository = HabitInsightRepositoryImpl(
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            settingsRepository = settingsRepo,
            geminiClient = client,
            insightCacheRepository = cacheRepo,
            toneHook = DefaultHabitInsightToneHook(),
            clock = { simulatedNow }
        )

        // 1. Initial invocation (no prior calls, cooldown = 0)
        assertEquals("Initial cooldown should be 0", 0L, repository.getRemainingCooldownSeconds())
        val firstResult = repository.generateHabitInsight()
        assertTrue("First call must succeed", firstResult is GeminiResult.Success)
        assertEquals("Gemini called once", 1, client.callCount)
        assertEquals(simulatedNow, settingsRepo.lastTimestampFlow.value)

        // 2. Immediate rapid request with forceRefresh (10 seconds later)
        simulatedNow += 10_000L
        val rapidResult = repository.generateHabitInsight(forceRefresh = true)
        assertTrue("Rapid call must be RateLimited", rapidResult is GeminiResult.RateLimited)
        val rateLimited1 = rapidResult as GeminiResult.RateLimited
        val expectedCooldown1 = (HabitInsightRepositoryImpl.MIN_CALL_INTERVAL_MS - 10_000L) / 1000L
        assertEquals(expectedCooldown1, rateLimited1.retryAfterSeconds)
        assertEquals("Check back in 6 hours for a fresh insight.", rateLimited1.message)
        assertEquals("Gemini call count must remain 1", 1, client.callCount)

        // 3. Mid-cooldown request (3 hours later)
        simulatedNow += (3 * 3600 * 1000L) - 10_000L // Exactly T + 3 hours
        val midResult = repository.generateHabitInsight(forceRefresh = true)
        assertTrue("Call at 3 hours must be RateLimited", midResult is GeminiResult.RateLimited)
        val rateLimited2 = midResult as GeminiResult.RateLimited
        assertEquals("Check back in 3 hours for a fresh insight.", rateLimited2.message)
        assertEquals(1, client.callCount)

        // 4. Boundary condition: 1 second before expiry (T + 5 hours 59 mins 59 seconds)
        simulatedNow = 1_000_000_000L + HabitInsightRepositoryImpl.MIN_CALL_INTERVAL_MS - 1000L
        val boundaryBeforeResult = repository.generateHabitInsight(forceRefresh = true)
        assertTrue("Call 1 second before expiry must be RateLimited", boundaryBeforeResult is GeminiResult.RateLimited)
        val rateLimited3 = boundaryBeforeResult as GeminiResult.RateLimited
        assertEquals(1L, rateLimited3.retryAfterSeconds)
        assertEquals("Check back in 1 seconds for a fresh insight.", rateLimited3.message)
        assertEquals(1, client.callCount)

        // 5. Boundary condition: Exactly at expiry + 10 ms (T + 6 hours + 10 ms)
        simulatedNow = 1_000_000_000L + HabitInsightRepositoryImpl.MIN_CALL_INTERVAL_MS + 10L
        assertEquals("Cooldown must be 0 after expiry", 0L, repository.getRemainingCooldownSeconds())
        val afterExpiryResult = repository.generateHabitInsight(forceRefresh = true)
        assertTrue("Call after expiry must succeed", afterExpiryResult is GeminiResult.Success)
        assertEquals("Gemini call count must now increment to 2", 2, client.callCount)
        assertEquals(simulatedNow, settingsRepo.lastTimestampFlow.value)
    }

    @Test
    fun rateLimit_doesNotLockOutOnNetworkFailure() = runBlocking {
        var simulatedNow = 2_000_000_000L
        val settingsRepo = TestSettingsRepository()
        val fakeDao = FakeInsightCacheDao()
        val cacheRepo = InsightCacheRepositoryImpl(fakeDao, clock = { simulatedNow })
        val client = CallTrackingGeminiClient()
        client.shouldThrowNetworkError = true

        val streakRepo = object : StreakRepository {
            override suspend fun insertStreak(streak: StreakEntity) {}
            override suspend fun updateStreak(streak: StreakEntity) {}
            override fun getCurrentStreak(): Flow<StreakEntity?> = flowOf(StreakEntity())
        }

        val profileRepo = object : UserProfileRepository {
            override suspend fun insertProfile(profile: UserProfileEntity) {}
            override suspend fun updateProfile(profile: UserProfileEntity) {}
            override fun getProfile(): Flow<UserProfileEntity?> = flowOf(UserProfileEntity())
        }

        val taskRepo = object : TaskRepository {
            override suspend fun insertTask(task: com.pixelquest.app.data.local.entity.TaskEntity): Long = 1L
            override suspend fun updateTask(task: com.pixelquest.app.data.local.entity.TaskEntity) {}
            override suspend fun deleteTask(task: com.pixelquest.app.data.local.entity.TaskEntity) {}
            override fun getAllTasks(): Flow<List<com.pixelquest.app.data.local.entity.TaskEntity>> = flowOf(emptyList())
            override fun getTaskById(taskId: Long): Flow<com.pixelquest.app.data.local.entity.TaskEntity?> = flowOf(null)
            override fun getTasksForDay(day: java.time.LocalDate): Flow<List<com.pixelquest.app.data.local.entity.TaskEntity>> = flowOf(emptyList())
        }

        val completionRepo = object : TaskCompletionRepository {
            override suspend fun insertLog(log: com.pixelquest.app.data.local.entity.TaskCompletionLogEntity): Long = 1L
            override fun getLogsForDate(date: java.time.LocalDate): Flow<List<com.pixelquest.app.data.local.entity.TaskCompletionLogEntity>> = flowOf(emptyList())
            override fun getLogsForTask(taskId: Long): Flow<List<com.pixelquest.app.data.local.entity.TaskCompletionLogEntity>> = flowOf(emptyList())
            override fun getCompletionHistory(startDate: java.time.LocalDate, endDate: java.time.LocalDate): Flow<List<com.pixelquest.app.data.local.entity.TaskCompletionLogEntity>> = flowOf(emptyList())
            override fun getAllLogs(): Flow<List<com.pixelquest.app.data.local.entity.TaskCompletionLogEntity>> = flowOf(emptyList())
        }

        val repository = HabitInsightRepositoryImpl(
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            settingsRepository = settingsRepo,
            geminiClient = client,
            insightCacheRepository = cacheRepo,
            toneHook = DefaultHabitInsightToneHook(),
            clock = { simulatedNow }
        )

        // First call fails with NetworkError
        val failResult = repository.generateHabitInsight()
        assertTrue(failResult is GeminiResult.NetworkError)
        assertEquals("Last timestamp must remain 0 on failure", 0L, settingsRepo.lastTimestampFlow.value)

        // Retry 5 seconds later should NOT be throttled
        simulatedNow += 5000L
        client.shouldThrowNetworkError = false
        val retryResult = repository.generateHabitInsight()
        assertTrue("Retry after network fix must succeed without rate limit lockout", retryResult is GeminiResult.Success)
        assertEquals(1, client.callCount)
    }
}
