package com.pixelquest.app.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OnboardingFlowScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onOnboardingComplete: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.currentStep) {
        OnboardingStep.Welcome -> {
            OnboardingWelcomeScreen(
                onStartClick = { viewModel.nextStep() }
            )
        }
        OnboardingStep.NameEntry -> {
            OnboardingNameEntryScreen(
                username = uiState.username,
                onUsernameChange = { viewModel.updateUsername(it) },
                isValid = uiState.isNameValid,
                nameError = uiState.nameError,
                onNextClick = {
                    if (uiState.isNameValid) {
                        viewModel.nextStep()
                    }
                },
                onBackClick = { viewModel.previousStep() }
            )
        }
        OnboardingStep.AvatarPick -> {
            // Skeleton placeholder until Section C steps
        }
        OnboardingStep.DifficultyPick -> {
            // Skeleton placeholder until Section C steps
        }
        OnboardingStep.Summary -> {
            // Skeleton placeholder until Section D steps
        }
    }
}
