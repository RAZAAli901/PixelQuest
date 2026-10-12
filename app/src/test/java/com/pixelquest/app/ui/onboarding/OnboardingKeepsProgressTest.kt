package com.pixelquest.app.ui.onboarding

import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.OnboardingGate
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * An update never costs a player their progress through onboarding. Found on the emulator by
 * updating a v1.1.0 build in place: 1.1.0 never showed onboarding (its splash read the flag before it
 * had loaded) and so never set its flag; after the update the player was sent through onboarding,
 * whose last step wrote a level-1, 0-XP profile over theirs.
 */
class OnboardingKeepsProgressTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val played = UserProfileEntity(username = "PixelHero", avatarId = "avatar_hero", level = 1, totalXp = 100)

    @Test
    fun aPlayerWhoHasPlayed_countsAsOnboarded_withoutTheFlag() {
        assertTrue(OnboardingGate.isDone(flag = false, profile = played))
        assertTrue(OnboardingGate.isDone(flag = false, profile = played.copy(totalXp = 0, level = 2)))
    }

    @Test
    fun aNewPlayer_orAResetOne_stillGetsOnboarding() {
        val fresh = UserProfileEntity(username = "PixelHero", avatarId = "avatar_hero")
        assertFalse(OnboardingGate.isDone(flag = false, profile = fresh))
        assertFalse(OnboardingGate.isDone(flag = false, profile = null))
        assertTrue(OnboardingGate.isDone(flag = true, profile = fresh))
    }

    @Test
    fun finishingOnboarding_keepsAnExistingPlayersLevelAndXp() {
        val profiles = FakeUserProfileRepository(played.copy(level = 3, totalXp = 900, perfectDaysTowardNextLevel = 4, createdAt = 1_700_000_000_000L))
        val viewModel = OnboardingViewModel(profiles, FakeDifficultySettingsRepository(), FakeSettingsRepository(onboardingDone = false))

        viewModel.updateUsername("Aria")
        viewModel.completeOnboarding()

        val saved = profiles.profile.value!!
        assertEquals("Aria", saved.username)
        assertEquals(3, saved.level)
        assertEquals(900, saved.totalXp)
        assertEquals(4, saved.perfectDaysTowardNextLevel)
        assertEquals(1_700_000_000_000L, saved.createdAt)
    }
}
