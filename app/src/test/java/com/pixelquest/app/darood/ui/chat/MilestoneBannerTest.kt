package com.pixelquest.app.darood.ui.chat

import com.pixelquest.app.testing.closeAfterWrites
import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.data.MilestoneSeenStore
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
import java.time.ZoneId

/** A new milestone, announced once at the top of the chat. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class MilestoneBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class MemorySeen : MilestoneSeenStore {
        val seen = mutableMapOf<String, Long>()
        override fun seenAt(partyId: String) = seen[partyId]
        override fun markSeen(partyId: String, setAt: Long) {
            seen[partyId] = setAt
        }
    }

    @Test
    fun theBanner_saysTheMilestone_andGoesOnATap() {
        var dismissed = 0
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PartyChatScreen(partyName = "Friday circle", onNavigateBack = {}, onOpenInfo = {}, milestoneBanner = 10_000, onDismissBanner = { dismissed++ })
            }
        }
        composeTestRule.onNodeWithText("🎯 New milestone: 10,000. Let's reach it together!").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_BANNER).performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun theBanner_goesOnItsOwn_after5Seconds() {
        var dismissed = 0
        val target = mutableStateOf<Long?>(10_000)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                MilestoneBanner(target = target.value, onDismiss = { dismissed++; target.value = null })
            }
        }
        composeTestRule.mainClock.advanceTimeBy(MILESTONE_BANNER_MS - 500)
        assertEquals(0, dismissed)
        composeTestRule.mainClock.advanceTimeBy(1_000)
        assertEquals(1, dismissed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun aNewMilestone_isAnnouncedOnce_andARaisedOneAgain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val store = ViewModelStore()
        val collectors = CoroutineScope(Dispatchers.Unconfined)
        val seen = MemorySeen()
        try {
            val server = FakeDaroodServer(me = "aria-id").apply { usernames += mapOf("aria-id" to "aria_k") }
            val partyId = server.seedParty("Friday circle", "aria-id", milestone = 10_000)
            val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { ZoneId.of("UTC") })
            runBlocking { repository.refreshParty(partyId) }
            var made = 0
            fun viewModel(): PartyChatViewModel {
                val factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        PartyChatViewModel(SavedStateHandle(mapOf("partyId" to partyId)), repository, { "aria-id" }, null, seen) as T
                }
                return ViewModelProvider(store, factory)["chat-${made++}", PartyChatViewModel::class.java]
            }
            fun waitFor(what: String, condition: () -> Boolean) = run {
                repeat(150) {
                    if (condition()) return@run
                    Thread.sleep(20)
                }
                throw AssertionError("still waiting: $what")
            }

            val first = viewModel()
            collectors.launch { first.milestoneBanner.collect { } }
            waitFor("announced") { first.milestoneBanner.value == 10_000L }
            first.dismissMilestoneBanner()
            assertEquals(null, first.milestoneBanner.value)

            // Opened again: already seen.
            val again = viewModel()
            Thread.sleep(200)
            assertEquals(null, again.milestoneBanner.value)

            // Raised: announced again.
            server.now = server.now.plusSeconds(60)
            runBlocking { repository.setMilestone(partyId, 20_000) }
            waitFor("the raise announced") { again.milestoneBanner.value == 20_000L }
        } finally {
            collectors.cancel()
            store.clear()
            db.closeAfterWrites()
            Dispatchers.resetMain()
        }
    }
}
