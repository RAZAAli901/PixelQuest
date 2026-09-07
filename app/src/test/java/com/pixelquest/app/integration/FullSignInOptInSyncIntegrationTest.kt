package com.pixelquest.app.integration

import android.content.Context
import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.AuthViewModel
import com.pixelquest.app.auth.GoogleAuthManager
import com.pixelquest.app.auth.GoogleAuthResult
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class InMemoryCloudDatabase {
    val profilesTable = mutableMapOf<String, CloudProfileDto>()
}

class TestSupabaseAuthRepository(
    private val inMemoryDb: InMemoryCloudDatabase
) : AuthRepository {
    override val currentUser = MutableStateFlow<AuthUser?>(null)

    override suspend fun exchangeGoogleIdToken(idToken: String): SupabaseResult<AuthUser> {
        val authUser = AuthUser(
            id = "supabase-uuid-9999",
            email = "player@google.com",
            displayName = "Google Real Name"
        )
        currentUser.value = authUser
        return SupabaseResult.Success(authUser)
    }

    override suspend fun signOut(): SupabaseResult<Unit> {
        currentUser.value = null
        return SupabaseResult.Success(Unit)
    }

    override suspend fun getInitialUser(): AuthUser? = currentUser.value
}

class TestCloudProfileRepository(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val inMemoryDb: InMemoryCloudDatabase
) : CloudProfileRepository {

    override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> {
        userProfileRepository.updateLeaderboardSettings(optIn, displayName)
        val user = authRepository.currentUser.value
            ?: return SupabaseResult.AuthError(IllegalStateException("Not authenticated"), "Auth required")

        val profile = (userProfileRepository as TestUserProfileRepository).currentProfile
        val dto = CloudProfileDto(
            id = user.id,
            displayName = displayName,
            currentStreak = 10,
            longestStreak = 22,
            level = profile.level,
            totalXp = profile.totalXp,
            leaderboardOptIn = optIn,
            updatedAt = "2026-09-08T03:00:00Z"
        )
        inMemoryDb.profilesTable[dto.id] = dto
        return SupabaseResult.Success(Unit)
    }

    override suspend fun syncProfileToCloud(): SupabaseResult<Unit> {
        val user = authRepository.currentUser.value
            ?: return SupabaseResult.AuthError(IllegalStateException("Not authenticated"), "Auth required")
        val profile = (userProfileRepository as TestUserProfileRepository).currentProfile
        val dto = CloudProfileDto(
            id = user.id,
            displayName = profile.leaderboardDisplayName ?: "Hero",
            currentStreak = 10,
            longestStreak = 22,
            level = profile.level,
            totalXp = profile.totalXp,
            leaderboardOptIn = profile.leaderboardOptIn,
            updatedAt = "2026-09-08T03:05:00Z"
        )
        inMemoryDb.profilesTable[dto.id] = dto
        return SupabaseResult.Success(Unit)
    }
}

class TestUserProfileRepository : UserProfileRepository {
    var currentProfile = UserProfileEntity(
        id = 1,
        username = "SecretLocalPlayer",
        avatarId = "avatar_knight",
        level = 7,
        totalXp = 1950,
        leaderboardOptIn = false,
        leaderboardDisplayName = null
    )
    val flow = MutableStateFlow<UserProfileEntity?>(currentProfile)

    override fun getProfile(): Flow<UserProfileEntity?> = flow
    override suspend fun insertProfile(profile: UserProfileEntity) {
        currentProfile = profile
        flow.value = profile
    }
    override suspend fun updateProfile(profile: UserProfileEntity) {
        currentProfile = profile
        flow.value = profile
    }
    override suspend fun performLevelUp(): UserProfileEntity? = null
    override suspend fun updateSupabaseUserId(userId: String?) {
        currentProfile = currentProfile.copy(supabaseUserId = userId)
        flow.value = currentProfile
    }
    override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) {
        currentProfile = currentProfile.copy(leaderboardOptIn = optIn, leaderboardDisplayName = displayName)
        flow.value = currentProfile
    }
    override suspend fun updateLeaderboardOptIn(optIn: Boolean) {
        currentProfile = currentProfile.copy(leaderboardOptIn = optIn)
        flow.value = currentProfile
    }
}

