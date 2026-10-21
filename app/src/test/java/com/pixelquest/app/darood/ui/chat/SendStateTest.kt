package com.pixelquest.app.darood.ui.chat

import com.pixelquest.app.testing.closeAfterWrites
import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The player's own entry shows at once, with a clock while it waits, then a tick once it's sent. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class SendStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun entry(key: String, state: EntryState, user: String = "aria-id") =
        DaroodEntry(null, key, "p1", user, 313, LocalDate.of(2026, 10, 19), "UTC", Instant.parse("2026-10-19T10:00:00Z"), state)

    @Test
    fun myBubbles_showAClockWhileWaiting_andATickOnceSent_othersShowNeither() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                Column {
                    EntryBubble(entry("waiting", EntryState.PENDING), sender = "aria_k", mine = true)
                    EntryBubble(entry("sent", EntryState.SYNCED), sender = "aria_k", mine = true)
                    EntryBubble(entry("theirs", EntryState.SYNCED, user = "bram-id"), sender = "bram")
                }
            }
        }
        fun mark(tag: String, bubble: String) = composeTestRule.onNode(hasTestTag(tag) and hasAnyAncestor(hasTestTag(DaroodTestTags.bubble(bubble))), useUnmergedTree = true)
        mark(DaroodTestTags.ENTRY_PENDING, "waiting").assertIsDisplayed().assertContentDescriptionEquals("Sending")
        mark(DaroodTestTags.ENTRY_SENT, "sent").assertIsDisplayed().assertContentDescriptionEquals("Sent")
        mark(DaroodTestTags.ENTRY_SENT, "theirs").assertDoesNotExist()
        mark(DaroodTestTags.ENTRY_PENDING, "theirs").assertDoesNotExist()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun offline_theBubbleIsThereAtOnce_waiting_thenTheSameBubbleIsSent() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val store = ViewModelStore()
        val collectors = CoroutineScope(Dispatchers.Unconfined)
        try {
            val server = FakeDaroodServer(me = "aria-id").apply { usernames["aria-id"] = "aria_k" }
            val partyId = server.seedParty("Friday circle", "aria-id")
            val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { ZoneId.of("UTC") })
            runBlocking { repository.refreshParty(partyId) }
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PartyChatViewModel(SavedStateHandle(mapOf("partyId" to partyId)), repository, { "aria-id" }) as T
            }
            val vm = ViewModelProvider(store, factory)[PartyChatViewModel::class.java]
            collectors.launch { vm.items.collect { } }
            fun mine() = vm.items.value.filterIsInstance<EntryItem>().map { it.bubble }.filter { it.mine }
            fun waitFor(what: String, condition: () -> Boolean) = run {
                repeat(150) {
                    if (condition()) return@run
                    Thread.sleep(20)
                }
                throw AssertionError("still waiting: $what")
            }

            server.offline = true
            vm.onDraftChange("313")
            vm.send()
            waitFor("the bubble, waiting") { mine().singleOrNull()?.entry?.state == EntryState.PENDING }
            val key = mine().single().entry.clientId
            assertEquals(emptyList<Int>(), server.entries.map { it.amount })

            server.offline = false
            runBlocking { repository.sendPending() }
            waitFor("the same bubble, sent") { mine().singleOrNull()?.entry?.state == EntryState.SYNCED }
            assertEquals("the same bubble: its key stays", key, mine().single().entry.clientId)
            assertEquals(listOf(313), server.entries.map { it.amount })
        } finally {
            collectors.cancel()
            store.clear()
            db.closeAfterWrites()
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun aBigNumber_waitsForThePlayerToConfirmIt() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val store = ViewModelStore()
        try {
            val server = FakeDaroodServer(me = "aria-id").apply { usernames["aria-id"] = "aria_k" }
            val partyId = server.seedParty("Friday circle", "aria-id")
            val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { ZoneId.of("UTC") })
            runBlocking { repository.refreshParty(partyId) }
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PartyChatViewModel(SavedStateHandle(mapOf("partyId" to partyId)), repository, { "aria-id" }) as T
            }
            val vm = ViewModelProvider(store, factory)[PartyChatViewModel::class.java]
            fun posted(): List<Int> {
                Thread.sleep(100)
                return server.entries.map { it.amount }
            }

            // Under 10,000: straight out.
            vm.onDraftChange("9999")
            vm.send()
            assertEquals(null, vm.confirming.value)
            assertEquals(listOf(9999), posted())

            // 10,000: asked first; "Change it" keeps it in the field, unsent.
            vm.onDraftChange("10000")
            vm.send()
            assertEquals(10_000, vm.confirming.value)
            assertEquals(listOf(9999), posted())
            vm.cancelSend()
            assertEquals(null, vm.confirming.value)
            assertEquals("10000", vm.draft.value)

            // Asked again, and confirmed: it goes, once.
            vm.send()
            vm.send()
            vm.confirmSend()
            vm.confirmSend()
            assertEquals("", vm.draft.value)
            assertEquals(listOf(9999, 10_000), posted())
        } finally {
            store.clear()
            db.closeAfterWrites()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun theDialog_saysTheNumber_andSendsOnlyWhenTold() {
        var sent = 0
        var changed = 0
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                PartyChatScreen(
                    partyName = "Friday circle", onNavigateBack = {}, onOpenInfo = {}, canPost = true, draft = "12000",
                    confirming = 12_000, onConfirmSend = { sent++ }, onCancelSend = { changed++ }
                )
            }
        }
        composeTestRule.onNode(androidx.compose.ui.test.hasText("Send 12,000?") and androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.isDialog())).assertIsDisplayed()
        composeTestRule.onNode(androidx.compose.ui.test.hasText("Change it") and androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.isDialog())).performClick()
        assertEquals(1, changed)
        composeTestRule.onNode(androidx.compose.ui.test.hasText("Send") and androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.isDialog())).performClick()
        assertEquals(1, sent)
    }

    @Test
    fun aRefusedEntry_saysWhy_andOffersRetry() {
        var retried: String? = null
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                Column {
                    ChatEntryRow(entry("refused", EntryState.FAILED).copy(refusal = "daily_limit"), sender = "aria_k", mine = true, onRetry = { retried = it })
                    ChatEntryRow(entry("fine", EntryState.SYNCED), sender = "aria_k", mine = true)
                }
            }
        }
        composeTestRule.onNode(hasTestTag(DaroodTestTags.ENTRY_FAILED) and hasAnyAncestor(hasTestTag(DaroodTestTags.bubble("refused"))), useUnmergedTree = true)
            .assertContentDescriptionEquals("Not sent")
        composeTestRule.onNodeWithTag(DaroodTestTags.ENTRY_REFUSAL).assertTextEquals("Not sent: 500,000 is the most for one day.")
        composeTestRule.onNodeWithTag(DaroodTestTags.ENTRY_RETRY).performClick()
        assertEquals("refused", retried)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun retry_triesTheSameEntryAgain_andItGoesOnceTheRuleAllows() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val store = ViewModelStore()
        val collectors = CoroutineScope(Dispatchers.Unconfined)
        try {
            val server = FakeDaroodServer(me = "aria-id").apply { usernames["aria-id"] = "aria_k" }
            val partyId = server.seedParty("Friday circle", "aria-id")
            // The phone's day (its clock reads 1970-01-01 here) already has the day's 500,000.
            repeat(5) { server.seedEntry(partyId, "aria-id", 100_000, day = LocalDate.of(1970, 1, 1)) }
            val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { ZoneId.of("UTC") })
            runBlocking { repository.refreshParty(partyId) }
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PartyChatViewModel(SavedStateHandle(mapOf("partyId" to partyId)), repository, { "aria-id" }) as T
            }
            val vm = ViewModelProvider(store, factory)[PartyChatViewModel::class.java]
            collectors.launch { vm.items.collect { } }
            fun waiting() = vm.items.value.filterIsInstance<EntryItem>().map { it.bubble.entry }.filter { it.id == null }
            fun waitFor(what: String, condition: () -> Boolean) = run {
                repeat(150) {
                    if (condition()) return@run
                    Thread.sleep(20)
                }
                throw AssertionError("still waiting: $what")
            }

            vm.onDraftChange("313")
            vm.send()
            waitFor("refused") { waiting().singleOrNull()?.state == EntryState.FAILED }
            assertEquals("daily_limit", waiting().single().refusal)
            val key = waiting().single().clientId!!

            // Still over the day's limit: refused again, still there with its reason.
            vm.retry(key)
            Thread.sleep(200)
            assertEquals(EntryState.FAILED, waiting().single().state)

            // The rule allows it now: the same entry goes.
            server.entries.clear()
            vm.retry(key)
            waitFor("sent") { waiting().isEmpty() }
            assertEquals(listOf(313), server.entries.map { it.amount })
            assertEquals(key, server.entries.single().clientId)
        } finally {
            collectors.cancel()
            store.clear()
            db.closeAfterWrites()
            Dispatchers.resetMain()
        }
    }
}
