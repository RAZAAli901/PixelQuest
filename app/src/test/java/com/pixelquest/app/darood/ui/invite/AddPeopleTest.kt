package com.pixelquest.app.darood.ui.invite

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.darood.data.MyUsername
import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.darood.domain.UsernameProblem
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.testing.FakeUsernameCache
import com.pixelquest.app.testing.FakeUsernameRepository
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Adding people: an exact username, looked up once the typing pauses. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class AddPeopleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dispatcher = StandardTestDispatcher()
    private val usernames = FakeUsernameRepository(taken = mutableSetOf("bram"))
    private val auth = FakeAuthRepository().apply { userFlow.value = AuthUser("aria-id", null, null) }
    private val cache = FakeUsernameCache().apply { save("aria-id", Username.parse("aria_k")!!) }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AddPeopleViewModel(SavedStateHandle(mapOf("partyId" to "p1")), usernames, MyUsername(auth, cache))

    @Test
    fun aTakenName_isSomeoneToInvite_andTheServerIsAskedOnceTheTypingPauses() = runTest(dispatcher) {
        val vm = viewModel()
        "bram".indices.forEach { i ->
            vm.onQueryChange("Bram".take(i + 1))
            advanceTimeBy(100)
        }
        runCurrent()
        assertTrue(usernames.availabilityChecks.isEmpty())

        advanceTimeBy(AddPeopleViewModel.LOOKUP_DELAY_MS)
        runCurrent()
        assertEquals(listOf("bram"), usernames.availabilityChecks)
        assertEquals(Lookup.Found(Username.parse("bram")!!), vm.uiState.value.lookup)
    }

    @Test
    fun aFreeName_isNobody_ownNameIsYou_andABrokenOneSaysWhy() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onQueryChange("cleo")
        advanceUntilIdle()
        assertEquals(Lookup.NotFound(Username.parse("cleo")!!), vm.uiState.value.lookup)

        vm.onQueryChange("@Aria_K")
        advanceUntilIdle()
        assertEquals(Lookup.Yours, vm.uiState.value.lookup)

        vm.onQueryChange("ab")
        advanceUntilIdle()
        assertEquals(Lookup.Invalid(UsernameProblem.TOO_SHORT), vm.uiState.value.lookup)
        assertEquals("never asked about yourself or a broken name", listOf("cleo"), usernames.availabilityChecks)

        usernames.offline = true
        vm.onQueryChange("dara")
        advanceUntilIdle()
        assertEquals(Lookup.Unknown, vm.uiState.value.lookup)
    }

    @Test
    fun theScreen_showsARowToInvite_andWhyNotOtherwise() {
        var invited: String? = null
        val state = mutableListOf(
            AddPeopleUiState(query = "bram", lookup = Lookup.Found(Username.parse("bram")!!))
        )
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                AddPeopleContent(state = state.first(), onQueryChange = {}, onNavigateBack = {}, onInvite = { invited = it })
            }
        }
        composeTestRule.onNodeWithText("@bram").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.ADD_INVITE).performClick()
        assertEquals("bram", invited)
    }

    @Test
    fun theScreen_saysWhenNobodyHasTheName() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AddPeopleContent(state = AddPeopleUiState("cleo", Lookup.NotFound(Username.parse("cleo")!!)), onQueryChange = {}, onNavigateBack = {})
            }
        }
        composeTestRule.onNodeWithText("Nobody uses @cleo. Check the spelling with your friend.").assertIsDisplayed()
    }
}
