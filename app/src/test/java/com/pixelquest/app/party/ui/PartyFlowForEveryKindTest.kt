package com.pixelquest.app.party.ui

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.data.remote.DaroodPartyRemoteDataSource
import com.pixelquest.app.darood.ui.DaroodPartyScreen
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.create.CreatePartyScreen
import com.pixelquest.app.darood.ui.create.CreatePartyViewModel
import com.pixelquest.app.darood.ui.info.PartyInfoScreen
import com.pixelquest.app.darood.ui.info.PartyInfoViewModel
import com.pixelquest.app.darood.ui.invite.AddPeopleScreen
import com.pixelquest.app.darood.ui.invite.AddPeopleViewModel
import com.pixelquest.app.darood.ui.list.DaroodPartiesViewModel
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeUsernameRepository
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.ZoneId

/**
 * The same flow, through the same screens, for every kind of party: Aria starts one, invites Bram by
 * his username, Bram accepts it from his kind's list (the Islamic hub's Darood Parties, or the main
 * app's Parties), and then leaves it from its info. One server; each player's own phone and cache.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class PartyFlowForEveryKindTest(private val type: PartyType) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun kinds() = PartyType.entries.map { arrayOf<Any>(it) }
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    private val server = FakeDaroodServer(me = null).apply { usernames += mapOf("aria-id" to "aria_k", "bram-id" to "bram") }
    private val databases = mutableListOf<AppDatabase>()
    private val viewModels = TestViewModels()

    @After
    fun tearDown() {
        viewModels.clear()
        databases.forEach { it.closeAfterWrites() }
    }

    /** A player's phone: its own cache, and the server answering it as its player. */
    private inner class Phone(val userId: String) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build().also { databases += it }
        val parties = DaroodPartyRepositoryImpl(asPlayer(userId), db.daroodDao(), { userId }, now = { 1_000L }, zone = { ZoneId.of("UTC") })
        val auth = FakeAuthRepository().apply { userFlow.value = AuthUser(userId, null, null) }
    }

    private fun asPlayer(userId: String): DaroodPartyRemoteDataSource = java.lang.reflect.Proxy.newProxyInstance(
        DaroodPartyRemoteDataSource::class.java.classLoader,
        arrayOf(DaroodPartyRemoteDataSource::class.java)
    ) { _, method, args ->
        synchronized(server) {
            server.me = userId
            method.invoke(server, *(args ?: emptyArray()))
        }
    } as DaroodPartyRemoteDataSource

    /** Waits as the screen tests do, letting time pass on the main looper too (the search waits before looking). */
    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(250) {
            composeTestRule.waitForIdle()
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(10)
        }
        throw AssertionError("still waiting: $what")
    }

    private fun shown(tag: String) = composeTestRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun startInviteAcceptAndLeave_workTheSame() {
        val aria = Phone("aria-id")
        val bram = Phone("bram-id")
        val kind = mapOf(DaroodPartiesViewModel.ARG_TYPE to type.wire)
        var screen by mutableStateOf<@Composable () -> Unit>({})
        composeTestRule.setContent { PixelQuestTheme { screen() } }

        // Aria starts one, of this kind.
        var created: String? = null
        val create = viewModels.keep(CreatePartyViewModel::class.java) { CreatePartyViewModel(aria.parties, SavedStateHandle(kind)) }
        screen = { CreatePartyScreen(onNavigateBack = {}, onCreated = { created = it }, viewModel = create) }
        composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(DaroodTestTags.CREATE_NAME))).performTextInput("Friday circle")
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_BUTTON).performClick()
        waitFor("the party made") { created != null }
        val party = created!!
        assertEquals(type.wire, server.parties.getValue(party).partyType)

        // She invites Bram, found by his username.
        val add = viewModels.keep(AddPeopleViewModel::class.java) {
            AddPeopleViewModel(SavedStateHandle(mapOf("partyId" to party)), FakeUsernameRepository(taken = mutableSetOf("bram")), aria.parties)
        }
        screen = { AddPeopleScreen(onNavigateBack = {}, viewModel = add) }
        composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(DaroodTestTags.ADD_SEARCH))).performTextInput("bram")
        waitFor("Bram found") { shown(DaroodTestTags.ADD_INVITE) }
        composeTestRule.onNodeWithTag(DaroodTestTags.ADD_INVITE).performClick()
        waitFor("Bram invited") { server.members.any { it.partyId == party && it.userId == "bram-id" && it.status == "invited" } }

        // Bram accepts, from his kind's list.
        val list = viewModels.keep(DaroodPartiesViewModel::class.java) { DaroodPartiesViewModel(bram.auth, bram.parties, null, SavedStateHandle(kind)) }
        screen = when (type) {
            PartyType.DAROOD -> ({ DaroodPartyScreen(onNavigateBack = null, onSignIn = {}, onOpenParty = {}, viewModel = list) })
            PartyType.STUDY -> ({ PartiesScreen(onSignIn = {}, onOpenParty = {}, onCreateParty = {}, viewModel = list) })
        }
        waitFor("Bram's invitation") { shown(DaroodTestTags.inviteRow(party)) }
        composeTestRule.onNodeWithTag(DaroodTestTags.inviteAccept(party)).performClick()
        waitFor("Bram in") { shown(DaroodTestTags.partyRow(party)) }
        assertEquals("active", server.members.first { it.partyId == party && it.userId == "bram-id" }.status)

        // And leaves, from its info.
        var left = false
        val info = viewModels.keep(PartyInfoViewModel::class.java) { PartyInfoViewModel(SavedStateHandle(mapOf("partyId" to party)), bram.parties) { "bram-id" } }
        screen = { PartyInfoScreen(onNavigateBack = {}, onAddPeople = {}, onLeft = { left = true }, viewModel = info) }
        waitFor("his info") { shown(DaroodTestTags.INFO_LEAVE) }
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_LEAVE).performClick()
        waitFor("asked to be sure") { info.uiState.value.confirmingLeave }
        // The dialog's own window is out of Robolectric's reach (as in Darood's info tests): its Leave, as it calls it.
        info.confirmLeave()
        waitFor("Bram out") { left }
        assertEquals("left", server.members.first { it.partyId == party && it.userId == "bram-id" }.status)
    }
}