class TestGoogleAuthManager : GoogleAuthManager {
    override suspend fun signInWithGoogle(activityContext: Context): GoogleAuthResult {
        return GoogleAuthResult.Success(
            idToken = "valid-mock-google-id-token",
            email = "player@google.com",
            displayName = "Google Real Name"
        )
    }
    override suspend fun signOut() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class FullSignInOptInSyncIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var inMemoryDb: InMemoryCloudDatabase
    private lateinit var googleAuthManager: TestGoogleAuthManager
    private lateinit var authRepository: TestSupabaseAuthRepository
    private lateinit var userProfileRepository: TestUserProfileRepository
    private lateinit var cloudProfileRepository: TestCloudProfileRepository
    private lateinit var authViewModel: AuthViewModel
    private lateinit var accountViewModel: AccountViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        inMemoryDb = InMemoryCloudDatabase()
        googleAuthManager = TestGoogleAuthManager()
        authRepository = TestSupabaseAuthRepository(inMemoryDb)
        userProfileRepository = TestUserProfileRepository()
        cloudProfileRepository = TestCloudProfileRepository(authRepository, userProfileRepository, inMemoryDb)

        authViewModel = AuthViewModel(googleAuthManager, authRepository, userProfileRepository)
        accountViewModel = AccountViewModel(userProfileRepository, cloudProfileRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFullSignInOptInSyncFlow_executesEndToEndSuccessfully() = runTest {
        advanceUntilIdle()

        // 1. Initial State: Signed out, Opt-in defaults to OFF
        assertTrue(authViewModel.uiState.value is AuthUiState.SignedOut)
        assertFalse(accountViewModel.uiState.value.isOptedIn)
        assertEquals(0, inMemoryDb.profilesTable.size)

        // 2. Perform Google Sign-In with token exchange to Supabase
        val mockContext = mock(Context::class.java)
        authViewModel.signInWithGoogle(mockContext)
        advanceUntilIdle()

        // Verify signed in state
        assertTrue(authViewModel.uiState.value is AuthUiState.SignedIn)
        val signedInUser = (authViewModel.uiState.value as AuthUiState.SignedIn).user
        assertEquals("supabase-uuid-9999", signedInUser.id)
        assertEquals("supabase-uuid-9999", userProfileRepository.currentProfile.supabaseUserId)

        // 3. User configures public pseudonym
        accountViewModel.onDisplayNameChanged("Arcane_Hunter")
        advanceUntilIdle()

        // 4. User triggers opt-in toggle -> confirmation dialog required
        accountViewModel.onOptInToggleClicked(true)
        advanceUntilIdle()
        assertTrue("Confirmation dialog must be visible", accountViewModel.uiState.value.showConfirmDialog)

        // 5. User confirms opt-in in dialog -> triggers initial sync to cloud
        accountViewModel.confirmOptIn()
        advanceUntilIdle()

        // Verify local Room state updated
        assertTrue(accountViewModel.uiState.value.isOptedIn)
        assertFalse(accountViewModel.uiState.value.showConfirmDialog)
        assertTrue(userProfileRepository.currentProfile.leaderboardOptIn)
        assertEquals("Arcane_Hunter", userProfileRepository.currentProfile.leaderboardDisplayName)

        // 6. Verify cloud database row
        assertEquals(1, inMemoryDb.profilesTable.size)
        val cloudRow = inMemoryDb.profilesTable["supabase-uuid-9999"]
        assertNotNull("Cloud profile row must exist", cloudRow)
        assertEquals("supabase-uuid-9999", cloudRow!!.id)
        assertEquals("Arcane_Hunter", cloudRow.displayName)
        // Ensure privacy requirement: Google name and local hero username are NEVER leaked
        assertNotEquals("Google Real Name", cloudRow.displayName)
        assertNotEquals("player@google.com", cloudRow.displayName)
        assertNotEquals("SecretLocalPlayer", cloudRow.displayName)
        assertEquals(7, cloudRow.level)
        assertEquals(1950, cloudRow.totalXp)
        assertEquals(10, cloudRow.currentStreak)
        assertEquals(22, cloudRow.longestStreak)
        assertTrue(cloudRow.leaderboardOptIn)

        // 7. Test manual "Sync Now" debugging push
        accountViewModel.syncNow()
        advanceUntilIdle()
        assertEquals("Cloud sync successful!", accountViewModel.uiState.value.syncMessage)
        assertNotNull(accountViewModel.uiState.value.lastSyncTime)
    }
}
