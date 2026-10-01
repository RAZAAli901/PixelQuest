package com.pixelquest.app.qa

import com.pixelquest.app.data.local.dao.InsightCacheDao
import com.pixelquest.app.data.local.entity.InsightCacheEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.repository.HabitInsightRepositoryImpl
import com.pixelquest.app.data.repository.InsightCacheRepositoryImpl
import com.pixelquest.app.domain.ai.DefaultHabitInsightToneHook
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 10: Manual QA verification test simulating rapid repeated insight requests.
 * Verifies that spamming requests in rapid succession correctly hits the 6-hour rate limit
 * and makes zero duplicate network calls to Gemini.
 */
class Day25RateLimitManualQaTest {

    private class SpammedMockGeminiClient : GeminiClient {
        var networkDispatches = 0

        override suspend fun generateContent(prompt: String, systemInstruction: String?): String {
            networkDispatches++
            return """
                {
                  "summary": "You have sustained an exceptional habit streak.",
                  "suggestion": "Maintain regular bedtime schedule.",
                  "encouragement": "Every day is progress toward mastery.",
                  "highlightCategory": "HEALTH",
                  "specificTaskCallout": "Hydration habit"
                }
            """.trimIndent()
        }
    }

    private class MemoryCacheDao : InsightCacheDao {
        val list = mutableListOf<InsightCacheEntity>()
        val flow = MutableStateFlow<InsightCacheEntity?>(null)
        override suspend fun insertInsight(insight: InsightCacheEntity): Long {
            list.add(insight)
            flow.value = insight
            return list.size.toLong()
        }
        override suspend fun getLatestInsight(): InsightCacheEntity? = list.lastOrNull()
        override fun observeLatestInsight(): Flow<InsightCacheEntity?> = flow.asStateFlow()
        override suspend fun deleteOld(expiryTimestamp: Long): Int = 0
        override suspend fun clearCache(): Int = 0
    }

    @Test
    fun manualQa_rapidSpammedRequests_stopsAtRateLimitWithSingleNetworkCall() = runBlocking {
        var simulatedClock = 500_000_000L
        val client = SpammedMockGeminiClient()
        val dao = MemoryCacheDao()
        val cacheRepo = InsightCacheRepositoryImpl(dao, clock = { simulatedClock })

        val lastTimestampFlow = MutableStateFlow(0L)
        val settingsRepo = object : SettingsRepository {
            override val aiInsightsEnabled: Flow<Boolean> = flowOf(true)
            override val lastAiInsightTimestamp: Flow<Long> = lastTimestampFlow
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

        val streakRepo = object : StreakRepository {
            override suspend fun insertStreak(streak: StreakEntity) {}
            override suspend fun updateStreak(streak: StreakEntity) {}
            override fun getCurrentStreak(): Flow<StreakEntity?> = flowOf(StreakEntity(currentStreak = 7))
        }

        val profileRepo = object : UserProfileRepository {
            override suspend fun insertProfile(profile: UserProfileEntity) {}
            override suspend fun updateProfile(profile: UserProfileEntity) {}
            override fun getProfile(): Flow<UserProfileEntity?> = flowOf(UserProfileEntity(level = 3))
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
            clock = { simulatedClock }
        )

        // Rapid tap sequence: 10 calls in 1 second
        val results = mutableListOf<GeminiResult<*>>()
        for (i in 1..10) {
            simulatedClock += 100L // 100ms between taps
            val res = repository.generateHabitInsight(forceRefresh = (i > 1))
            results.add(res)
        }

        // Tap 1 should succeed
        assertTrue("Tap 1 must succeed", results[0] is GeminiResult.Success)

        // Taps 2 through 10 must ALL be RateLimited
        for (i in 1..9) {
            val res = results[i]
            assertTrue("Tap ${i + 1} must be RateLimited", res is GeminiResult.RateLimited)
            val rateLimited = res as GeminiResult.RateLimited
            assertTrue("Message informs user of cooldown", rateLimited.message.contains("hours") || rateLimited.message.contains("minutes"))
        }

        // CRITICAL CHECK: Exactly 1 network call made to Gemini despite 10 requests
        assertEquals("Gemini network calls must be capped at 1", 1, client.networkDispatches)

        // Verify latest insight remains intact
        val latest = repository.latestInsight.first()
        assertNotNull("Latest insight must not be null", latest)
        assertEquals("You have sustained an exceptional habit streak.", latest?.summary)
    }
}
