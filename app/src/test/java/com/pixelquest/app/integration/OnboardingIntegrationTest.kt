package com.pixelquest.app.integration

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.ui.screens.onboarding.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private var savedProfile: UserProfileEntity? = null
    private var savedDifficulty: DifficultySettingsEntity? = null
    private var onboardingCompletedFlag = false

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val fakeUserRepo = object : UserProfileRepository {
            override fun getProfile() = flowOf(savedProfile)
            override suspend fun saveProfile(profile: UserProfileEntity) {
                savedProfile = profile
            }
        }

        val fakeDiffRepo = object : DifficultySettingsRepository {
            override fun getCurrentDifficulty() = flowOf(savedDifficulty ?: DifficultySettingsEntity(1, DifficultyLevel.MEDIUM, 0.7f, 7))
            override suspend fun updateDifficultySettings(settings: DifficultySettingsEntity) {
                savedDifficulty = settings
            }
        }

        val fakeSettingsRepo = object : SettingsRepository {
            override val isSoundEnabled: Flow<Boolean> = flowOf(true)
            override val isCrtEnabled: Flow<Boolean> = flowOf(false)
            override val onboardingComplete: Flow<Boolean> = flowOf(onboardingCompletedFlag)
            override suspend fun setSoundEnabled(enabled: Boolean) {}
            override suspend fun setCrtEnabled(enabled: Boolean) {}
            override suspend fun setOnboardingComplete(complete: Boolean) {
                onboardingCompletedFlag = complete
            }
        }

        viewModel = OnboardingViewModel(fakeUserRepo, fakeDiffRepo, fakeSettingsRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFullOnboardingFlowPersistsDataAtomically() = runTest {
        viewModel.updateUsername("DragonSlayer")
        viewModel.updateAvatar("avatar_mage")
        viewModel.updateDifficulty(DifficultyLevel.HARD)

        var completedCallbackFired = false
        viewModel.completeOnboarding {
            completedCallbackFired = true
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(completedCallbackFired)
        assertTrue(onboardingCompletedFlag)
        assertEquals("DragonSlayer", savedProfile?.username)
        assertEquals("avatar_mage", savedProfile?.avatarId)
        assertEquals(DifficultyLevel.HARD, savedDifficulty?.difficultyLevel)
    }
}
