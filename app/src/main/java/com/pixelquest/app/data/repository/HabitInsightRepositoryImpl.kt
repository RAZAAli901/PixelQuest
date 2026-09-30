package com.pixelquest.app.data.repository

import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.remote.safeGeminiCall
import com.pixelquest.app.domain.ai.DefaultHabitInsightToneHook
import com.pixelquest.app.domain.ai.HabitInsightPromptBuilder
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.domain.ai.HabitInsightToneHook
import com.pixelquest.app.domain.repository.HabitInsightRepository
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

@Singleton
class HabitInsightRepositoryImpl @Inject constructor(
    private val streakRepository: StreakRepository,
    private val userProfileRepository: UserProfileRepository,
    private val taskRepository: TaskRepository,
    private val taskCompletionRepository: TaskCompletionRepository,
    private val settingsRepository: SettingsRepository,
    private val geminiClient: GeminiClient,
    private val toneHook: HabitInsightToneHook = DefaultHabitInsightToneHook()
) : HabitInsightRepository {

    private val _latestInsight = MutableStateFlow<HabitInsightResponse?>(null)
    override val latestInsight: Flow<HabitInsightResponse?> = _latestInsight.asStateFlow()

    override suspend fun generateHabitInsight(): GeminiResult<HabitInsightResponse> {
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

        val prompt = HabitInsightPromptBuilder.buildPrompt(telemetry, tone)
        val systemInstruction = HabitInsightPromptBuilder.buildSystemInstruction(tone)

        val rawCallResult = safeGeminiCall {
            geminiClient.generateContent(
                prompt = prompt,
                systemInstruction = systemInstruction
            )
        }

        return when (rawCallResult) {
            is GeminiResult.Success -> {
                try {
                    val parsed = HabitInsightResponse.parseFromJson(rawCallResult.data)
                    _latestInsight.value = parsed
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
