package com.pixelquest.app.ui.screens.leaderboard

import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.LeaderboardRepository
import com.pixelquest.app.data.repository.LeaderboardSortMode
import com.pixelquest.app.data.repository.UserLeaderboardRank
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

private class FakeAuthRepoForErrorTest : AuthRepository {
    val userFlow = MutableStateFlow<AuthUser?>(
        AuthUser(id = "user-123", email = "hero@pixelquest.test", displayName = "KnightTester")
    )
    override val currentUser: Flow<AuthUser?> = userFlow
    override suspend fun exchangeGoogleIdToken(idToken: String, rawNonce: String?): SupabaseResult<AuthUser> =
        SupabaseResult.Success(userFlow.value!!)
    override suspend fun signOut(): SupabaseResult<Unit> {
        userFlow.value = null
        return SupabaseResult.Success(Unit)
    }
    override suspend fun getInitialUser(): AuthUser? = userFlow.value
}

private class FakeUserProfileRepoForErrorTest : UserProfileRepository {
    val profileFlow = MutableStateFlow<UserProfileEntity?>(
        UserProfileEntity(
            id = 1,
            username = "KnightHero",
            avatarId = "1",
            level = 5,
            totalXp = 1500,
            supabaseUserId = "user-123",
            leaderboardOptIn = true,
            leaderboardDisplayName = "Knight_Leader"
        )
    )

    override fun getProfile(): Flow<UserProfileEntity?> = profileFlow
    override suspend fun insertProfile(profile: UserProfileEntity) { profileFlow.value = profile }
    override suspend fun updateProfile(profile: UserProfileEntity) { profileFlow.value = profile }
    override suspend fun performLevelUp(): UserProfileEntity? = profileFlow.value
    override suspend fun updateSupabaseUserId(userId: String?) {
        profileFlow.value = profileFlow.value?.copy(supabaseUserId = userId)
    }
    override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) {
        profileFlow.value = profileFlow.value?.copy(leaderboardOptIn = optIn, leaderboardDisplayName = displayName)
    }
    override suspend fun updateLeaderboardOptIn(optIn: Boolean) {
        profileFlow.value = profileFlow.value?.copy(leaderboardOptIn = optIn)
    }
}

private class FakeLeaderboardRepoForErrorTest : LeaderboardRepository {
    var resultStreak: SupabaseResult<List<CloudProfileDto>> = SupabaseResult.Success(emptyList())
    var resultLevel: SupabaseResult<List<CloudProfileDto>> = SupabaseResult.Success(emptyList())
    var fetchCount = 0

    override suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>> = resultStreak

    override suspend fun getTopByStreak(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
        fetchCount++
        return resultStreak
    }

    override suspend fun getTopByLevel(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
        fetchCount++
        return resultLevel
    }

    override suspend fun getCurrentUserRank(
        sortMode: LeaderboardSortMode,
        userId: String?
    ): SupabaseResult<UserLeaderboardRank?> = SupabaseResult.Success(null)
}

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardErrorStateTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeLeaderboardRepoForErrorTest
    private lateinit var fakeAuth: FakeAuthRepoForErrorTest
    private lateinit var fakeUserProfile: FakeUserProfileRepoForErrorTest
    private lateinit var viewModel: LeaderboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeLeaderboardRepoForErrorTest()
        fakeAuth = FakeAuthRepoForErrorTest()
        fakeUserProfile = FakeUserProfileRepoForErrorTest()
        viewModel = LeaderboardViewModel(
            leaderboardRepository = fakeRepo,
            authRepository = fakeAuth,
            userProfileRepository = fakeUserProfile
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun whenRepositoryReturnsNetworkError_uiStateReflectsNetworkErrorMessage() = runTest(testDispatcher) {
        fakeRepo.resultStreak = SupabaseResult.NetworkError(IOException("Connection timed out"))

        viewModel.loadInitialData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Network error", ignoreCase = true))
        assertTrue(state.errorMessage!!.contains("Connection timed out"))
        assertTrue(state.streakEntries.isEmpty())
    }

    @Test
    fun whenRepositoryReturnsServerError_uiStateReflectsServerErrorMessage() = runTest(testDispatcher) {
        fakeRepo.resultStreak = SupabaseResult.ServerError(code = 503, message = "Backend unreachable")

        viewModel.loadInitialData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("503"))
        assertTrue(state.errorMessage!!.contains("Leaderboard service unavailable", ignoreCase = true))
    }

    @Test
    fun whenRepositoryReturnsAuthError_uiStateReflectsAuthErrorMessage() = runTest(testDispatcher) {
        fakeRepo.resultStreak = SupabaseResult.AuthError("Session token expired")

        viewModel.loadInitialData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Authentication error", ignoreCase = true))
        assertTrue(state.errorMessage!!.contains("Session token expired"))
    }

    @Test
    fun whenRetryCalledAfterFailure_resetsErrorMessageAndLoadsData() = runTest(testDispatcher) {
        // First fail with network error
        fakeRepo.resultStreak = SupabaseResult.NetworkError(IOException("No route to host"))
        viewModel.loadInitialData()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage != null)

        // Then network is restored; retry/refresh succeeds
        val sampleData = listOf(
            CloudProfileDto(
                id = "hero-1",
                displayName = "DragonSlayer",
                currentStreak = 12,
                longestStreak = 15,
                level = 7,
                totalXp = 2400,
                leaderboardOptIn = true
            )
        )
        fakeRepo.resultStreak = SupabaseResult.Success(sampleData)

        viewModel.refresh()
        advanceUntilIdle()

        val refreshedState = viewModel.uiState.value
        assertFalse(refreshedState.isLoading)
        assertNull(refreshedState.errorMessage)
        assertEquals(1, refreshedState.streakEntries.size)
        assertEquals("DragonSlayer", refreshedState.streakEntries[0].displayName)
    }

    @Test
    fun clearError_removesErrorMessageFromState() = runTest(testDispatcher) {
        fakeRepo.resultStreak = SupabaseResult.UnknownError(RuntimeException("Unexpected crash"))
        viewModel.loadInitialData()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
