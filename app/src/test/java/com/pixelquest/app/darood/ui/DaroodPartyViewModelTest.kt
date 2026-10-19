package com.pixelquest.app.darood.ui

import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Darood Party's home follows the sign-in: parties need an account. */
@OptIn(ExperimentalCoroutinesApi::class)
class DaroodPartyViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signedIn_followsTheAccount() = runTest {
        val auth = FakeAuthRepository()
        val viewModel = DaroodPartyViewModel(auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.signedIn.collect {} }

        assertEquals(false, viewModel.signedIn.value)
        auth.userFlow.value = AuthUser(id = "aria-id", email = null, displayName = null)
        assertEquals(true, viewModel.signedIn.value)
        auth.userFlow.value = null
        assertEquals(false, viewModel.signedIn.value)
    }
}
