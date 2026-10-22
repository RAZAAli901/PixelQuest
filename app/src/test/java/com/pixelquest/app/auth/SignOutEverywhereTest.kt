package com.pixelquest.app.auth

import com.pixelquest.app.data.remote.SupabaseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/** S059: "Sign out on every device" revokes every session, and does nothing at all when offline. */
@OptIn(ExperimentalCoroutinesApi::class)
class SignOutEverywhereTest {

    private class Repo(var everywhere: SupabaseResult<Unit>) : AuthRepository {
        val calls = mutableListOf<String>()
        override val currentUser = MutableStateFlow<AuthUser?>(AuthUser("u1", "hero@example.com", null))
        override suspend fun exchangeGoogleIdToken(idToken: String, rawNonce: String?): SupabaseResult<AuthUser> = error("unused")
        override suspend fun signOut(): SupabaseResult<Unit> {
            calls += "local"
            currentUser.value = null
            return SupabaseResult.Success(Unit)
        }
        override suspend fun signOutEverywhere(): SupabaseResult<Unit> {
            calls += "everywhere"
            if (everywhere is SupabaseResult.Success) currentUser.value = null
            return everywhere
        }
        override suspend fun getInitialUser(): AuthUser? = currentUser.value
    }

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun everywhere_revokesEverySession_andSignsThisPhoneOut() = runTest(dispatcher) {
        val repo = Repo(SupabaseResult.Success(Unit))
        val google = FakeGoogleAuthManagerForFailure()
        val profiles = FakeUserProfileRepoForFailure()
        val viewModel = AuthViewModel(google, repo, profiles)
        advanceUntilIdle()
        var signedOut = false
        viewModel.signOut(onSignedOut = { signedOut = true }, everywhere = true)
        advanceUntilIdle()
        assertEquals(listOf("everywhere"), repo.calls)
        assertTrue(signedOut)
        assertTrue(google.signOutCalled)
        assertEquals(AuthUiState.SignedOut, viewModel.uiState.value)
        assertFalse(viewModel.signOutEverywhereFailed.value)
    }

    @Test
    fun offline_nothingIsSignedOut_andThePlayerIsTold() = runTest(dispatcher) {
        val repo = Repo(SupabaseResult.NetworkError(java.io.IOException("offline"), "offline"))
        val google = FakeGoogleAuthManagerForFailure()
        val profiles = FakeUserProfileRepoForFailure()
        val viewModel = AuthViewModel(google, repo, profiles)
        advanceUntilIdle()
        var signedOut = false
        viewModel.signOut(onSignedOut = { signedOut = true }, everywhere = true)
        advanceUntilIdle()
        assertEquals(listOf("everywhere"), repo.calls)
        assertFalse("still signed in here", signedOut)
        assertFalse(google.signOutCalled)
        assertEquals("the signed-in account stays", "u1", profiles.assignedUserId)
        assertTrue(viewModel.signOutEverywhereFailed.value)
    }

    @Test
    fun aPlainSignOut_staysOnThisPhone() = runTest(dispatcher) {
        val repo = Repo(SupabaseResult.Success(Unit))
        val viewModel = AuthViewModel(FakeGoogleAuthManagerForFailure(), repo, FakeUserProfileRepoForFailure())
        advanceUntilIdle()
        viewModel.signOut()
        advanceUntilIdle()
        assertEquals(listOf("local"), repo.calls)
    }
}
