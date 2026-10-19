package com.pixelquest.app.darood.ui.chat

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.domain.DayTotals
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
import java.time.LocalDate
import java.time.ZoneId

/** The strip under the chat's top bar: the player's today, and the party's. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class TodayStripTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theStrip_saysMineAndThePartys() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PartyChatScreen(partyName = "Friday circle", onNavigateBack = {}, onOpenInfo = {}, todayTotals = DayTotals(mine = 300, party = 2_450))
            }
        }
        composeTestRule.onNodeWithText("You today: 300", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Party today: 2,450", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun notYetKnown_thereIsNoStrip() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                PartyChatScreen(partyName = "Friday circle", onNavigateBack = {}, onOpenInfo = {})
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_TODAY).assertDoesNotExist()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun theNumbers_comeFromTheServer_withMyWaitingEntriesOnTop() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val store = ViewModelStore()
        val collectors = CoroutineScope(Dispatchers.Unconfined)
        try {
            // The phone's clock reads 1970-01-01 here: that's its today.
            val today = LocalDate.of(1970, 1, 1)
            val server = FakeDaroodServer(me = "aria-id").apply { usernames += mapOf("aria-id" to "aria_k", "bram-id" to "bram") }
            val partyId = server.seedParty("Friday circle", "aria-id", "bram-id")
            server.seedEntry(partyId, "aria-id", 300, day = today)
            server.seedEntry(partyId, "bram-id", 2_000, day = today)
            server.seedEntry(partyId, "bram-id", 150, day = today.minusDays(1))
            val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { ZoneId.of("UTC") })
            runBlocking { repository.refreshParty(partyId) }
            assertEquals(today, repository.today())

            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PartyChatViewModel(SavedStateHandle(mapOf("partyId" to partyId)), repository, { "aria-id" }) as T
            }
            val vm = ViewModelProvider(store, factory)[PartyChatViewModel::class.java]
            collectors.launch { vm.today.collect { } }
            fun waitFor(expected: DayTotals) = run {
                repeat(200) {
                    if (vm.today.value == expected) return@run
                    Thread.sleep(20)
                }
                throw AssertionError("still ${vm.today.value}, not $expected")
            }
            waitFor(DayTotals(mine = 300, party = 2_300))

            // Offline, a number sent shows at once, waiting on the phone.
            server.offline = true
            vm.onDraftChange("13")
            vm.send()
            waitFor(DayTotals(mine = 313, party = 2_313))

            // Sent once there's a connection: the server's numbers take over, the same.
            server.offline = false
            runBlocking { repository.sendPending() }
            vm.refreshToday()
            waitFor(DayTotals(mine = 313, party = 2_313))
            assertEquals(listOf(300, 2_000, 150, 13), server.entries.map { it.amount })
        } finally {
            collectors.cancel()
            store.clear()
            Dispatchers.resetMain()
            db.close()
        }
    }
}
