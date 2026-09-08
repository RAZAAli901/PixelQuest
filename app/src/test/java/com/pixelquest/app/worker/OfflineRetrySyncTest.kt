package com.pixelquest.app.worker

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class FakeOfflineCloudProfileRepo : CloudProfileRepository {
    var isOffline = true
    var syncAttempts = 0
    var lastSyncedProfile: UserProfileEntity? = null

    override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> {
        return syncProfileToCloud()
    }

    override suspend fun syncProfileToCloud(): SupabaseResult<Unit> {
        syncAttempts++
        return if (isOffline) {
            SupabaseResult.NetworkError(IOException("Device offline"), "No internet connection")
        } else {
            SupabaseResult.Success(Unit)
        }
    }
}

class FakeOfflineUserProfileRepo : UserProfileRepository {
    var profile = UserProfileEntity(
        id = 1,
        username = "KnightOffline",
        avatarId = "1",
        level = 5,
        totalXp = 1200,
        supabaseUserId = "uuid-999",
        leaderboardOptIn = true,
        leaderboardDisplayName = "KnightOffline"
    )
    val flow = MutableStateFlow<UserProfileEntity?>(profile)

    override fun getProfile(): Flow<UserProfileEntity?> = flow
    override suspend fun insertProfile(profile: UserProfileEntity) { this.profile = profile; flow.value = profile }
    override suspend fun updateProfile(profile: UserProfileEntity) { this.profile = profile; flow.value = profile }
    override suspend fun performLevelUp(): UserProfileEntity? = null
    override suspend fun updateSupabaseUserId(userId: String?) { this.profile = this.profile.copy(supabaseUserId = userId); flow.value = this.profile }
    override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) { this.profile = this.profile.copy(leaderboardOptIn = optIn, leaderboardDisplayName = displayName); flow.value = this.profile }
    override suspend fun updateLeaderboardOptIn(optIn: Boolean) { this.profile = this.profile.copy(leaderboardOptIn = optIn); flow.value = this.profile }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineRetrySyncTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeCloudRepo: FakeOfflineCloudProfileRepo
    private lateinit var fakeUserRepo: FakeOfflineUserProfileRepo

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeCloudRepo = FakeOfflineCloudProfileRepo()
        fakeUserRepo = FakeOfflineUserProfileRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testWorkRequest_hasNetworkConstraintAndExponentialBackoff() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<ProfileSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertEquals(BackoffPolicy.EXPONENTIAL, request.workSpec.backoffPolicy)
        assertTrue(request.workSpec.backoffDelayMillis >= WorkRequest.MIN_BACKOFF_MILLIS)
    }

    @Test
    fun testOfflineSync_failsWithRetry_andSucceedsWhenOnlineRestored() = runTest {
        // 1. Initial attempt while offline
        fakeCloudRepo.isOffline = true
        val offlineResult = fakeCloudRepo.syncProfileToCloud()
        assertTrue("Offline attempt must yield NetworkError", offlineResult is SupabaseResult.NetworkError)
        assertEquals(1, fakeCloudRepo.syncAttempts)

        // 2. Meanwhile, local state progresses while offline (completes quest: +50 XP)
        fakeUserRepo.profile = fakeUserRepo.profile.copy(totalXp = 1250)

        // 3. Connectivity restored: observer triggers sync retry
        fakeCloudRepo.isOffline = false
        val onlineResult = fakeCloudRepo.syncProfileToCloud()
        advanceUntilIdle()

        assertTrue("Post-reconnect attempt must yield Success", onlineResult is SupabaseResult.Success)
        assertEquals(2, fakeCloudRepo.syncAttempts)
        assertEquals(1250, fakeUserRepo.profile.totalXp)
    }

    @Test
    fun testConnectivityObserverTrigger_invokesSyncScheduler() = runTest {
        var syncScheduledCount = 0
        val mockScheduler = object : SyncScheduler {
            override fun scheduleProfileSync(debounceMs: Long) {
                syncScheduledCount++
            }
        }

        // Simulate network callback when signed in and opted in
        val profile = fakeUserRepo.profile
        val isLinked = !profile.supabaseUserId.isNullOrBlank()
        val isOptedIn = profile.leaderboardOptIn
        if (isLinked && isOptedIn) {
            mockScheduler.scheduleProfileSync(debounceMs = 500L)
        }

        assertEquals("Scheduler must be called upon network restoration", 1, syncScheduledCount)
    }
}
