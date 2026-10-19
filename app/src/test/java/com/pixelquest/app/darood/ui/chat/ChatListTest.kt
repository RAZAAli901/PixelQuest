package com.pixelquest.app.darood.ui.chat

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The chat's list: it opens at the newest, follows new entries, and brings the reader back down. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChatListTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 19)
    private val usernames = mapOf("aria-id" to "aria_k", "bram-id" to "bram")

    private fun entry(n: Int, user: String = "bram-id") = DaroodEntry(
        n.toLong(), "c$n", "p1", user, n, today, "UTC", Instant.parse("2026-10-19T08:00:00Z").plusSeconds(60L * n)
    )

    private fun lines(entries: List<DaroodEntry>) = chatItemsOf(entries, me = "aria-id", zone = ZoneId.of("UTC"))

    private val chat = mutableStateOf(lines((1..40).map { entry(it) }))

    private fun show() = composeTestRule.setContent {
        PixelQuestTheme(themeMode = ThemeMode.Pixel) {
            ChatList(items = chat.value, usernames = usernames, today = today)
        }
    }

    private fun bubble(n: Int) = composeTestRule.onNodeWithTag(DaroodTestTags.bubble("c$n"))

    @Test
    fun itOpensAtTheNewest() {
        show()
        bubble(40).assertIsDisplayed()
        bubble(1).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_JUMP).assertDoesNotExist()
    }

    @Test
    fun atTheBottom_aNewEntryScrollsIntoView() {
        show()
        chat.value = lines((1..41).map { entry(it) })
        bubble(41).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_JUMP).assertDoesNotExist()
    }

    @Test
    fun scrolledUp_newEntriesWait_andTheButtonCountsThem_thenBringsTheReaderDown() {
        show()
        // Up to the oldest: the reader is reading, so the button offers the way back.
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_LIST).performScrollToIndex(chat.value.lastIndex)
        bubble(1).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_JUMP).assertContentDescriptionEquals("Jump to the newest")

        // Two arrive: the reader stays where they are, and the button says so.
        chat.value = lines((1..42).map { entry(it) })
        composeTestRule.waitForIdle()
        bubble(1).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_JUMP).assertContentDescriptionEquals("Jump to the newest, 2 new")

        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_JUMP).performClick()
        composeTestRule.waitForIdle()
        bubble(42).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_JUMP).assertDoesNotExist()
    }

    @Test
    fun scrolledUp_myOwnNewEntry_stillComesIntoView() {
        show()
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_LIST).performScrollToIndex(chat.value.lastIndex)
        chat.value = lines((1..40).map { entry(it) } + entry(41, user = "aria-id"))
        composeTestRule.waitForIdle()
        bubble(41).assertIsDisplayed()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun theViewModel_makesTheLines_fromTheCache() {
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.UnconfinedTestDispatcher())
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val store = androidx.lifecycle.ViewModelStore()
        val collectors = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        try {
            val server = FakeDaroodServer(me = "aria-id").apply { this.usernames += this@ChatListTest.usernames }
            val partyId = server.seedParty("Friday circle", "aria-id", "bram-id")
            server.seedEntry(partyId, "bram-id", 300)
            val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { ZoneId.of("UTC") })
            runBlocking {
                repository.refreshParty(partyId)
                repository.addEntry(partyId, 11)
            }

            val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    PartyChatViewModel(SavedStateHandle(mapOf("partyId" to partyId)), repository) { "aria-id" } as T
            }
            val vm = androidx.lifecycle.ViewModelProvider(store, factory)[PartyChatViewModel::class.java]
            // The screen collects them; so does the test.
            collectors.launch { vm.items.collect { } }
            collectors.launch { vm.usernames.collect { } }
            run {
                repeat(150) {
                    if (vm.items.value.filterIsInstance<EntryItem>().size == 2 && vm.usernames.value.isNotEmpty()) return@run
                    Thread.sleep(20)
                }
            }

            val entries = vm.items.value.filterIsInstance<EntryItem>().map { it.bubble }
            assertEquals("oldest first: Bram's, then mine", listOf(300, 11), entries.map { it.entry.amount })
            assertEquals(listOf(false, true), entries.map { it.mine })
            assertTrue("the joins are told", vm.items.value.any { it is EventItem && it.event is PartyEvent.Joined })
            assertEquals("bram", vm.usernames.value["bram-id"])
        } finally {
            collectors.cancel()
            store.clear()
            kotlinx.coroutines.Dispatchers.resetMain()
            db.close()
        }
    }
}
