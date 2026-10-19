package com.pixelquest.app.darood.ui.username

import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.darood.data.OwnUsername
import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.testing.FakeUsernamePromptStore
import com.pixelquest.app.testing.FakeUsernameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** A signed-in account without a username is asked for one, once on this phone. */
@OptIn(ExperimentalCoroutinesApi::class)
class UsernamePromptViewModelTest {

    private val auth = FakeAuthRepository()
    private val usernames = FakeUsernameRepository()
    private val store = FakeUsernamePromptStore()
    private val aria = AuthUser(id = "aria-id", email = null, displayName = null)
    private val bram = AuthUser(id = "bram-id", email = null, displayName = null)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = UsernamePromptViewModel(auth, usernames, store)

    @Test
    fun signedOut_isNeverAsked() = runTest {
        assertFalse(viewModel().prompt.value)
    }

    @Test
    fun signingInWithoutAName_asksOnce() = runTest {
        val viewModel = viewModel()
        auth.userFlow.value = aria
        assertTrue(viewModel.prompt.value)

        viewModel.promptShown()
        assertFalse(viewModel.prompt.value)
        assertEquals(setOf("aria-id"), store.prompted)

        // Signing out and in again, or the next launch: not asked again.
        auth.userFlow.value = null
        auth.userFlow.value = aria
        assertFalse(viewModel.prompt.value)
        assertFalse(viewModel().prompt.value)
    }

    @Test
    fun anAccountWithAName_isntAsked() = runTest {
        usernames.mine = OwnUsername(Username.parse("aria_k")!!, Instant.parse("2026-11-18T00:00:00Z"))
        val viewModel = viewModel()
        auth.userFlow.value = aria

        assertFalse(viewModel.prompt.value)
        assertEquals(setOf("aria-id"), store.prompted)
    }

    @Test
    fun withNoConnection_itAsksAtALaterLaunch() = runTest {
        usernames.offline = true
        auth.userFlow.value = aria
        assertFalse(viewModel().prompt.value)
        assertTrue(store.prompted.isEmpty())

        usernames.offline = false
        assertTrue(viewModel().prompt.value)
    }

    @Test
    fun eachAccountIsAskedForItself() = runTest {
        val viewModel = viewModel()
        auth.userFlow.value = aria
        viewModel.promptShown()

        auth.userFlow.value = bram
        assertTrue(viewModel.prompt.value)
    }

    @Test
    fun signingOutBeforeItOpened_dropsTheQuestion() = runTest {
        val viewModel = viewModel()
        auth.userFlow.value = aria
        auth.userFlow.value = null

        assertFalse(viewModel.prompt.value)
        assertTrue(store.prompted.isEmpty())
    }

    @Test
    fun withDaroodTurnedOff_nobodyIsAsked_untilItsBackOn() = runTest {
        val settings = com.pixelquest.app.testing.FakeSettingsRepository()
        settings.setDaroodAvailable(false)
        val viewModel = UsernamePromptViewModel(auth, usernames, store, settings)
        auth.userFlow.value = aria
        assertFalse(viewModel.prompt.value)
        assertTrue(store.prompted.isEmpty())

        settings.setDaroodAvailable(true)
        assertTrue(viewModel.prompt.value)
    }
}
