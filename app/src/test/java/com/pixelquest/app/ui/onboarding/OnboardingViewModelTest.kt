package com.pixelquest.app.ui.onboarding

import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingViewModelTest {

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        val fakeUserRepo = object : UserProfileRepository {
            override fun getProfile() = flowOf(null)
            override suspend fun saveProfile(profile: com.pixelquest.app.data.local.entity.UserProfileEntity) {}
        }
        val fakeDiffRepo = object : DifficultySettingsRepository {
            override fun getCurrentDifficulty() = flowOf(com.pixelquest.app.data.local.entity.DifficultySettingsEntity(1, DifficultyLevel.MEDIUM, 0.7f, 7))
            override suspend fun updateDifficultySettings(settings: com.pixelquest.app.data.local.entity.DifficultySettingsEntity) {}
        }
        val fakeSettingsRepo = object : SettingsRepository {
            override val isSoundEnabled: Flow<Boolean> = flowOf(true)
            override val isCrtEnabled: Flow<Boolean> = flowOf(false)
            override val onboardingComplete: Flow<Boolean> = flowOf(false)
            override suspend fun setSoundEnabled(enabled: Boolean) {}
            override suspend fun setCrtEnabled(enabled: Boolean) {}
            override suspend fun setOnboardingComplete(complete: Boolean) {}
        }

        viewModel = OnboardingViewModel(fakeUserRepo, fakeDiffRepo, fakeSettingsRepo)
    }

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
