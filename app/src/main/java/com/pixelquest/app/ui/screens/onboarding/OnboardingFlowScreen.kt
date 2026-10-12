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
    val soundManager = com.pixelquest.app.audio.LocalSoundManager.current
    val uiState by viewModel.uiState.collectAsState()

    // System back steps back through onboarding instead of closing the app and losing the answers.
    androidx.activity.compose.BackHandler(enabled = uiState.currentStep != OnboardingStep.Welcome) {
        viewModel.previousStep()
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val backupPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.onBackupFileSelected(context, uri) }

    uiState.restoreSummary?.let { summary ->
        com.pixelquest.app.ui.screens.settings.RestoreDataConfirmDialog(
            onConfirm = { viewModel.confirmRestore { onOnboardingComplete() } },
            onDismiss = { viewModel.dismissRestore() },
            summary = summary
        )
    }

    when (uiState.currentStep) {
        OnboardingStep.Welcome -> {
            OnboardingWelcomeScreen(
                onStartClick = { viewModel.nextStep() },
                onRestoreClick = if (viewModel.canRestoreBackup) {
                    { backupPicker.launch(arrayOf("application/json", "*/*")) }
                } else null,
                restoreMessage = uiState.restoreMessage
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
            OnboardingAvatarStepScreen(
                selectedAvatarId = uiState.avatarId,
                onAvatarSelected = { viewModel.updateAvatar(it) },
                onNextClick = { viewModel.nextStep() },
                onBackClick = { viewModel.previousStep() }
            )
        }
        OnboardingStep.DifficultyPick -> {
            OnboardingDifficultyStepScreen(
                selectedLevel = uiState.difficultyLevel,
                onLevelSelected = { viewModel.updateDifficulty(it) },
                onNextClick = { viewModel.nextStep() },
                onBackClick = { viewModel.previousStep() }
            )
        }
        OnboardingStep.Summary -> {
            OnboardingSummaryScreen(
                username = uiState.username,
                avatarId = uiState.avatarId,
                difficultyLevel = uiState.difficultyLevel,
                onConfirmClick = {
                    soundManager?.playQuestBeginSound()
                    viewModel.completeOnboarding {
                        onOnboardingComplete()
                    }
                },
                onBackClick = { viewModel.previousStep() }
            )
        }
    }
}
