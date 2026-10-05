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
    private lateinit var fakeUserRepo: com.pixelquest.app.testing.FakeUserProfileRepository
    private lateinit var fakeDiffRepo: com.pixelquest.app.testing.FakeDifficultySettingsRepository
    private lateinit var fakeSettingsRepo: com.pixelquest.app.testing.FakeSettingsRepository

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // New players start from the seeded (default) profile and Medium difficulty, like the app.
        fakeUserRepo = com.pixelquest.app.testing.FakeUserProfileRepository()
        fakeDiffRepo = com.pixelquest.app.testing.FakeDifficultySettingsRepository()
        fakeSettingsRepo = com.pixelquest.app.testing.FakeSettingsRepository(onboardingDone = false)

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
        assertTrue(fakeSettingsRepo.onboardingComplete.value)
        assertEquals("DragonSlayer", fakeUserRepo.profile.value?.username)
        assertEquals("avatar_mage", fakeUserRepo.profile.value?.avatarId)
        assertEquals(DifficultyLevel.HARD, fakeDiffRepo.settings.value?.difficultyLevel)
    }
}
