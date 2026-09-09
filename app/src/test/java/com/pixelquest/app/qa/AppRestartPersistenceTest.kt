package com.pixelquest.app.qa

import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.AuthViewModel
import com.pixelquest.app.auth.GoogleAuthManager
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Step 46 Verification Test:
 * Validates that upon a cold app restart:
 * 1. Supabase cached auth session is restored by AuthViewModel during initialization.
 * 2. Room database persists user profile attributes (supabaseUserId, leaderboardOptIn, leaderboardDisplayName).
 * 3. AccountViewModel immediately restores the opted-in status and chosen display name.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppRestartPersistenceTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testStatePersistenceAcrossAppRestart() = runTest {
        // Simulated persisted storage in Room
        val persistedProfile = UserProfileEntity(
            id = 1,
            username = "LocalHero",
            avatarId = "avatar_1",
            level = 9,
            totalXp = 2700,
            supabaseUserId = "restored-user-uuid-888",
            leaderboardOptIn = true,
            leaderboardDisplayName = "PixelLegend_88"
        )

        val fakeUserRepo = object : UserProfileRepository {
            val flow = MutableStateFlow<UserProfileEntity?>(persistedProfile)
            override fun getProfile(): Flow<UserProfileEntity?> = flow
            override suspend fun insertProfile(profile: UserProfileEntity) {}
            override suspend fun updateProfile(profile: UserProfileEntity) {}
            override suspend fun performLevelUp(): UserProfileEntity? = null
            override suspend fun updateSupabaseUserId(userId: String?) {}
            override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) {}
            override suspend fun updateLeaderboardOptIn(optIn: Boolean) {}
        }

        val fakeAuthRepo = object : AuthRepository {
            override val currentUser = MutableStateFlow<AuthUser?>(
                AuthUser("restored-user-uuid-888", "hero@gmail.com", "Hero")
            )
            override suspend fun exchangeGoogleIdToken(idToken: String): SupabaseResult<AuthUser> =
                SupabaseResult.Success(currentUser.value!!)
            override suspend fun signOut(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
            override suspend fun getInitialUser(): AuthUser? = currentUser.value
        }

        val fakeCloudRepo = object : CloudProfileRepository {
            override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> =
                SupabaseResult.Success(Unit)
            override suspend fun syncProfileToCloud(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
        }

        val mockGoogleAuthManager = mock(GoogleAuthManager::class.java)

        // Simulate app restart: Instantiate new ViewModels
        val restoredAuthViewModel = AuthViewModel(mockGoogleAuthManager, fakeAuthRepo, fakeUserRepo)
        val restoredAccountViewModel = AccountViewModel(fakeUserRepo, fakeCloudRepo)
        advanceUntilIdle()

        // 1. Verify AuthViewModel restored to SignedIn
        assertTrue("AuthViewModel must restore SignedIn state", restoredAuthViewModel.uiState.value is AuthUiState.SignedIn)
        val user = (restoredAuthViewModel.uiState.value as AuthUiState.SignedIn).user
        assertEquals("restored-user-uuid-888", user.id)

        // 2. Verify AccountViewModel restored opted-in flag and display name
        assertTrue("Opt-in preference must be persisted across restart", restoredAccountViewModel.uiState.value.isOptedIn)
        assertEquals("PixelLegend_88", restoredAccountViewModel.uiState.value.displayNameInput)
        assertEquals(9, restoredAccountViewModel.uiState.value.profile?.level)
        assertEquals(2700, restoredAccountViewModel.uiState.value.profile?.totalXp)

        // 3. Verify LeaderboardViewModel restores into SignedInAndOptedIn across cold restart
        val fakeLeaderboardRepo = object : com.pixelquest.app.data.repository.LeaderboardRepository {
            override suspend fun getProfiles(): SupabaseResult<List<com.pixelquest.app.data.remote.model.CloudProfileDto>> =
                SupabaseResult.Success(emptyList())
            override suspend fun getTopByStreak(limit: Long, offset: Long): SupabaseResult<List<com.pixelquest.app.data.remote.model.CloudProfileDto>> =
                SupabaseResult.Success(listOf(
                    com.pixelquest.app.data.remote.model.CloudProfileDto(
                        id = "restored-user-uuid-888",
                        displayName = "PixelLegend_88",
                        currentStreak = 14,
                        longestStreak = 20,
                        level = 9,
                        totalXp = 2700,
                        leaderboardOptIn = true
                    )
                ))
            override suspend fun getTopByLevel(limit: Long, offset: Long): SupabaseResult<List<com.pixelquest.app.data.remote.model.CloudProfileDto>> =
                SupabaseResult.Success(emptyList())
            override suspend fun getCurrentUserRank(sortMode: com.pixelquest.app.data.repository.LeaderboardSortMode, userId: String?): SupabaseResult<com.pixelquest.app.data.repository.UserLeaderboardRank?> =
                SupabaseResult.Success(null)
        }

        val restoredLeaderboardViewModel = com.pixelquest.app.ui.screens.leaderboard.LeaderboardViewModel(
            fakeLeaderboardRepo,
            fakeAuthRepo,
            fakeUserRepo
        )
        advanceUntilIdle()

        val leaderboardState = restoredLeaderboardViewModel.uiState.value
        assertTrue("Leaderboard must restore SignedInAndOptedIn state", leaderboardState.authState is com.pixelquest.app.ui.screens.leaderboard.LeaderboardAuthState.SignedInAndOptedIn)
        assertEquals("PixelLegend_88", (leaderboardState.authState as com.pixelquest.app.ui.screens.leaderboard.LeaderboardAuthState.SignedInAndOptedIn).displayName)
    }
}
