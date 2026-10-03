package com.pixelquest.app.ui.screens.account

import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** "Delete my cloud data" only reports success, and only forgets the account, when the server calls worked. */
@OptIn(ExperimentalCoroutinesApi::class)
class CloudDeletionResultTest {

    private val profiles = mockk<UserProfileRepository>(relaxed = true) {
        every { getProfile() } returns MutableStateFlow(
            UserProfileEntity(username = "Hero", avatarId = "avatar_hero", supabaseUserId = "u1", leaderboardOptIn = true)
        )
    }
    private val cloud = mockk<CloudProfileRepository>(relaxed = true)
    private val auth = mockk<AuthRepository>(relaxed = true)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun delete(): AccountUiState {
        val viewModel = AccountViewModel(profiles, cloud, auth)
        viewModel.confirmDeleteCloudAccount()
        return viewModel.uiState.value
    }

    @Test
    fun offline_nothingIsClearedAndItSaysSo() {
        coEvery { cloud.deleteCloudProfile() } returns SupabaseResult.NetworkError(RuntimeException("offline"))

        val state = delete()

        assertTrue(state.syncMessage!!.contains("nothing was deleted"))
        assertFalse(state.isDeletingCloudData)
        coVerify(exactly = 0) { auth.deleteAccount() }
        coVerify(exactly = 0) { profiles.clearCloudData() }
    }

    @Test
    fun accountDeletionFailing_keepsTheLocalLink() {
        coEvery { cloud.deleteCloudProfile() } returns SupabaseResult.Success(Unit)
        coEvery { auth.deleteAccount() } returns SupabaseResult.NetworkError(RuntimeException("offline"))

        val state = delete()

        assertTrue(state.syncMessage!!.contains("couldn't be"))
        coVerify(exactly = 0) { profiles.clearCloudData() }
    }

    @Test
    fun bothSucceeding_clearsTheLinkAndReportsSuccess() {
        coEvery { cloud.deleteCloudProfile() } returns SupabaseResult.Success(Unit)
        coEvery { auth.deleteAccount() } returns SupabaseResult.Success(Unit)

        val state = delete()

        assertEquals("Cloud data and account deleted successfully.", state.syncMessage)
        assertFalse(state.isOptedIn)
        coVerify(exactly = 1) { profiles.clearCloudData() }
    }
}
