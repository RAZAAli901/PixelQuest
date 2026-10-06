package com.pixelquest.app.ui.screens.account

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.worker.RecordingSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Leaving the leaderboard offline used to say "queued when online" without queueing anything: the
 * player's row stayed public until some later progress sync. A failed opt-out now schedules the sync
 * worker, which opts out whenever the local flag is off.
 */
class OfflineOptOutQueueTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class CloudReturning(private val result: SupabaseResult<Unit>) : CloudProfileRepository {
        override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
        override suspend fun syncProfileToCloud(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
        override suspend fun optOutFromLeaderboard(): SupabaseResult<Unit> = result
    }

    private fun optOutWith(result: SupabaseResult<Unit>): RecordingSyncScheduler {
        val scheduler = RecordingSyncScheduler()
        val profiles = FakeUserProfileRepository(
            UserProfileEntity(username = "Aria", avatarId = "avatar_hero", supabaseUserId = "user-1", leaderboardOptIn = true)
        )
        val viewModel = AccountViewModel(profiles, CloudReturning(result), syncScheduler = scheduler)

        viewModel.optOut()

        assertFalse(viewModel.uiState.value.isOptedIn)
        return scheduler
    }

    @Test
    fun offline_queuesTheOptOut_withoutDelay() {
        val scheduler = optOutWith(SupabaseResult.NetworkError(java.io.IOException("offline")))
        assertEquals(1, scheduler.callCount)
        assertEquals(0L, scheduler.lastDebounceMs)
    }

    @Test
    fun aServerError_isRetriedInTheBackground() {
        assertEquals(1, optOutWith(SupabaseResult.ServerError(503, "unavailable")).callCount)
    }

    @Test
    fun aConfirmedOptOut_needsNoRetry() {
        assertEquals(0, optOutWith(SupabaseResult.Success(Unit)).callCount)
    }
}
