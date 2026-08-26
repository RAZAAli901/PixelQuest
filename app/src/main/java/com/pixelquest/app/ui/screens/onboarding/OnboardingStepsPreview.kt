package com.pixelquest.app.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.theme.PixelQuestTheme

@Preview(name = "Onboarding Avatar Step Screen", showBackground = true)
@Composable
fun OnboardingAvatarStepScreenPreview() {
    PixelQuestTheme {
        OnboardingAvatarStepScreen(
            selectedAvatarId = "avatar_hero",
            onAvatarSelected = {},
            onNextClick = {},
            onBackClick = {}
        )
    }
}

@Preview(name = "Onboarding Difficulty Step Screen", showBackground = true)
@Composable
fun OnboardingDifficultyStepScreenPreview() {
    PixelQuestTheme {
        OnboardingDifficultyStepScreen(
            selectedLevel = DifficultyLevel.MEDIUM,
            onLevelSelected = {},
            onNextClick = {},
            onBackClick = {}
        )
    }
}
