package com.pixelquest.app.ui.screens.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.domain.repository.HabitInsightRepository
import com.pixelquest.app.domain.repository.InsightCacheRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Step 11 & 12: ViewModel orchestrating the AI habit insights screen.
 * Wires HabitInsightRepository, InsightCacheRepository, and SettingsRepository's aiInsightsEnabled.
 * Requests cached insight on load for instant display, only calling Gemini fresh when the cache
 * is stale or missing and the 6-hour rate limit allows it.
 */
@HiltViewModel
class AiInsightViewModel @Inject constructor(
    private val habitInsightRepository: HabitInsightRepository,
    private val settingsRepository: SettingsRepository,
    private val taskCompletionRepository: TaskCompletionRepository,
    private val insightCacheRepository: InsightCacheRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiInsightUiState>(AiInsightUiState.Loading)
    val uiState: StateFlow<AiInsightUiState> = _uiState.asStateFlow()

    val isSimpleMode: StateFlow<Boolean> = settingsRepository.simpleModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        // Reload whenever the opt-in changes, so the card updates as soon as the user
        // enables or disables AI Coach in Settings (the first emission does the initial load).
        settingsRepository.aiInsightsEnabled
            .distinctUntilChanged()
            .onEach { loadInsight(forceRefresh = false) }
            .launchIn(viewModelScope)
    }

    fun loadInsight(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val isOptedIn = try { settingsRepository.aiInsightsEnabled.first() } catch (e: Exception) { false }
            if (!isOptedIn) {
                _uiState.value = AiInsightUiState.Disabled()
                return@launch
            }

            // Check if player has enough completion history (Step 13)
            val logs = try { taskCompletionRepository.getAllLogs().first() } catch (e: Exception) { emptyList() }
            val distinctDays = logs.map { it.completedDate }.distinct().size
            if (distinctDays < 3 && logs.size < 3) {
                val isSimple = try { settingsRepository.simpleModeEnabled.first() } catch (e: Exception) { false }
                val msg = if (isSimple) {
                    "Keep up the steady effort! Complete habits for at least 3 days to unlock personalized AI habit coaching."
                } else {
                    "Keep adventuring! Complete daily quests for at least 3 days to unlock AI-powered Questmaster Insights."
                }
                val tip = if (isSimple) {
                    "Consistent daily routines build sustainable habits over time."
                } else {
                    "Every hero's legend begins with a single quest. Return after logging more momentum!"
                }
                _uiState.value = AiInsightUiState.NotEnoughData(
                    daysLogged = distinctDays,
                    minimumRequiredDays = 3,
                    message = msg,
                    encouragingTip = tip
                )
                return@launch
            }

            // Fast path on initial load: immediately show existing cache if available
            val cachedEntry = insightCacheRepository.getLatestInsight()
            val cooldown = habitInsightRepository.getRemainingCooldownSeconds()

            if (!forceRefresh && cachedEntry != null) {
                val cachedResponse = cachedEntry.toInsightResponse()
                _uiState.value = AiInsightUiState.Success(
                    insight = cachedResponse,
                    isCached = true,
                    remainingCooldownSeconds = cooldown,
                    canRefresh = cooldown == 0L
                )
            } else if (_uiState.value !is AiInsightUiState.Success) {
                _uiState.value = AiInsightUiState.Loading
            }

            // Request insight via repository (validates dataHash, staleness, and rate limit)
            val result = habitInsightRepository.generateHabitInsight(forceRefresh = forceRefresh)
            val updatedCooldown = habitInsightRepository.getRemainingCooldownSeconds()

            _uiState.value = when (result) {
                is GeminiResult.Success -> {
                    val wasCached = cachedEntry != null && cachedEntry.generatedAt == result.data.generatedAt
                    AiInsightUiState.Success(
                        insight = result.data,
                        isCached = wasCached,
                        remainingCooldownSeconds = updatedCooldown,
                        canRefresh = updatedCooldown == 0L
                    )
                }
                is GeminiResult.Disabled -> AiInsightUiState.Disabled(result.message)
                is GeminiResult.SignInRequired -> AiInsightUiState.Error(message = result.message, canRetry = false)
                is GeminiResult.RateLimited -> {
                    val fallback = cachedEntry?.toInsightResponse() ?: habitInsightRepository.latestInsight.first()
                    if (result.message.contains("limit", ignoreCase = true) || result.message.contains("cap", ignoreCase = true)) {
                        AiInsightUiState.CapReached(
                            message = result.message,
                            isMonthly = result.message.contains("monthly", ignoreCase = true),
                            lastInsight = fallback
                        )
                    } else {
                        AiInsightUiState.RateLimited(
                            retryAfterSeconds = result.retryAfterSeconds ?: updatedCooldown,
                            message = result.message,
                            lastInsight = fallback
                        )
                    }
                }
                is GeminiResult.NetworkError -> {
                    val fallback = cachedEntry?.toInsightResponse() ?: habitInsightRepository.latestInsight.first()
                    AiInsightUiState.Error(
                        message = result.message,
                        canRetry = true,
                        fallbackInsight = fallback
                    )
                }
                is GeminiResult.ApiError -> {
                    val fallback = cachedEntry?.toInsightResponse() ?: habitInsightRepository.latestInsight.first()
                    AiInsightUiState.Error(
                        message = result.message,
                        // Retrying can't help a build without AI set up, or before tomorrow's limit resets.
                        canRetry = result.message != com.pixelquest.app.domain.ai.AiErrorCopy.NOT_CONFIGURED &&
                            result.message != com.pixelquest.app.domain.ai.AiErrorCopy.DAILY_LIMIT,
                        fallbackInsight = fallback
                    )
                }
                is GeminiResult.MalformedResponse -> {
                    val fallback = cachedEntry?.toInsightResponse() ?: habitInsightRepository.latestInsight.first()
                    AiInsightUiState.Error(
                        message = com.pixelquest.app.domain.ai.AiErrorCopy.UNREADABLE,
                        canRetry = true,
                        fallbackInsight = fallback
                    )
                }
            }
        }
    }

    /**
     * Step 15: Manual action to refresh insight.
     * Evaluates the rate-limit cooldown window and blocks refresh if cooldown is active,
     * updating state to RateLimited with remaining cooldown countdown.
     */
    fun refreshInsight() {
        viewModelScope.launch {
            val cooldown = habitInsightRepository.getRemainingCooldownSeconds()
            if (cooldown > 0L) {
                val current = _uiState.value
                val fallback = when (current) {
                    is AiInsightUiState.Success -> current.insight
                    is AiInsightUiState.RateLimited -> current.lastInsight
                    is AiInsightUiState.CapReached -> current.lastInsight
                    is AiInsightUiState.Error -> current.fallbackInsight
                    else -> null
                }
                _uiState.value = AiInsightUiState.RateLimited(
                    retryAfterSeconds = cooldown,
                    message = com.pixelquest.app.domain.ai.AiRateLimitFormatter.formatCooldownMessage(cooldown),
                    lastInsight = fallback
                )
                return@launch
            }
            loadInsight(forceRefresh = true)
        }
    }
}
