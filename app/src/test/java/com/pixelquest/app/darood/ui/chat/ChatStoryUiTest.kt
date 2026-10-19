package com.pixelquest.app.darood.ui.chat

import android.app.Application
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The party chat through its own screen and view model, as a player uses it, on the phone's real
 * clock: the days headed, only digits typed, a number shown the moment it's sent, and a refused one
 * tried again.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChatStoryUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val store = ViewModelStore()

    @After
    fun tearDown() {
        store.clear()
        db.close()
    }

    private fun waitFor(matcher: SemanticsMatcher) =
        composeTestRule.waitUntil(10_000) { composeTestRule.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun theDays_digitsOnly_sentAtOnce_andARefusedOneTriedAgain() {
        val zone = ZoneId.systemDefault()
        val server = FakeDaroodServer(me = "aria-id").apply { usernames += mapOf("aria-id" to "aria_k", "bram-id" to "bram") }
        val partyId = server.seedParty("Friday circle", "aria-id", "bram-id")
        // Bram posted yesterday and today, on the phone's own clock.
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        server.now = now.minus(1, ChronoUnit.DAYS)
        server.seedEntry(partyId, "bram-id", 300, day = LocalDate.now(zone).minusDays(1))
        server.now = now
        server.seedEntry(partyId, "bram-id", 33, day = LocalDate.now(zone))
        val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = System::currentTimeMillis, zone = { zone })
        runBlocking { repository.refreshParty(partyId) }

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PartyChatViewModel(SavedStateHandle(mapOf("partyId" to partyId)), repository, { "aria-id" }) as T
        }
        val vm = ViewModelProvider(store, factory)[PartyChatViewModel::class.java]
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PartyChatRoute(partyId = partyId, onNavigateBack = {}, onOpenInfo = {}, viewModel = vm)
            }
        }

        // The days, headed.
        waitFor(hasText("Yesterday"))
        waitFor(hasText("Today"))
        waitFor(hasContentDescription("bram added 300", substring = true))

        // Only digits get in.
        val field = composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_INPUT)
        field.performTextInput("3a1b3")
        field.assertTextEquals("313", includeEditableText = true)

        // Offline, it's there at once, waiting; back online, the same bubble is sent.
        server.offline = true
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_SEND).performClick()
        waitFor(hasContentDescription("You added 313", substring = true) and hasContentDescription("Sending.", substring = true))
        field.assertTextEquals("", includeEditableText = true)
        server.offline = false
        runBlocking { repository.sendPending() }
        waitFor(hasContentDescription("You added 313", substring = true) and hasContentDescription("Sent.", substring = true))

        // Over the day's 500,000, it's refused and says why; Retry sends it once the rule allows.
        val today = LocalDate.now(zone)
        val fullDay = List(5) { server.seedEntry(partyId, "aria-id", 100_000, day = today) }
        field.performTextInput("11")
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_SEND).performClick()
        waitFor(hasContentDescription("You added 11", substring = true) and hasContentDescription("Not sent.", substring = true))
        waitFor(hasText("Not sent: 500,000 is the most for one day."))
        server.entries.removeAll(fullDay)
        composeTestRule.onNodeWithTag(DaroodTestTags.ENTRY_RETRY).performClick()
        waitFor(hasContentDescription("You added 11", substring = true) and hasContentDescription("Sent.", substring = true))

        assertEquals(listOf(300, 33, 313, 11), server.entries.map { it.amount })
    }
}
