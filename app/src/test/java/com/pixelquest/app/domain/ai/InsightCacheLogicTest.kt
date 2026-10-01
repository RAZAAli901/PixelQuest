package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.dao.InsightCacheDao
import com.pixelquest.app.data.local.entity.InsightCacheEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.repository.HabitInsightRepositoryImpl
import com.pixelquest.app.data.repository.InsightCacheRepositoryImpl
import com.pixelquest.app.domain.repository.InsightCacheRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 6: Unit tests verifying AI habit insight caching logic:
 * cache-hit (zero Gemini calls), cache-miss (Gemini invoked and cached),
 * cache-stale (TTL expired, Gemini invoked), and dataHash invalidation.
 */
class InsightCacheLogicTest {

    private class FakeInsightCacheDao : InsightCacheDao {
        val storage = mutableListOf<InsightCacheEntity>()
        private val _flow = MutableStateFlow<InsightCacheEntity?>(null)

        override suspend fun insertInsight(insight: InsightCacheEntity): Long {
            storage.add(insight)
            _flow.value = insight
            return storage.size.toLong()
        }

        override suspend fun getLatestInsight(): InsightCacheEntity? {
            return storage.maxByOrNull { it.generatedAt }
        }

        override fun observeLatestInsight(): Flow<InsightCacheEntity?> {
            return _flow.asStateFlow()
        }

        override suspend fun deleteOld(expiryTimestamp: Long): Int {
            val countBefore = storage.size
            storage.removeAll { it.generatedAt < expiryTimestamp }
            return countBefore - storage.size
        }

        override suspend fun clearCache(): Int {
            val count = storage.size
            storage.clear()
            _flow.value = null
            return count
        }
    }

    private class CallTrackingGeminiClient(
        var responseToReturn: String = """
            {
              "summary": "Mighty adventurer, your consistency burns bright!",
              "suggestion": "Keep your evening study quests disciplined.",
              "encouragement": "Victory awaits you at Level 5!",
              "highlightCategory": "FITNESS",
              "specificTaskCallout": "Morning workout"
            }
        """.trimIndent()
    ) : GeminiClient {
        var callCount = 0

        override suspend fun generateContent(prompt: String, systemInstruction: String?): String {
            callCount++
            return responseToReturn
        }
    }

