package com.pixelquest.app.domain.ai

import com.pixelquest.app.BuildConfig
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.domain.repository.HabitInsightRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Step 15: Temporary debug-only trigger to exercise the AI habit insight pipeline end-to-end
 * without requiring any user-facing UI (which is scheduled for Day 25).
 * Gated behind BuildConfig.DEBUG.
 */
@Singleton
class DebugAiInsightTrigger @Inject constructor(
    private val habitInsightRepository: HabitInsightRepository
) {
    private val _debugStatus = MutableStateFlow("IDLE")
    val debugStatus: StateFlow<String> = _debugStatus.asStateFlow()

    private val _lastResult = MutableStateFlow<GeminiResult<HabitInsightResponse>?>(null)
    val lastResult: StateFlow<GeminiResult<HabitInsightResponse>?> = _lastResult.asStateFlow()

    /**
     * Executes the insight generation pipeline asynchronously in debug mode.
     */
    fun triggerInsightGeneration(
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        onComplete: ((GeminiResult<HabitInsightResponse>) -> Unit)? = null
    ) {
        if (!BuildConfig.DEBUG) {
            _debugStatus.value = "DEBUG_ONLY_RESTRICTION"
            return
        }

        _debugStatus.value = "RUNNING"
        scope.launch {
            val result = habitInsightRepository.generateHabitInsight()
            _lastResult.value = result
            _debugStatus.value = when (result) {
                is GeminiResult.Success -> "SUCCESS"
                is GeminiResult.RateLimited -> "RATE_LIMITED: ${result.message}"
                is GeminiResult.NetworkError -> "NETWORK_ERROR: ${result.message}"
                is GeminiResult.ApiError -> "API_ERROR: ${result.message}"
                is GeminiResult.Disabled -> "DISABLED: ${result.message}"
                is GeminiResult.MalformedResponse -> "MALFORMED: ${result.message}"
            }
            onComplete?.invoke(result)
        }
    }
}
