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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class FakeAuthRepoForFailure : AuthRepository {
    var exchangeResult: SupabaseResult<AuthUser> = SupabaseResult.AuthError(
        IllegalStateException("Invalid token"),
        "Bad audience in token"
    )
    var signOutCalled: Boolean = false

    override val currentUser = MutableStateFlow<AuthUser?>(null)
    override suspend fun exchangeGoogleIdToken(idToken: String): SupabaseResult<AuthUser> = exchangeResult
    override suspend fun signOut(): SupabaseResult<Unit> {
        signOutCalled = true
        currentUser.value = null
        return SupabaseResult.Success(Unit)
    }
    override suspend fun getInitialUser(): AuthUser? = null
}

class FakeGoogleAuthManagerForFailure : GoogleAuthManager {
    var signOutCalled: Boolean = false
    var resultToReturn: GoogleAuthResult = GoogleAuthResult.Success("sample-google-id-token")

    override suspend fun signInWithGoogle(activityContext: Context): GoogleAuthResult = resultToReturn
    override suspend fun signOut() { signOutCalled = true }
}

class FakeUserProfileRepoForFailure : UserProfileRepository {
    var assignedUserId: String? = "initial-null"
    override fun getProfile(): Flow<UserProfileEntity?> = MutableStateFlow(null)
    override suspend fun insertProfile(profile: UserProfileEntity) {}
    override suspend fun updateProfile(profile: UserProfileEntity) {}
    override suspend fun performLevelUp(): UserProfileEntity? = null
    override suspend fun updateSupabaseUserId(userId: String?) { assignedUserId = userId }
    override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) {}
    override suspend fun updateLeaderboardOptIn(optIn: Boolean) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class TokenExchangeFailureTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var googleAuthManager: FakeGoogleAuthManagerForFailure
    private lateinit var authRepo: FakeAuthRepoForFailure
    private lateinit var userProfileRepo: FakeUserProfileRepoForFailure
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        googleAuthManager = FakeGoogleAuthManagerForFailure()
        authRepo = FakeAuthRepoForFailure()
        userProfileRepo = FakeUserProfileRepoForFailure()
        viewModel = AuthViewModel(googleAuthManager, authRepo, userProfileRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun whenGoogleSignInSucceeds_butSupabaseExchangeFails_performsRollbackAndShowsSpecificError() = runTest {
        advanceUntilIdle()

        val mockContext = mock(Context::class.java)
        viewModel.signInWithGoogle(mockContext)
        advanceUntilIdle()

        val currentState = viewModel.uiState.value
        assertTrue("State must be Error", currentState is AuthUiState.Error)
        val errorMsg = (currentState as AuthUiState.Error).message
        assertTrue("Error message must be specific", errorMsg.contains("cloud token exchange failed"))

        // Verify clean rollback - no half-signed-in state
        assertTrue("GoogleAuthManager.signOut must be called", googleAuthManager.signOutCalled)
        assertTrue("AuthRepository.signOut must be called", authRepo.signOutCalled)
        assertNull("Local profile supabaseUserId must be rolled back to null", userProfileRepo.assignedUserId)
    }
}
