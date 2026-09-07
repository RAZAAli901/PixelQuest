package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.ui.screens.account.AccountViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ConfigurableCloudProfileRepo : CloudProfileRepository {
    var syncResult: SupabaseResult<Unit> = SupabaseResult.Success(Unit)
    var syncCallsCount = 0

    override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> {
        syncCallsCount++
        return syncResult
    }

    override suspend fun syncProfileToCloud(): SupabaseResult<Unit> {
        syncCallsCount++
        return syncResult
    }
}

class FakeUserProfileRepoForEdgeCases : UserProfileRepository {
    val flow = MutableStateFlow(
        UserProfileEntity(
            id = 1,
            username = "EdgeHero",
            avatarId = "1",
            leaderboardOptIn = false,
            leaderboardDisplayName = null
        )
    )

    override fun getProfile(): Flow<UserProfileEntity?> = flow
    override suspend fun insertProfile(profile: UserProfileEntity) {}
    override suspend fun updateProfile(profile: UserProfileEntity) {}
    override suspend fun performLevelUp(): UserProfileEntity? = null
    override suspend fun updateSupabaseUserId(userId: String?) {}
    override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) {
        flow.value = flow.value.copy(leaderboardOptIn = optIn, leaderboardDisplayName = displayName)
    }
    override suspend fun updateLeaderboardOptIn(optIn: Boolean) {
        flow.value = flow.value.copy(leaderboardOptIn = optIn)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CloudSyncEdgeCasesTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var cloudRepo: ConfigurableCloudProfileRepo
    private lateinit var userProfileRepo: FakeUserProfileRepoForEdgeCases
    private lateinit var accountViewModel: AccountViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        cloudRepo = ConfigurableCloudProfileRepo()
        userProfileRepo = FakeUserProfileRepoForEdgeCases()
        accountViewModel = AccountViewModel(userProfileRepo, cloudRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun syncNow_whenNotOptedIn_blocksCallAndShowsMessage() = runTest {
        advanceUntilIdle()
        assertFalse(accountViewModel.uiState.value.isOptedIn)

        accountViewModel.syncNow()
        advanceUntilIdle()

        assertEquals(0, cloudRepo.syncCallsCount)
        assertEquals("Opt-in to leaderboard before syncing.", accountViewModel.uiState.value.syncMessage)
        assertFalse(accountViewModel.uiState.value.isSyncing)
    }

    @Test
    fun syncNow_whenNetworkFails_setsNetworkErrorMessageAndResetsSyncing() = runTest {
        advanceUntilIdle()
        // Opt in first
        accountViewModel.onDisplayNameChanged("EdgeMaster")
        accountViewModel.confirmOptIn()
        advanceUntilIdle()

        // Configure network failure
        cloudRepo.syncResult = SupabaseResult.NetworkError(IOException("Connection timed out"))

        accountViewModel.syncNow()
        advanceUntilIdle()

        assertFalse(accountViewModel.uiState.value.isSyncing)
        val msg = accountViewModel.uiState.value.syncMessage
        assertNotNull(msg)
        assertTrue(msg!!.contains("Network error", ignoreCase = true))
    }

    @Test
    fun syncNow_whenAuthExpired_promptsUserToSignInAgain() = runTest {
        advanceUntilIdle()
        accountViewModel.onDisplayNameChanged("EdgeMaster")
        accountViewModel.confirmOptIn()
        advanceUntilIdle()

        // Configure Auth failure
        cloudRepo.syncResult = SupabaseResult.AuthError(IllegalStateException("JWT expired"))

        accountViewModel.syncNow()
        advanceUntilIdle()

        assertFalse(accountViewModel.uiState.value.isSyncing)
        val msg = accountViewModel.uiState.value.syncMessage
        assertNotNull(msg)
        assertTrue(msg!!.contains("Auth error", ignoreCase = true))
    }

    @Test
    fun syncNow_whenServerErrorOccurs_displaysErrorMessage() = runTest {
        advanceUntilIdle()
        accountViewModel.onDisplayNameChanged("EdgeMaster")
        accountViewModel.confirmOptIn()
        advanceUntilIdle()

        // Configure 500 error
        cloudRepo.syncResult = SupabaseResult.ServerError(500, "Internal Server Error")

        accountViewModel.syncNow()
        advanceUntilIdle()

        assertFalse(accountViewModel.uiState.value.isSyncing)
        val msg = accountViewModel.uiState.value.syncMessage
        assertNotNull(msg)
        assertTrue(msg!!.contains("Sync failed", ignoreCase = true))
    }
}
