package com.pixelquest.app.ui.onboarding

import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.ui.screens.onboarding.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * RESET ALL PROGRESS keeps the cloud link and leaderboard choice, then opens onboarding. Onboarding
 * used to save a brand-new profile and drop them, so the old public row stayed on the leaderboard.
 */
class OnboardingAfterResetTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun onboardingAfterAReset_keepsTheCloudLinkAndLeaderboardChoice() {
        // What ProgressReset leaves behind for a signed-in, opted-in player.
        val profiles = FakeUserProfileRepository(
            UserProfileEntity(username = "PixelHero", avatarId = "avatar_hero", supabaseUserId = "user-1", leaderboardOptIn = true, leaderboardDisplayName = "Aria_Q")
        )
        val viewModel = OnboardingViewModel(profiles, FakeDifficultySettingsRepository(), FakeSettingsRepository(onboardingDone = false))

        viewModel.updateUsername("Aria")
        viewModel.completeOnboarding()

        val saved = profiles.profile.value!!
        assertEquals("Aria", saved.username)
        assertEquals(1, saved.level)
        assertEquals("user-1", saved.supabaseUserId)
        assertTrue(saved.leaderboardOptIn)
        assertEquals("Aria_Q", saved.leaderboardDisplayName)
    }

    @Test
    fun aFirstOnboarding_hasNoCloudLink() {
        val profiles = FakeUserProfileRepository(null)
        val viewModel = OnboardingViewModel(profiles, FakeDifficultySettingsRepository(), FakeSettingsRepository(onboardingDone = false))

        viewModel.updateUsername("Aria")
        viewModel.completeOnboarding()

        assertNull(profiles.profile.value!!.supabaseUserId)
    }
}
