package com.pixelquest.app.auth

import android.content.Context
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.IOException

class FakeGoogleAuthManagerForOffline : GoogleAuthManager {
    var isOffline = false

    override suspend fun signInWithGoogle(activityContext: Context): GoogleAuthResult {
        return if (isOffline) {
            GoogleAuthResult.Failure(
                IOException("Device offline"),
                "No internet connection detected. Please connect to the internet to sign in."
            )
        } else {
            GoogleAuthResult.Success("id-token", "hero@gmail.com", "Hero")
        }
    }

    override suspend fun signOut() {}
}

class FakeAuthRepoForOffline : AuthRepository {
    var returnNetworkError = false
    override val currentUser = MutableStateFlow<AuthUser?>(null)
    override suspend fun exchangeGoogleIdToken(idToken: String): SupabaseResult<AuthUser> {
        return if (returnNetworkError) {
            SupabaseResult.NetworkError(IOException("Network unreachable"), "Failed to connect to Supabase server.")
        } else {
            SupabaseResult.Success(AuthUser("uid-1", "hero@gmail.com", "Hero"))
        }
    }
    override suspend fun signOut(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
    override suspend fun getInitialUser(): AuthUser? = null
}

class FakeUserProfileRepoForOffline : UserProfileRepository {
    override fun getProfile(): Flow<UserProfileEntity?> = MutableStateFlow(null)
    override suspend fun insertProfile(profile: UserProfileEntity) {}
    override suspend fun updateProfile(profile: UserProfileEntity) {}
    override suspend fun performLevelUp(): UserProfileEntity? = null
    override suspend fun updateSupabaseUserId(userId: String?) {}
    override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) {}
    override suspend fun updateLeaderboardOptIn(optIn: Boolean) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineSignInTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var googleAuthManager: FakeGoogleAuthManagerForOffline
    private lateinit var authRepo: FakeAuthRepoForOffline
    private lateinit var userProfileRepo: FakeUserProfileRepoForOffline
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        googleAuthManager = FakeGoogleAuthManagerForOffline()
        authRepo = FakeAuthRepoForOffline()
        userProfileRepo = FakeUserProfileRepoForOffline()
        viewModel = AuthViewModel(googleAuthManager, authRepo, userProfileRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun whenDeviceOfflineDuringGoogleSignIn_showsClearNoInternetMessage() = runTest {
        advanceUntilIdle()
        googleAuthManager.isOffline = true

        val mockContext = mock(Context::class.java)
        viewModel.signInWithGoogle(mockContext)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("UI state must be Error", state is AuthUiState.Error)
        val msg = (state as AuthUiState.Error).message
        assertTrue(msg.contains("No internet connection", ignoreCase = true))
    }

    @Test
    fun whenOfflineDuringSupabaseTokenExchange_showsClearNoInternetMessage() = runTest {
        advanceUntilIdle()
        googleAuthManager.isOffline = false
        authRepo.returnNetworkError = true

        val mockContext = mock(Context::class.java)
        viewModel.signInWithGoogle(mockContext)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("UI state must be Error", state is AuthUiState.Error)
        val msg = (state as AuthUiState.Error).message
        assertTrue(msg.contains("connection failed", ignoreCase = true) || msg.contains("internet", ignoreCase = true))
    }
}
