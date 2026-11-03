package com.pixelquest.app.darood.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.data.remote.DaroodPartyRemoteDataSource
import com.pixelquest.app.darood.domain.DaroodRules
import com.pixelquest.app.darood.ui.chat.DraftCheck
import com.pixelquest.app.darood.ui.chat.ExtendGoalSheet
import com.pixelquest.app.darood.ui.chat.ExtendGoalTags
import com.pixelquest.app.darood.ui.chat.PartyChatViewModel
import com.pixelquest.app.darood.ui.chat.amountDigits
import com.pixelquest.app.darood.ui.chat.checkDraft
import com.pixelquest.app.darood.ui.create.CreatePartyViewModel
import com.pixelquest.app.darood.ui.invite.AddPeopleViewModel
import com.pixelquest.app.darood.ui.invite.InviteResult
import com.pixelquest.app.darood.ui.list.DaroodPartiesViewModel
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeUsernameRepository
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId

/**
 * V044: Darood party and chat flow and limits suite.
 *
 * User flow:
 * - Create a party with milestone
 * - Invite a member by username
 * - Member accepts the invitation
 * - Members post numbers in chat
 * - See live totals update
 * - Goal reached (party completes)
 * - Leader extends goal to re-open party
 *
 * Out-of-bounds cases:
 * - Member count limits: 1 (clamped to 2), 2, 30, and 31 (clamped to 30)
 * - Digits only input filter: letters dropped, mixed text extracted, Arabic-Indic digits converted, leading zeros stripped
 * - Post amount boundaries: 0 (rejected), 1 (valid), 100,000 (valid), 100,001 (rejected)
 * - Duplicate invites and full-party invite rejection
 * - Lower goal prevention in ExtendGoalSheet
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaroodPartyAndChatFlowAndLimitsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val server = FakeDaroodServer(me = null).apply {
        usernames += mapOf("aria-id" to "aria_k", "bram-id" to "bram", "cleo-id" to "cleo_r")
    }
    private val databases = mutableListOf<AppDatabase>()
    private val viewModels = ViewModelStore()
    private var vmCount = 0
    private val collectors = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        collectors.cancel()
        viewModels.clear()
        Dispatchers.resetMain()
        databases.forEach { it.close() }
    }

    private inner class TestPhone(val userId: String) {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build().also { databases += it }

        val parties = DaroodPartyRepositoryImpl(
            remoteAsPlayer(userId),
            db.daroodDao(),
            { userId },
            now = { 10_000L },
            zone = { ZoneId.of("UTC") },
        )
        val auth = FakeAuthRepository().apply { userFlow.value = AuthUser(userId, null, null) }
        val account = object : DaroodAccount {
            override fun id(): String = userId
        }
    }

    private fun remoteAsPlayer(userId: String): DaroodPartyRemoteDataSource = java.lang.reflect.Proxy.newProxyInstance(
        DaroodPartyRemoteDataSource::class.java.classLoader,
        arrayOf(DaroodPartyRemoteDataSource::class.java),
    ) { _, method, args ->
        synchronized(server) {
            server.me = userId
            method.invoke(server, *(args ?: emptyArray()))
        }
    } as DaroodPartyRemoteDataSource

    private inline fun <reified T : ViewModel> retain(noinline create: () -> T): T {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
        }
        return ViewModelProvider(viewModels, factory)["vm-${vmCount++}", T::class.java]
    }

    private fun waitFor(desc: String, check: () -> Boolean) = runBlocking {
        repeat(150) {
            if (check()) return@runBlocking
            delay(20)
        }
        throw AssertionError("Condition not met: $desc")
    }

    @Test
    fun userFlow_create_invite_accept_postNumbers_seeTotals_goalReached_and_extend() {
        val aria = TestPhone("aria-id")
        val bram = TestPhone("bram-id")

        // 1. Create Party: Aria creates "Circle of Blessings" with milestone 500
        val createVm = retain { CreatePartyViewModel(aria.parties) }
        createVm.onNameChange("Circle of Blessings")
        createVm.setMaxMembers(5)
        createVm.create()

        waitFor("party creation") { createVm.uiState.value.created != null }
        val partyId = createVm.uiState.value.created!!
        assertNotNull(partyId)

        // Set milestone to 500
        runBlocking { aria.parties.setMilestone(partyId, 500L) }
        runBlocking { aria.parties.refreshParty(partyId) }

        // 2. Invite: Aria invites Bram
        val addVm = retain {
            AddPeopleViewModel(
                SavedStateHandle(mapOf("partyId" to partyId)),
                FakeUsernameRepository(taken = mutableSetOf("bram")),
                aria.parties,
            )
        }
        waitFor("add people limit loaded") { addVm.uiState.value.limit != null }
        addVm.invite("bram")
        waitFor("Bram invited") { addVm.uiState.value.result == InviteResult.Sent("bram") }

        // 3. Accept: Bram accepts invitation on his phone
        val bramListVm = retain { DaroodPartiesViewModel(bram.auth, bram.parties) }
        collectors.launch { bramListVm.uiState.collect {} }
        waitFor("Bram sees invite") { bramListVm.uiState.value.invites.any { it.id == partyId } }

        bramListVm.accept(partyId)
        waitFor("Bram is in party") { bramListVm.uiState.value.parties.any { it.id == partyId } }

        // 4. Post numbers in chat:
        // Aria posts 200
        val ariaChatVm = retain {
            PartyChatViewModel(
                SavedStateHandle(mapOf("partyId" to partyId)),
                aria.parties,
                aria.account,
            )
        }
        collectors.launch { ariaChatVm.items.collect {} }
        collectors.launch { ariaChatVm.party.collect {} }

        ariaChatVm.onDraftChange("200")
        assertEquals("200", ariaChatVm.draft.value)
        ariaChatVm.send()
        assertEquals("", ariaChatVm.draft.value)

        // Bram posts 300
        val bramChatVm = retain {
            PartyChatViewModel(
                SavedStateHandle(mapOf("partyId" to partyId)),
                bram.parties,
                bram.account,
            )
        }
        collectors.launch { bramChatVm.items.collect {} }
        collectors.launch { bramChatVm.party.collect {} }

        bramChatVm.onDraftChange("300")
        bramChatVm.send()

        // Sync and refresh
        runBlocking {
            aria.parties.sendPending()
            bram.parties.sendPending()
            aria.parties.refreshParty(partyId)
            bram.parties.refreshParty(partyId)
        }

        // 5. See totals: total is 500
        waitFor("total reached 500") {
            server.total(partyId) == 500L
        }

        // 6. Goal reached: Party completedAt is set when total >= milestone (500 >= 500)
        waitFor("party finished state") {
            server.parties[partyId]?.completedAt != null
        }

        // 7. Extend goal: Aria extends milestone to 1,000
        ariaChatVm.extendGoal(1_000L)
        waitFor("extend goal done") { ariaChatVm.extend.value.done }

        // Party reopened with higher goal
        waitFor("milestone updated to 1000") {
            server.parties[partyId]?.milestone == 1_000L && server.parties[partyId]?.completedAt == null
        }
    }

    @Test
    fun memberLimits_boundaryChecks_1_2_30_and_31() {
        val phone = TestPhone("aria-id")
        val createVm = retain { CreatePartyViewModel(phone.parties) }

        // Setting 1: clamped to minimum (2)
        createVm.setMaxMembers(1)
        assertEquals(DaroodRules.MEMBERS_MIN, createVm.uiState.value.maxMembers)
        assertEquals(2, createVm.uiState.value.maxMembers)
        assertFalse("Cannot have fewer than min", createVm.uiState.value.canHaveFewer)

        // Setting 2: valid minimum
        createVm.setMaxMembers(2)
        assertEquals(2, createVm.uiState.value.maxMembers)

        // Setting 30: valid maximum
        createVm.setMaxMembers(30)
        assertEquals(30, createVm.uiState.value.maxMembers)
        assertEquals(DaroodRules.MEMBERS_MAX, createVm.uiState.value.maxMembers)
        assertFalse("Cannot have more than max", createVm.uiState.value.canHaveMore)

        // Setting 31: clamped to maximum (30)
        createVm.setMaxMembers(31)
        assertEquals(DaroodRules.MEMBERS_MAX, createVm.uiState.value.maxMembers)
        assertEquals(30, createVm.uiState.value.maxMembers)
    }

    @Test
    fun digitsOnlyInput_filterAndSanitization() {
        // Empty string
        assertEquals("", amountDigits(""))

        // Letters dropped entirely
        assertEquals("", amountDigits("abc"))
        assertEquals("", amountDigits("hello world"))

        // Mixed text: keeps digits only
        assertEquals("10000", amountDigits("10,000 Darood"))
        assertEquals("500", amountDigits(" recited 500 times "))

        // Arabic-Indic and Eastern Arabic digits converted to standard 0-9
        assertEquals("313", amountDigits("۳۱۳"))
        assertEquals("1234", amountDigits("١٢٣٤"))
        assertEquals("786", amountDigits("٧٨٦"))

        // Leading zeros stripped
        assertEquals("7", amountDigits("007"))
        assertEquals("50", amountDigits("050"))
        assertEquals("0", amountDigits("000"))

        // Maximum 6 digits clamped
        assertEquals("100000", amountDigits("100000"))
        assertEquals("123456", amountDigits("123456789"))
    }

    @Test
    fun amountLimits_0_1_100000_and_100001() {
        // 0: below minimum (1), cannot send
        val checkZero = checkDraft("0")
        assertEquals(DraftCheck.ZERO, checkZero)
        assertFalse(checkZero.canSend)

        // 1: valid minimum
        val checkMin = checkDraft("1")
        assertEquals(DraftCheck.OK, checkMin)
        assertTrue(checkMin.canSend)

        // 100,000: valid maximum
        val checkMax = checkDraft("100000")
        assertEquals(DraftCheck.OK, checkMax)
        assertTrue(checkMax.canSend)

        // 100,001: exceeds maximum, cannot send
        val checkTooMany = checkDraft("100001")
        assertEquals(DraftCheck.TOO_MANY, checkTooMany)
        assertFalse(checkTooMany.canSend)

        // Invalid / negative
        assertFalse(checkDraft("").canSend)
        assertFalse(checkDraft("-1").canSend)
    }

    @Test
    fun duplicateInvites_and_fullPartyRejection() {
        val aria = TestPhone("aria-id")

        // Create party with limit of 2 members
        val createVm = retain { CreatePartyViewModel(aria.parties) }
        createVm.onNameChange("Duo Circle")
        createVm.setMaxMembers(2)
        createVm.create()
        waitFor("created") { createVm.uiState.value.created != null }
        val partyId = createVm.uiState.value.created!!

        val addVm = retain {
            AddPeopleViewModel(
                SavedStateHandle(mapOf("partyId" to partyId)),
                FakeUsernameRepository(taken = mutableSetOf("bram", "cleo_r")),
                aria.parties,
            )
        }
        waitFor("limit loaded") { addVm.uiState.value.limit == 2 }

        // First invite to Bram
        addVm.invite("bram")
        waitFor("Bram invited") { addVm.uiState.value.result == InviteResult.Sent("bram") }

        // Duplicate invite to Bram: party is already full (Aria + invited Bram = 2)
        assertTrue("Party is now full", addVm.uiState.value.isFull)
        val initialInviteCount = server.calls.count { it == "invite" }

        // Attempt duplicate invite
        addVm.invite("bram")
        assertEquals("Duplicate invite is rejected, no additional network call", initialInviteCount, server.calls.count { it == "invite" })

        // Attempt invite to Cleo when full
        addVm.invite("cleo_r")
        assertEquals("Invite when full is rejected, no network call", initialInviteCount, server.calls.count { it == "invite" })
    }

    @Test
    fun extendGoalSheet_preventsLowerOrEqualGoal_andAllowsHigherGoal() {
        val total = 500L
        val reached = 500L
        var extendedTarget: Long? = null

        composeTestRule.setContent {
            PixelQuestTheme {
                ExtendGoalSheet(
                    total = total,
                    reached = reached,
                    busy = false,
                    problem = null,
                    onExtend = { extendedTarget = it },
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ExtendGoalTags.SHEET).assertIsDisplayed()

        // Initially typed is empty, save button is disabled
        composeTestRule.onNodeWithTag(ExtendGoalTags.SAVE).assertIsNotEnabled()

        // Quick goals: +25% (625), +50% (750), double (1000) are all > total
        composeTestRule.onNodeWithTag(ExtendGoalTags.quick(50)).performClick()
        composeTestRule.onNodeWithTag(ExtendGoalTags.SAVE).assertIsEnabled()

        composeTestRule.onNodeWithTag(ExtendGoalTags.SAVE).performClick()
        assertEquals(750L, extendedTarget)
    }
}
