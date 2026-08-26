package com.pixelquest.app.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.pixelquest.app.ui.theme.PixelQuestTheme

@Preview(name = "Onboarding Welcome Screen", showBackground = true)
@Composable
fun OnboardingWelcomeScreenPreview() {
    PixelQuestTheme {
        OnboardingWelcomeScreen(
            onStartClick = {}
        )
    }
}

@Preview(name = "Onboarding Name Entry Screen - Empty", showBackground = true)
@Composable
fun OnboardingNameEntryScreenEmptyPreview() {
    PixelQuestTheme {
        OnboardingNameEntryScreen(
            username = "",
            onUsernameChange = {},
            isValid = false,
            nameError = null,
            onNextClick = {},
            onBackClick = {}
        )
    }
}

@Preview(name = "Onboarding Name Entry Screen - Valid", showBackground = true)
@Composable
fun OnboardingNameEntryScreenValidPreview() {
    PixelQuestTheme {
        OnboardingNameEntryScreen(
            username = "PixelKnight",
            onUsernameChange = {},
            isValid = true,
            nameError = null,
            onNextClick = {},
            onBackClick = {}
        )
    }
}
