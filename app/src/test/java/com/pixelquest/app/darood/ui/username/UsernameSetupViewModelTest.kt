package com.pixelquest.app.darood.ui.username

import com.pixelquest.app.darood.data.OwnUsername
import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.darood.domain.UsernameProblem
import com.pixelquest.app.testing.FakeUsernameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** The username screen's logic: checking as it's typed (debounced), and saving. */
@OptIn(ExperimentalCoroutinesApi::class)
class UsernameSetupViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeUsernameRepository(taken = mutableSetOf("bram"))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theServerIsAsked_onlyOnceTheTypingPauses() = runTest(dispatcher) {
        val viewModel = UsernameSetupViewModel(repository)
        runCurrent()

        "aria_k".indices.forEach { i ->
            viewModel.onInputChange("aria_k".take(i + 1))
            advanceTimeBy(100)
        }
        // 400 ms after the last key, and not before.
        advanceTimeBy(UsernameSetupViewModel.CHECK_DELAY_MS - 101)
        runCurrent()
        assertTrue(repository.availabilityChecks.toString(), repository.availabilityChecks.isEmpty())

        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf("aria_k"), repository.availabilityChecks)
        assertEquals(UsernameAvailability.AVAILABLE, viewModel.uiState.value.availability)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun aNameThatBreaksARule_isExplained_andNeverSent() = runTest(dispatcher) {
        val viewModel = UsernameSetupViewModel(repository)
        listOf(
            "ab" to UsernameProblem.TOO_SHORT,
            "aria k" to UsernameProblem.INVALID_CHARACTERS,
            "Admin_1" to UsernameProblem.RESERVED
        ).forEach { (typed, problem) ->
            viewModel.onInputChange(typed)
            advanceUntilIdle()
            assertEquals(typed, problem, viewModel.uiState.value.problem)
            assertFalse(typed, viewModel.uiState.value.canSave)
        }
        assertTrue(repository.availabilityChecks.isEmpty())
    }

    @Test
    fun typing_isShownLowercase_asItWillBeStored() = runTest(dispatcher) {
        val viewModel = UsernameSetupViewModel(repository)
        viewModel.onInputChange(" Aria_K ")
        assertEquals("aria_k", viewModel.uiState.value.input)
    }

    @Test
    fun aTakenName_cantBeSaved() = runTest(dispatcher) {
        val viewModel = UsernameSetupViewModel(repository)
        viewModel.onInputChange("bram")
        advanceUntilIdle()

        assertEquals(UsernameAvailability.TAKEN, viewModel.uiState.value.availability)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun saving_claimsTheName_andFinishes() = runTest(dispatcher) {
        val viewModel = UsernameSetupViewModel(repository)
        viewModel.onInputChange("aria_k")
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(Username.parse("aria_k"), viewModel.uiState.value.saved)
        assertEquals(Username.parse("aria_k"), repository.mine?.username)
    }

    @Test
    fun aNameTakenMeanwhile_orNoConnection_saysWhy() = runTest(dispatcher) {
        val viewModel = UsernameSetupViewModel(repository)
        viewModel.onInputChange("cleo")
        advanceUntilIdle()
        repository.taken += "cleo"
        viewModel.save()
        advanceUntilIdle()
        assertEquals(UsernameSaveError.Taken, viewModel.uiState.value.saveError)
        assertNull(viewModel.uiState.value.saved)

        repository.offline = true
        viewModel.onInputChange("dara")
        advanceUntilIdle()
        assertEquals(UsernameAvailability.UNKNOWN, viewModel.uiState.value.availability)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun anExistingName_isShown_andChangingItTooSoon_isRefused() = runTest(dispatcher) {
        val changeableAt = Instant.parse("2026-11-10T00:00:00Z")
        repository.mine = OwnUsername(Username.parse("aria_k")!!, changeableAt)
        val viewModel = UsernameSetupViewModel(repository)
        advanceUntilIdle()

        assertEquals("aria_k", viewModel.uiState.value.input)
        assertTrue(viewModel.uiState.value.isCurrent)
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onInputChange("aria_new")
        advanceUntilIdle()
        viewModel.save()
        advanceUntilIdle()
        assertEquals(UsernameSaveError.TooSoon(changeableAt), viewModel.uiState.value.saveError)
        assertEquals(Username.parse("aria_k"), repository.mine?.username)
    }
}
