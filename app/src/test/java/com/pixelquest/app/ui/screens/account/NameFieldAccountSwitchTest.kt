package com.pixelquest.app.ui.screens.account

import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The public-name field belongs to the signed-in account. Found on the emulator: after account B
 * signed in on the phone account A had used, the field still offered A's public name, so B could
 * join the leaderboard under it.
 */
class NameFieldAccountSwitchTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val cloud = object : CloudProfileRepository {
        override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String) = SupabaseResult.Success(Unit)
        override suspend fun syncProfileToCloud() = SupabaseResult.Success(Unit)
    }

    @Test
    fun anotherAccount_getsAnEmptyNameField_theSameAccountKeepsItsDraft() {
        val profiles = FakeUserProfileRepository(
            UserProfileEntity(username = "Hero", avatarId = "avatar_hero", supabaseUserId = "alice", leaderboardOptIn = true, leaderboardDisplayName = "Alpha")
        )
        val viewModel = AccountViewModel(profiles, cloud)
        assertEquals("Alpha", viewModel.uiState.value.displayNameInput)

        // Alice signs out (the link is cleared) and back in: her name stays.
        profiles.profile.value = profiles.profile.value!!.copy(supabaseUserId = null)
        profiles.profile.value = profiles.profile.value!!.copy(supabaseUserId = "alice")
        assertEquals("Alpha", viewModel.uiState.value.displayNameInput)

        // Bob signs in: CloudAccountLink clears the opt-in and the name, and the field follows.
        profiles.profile.value = profiles.profile.value!!.copy(supabaseUserId = null)
        profiles.profile.value = profiles.profile.value!!.copy(supabaseUserId = "bob", leaderboardOptIn = false, leaderboardDisplayName = null)
        assertEquals("", viewModel.uiState.value.displayNameInput)
    }
}
