package com.pixelquest.app.darood.ui.chat

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** What happened to a party, told in its chat as pills between the entries. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChatEventsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun at(hour: Int) = Instant.parse("2026-10-19T%02d:00:00Z".format(hour))

    private fun member(user: String, name: String?, status: MemberStatus, joined: Int?, changed: Int? = null, role: PartyRole = PartyRole.MEMBER) =
        PartyMember("p1", user, name, role, status, null, joined?.let(::at), changed?.let(::at))

    private val party = Party("p1", "Friday circle", "aria-id", 10, 1_000, at(4), at(7), at(1))

    private val members = listOf(
        member("aria-id", "aria_k", MemberStatus.ACTIVE, joined = 1, role = PartyRole.LEADER),
        member("bram-id", "bram", MemberStatus.LEFT, joined = 2, changed = 5),
        member("cleo-id", "cleo_r", MemberStatus.REMOVED, joined = 3, changed = 6),
        // Asked, not yet in: nothing to tell.
        member("dara-id", "dara", MemberStatus.INVITED, joined = null),
        // Removed before the phone knew when rows changed: only the joining is told.
        member("eli-id", null, MemberStatus.REMOVED, joined = 2),
    )

    private fun describe(event: PartyEvent) = when (event) {
        is PartyEvent.Joined -> "${event.userId} joined"
        is PartyEvent.Left -> "${event.userId} left"
        is PartyEvent.Removed -> "${event.userId} removed"
        is PartyEvent.MilestoneSet -> "milestone ${event.target}"
        is PartyEvent.MilestoneReached -> "reached ${event.target}"
        is PartyEvent.WeekClosed -> "week ${event.week.weekStart}"
    }

    @Test
    fun theEvents_areWhatTheCacheKnows_oldestFirst() {
        assertEquals(
            listOf("aria-id joined", "bram-id joined", "eli-id joined", "cleo-id joined", "milestone 1000", "bram-id left", "cleo-id removed", "reached 1000"),
            partyEventsOf(party, members).map(::describe),
        )
        assertEquals(listOf("aria-id joined"), partyEventsOf(null, members.take(1)).map(::describe))
    }

    @Test
    fun theTimeline_putsEventsAmongTheEntries_andAnEventStartsANewRun() {
        var next = 1L
        fun entry(user: String, time: Instant) = DaroodEntry(next, "c${next++}", "p1", user, 100, LocalDate.of(2026, 10, 19), "UTC", time)
        val entries = listOf(
            entry("bram-id", at(2).plusSeconds(60)),
            entry("bram-id", at(2).plusSeconds(120)),
            entry("bram-id", at(4).plusSeconds(60)),
            // The entry that finished it, at the same moment as the finish: the entry comes first.
            entry("aria-id", at(7)),
        )
        val events = partyEventsOf(party, members)
        val lines = chatItemsOf(entries, me = "aria-id", zone = ZoneId.of("UTC"), events = events).map {
            when (it) {
                is DayItem -> "day"
                is EventItem -> describe(it.event)
                is EntryItem -> if (it.bubble.showName) {
                    "named ${it.bubble.entry.userId}"
                } else if (it.bubble.mine) {
                    "mine"
                } else {
                    "bubble"
                }
            }
        }
        assertEquals(
            listOf(
                "day", "aria-id joined", "bram-id joined", "eli-id joined", "named bram-id", "bubble", "cleo-id joined",
                "milestone 1000", "named bram-id", "bram-id left", "cleo-id removed", "mine", "reached 1000",
            ),
            lines,
        )
    }

    @Test
    fun aMemberLeaving_reachesTheCache_withItsTime() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val server = FakeDaroodServer(me = "bram-id").apply { usernames += mapOf("aria-id" to "aria_k", "bram-id" to "bram") }
            val partyId = server.seedParty("Friday circle", "aria-id", "bram-id")
            server.leaveParty(partyId)
            server.me = "aria-id"
            val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { ZoneId.of("UTC") })
            repository.refreshParty(partyId)

            val bram = repository.observeMembers(partyId).first().single { it.userId == "bram-id" }
            assertEquals(MemberStatus.LEFT, bram.status)
            assertEquals(server.now, bram.statusChangedAt)
            assertEquals(1, partyEventsOf(repository.observeParty(partyId).first(), listOf(bram)).count { it is PartyEvent.Left })
        } finally {
            db.close()
        }
    }

    @Test
    fun eachEvent_isAPill_inWords() {
        val default = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            val finishedWithoutMilestone = PartyEvent.MilestoneReached(null, at(9))
            composeTestRule.setContent {
                PixelQuestTheme(themeMode = ThemeMode.Comic) {
                    Column {
                        partyEventsOf(party, members).forEach { EventPill(it) }
                        EventPill(finishedWithoutMilestone)
                    }
                }
            }
            for (text in listOf("@aria_k joined", "@bram left", "@cleo_r was removed", "Someone joined", "Milestone set: 1,000", "Milestone reached: 1,000!", "The circle is finished")) {
                composeTestRule.onNodeWithText(text).assertIsDisplayed()
            }
        } finally {
            Locale.setDefault(default)
        }
    }
}
