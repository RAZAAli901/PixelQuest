package com.pixelquest.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.pixelquest.app.domain.model.DifficultyLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface OnboardingStep {
    object Welcome : OnboardingStep
    object NameEntry : OnboardingStep
    object AvatarPick : OnboardingStep
    object DifficultyPick : OnboardingStep
    object Summary : OnboardingStep
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.Welcome,
    val username: String = "",
    val avatarId: String = "avatar_hero",
    val difficultyLevel: DifficultyLevel = DifficultyLevel.MEDIUM,
    val isNameValid: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateUsername(name: String) {
        val trimmed = name.take(20)
        val valid = trimmed.isNotBlank() && trimmed.length <= 20
        _uiState.update {
            it.copy(username = trimmed, isNameValid = valid)
        }
    }

    fun updateAvatar(avatarId: String) {
        _uiState.update { it.copy(avatarId = avatarId) }
    }

    fun updateDifficulty(difficultyLevel: DifficultyLevel) {
        _uiState.update { it.copy(difficultyLevel = difficultyLevel) }
    }

    fun goToStep(step: OnboardingStep) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun nextStep() {
        val next = when (_uiState.value.currentStep) {
            OnboardingStep.Welcome -> OnboardingStep.NameEntry
            OnboardingStep.NameEntry -> if (_uiState.value.isNameValid) OnboardingStep.AvatarPick else _uiState.value.currentStep
            OnboardingStep.AvatarPick -> OnboardingStep.DifficultyPick
            OnboardingStep.DifficultyPick -> OnboardingStep.Summary
            OnboardingStep.Summary -> OnboardingStep.Summary
        }
        _uiState.update { it.copy(currentStep = next) }
    }

    fun previousStep() {
        val prev = when (_uiState.value.currentStep) {
            OnboardingStep.Welcome -> OnboardingStep.Welcome
            OnboardingStep.NameEntry -> OnboardingStep.Welcome
            OnboardingStep.AvatarPick -> OnboardingStep.NameEntry
            OnboardingStep.DifficultyPick -> OnboardingStep.AvatarPick
            OnboardingStep.Summary -> OnboardingStep.DifficultyPick
        }
        _uiState.update { it.copy(currentStep = prev) }
    }
}
