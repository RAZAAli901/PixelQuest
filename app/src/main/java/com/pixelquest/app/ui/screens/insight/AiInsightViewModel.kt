package com.pixelquest.app.ui.screens.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.domain.repository.HabitInsightRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Step 11: ViewModel orchestrating the AI habit insights screen.
 * Wires HabitInsightRepository and SettingsRepository's aiInsightsEnabled,
 * exposing reactive AiInsightUiState across Loading, Success, Error, Disabled, RateLimited, and NotEnoughData.
 */
@HiltViewModel
class AiInsightViewModel @Inject constructor(
    private val habitInsightRepository: HabitInsightRepository,
    private val settingsRepository: SettingsRepository,
    private val taskCompletionRepository: TaskCompletionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiInsightUiState>(AiInsightUiState.Loading)
    val uiState: StateFlow<AiInsightUiState> = _uiState.asStateFlow()

    init {
        loadInsight(forceRefresh = false)
    }

    fun loadInsight(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = AiInsightUiState.Loading

            val isOptedIn = try { settingsRepository.aiInsightsEnabled.first() } catch (e: Exception) { false }
            if (!isOptedIn) {
                _uiState.value = AiInsightUiState.Disabled()
                return@launch
            }

            // Check if player has enough completion history (Step 13)
            val logs = try { taskCompletionRepository.getAllLogs().first() } catch (e: Exception) { emptyList() }
            val distinctDays = logs.map { it.completedDate }.distinct().size
            if (distinctDays < 3 && logs.size < 3) {
                _uiState.value = AiInsightUiState.NotEnoughData(daysLogged = distinctDays)
                return@launch
            }

            // Request insight (cache-first unless forceRefresh = true)
            val result = habitInsightRepository.generateHabitInsight(forceRefresh = forceRefresh)
            val cooldown = habitInsightRepository.getRemainingCooldownSeconds()

            _uiState.value = when (result) {
                is GeminiResult.Success -> {
                    AiInsightUiState.Success(
                        insight = result.data,
                        isCached = !forceRefresh,
                        remainingCooldownSeconds = cooldown,
                        canRefresh = cooldown == 0L
                    )
                }
                is GeminiResult.Disabled -> AiInsightUiState.Disabled(result.message)
                is GeminiResult.RateLimited -> {
                    val last = habitInsightRepository.latestInsight.first()
                    AiInsightUiState.RateLimited(
                        retryAfterSeconds = result.retryAfterSeconds ?: cooldown,
                        message = result.message,
                        lastInsight = last
                    )
                }
                is GeminiResult.NetworkError -> {
                    val last = habitInsightRepository.latestInsight.first()
                    AiInsightUiState.Error(
                        message = result.message,
                        canRetry = true,
                        fallbackInsight = last
                    )
                }
                is GeminiResult.ApiError -> {
                    val last = habitInsightRepository.latestInsight.first()
                    AiInsightUiState.Error(
                        message = result.message,
                        canRetry = true,
                        fallbackInsight = last
                    )
                }
                is GeminiResult.MalformedResponse -> {
                    val last = habitInsightRepository.latestInsight.first()
                    AiInsightUiState.Error(
                        message = "Could not parse AI response: ${result.message}",
                        canRetry = true,
                        fallbackInsight = last
                    )
                }
            }
        }
    }

    fun refreshInsight() {
        loadInsight(forceRefresh = true)
    }
}
