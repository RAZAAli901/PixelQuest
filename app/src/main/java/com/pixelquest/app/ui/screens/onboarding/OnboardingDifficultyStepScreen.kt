package com.pixelquest.app.ui.screens.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.components.PixelDifficultyCards

@Composable
fun OnboardingDifficultyStepScreen(
    selectedLevel: DifficultyLevel,
    onLevelSelected: (DifficultyLevel) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    OnboardingStepFrame(
        step = 3,
        title = "Choose difficulty",
        body = "How much of each day's quests counts as a perfect day.",
        primary = "Next",
        onPrimary = onNextClick,
        onBack = onBackClick
    ) {
        PixelDifficultyCards(
            selectedLevel = selectedLevel,
            onLevelSelected = onLevelSelected,
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        )
    }
}
