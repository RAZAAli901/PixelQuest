package com.pixelquest.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.screens.onboarding.OnboardingAvatarStepScreen
import com.pixelquest.app.ui.screens.onboarding.OnboardingDifficultyStepScreen
import com.pixelquest.app.ui.screens.onboarding.OnboardingNameEntryScreen
import com.pixelquest.app.ui.screens.onboarding.OnboardingStep
import com.pixelquest.app.ui.screens.onboarding.OnboardingSummaryScreen
import com.pixelquest.app.ui.screens.onboarding.OnboardingWelcomeScreen
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onboardingFlow_navigatesFromWelcomeToSummary_andCompletes() {
        var currentStep by mutableStateOf<OnboardingStep>(OnboardingStep.Welcome)
        var username by mutableStateOf("")
        var selectedAvatar by mutableStateOf("avatar_hero")
        var selectedDifficulty by mutableStateOf(DifficultyLevel.MEDIUM)
        var onboardingCompleted by mutableStateOf(false)

        composeTestRule.setContent {
            PixelQuestTheme {
                when (currentStep) {
                    OnboardingStep.Welcome -> OnboardingWelcomeScreen(
                        onStartClick = { currentStep = OnboardingStep.NameEntry }
                    )
                    OnboardingStep.NameEntry -> OnboardingNameEntryScreen(
                        username = username,
                        onUsernameChange = { username = it },
                        isValid = username.trim().isNotBlank(),
                        onNextClick = { currentStep = OnboardingStep.AvatarPick },
                        onBackClick = { currentStep = OnboardingStep.Welcome }
                    )
                    OnboardingStep.AvatarPick -> OnboardingAvatarStepScreen(
                        selectedAvatarId = selectedAvatar,
                        onAvatarSelected = { selectedAvatar = it },
                        onNextClick = { currentStep = OnboardingStep.DifficultyPick },
                        onBackClick = { currentStep = OnboardingStep.NameEntry }
                    )
                    OnboardingStep.DifficultyPick -> OnboardingDifficultyStepScreen(
                        selectedLevel = selectedDifficulty,
                        onLevelSelected = { selectedDifficulty = it },
                        onNextClick = { currentStep = OnboardingStep.Summary },
                        onBackClick = { currentStep = OnboardingStep.AvatarPick }
                    )
                    OnboardingStep.Summary -> OnboardingSummaryScreen(
                        username = username,
                        avatarId = selectedAvatar,
                        difficultyLevel = selectedDifficulty,
                        onConfirmClick = { onboardingCompleted = true },
                        onBackClick = { currentStep = OnboardingStep.DifficultyPick }
                    )
                }
            }
        }

        // 1. Welcome Screen
        composeTestRule.onNodeWithText("⚔️ WELCOME HERO ⚔️").assertIsDisplayed()
        composeTestRule.onNodeWithText("START YOUR JOURNEY ▶").performClick()

        // 2. Name Entry Screen
        composeTestRule.onNodeWithText("ENTER HERO NAME").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter your name...").performTextInput("KnightArthur")
        composeTestRule.onNodeWithText("NEXT: CHOOSE AVATAR ▶").performClick()

        // 3. Avatar Selection Screen
        composeTestRule.onNodeWithText("CHOOSE YOUR AVATAR").assertIsDisplayed()
        composeTestRule.onNodeWithText("NEXT: DIFFICULTY ▶").performClick()

        // 4. Difficulty Pick Screen
        composeTestRule.onNodeWithText("SELECT DIFFICULTY").assertIsDisplayed()
        composeTestRule.onNodeWithText("NEXT: SUMMARY ▶").performClick()

        // 5. Summary Screen
        composeTestRule.onNodeWithText("HERO SUMMARY").assertIsDisplayed()
        composeTestRule.onNodeWithText("BEGIN YOUR QUEST ▶").performClick()

        assertTrue(onboardingCompleted)
        assertEquals("KnightArthur", username)
    }
}