    private fun createRepository(
        geminiClient: GeminiClient,
        cacheRepository: InsightCacheRepository,
        streak: StreakEntity = StreakEntity(currentStreak = 5, longestStreak = 10, perfectDaysCount = 12)
    ): HabitInsightRepositoryImpl {
        val streakRepo = object : StreakRepository {
            override suspend fun insertStreak(streak: StreakEntity) {}
            override suspend fun updateStreak(streak: StreakEntity) {}
            override fun getCurrentStreak(): Flow<StreakEntity?> = flowOf(streak)
        }

        val profileRepo = object : UserProfileRepository {
            override suspend fun insertProfile(profile: UserProfileEntity) {}
            override suspend fun updateProfile(profile: UserProfileEntity) {}
            override fun getProfile(): Flow<UserProfileEntity?> = flowOf(UserProfileEntity(level = 4))
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

        val settingsRepo = object : SettingsRepository {
            override val aiInsightsEnabled: Flow<Boolean> = flowOf(true)
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

        return HabitInsightRepositoryImpl(
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            settingsRepository = settingsRepo,
            geminiClient = geminiClient,
            insightCacheRepository = cacheRepository,
            toneHook = DefaultHabitInsightToneHook()
        )
    }

    @Test
    fun cacheMiss_callsGeminiAndCachesResult() = runBlocking {
        val fakeDao = FakeInsightCacheDao()
        val cacheRepo = InsightCacheRepositoryImpl(fakeDao, clock = { 1000L })
        val client = CallTrackingGeminiClient()
        val repository = createRepository(client, cacheRepo)

        // 1. Initial call: cache is empty
        val result = repository.generateHabitInsight()

        assertTrue("Result must be Success", result is GeminiResult.Success)
        assertEquals("Gemini client must be called on cache miss", 1, client.callCount)

        // Verify insight was cached in DAO
        val cached = fakeDao.getLatestInsight()
        assertNotNull("Insight must be saved to cache", cached)
        assertEquals("Mighty adventurer, your consistency burns bright!", cached?.summary)
    }

    @Test
    fun cacheHit_returnsCachedInsightWithZeroGeminiCalls() = runBlocking {
        var currentTime = 10_000_000L
        val fakeDao = FakeInsightCacheDao()
        val cacheRepo = InsightCacheRepositoryImpl(fakeDao, clock = { currentTime })
        val client = CallTrackingGeminiClient()
        val repository = createRepository(client, cacheRepo)

        // 1. First call: miss, saves to cache
        repository.generateHabitInsight()
        assertEquals(1, client.callCount)

        // 2. Advance time by 2 hours (well within 12-hour TTL)
        currentTime += 2 * 60 * 60 * 1000L

        // 3. Second call: data has not changed, TTL valid -> Cache HIT
        val secondResult = repository.generateHabitInsight()
        assertTrue(secondResult is GeminiResult.Success)
        assertEquals("Gemini call count must remain 1 on cache hit", 1, client.callCount)

        val insight = (secondResult as GeminiResult.Success).data
        assertEquals("Mighty adventurer, your consistency burns bright!", insight.summary)
    }

    @Test
    fun cacheStale_callsGeminiWhenTtlExpired() = runBlocking {
        var currentTime = 10_000_000L
        val fakeDao = FakeInsightCacheDao()
        val cacheRepo = InsightCacheRepositoryImpl(fakeDao, clock = { currentTime })
        val client = CallTrackingGeminiClient()
        val repository = createRepository(client, cacheRepo)

        // 1. Initial call at T0
        repository.generateHabitInsight()
        assertEquals(1, client.callCount)

        // 2. Advance time by 13 hours (> 12-hour TTL)
        currentTime += 13 * 60 * 60 * 1000L

        // 3. Call again: cache is expired, must refresh from Gemini
        val staleResult = repository.generateHabitInsight()
        assertTrue(staleResult is GeminiResult.Success)
        assertEquals("Gemini client must be called when cache is stale", 2, client.callCount)
    }

    @Test
    fun cacheMiss_whenDataHashChanges_invalidatesCacheBeforeTtl() = runBlocking {
        var currentTime = 10_000_000L
        val fakeDao = FakeInsightCacheDao()
        val cacheRepo = InsightCacheRepositoryImpl(fakeDao, clock = { currentTime })
        val client = CallTrackingGeminiClient()

        // 1. Initial streak = 5
        val repo1 = createRepository(client, cacheRepo, streak = StreakEntity(currentStreak = 5, longestStreak = 10))
        repo1.generateHabitInsight()
        assertEquals(1, client.callCount)

        // 2. Only 30 minutes later, but user completes habit so streak = 6
        currentTime += 30 * 60 * 1000L
        val repo2 = createRepository(client, cacheRepo, streak = StreakEntity(currentStreak = 6, longestStreak = 10))

        // 3. DataHash has changed -> Cache should invalidate despite fresh timestamp
        val updatedResult = repo2.generateHabitInsight()
        assertTrue(updatedResult is GeminiResult.Success)
        assertEquals("Gemini must be called when dataHash changes", 2, client.callCount)
    }

    @Test
    fun insightCacheRepository_isCacheValid_evaluatesCorrectly() = runBlocking {
        var currentTime = 5000L
        val fakeDao = FakeInsightCacheDao()
        val cacheRepo = InsightCacheRepositoryImpl(fakeDao, clock = { currentTime })

        // Empty cache
        assertFalse(cacheRepo.isCacheValid("hash_1", 10000L))

        // Save entry with hash_1 at T = 5000
        cacheRepo.saveInsight(
            HabitInsightResponse("Summary", "Suggestion", "Encouragement", generatedAt = 5000L),
            "hash_1"
        )

        // Valid: matching hash, age = 1000 ms (< 5000 ms)
        currentTime = 6000L
        assertTrue(cacheRepo.isCacheValid("hash_1", 5000L))

        // Invalid: mismatched hash
        assertFalse(cacheRepo.isCacheValid("hash_2", 5000L))

        // Invalid: expired (age = 6000 ms > 5000 ms TTL)
        currentTime = 12000L
        assertFalse(cacheRepo.isCacheValid("hash_1", 5000L))
    }
}
