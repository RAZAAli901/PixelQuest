package com.pixelquest.app.data.repository

import com.pixelquest.app.data.remote.GeminiApiException
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiNetworkException
import com.pixelquest.app.data.remote.GeminiRateLimitException
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.domain.ai.AiUsagePolicy
import com.pixelquest.app.domain.ai.InMemoryAiUsageTracker
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every call Gemini answers counts toward PixelQuest's daily and monthly AI caps, even when the answer
 * can't be used. Before Day 29 only a usable insight was counted, so a reply that kept failing to parse
 * cost a live call on every visit to Today with no cap.
 */
class InsightUsageCountingTest {

    private val validInsight = """
        {"summary":"Steady week.","suggestion":"Keep the morning quest.","encouragement":"Onward!"}
    """.trimIndent()

    private val usage = InMemoryAiUsageTracker(AiUsagePolicy.MAX_CALLS_PER_DAY, AiUsagePolicy.MAX_CALLS_PER_MONTH)
    private val settings = FakeSettingsRepository(aiInsights = true)

    private fun repository(answer: () -> String) = HabitInsightRepositoryImpl(
        streakRepository = FakeStreakRepository(),
        userProfileRepository = FakeUserProfileRepository(),
        taskRepository = FakeTaskRepository(),
        taskCompletionRepository = FakeTaskCompletionRepository(),
        settingsRepository = settings,
        geminiClient = object : GeminiClient {
            override suspend fun generateContent(prompt: String, systemInstruction: String?): String = answer()
        },
        usageTracker = usage
    )

    @Test
    fun anUnreadableAnswer_countsTowardTheCaps_butLeavesRetryOpen() = runBlocking {
        val repo = repository { "the model rambled instead of answering in JSON" }

        val result = repo.generateHabitInsight()

        assertTrue(result is GeminiResult.MalformedResponse)
        assertEquals(1, usage.getDailyCallsCount())
        // No 6-hour cooldown after a failure: the player can try again.
        assertEquals(0L, repo.getRemainingCooldownSeconds())
    }

    @Test
    fun answersThatKeepFailing_stopAtTheDailyCap() = runBlocking {
        var liveCalls = 0
        val repo = repository { liveCalls++; "not json" }

        repeat(AiUsagePolicy.MAX_CALLS_PER_DAY + 3) { repo.generateHabitInsight(forceRefresh = true) }

        assertEquals(AiUsagePolicy.MAX_CALLS_PER_DAY, liveCalls)
        assertTrue(repo.generateHabitInsight(forceRefresh = true) is GeminiResult.RateLimited)
    }

    @Test
    fun anEmptyAnswer_counts() = runBlocking {
        val repo = repository { throw GeminiApiException(200, "Empty text in candidate (finishReason MAX_TOKENS)") }

        assertTrue(repo.generateHabitInsight() is GeminiResult.ApiError)
        assertEquals(1, usage.getDailyCallsCount())
    }

    @Test
    fun callsThatNeverGotAnAnswer_doNotCount() = runBlocking {
        repository { throw GeminiNetworkException("offline", java.io.IOException("no route")) }.generateHabitInsight()
        repository { throw GeminiRateLimitException("Gemini rate limit exceeded (HTTP 429).") }.generateHabitInsight()

        assertEquals(0, usage.getDailyCallsCount())
    }

    @Test
    fun aUsableAnswer_countsOnce_andStartsTheCooldown() = runBlocking {
        val repo = repository { validInsight }

        assertTrue(repo.generateHabitInsight() is GeminiResult.Success)
        assertEquals(1, usage.getDailyCallsCount())
        assertTrue(repo.getRemainingCooldownSeconds() > 0L)
    }
}
