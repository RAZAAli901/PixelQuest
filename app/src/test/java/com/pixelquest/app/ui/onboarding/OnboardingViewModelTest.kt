package com.pixelquest.app.ui.onboarding

import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.ui.screens.onboarding.OnboardingStep
import com.pixelquest.app.ui.screens.onboarding.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = OnboardingViewModel(
            FakeUserProfileRepository(),
            FakeDifficultySettingsRepository(),
            FakeSettingsRepository(onboardingDone = false)
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun testStepNavigationPreservesData() {
        viewModel.updateUsername("PixelHero")
        assertTrue(viewModel.uiState.value.isNameValid)

        viewModel.nextStep()
        assertEquals(OnboardingStep.NameEntry, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(OnboardingStep.AvatarPick, viewModel.uiState.value.currentStep)

        viewModel.updateAvatar("avatar_wizard")
        viewModel.nextStep()
        assertEquals(OnboardingStep.DifficultyPick, viewModel.uiState.value.currentStep)

        viewModel.updateDifficulty(DifficultyLevel.HARD)
        viewModel.nextStep()
        assertEquals(OnboardingStep.Summary, viewModel.uiState.value.currentStep)

        // Navigate back to Welcome
        viewModel.previousStep()
        assertEquals(OnboardingStep.DifficultyPick, viewModel.uiState.value.currentStep)
        viewModel.previousStep()
        assertEquals(OnboardingStep.AvatarPick, viewModel.uiState.value.currentStep)
        viewModel.previousStep()
        assertEquals(OnboardingStep.NameEntry, viewModel.uiState.value.currentStep)

        // Verify data preserved
        assertEquals("PixelHero", viewModel.uiState.value.username)
        assertEquals("avatar_wizard", viewModel.uiState.value.avatarId)
        assertEquals(DifficultyLevel.HARD, viewModel.uiState.value.difficultyLevel)
    }
}
