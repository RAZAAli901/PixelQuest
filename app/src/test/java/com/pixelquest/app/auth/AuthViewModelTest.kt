package com.pixelquest.app.auth

import android.content.Context
import com.pixelquest.app.data.remote.SupabaseResult
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
import java.io.IOException

class FakeAuthRepository : AuthRepository {
    val userFlow = MutableStateFlow<AuthUser?>(null)
    var exchangeResult: SupabaseResult<AuthUser> = SupabaseResult.Success(
        AuthUser(id = "user_123", email = "test@pixelquest.com", displayName = "HeroTester")
    )
    var initialUser: AuthUser? = null

    override val currentUser: Flow<AuthUser?> = userFlow

    override suspend fun exchangeGoogleIdToken(idToken: String, rawNonce: String?): SupabaseResult<AuthUser> {
        if (exchangeResult is SupabaseResult.Success) {
            userFlow.value = (exchangeResult as SupabaseResult.Success<AuthUser>).data
        }
        return exchangeResult
    }

    override suspend fun signOut(): SupabaseResult<Unit> {
        userFlow.value = null
        return SupabaseResult.Success(Unit)
    }

    override suspend fun getInitialUser(): AuthUser? = initialUser
}

class FakeGoogleAuthManager(context: Context) : GoogleAuthManager(context) {
    var signInResult: GoogleAuthResult = GoogleAuthResult.Success(
        idToken = "fake_google_id_token",
        email = "test@pixelquest.com",
        displayName = "HeroTester"
    )
    var signOutCalled = false

    override suspend fun signInWithGoogle(activityContext: Context): GoogleAuthResult {
        return signInResult
    }

    override suspend fun signOut() {
        signOutCalled = true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var fakeGoogleAuthManager: FakeGoogleAuthManager
    private lateinit var viewModel: AuthViewModel

    // Mock Context using dynamic proxy or dummy implementation
    private val dummyContext = java.lang.reflect.Proxy.newProxyInstance(
        Context::class.java.classLoader,
        arrayOf(Context::class.java)
    ) { _, _, _ -> null } as Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepo = FakeAuthRepository()
        fakeGoogleAuthManager = FakeGoogleAuthManager(dummyContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_whenNoUser_isSignedOut() = runTest {
        viewModel = AuthViewModel(fakeGoogleAuthManager, fakeAuthRepo)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AuthUiState.SignedOut)
    }

    @Test
    fun initialState_whenUserExists_isSignedIn() = runTest {
        fakeAuthRepo.initialUser = AuthUser("uid_existing", "hero@pixelquest.com", "QuestHero")
        viewModel = AuthViewModel(fakeGoogleAuthManager, fakeAuthRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.SignedIn)
        assertEquals("uid_existing", (state as AuthUiState.SignedIn).user.id)
    }

    @Test
    fun signInWithGoogle_onSuccess_transitionsToSignedIn() = runTest {
        viewModel = AuthViewModel(fakeGoogleAuthManager, fakeAuthRepo)
        advanceUntilIdle()

        fakeGoogleAuthManager.signInResult = GoogleAuthResult.Success(
            idToken = "token_abc",
            email = "hero@pixelquest.com",
            displayName = "QuestHero"
        )
        fakeAuthRepo.exchangeResult = SupabaseResult.Success(
            AuthUser(id = "uid_new", email = "hero@pixelquest.com", displayName = "QuestHero")
        )

        viewModel.signInWithGoogle(dummyContext)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be SignedIn on success", state is AuthUiState.SignedIn)
        assertEquals("uid_new", (state as AuthUiState.SignedIn).user.id)
    }

    @Test
    fun signInWithGoogle_onCancelled_returnsToSignedOut() = runTest {
        viewModel = AuthViewModel(fakeGoogleAuthManager, fakeAuthRepo)
        advanceUntilIdle()

        fakeGoogleAuthManager.signInResult = GoogleAuthResult.Cancelled()
        viewModel.signInWithGoogle(dummyContext)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.SignedOut)
    }

    @Test
    fun signInWithGoogle_onNetworkError_transitionsToErrorAndRollsBack() = runTest {
        viewModel = AuthViewModel(fakeGoogleAuthManager, fakeAuthRepo)
        advanceUntilIdle()

        fakeGoogleAuthManager.signInResult = GoogleAuthResult.Success(
            idToken = "token_abc",
            email = "hero@pixelquest.com",
            displayName = "QuestHero"
        )
        fakeAuthRepo.exchangeResult = SupabaseResult.NetworkError(IOException("Connection failed"))

        viewModel.signInWithGoogle(dummyContext)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be Error on network failure", state is AuthUiState.Error)
        assertTrue("Google auth should be rolled back", fakeGoogleAuthManager.signOutCalled)
    }

    @Test
    fun signOut_transitionsFromSignedInToSignedOut() = runTest {
        fakeAuthRepo.initialUser = AuthUser("uid_active", "hero@pixelquest.com", "QuestHero")
        viewModel = AuthViewModel(fakeGoogleAuthManager, fakeAuthRepo)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.SignedIn)

        viewModel.signOut()
        advanceUntilIdle()

        assertTrue("State should transition to SignedOut", viewModel.uiState.value is AuthUiState.SignedOut)
        assertTrue("GoogleAuthManager signOut should be called", fakeGoogleAuthManager.signOutCalled)
    }
}
