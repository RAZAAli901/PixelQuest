package com.pixelquest.app.data.repository

import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.remote.safeGeminiCall
import com.pixelquest.app.domain.ai.DefaultHabitInsightToneHook
import com.pixelquest.app.domain.ai.HabitInsightPromptBuilder
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.domain.ai.HabitInsightToneHook
import com.pixelquest.app.domain.repository.HabitInsightRepository
import com.pixelquest.app.domain.repository.InsightCacheRepository
import com.pixelquest.app.domain.repository.NoOpInsightCacheRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

class HabitInsightRepositoryImpl(
    private val streakRepository: StreakRepository,
    private val userProfileRepository: UserProfileRepository,
    private val taskRepository: TaskRepository,
    private val taskCompletionRepository: TaskCompletionRepository,
    private val settingsRepository: SettingsRepository,
    private val geminiClient: GeminiClient,
    private val insightCacheRepository: InsightCacheRepository = NoOpInsightCacheRepository(),
    private val toneHook: HabitInsightToneHook = DefaultHabitInsightToneHook(),
    private val usageTracker: com.pixelquest.app.domain.ai.AiUsageTracker = com.pixelquest.app.domain.ai.InMemoryAiUsageTracker(),
    private val clock: () -> Long = { System.currentTimeMillis() },
    /** Called after each live Gemini insight with the call time (Day 26: schedules the "ready" notification). */
    private val onLiveInsightGenerated: (Long) -> Unit = {}
) : HabitInsightRepository {

    constructor(
        streakRepository: StreakRepository,
        userProfileRepository: UserProfileRepository,
        taskRepository: TaskRepository,
        taskCompletionRepository: TaskCompletionRepository,
        settingsRepository: SettingsRepository,
        geminiClient: GeminiClient,
        toneHook: HabitInsightToneHook = DefaultHabitInsightToneHook()
    ) : this(
        streakRepository = streakRepository,
        userProfileRepository = userProfileRepository,
        taskRepository = taskRepository,
        taskCompletionRepository = taskCompletionRepository,
        settingsRepository = settingsRepository,
        geminiClient = geminiClient,
        insightCacheRepository = NoOpInsightCacheRepository(),
        toneHook = toneHook,
        usageTracker = com.pixelquest.app.domain.ai.InMemoryAiUsageTracker(),
        clock = { System.currentTimeMillis() }
    )

    companion object {
        /**
         * 12-hour standard staleness window (TTL) per Day 24 Step 17 / AI_INSIGHTS.md.
         */
        const val CACHE_TTL_MILLIS: Long = 12 * 60 * 60 * 1000L

        /**
         * 6-hour minimum interval between live Gemini API requests per user (Day 24 Step 16 / Day 25 Step 7).
         */
        const val MIN_CALL_INTERVAL_MS: Long = 6 * 60 * 60 * 1000L
    }

    private val _latestInsight = MutableStateFlow<HabitInsightResponse?>(null)
    override val latestInsight: Flow<HabitInsightResponse?> = _latestInsight.asStateFlow()

    override suspend fun getRemainingCooldownSeconds(): Long {
        val now = clock()
        val lastCallTime = try { settingsRepository.lastAiInsightTimestamp.first() } catch (e: Exception) { 0L }
            .let { if (it > 0L) it else (insightCacheRepository.getLatestInsight()?.generatedAt ?: 0L) }
        val elapsed = now - lastCallTime
        return if (lastCallTime > 0L && elapsed in 0 until MIN_CALL_INTERVAL_MS) {
            (MIN_CALL_INTERVAL_MS - elapsed) / 1000L
        } else {
            0L
        }
    }

    override suspend fun generateHabitInsight(forceRefresh: Boolean): GeminiResult<HabitInsightResponse> {
        val isOptedIn = try { settingsRepository.aiInsightsEnabled.first() } catch (e: Exception) { false }
        if (!isOptedIn) {
            return GeminiResult.Disabled("AI Habit Insights are disabled. Enable them in Settings to receive personalized insights.")
        }

        val streak = try { streakRepository.getCurrentStreak().first() } catch (e: Exception) { null }
        val profile = try { userProfileRepository.getProfile().first() } catch (e: Exception) { null }
        val tasks = try { taskRepository.getAllTasks().first() } catch (e: Exception) { emptyList() }
        val logs = try { taskCompletionRepository.getAllLogs().first() } catch (e: Exception) { emptyList() }

        val isSimpleMode = try { settingsRepository.simpleModeEnabled.first() } catch (e: Exception) { false }
        val tone = toneHook.resolveTone(isSimpleMode)

        val telemetry = HabitInsightPromptBuilder.buildTelemetrySummary(
            streak = streak,
            profile = profile,
            tasks = tasks,
            logs = logs
        )

        val dataHash = HabitInsightPromptBuilder.computeDataHash(telemetry, tone)

        // Step 5: Check caching layer first unless forceRefresh is explicitly requested
        if (!forceRefresh && insightCacheRepository.isCacheValid(dataHash, CACHE_TTL_MILLIS)) {
            val cached = insightCacheRepository.getLatestInsight()
            if (cached != null) {
                val cachedResponse = cached.toInsightResponse()
                _latestInsight.value = cachedResponse
                return GeminiResult.Success(cachedResponse)
            }
        }

        // Step 7 & 8: Enforce rate limiting before dispatching a live API call
        val cooldownSeconds = getRemainingCooldownSeconds()
        if (cooldownSeconds > 0L) {
            val message = com.pixelquest.app.domain.ai.AiRateLimitFormatter.formatCooldownMessage(cooldownSeconds)
            return GeminiResult.RateLimited(
                retryAfterSeconds = cooldownSeconds,
                message = message
            )
        }

        // Step 30 & 33: Check hard daily and monthly usage caps before dispatching live Gemini call
        if (!usageTracker.canMakeCall()) {
            val isMonthly = usageTracker.isMonthlyCapReached()
            val message = if (isMonthly) {
                "Monthly AI insight limit reached (${com.pixelquest.app.domain.ai.AiUsagePolicy.MAX_CALLS_PER_MONTH}/${com.pixelquest.app.domain.ai.AiUsagePolicy.MAX_CALLS_PER_MONTH} calls). Resets next month."
            } else {
                "You've reached today's insight limit (${com.pixelquest.app.domain.ai.AiUsagePolicy.MAX_CALLS_PER_DAY}/${com.pixelquest.app.domain.ai.AiUsagePolicy.MAX_CALLS_PER_DAY} calls). Check back tomorrow!"
            }
            return GeminiResult.RateLimited(
                retryAfterSeconds = if (isMonthly) 86400L * 7 else 86400L,
                message = message
            )
        }

        val prompt = HabitInsightPromptBuilder.buildPrompt(telemetry, tone)
        val systemInstruction = HabitInsightPromptBuilder.buildSystemInstruction(tone)

        val rawCallResult = safeGeminiCall {
            geminiClient.generateContent(
                prompt = prompt,
                systemInstruction = systemInstruction
            )
        }

        // Step 33: every call Gemini answered counts toward the daily and monthly caps, including an
        // answer that could not be used: otherwise a reply that keeps failing to parse costs a live
        // call on every visit with no cap. Calls that never got through (offline, timed out) and
        // Gemini's own 429 refusals don't count. Only a usable answer starts the 6-hour cooldown,
        // so the player can retry after a failure.
        val geminiAnswered = when (rawCallResult) {
            is GeminiResult.Success, is GeminiResult.MalformedResponse -> true
            is GeminiResult.ApiError -> rawCallResult.statusCode != null && rawCallResult.statusCode != 429
            else -> false
        }
        if (geminiAnswered) usageTracker.recordCall()

        return when (rawCallResult) {
            is GeminiResult.Success -> {
                try {
                    val parsed = HabitInsightResponse.parseFromJson(rawCallResult.data)
                    _latestInsight.value = parsed
                    // Persist to local cache with dataHash
                    insightCacheRepository.saveInsight(parsed, dataHash)
                    // Update persistent rate limit timestamp
                    val callTime = clock()
                    try { settingsRepository.setLastAiInsightTimestamp(callTime) } catch (_: Exception) {}
                    try { onLiveInsightGenerated(callTime) } catch (_: Exception) {}
                    GeminiResult.Success(parsed)
                } catch (e: Exception) {
                    GeminiResult.MalformedResponse(
                        cause = e,
                        rawResponse = rawCallResult.data,
                        message = "Failed to parse structured JSON from Gemini: ${e.message}"
                    )
                }
            }
            is GeminiResult.RateLimited -> GeminiResult.RateLimited(rawCallResult.retryAfterSeconds, rawCallResult.message)
            is GeminiResult.NetworkError -> GeminiResult.NetworkError(rawCallResult.cause, rawCallResult.message)
            is GeminiResult.ApiError -> GeminiResult.ApiError(rawCallResult.statusCode, rawCallResult.message)
            is GeminiResult.Disabled -> GeminiResult.Disabled(rawCallResult.message)
            is GeminiResult.MalformedResponse -> rawCallResult
        }
    }
}
