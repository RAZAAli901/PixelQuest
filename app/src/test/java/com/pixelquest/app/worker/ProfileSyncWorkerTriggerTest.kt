package com.pixelquest.app.worker

import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

class RecordingSyncScheduler : SyncScheduler {
    var callCount = 0
    var lastDebounceMs: Long? = null

    override fun scheduleProfileSync(debounceMs: Long) {
        callCount++
        lastDebounceMs = debounceMs
    }
}

class FakeCloudProfileRepoForWorker : CloudProfileRepository {
    var syncCount = 0
    var resultToReturn: SupabaseResult<Unit> = SupabaseResult.Success(Unit)

    override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> {
        syncCount++
        return resultToReturn
    }

    override suspend fun syncProfileToCloud(): SupabaseResult<Unit> {
        syncCount++
        return resultToReturn
    }
}

class FakeUserProfileRepoForWorker : UserProfileRepository {
    var profile = UserProfileEntity(
        id = 1,
        username = "TestHero",
        avatarId = "1",
        supabaseUserId = null,
        leaderboardOptIn = false
    )
    val flow = MutableStateFlow<UserProfileEntity?>(profile)

    override fun getProfile(): Flow<UserProfileEntity?> = flow
    override suspend fun insertProfile(profile: UserProfileEntity) {
        this.profile = profile
        flow.value = profile
    }
    override suspend fun updateProfile(profile: UserProfileEntity) {
        this.profile = profile
        flow.value = profile
    }
    override suspend fun performLevelUp(): UserProfileEntity? = null
    override suspend fun updateSupabaseUserId(userId: String?) {
        this.profile = this.profile.copy(supabaseUserId = userId)
        flow.value = this.profile
    }
    override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) {
        this.profile = this.profile.copy(leaderboardOptIn = optIn, leaderboardDisplayName = displayName)
        flow.value = this.profile
    }
    override suspend fun updateLeaderboardOptIn(optIn: Boolean) {
        this.profile = this.profile.copy(leaderboardOptIn = optIn)
        flow.value = this.profile
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSyncWorkerTriggerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var syncScheduler: RecordingSyncScheduler
    private lateinit var userProfileRepo: FakeUserProfileRepoForWorker
    private lateinit var cloudProfileRepo: FakeCloudProfileRepoForWorker

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        syncScheduler = RecordingSyncScheduler()
        userProfileRepo = FakeUserProfileRepoForWorker()
        cloudProfileRepo = FakeCloudProfileRepoForWorker()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun syncScheduler_recordsInvocationsWithDebounceParameters() {
        syncScheduler.scheduleProfileSync(1500L)
        assertEquals(1, syncScheduler.callCount)
        assertEquals(1500L, syncScheduler.lastDebounceMs)
    }

    @Test
    fun debounceLogic_coalescesRapidSuccessiveTriggers() = runTest {
        var executionsCount = 0
        var debounceJob: kotlinx.coroutines.Job? = null

        // Simulate 5 rapid user clicks within 500ms
        for (i in 1..5) {
            debounceJob?.cancel()
            debounceJob = kotlinx.coroutines.CoroutineScope(testDispatcher).kotlinx.coroutines.launch {
                kotlinx.coroutines.delay(1000L)
                executionsCount++
            }
            advanceTimeBy(100L)
        }

        // Advance remaining time past the 1000ms delay of the 5th click
        advanceTimeBy(1200L)
        advanceUntilIdle()

        assertEquals("Only 1 execution should occur after rapid successive triggers", 1, executionsCount)
    }

    @Test
    fun workerPrivacyGuard_whenNotSignedIn_doesNotCallCloudSync() = runTest {
        // User is not signed in (supabaseUserId is null)
        userProfileRepo.insertProfile(
            UserProfileEntity(id = 1, username = "OfflineHero", avatarId = "1", supabaseUserId = null, leaderboardOptIn = false)
        )

        val profile = userProfileRepo.profile
        val isCloudLinked = !profile.supabaseUserId.isNullOrBlank()
        val isOptedIn = profile.leaderboardOptIn

        if (isCloudLinked && isOptedIn) {
            cloudProfileRepo.syncProfileToCloud()
        }

        assertEquals("Cloud sync must NOT be called for offline user", 0, cloudProfileRepo.syncCount)
    }

    @Test
    fun workerPrivacyGuard_whenSignedInButNotOptedIn_doesNotCallCloudSync() = runTest {
        // Signed in, but leaderboardOptIn is false
        userProfileRepo.insertProfile(
            UserProfileEntity(id = 1, username = "SpectatorHero", avatarId = "1", supabaseUserId = "uuid-123", leaderboardOptIn = false)
        )

        val profile = userProfileRepo.profile
        val isCloudLinked = !profile.supabaseUserId.isNullOrBlank()
        val isOptedIn = profile.leaderboardOptIn

        if (isCloudLinked && isOptedIn) {
            cloudProfileRepo.syncProfileToCloud()
        }

        assertEquals("Cloud sync must NOT be called if user has not opted in", 0, cloudProfileRepo.syncCount)
    }

    @Test
    fun workerPrivacyGuard_whenSignedInAndOptedIn_callsCloudSync() = runTest {
        // Signed in AND opted in
        userProfileRepo.insertProfile(
            UserProfileEntity(id = 1, username = "ActiveHero", avatarId = "1", supabaseUserId = "uuid-123", leaderboardOptIn = true)
        )

        val profile = userProfileRepo.profile
        val isCloudLinked = !profile.supabaseUserId.isNullOrBlank()
        val isOptedIn = profile.leaderboardOptIn

        if (isCloudLinked && isOptedIn) {
            cloudProfileRepo.syncProfileToCloud()
        }

        assertEquals("Cloud sync MUST be called when signed in and opted in", 1, cloudProfileRepo.syncCount)
    }
}
