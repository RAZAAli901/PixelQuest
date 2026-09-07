package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.ui.screens.account.AccountViewModel
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HangingCloudProfileRepository : CloudProfileRepository {
    val syncStarted = CompletableDeferred<Unit>()
    val syncShouldComplete = CompletableDeferred<Unit>()
    var syncCancelled = false

    override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> {
        syncStarted.complete(Unit)
        try {
            syncShouldComplete.await()
            return SupabaseResult.Success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            syncCancelled = true
            throw e
        }
    }

    override suspend fun syncProfileToCloud(): SupabaseResult<Unit> {
        syncStarted.complete(Unit)
        try {
            syncShouldComplete.await()
            return SupabaseResult.Success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            syncCancelled = true
            throw e
        }
    }
}

class FakeUserProfileRepoForCancellation : UserProfileRepository {
    val flow = MutableStateFlow(UserProfileEntity(id = 1, username = "Hero", avatarId = "1", leaderboardOptIn = true))
    override fun getProfile(): Flow<UserProfileEntity?> = flow
    override suspend fun insertProfile(profile: UserProfileEntity) {}
    override suspend fun updateProfile(profile: UserProfileEntity) {}
    override suspend fun performLevelUp(): UserProfileEntity? = null
    override suspend fun updateSupabaseUserId(userId: String?) {}
    override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) {}
    override suspend fun updateLeaderboardOptIn(optIn: Boolean) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class SyncCancellationOnSignOutTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var hangingCloudRepo: HangingCloudProfileRepository
    private lateinit var userProfileRepo: FakeUserProfileRepoForCancellation
    private lateinit var accountViewModel: AccountViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        hangingCloudRepo = HangingCloudProfileRepository()
        userProfileRepo = FakeUserProfileRepoForCancellation()
        accountViewModel = AccountViewModel(userProfileRepo, hangingCloudRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun cancelActiveSync_duringSyncNow_cancelsGracefullyWithoutCrashing() = runTest {
        advanceUntilIdle()

        // Trigger sync
        accountViewModel.syncNow()
        testDispatcher.scheduler.runCurrent()

        assertTrue("Sync should be marked as active", accountViewModel.uiState.value.isSyncing)

        // Cancel sync (as happens during Sign Out)
        accountViewModel.cancelActiveSync()
        advanceUntilIdle()

        assertFalse("Syncing state must be false after cancellation", accountViewModel.uiState.value.isSyncing)
        assertEquals("Sync cancelled.", accountViewModel.uiState.value.syncMessage)
        assertTrue("Cloud repo must have observed CancellationException", hangingCloudRepo.syncCancelled)
    }
}
