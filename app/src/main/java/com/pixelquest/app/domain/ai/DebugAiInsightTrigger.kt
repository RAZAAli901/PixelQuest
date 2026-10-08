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
 * Step 15, 35 & 41: Temporary debug-only trigger to exercise the AI habit insight pipeline end-to-end
 * without requiring any user-facing UI (which is scheduled for Day 25).
 * Gated strictly behind BuildConfig.DEBUG and enforces the Section C Step 16 rate-limit throttle groundwork
 * (minimum 6-hour interval between successful calls per user).
 */
@Singleton
class DebugAiInsightTrigger @Inject constructor(
    private val habitInsightRepository: HabitInsightRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val isDebugProvider: () -> Boolean = { BuildConfig.DEBUG }
) {
    companion object {
        /**
         * Minimum interval between live insight calls: 6 hours (Section C Step 16).
         */
        const val MIN_CALL_INTERVAL_MS: Long = 6 * 60 * 60 * 1000L // 21,600,000 ms
    }

    private var lastSuccessfulCallTimestamp: Long = 0L

    private val _debugStatus = MutableStateFlow("IDLE")
    val debugStatus: StateFlow<String> = _debugStatus.asStateFlow()

    private val _lastResult = MutableStateFlow<GeminiResult<HabitInsightResponse>?>(null)
    val lastResult: StateFlow<GeminiResult<HabitInsightResponse>?> = _lastResult.asStateFlow()

    /**
     * Executes the insight generation pipeline asynchronously in debug mode.
     * Strictly blocked when not running in debug builds.
     */
    fun triggerInsightGeneration(
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        onComplete: ((GeminiResult<HabitInsightResponse>) -> Unit)? = null
    ) {
        if (!isDebugProvider()) {
            val disabled = GeminiResult.Disabled("AI debug trigger is restricted to debug builds only.")
            _lastResult.value = disabled
            _debugStatus.value = "DEBUG_ONLY_RESTRICTION"
            onComplete?.invoke(disabled)
            return
        }

        val currentTime = clock()
        val elapsed = currentTime - lastSuccessfulCallTimestamp
        if (lastSuccessfulCallTimestamp > 0L && elapsed < MIN_CALL_INTERVAL_MS) {
            val remainingSeconds = (MIN_CALL_INTERVAL_MS - elapsed) / 1000
            val throttled = GeminiResult.RateLimited(
                retryAfterSeconds = remainingSeconds,
                message = "Rate limit throttle enforced: minimum 6-hour interval between calls. Please wait $remainingSeconds seconds."
            )
            _lastResult.value = throttled
            _debugStatus.value = "THROTTLED: ${throttled.message}"
            onComplete?.invoke(throttled)
            return
        }

        _debugStatus.value = "RUNNING"
        scope.launch {
            val result = habitInsightRepository.generateHabitInsight()
            if (result is GeminiResult.Success) {
                lastSuccessfulCallTimestamp = clock()
            }
            _lastResult.value = result
            _debugStatus.value = when (result) {
                is GeminiResult.Success -> "SUCCESS"
                is GeminiResult.RateLimited -> "RATE_LIMITED: ${result.message}"
                is GeminiResult.NetworkError -> "NETWORK_ERROR: ${result.message}"
                is GeminiResult.ApiError -> "API_ERROR: ${result.message}"
                is GeminiResult.Disabled -> "DISABLED: ${result.message}"
                is GeminiResult.SignInRequired -> "SIGN_IN_REQUIRED: ${result.message}"
                is GeminiResult.MalformedResponse -> "MALFORMED: ${result.message}"
            }
            onComplete?.invoke(result)
        }
    }

    /**
     * Direct suspending trigger for synchronous testing and coroutine consumers.
     * Strictly blocked when not running in debug builds.
     */
    suspend fun triggerDirectly(): GeminiResult<HabitInsightResponse> {
        if (!isDebugProvider()) {
            val disabled = GeminiResult.Disabled("AI debug trigger is restricted to debug builds only.")
            _lastResult.value = disabled
            _debugStatus.value = "DEBUG_ONLY_RESTRICTION"
            return disabled
        }

        val currentTime = clock()
        val elapsed = currentTime - lastSuccessfulCallTimestamp
        if (lastSuccessfulCallTimestamp > 0L && elapsed < MIN_CALL_INTERVAL_MS) {
            val remainingSeconds = (MIN_CALL_INTERVAL_MS - elapsed) / 1000
            val throttled = GeminiResult.RateLimited(
                retryAfterSeconds = remainingSeconds,
                message = "Rate limit throttle enforced: minimum 6-hour interval between calls. Please wait $remainingSeconds seconds."
            )
            _lastResult.value = throttled
            _debugStatus.value = "THROTTLED: ${throttled.message}"
            return throttled
        }

        _debugStatus.value = "RUNNING"
        val result = habitInsightRepository.generateHabitInsight()
        if (result is GeminiResult.Success) {
            lastSuccessfulCallTimestamp = clock()
        }
        _lastResult.value = result
        _debugStatus.value = when (result) {
            is GeminiResult.Success -> "SUCCESS"
            is GeminiResult.RateLimited -> "RATE_LIMITED: ${result.message}"
            is GeminiResult.NetworkError -> "NETWORK_ERROR: ${result.message}"
            is GeminiResult.ApiError -> "API_ERROR: ${result.message}"
            is GeminiResult.Disabled -> "DISABLED: ${result.message}"
            is GeminiResult.SignInRequired -> "SIGN_IN_REQUIRED: ${result.message}"
            is GeminiResult.MalformedResponse -> "MALFORMED: ${result.message}"
        }
        return result
    }
}
