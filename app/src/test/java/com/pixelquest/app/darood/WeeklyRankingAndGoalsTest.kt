package com.pixelquest.app.darood

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.MainActivity
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.domain.PartyGoal
import com.pixelquest.app.darood.domain.WeekPlace
import com.pixelquest.app.darood.domain.WeekResult
import com.pixelquest.app.darood.domain.WeekTone
import com.pixelquest.app.darood.sync.GoalNews
import com.pixelquest.app.darood.sync.GoalNotifyRun
import com.pixelquest.app.darood.sync.GoalToldStore
import com.pixelquest.app.darood.sync.WeekRankingMutes
import com.pixelquest.app.darood.sync.WeekRankingText
import com.pixelquest.app.darood.sync.WeekResultsRun
import com.pixelquest.app.darood.ui.chat.quickGoals
import com.pixelquest.app.darood.ui.weekly.WeeklyRankingContent
import com.pixelquest.app.darood.ui.weekly.WeeklyRankingSheet
import com.pixelquest.app.darood.ui.weekly.WeeklyRankingState
import com.pixelquest.app.darood.ui.weekly.WeeklyRankingTags
import com.pixelquest.app.darood.ui.weekly.WeeklyScreenTags
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The weekly ranking's notification, its text and screens, and goals that go on (J090). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class WeeklyRankingAndGoalsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val zone = ZoneId.of("UTC")
    // Wednesday: last week (from Monday 12 October) is over.
    private val server = FakeDaroodServer(me = "aria").apply {
        usernames += mapOf("aria" to "aria_k", "bram" to "bram", "cleo" to "cleo")
        now = Instant.parse("2026-10-21T10:00:00Z")
    }
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { server.now.toEpochMilli() }, zone = { zone })
    private val monday = LocalDate.of(2026, 10, 12)

    @After
    fun tearDown() = db.closeAfterWrites()

    private class Posted : com.pixelquest.app.darood.sync.WeekResultNotifier {
        val weeks = mutableListOf<Pair<String, WeekResult>>()
        override fun post(party: com.pixelquest.app.party.domain.Party, week: WeekResult, me: String, names: Map<String, String?>) {
            weeks += party.id to week
        }
    }

    private class Mutes(all: Boolean = true, muted: Set<String> = emptySet()) : WeekRankingMutes {
        override val allOn = MutableStateFlow(all)
        override val muted = MutableStateFlow(muted)
        override fun setAllOn(on: Boolean) { allOn.value = on }
        override fun setMuted(partyId: String, muted: Boolean) { this.muted.value = if (muted) this.muted.value + partyId else this.muted.value - partyId }
    }

    private fun week(vararg places: WeekPlace) = WeekResult("p", monday, places.toList(), Instant.EPOCH)

    @Test
    fun theWeek_isClosedAndToldOnce_withEveryonesPlace(): Unit = runBlocking {
        val party = server.seedParty("Friday circle", "aria", "bram", "cleo")
        server.seedEntry(party, "aria", 300, monday.plusDays(1))
        server.seedEntry(party, "bram", 500, monday.plusDays(2))
        server.seedEntry(party, "bram", 100, monday.plusDays(6))
        server.seedEntry(party, "cleo", 999, monday.plusDays(7)) // This week: not last week's.
        val posted = Posted()
        val run = WeekResultsRun(repository, { "aria" }, posted, Mutes())

        assertEquals(1, run.run())
        val (id, told) = posted.weeks.single()
        assertEquals(party, id)
        assertEquals(monday, told.weekStart)
        assertEquals(listOf("bram" to 1, "aria" to 2, "cleo" to 3), told.places.map { it.userId to it.position })
        assertEquals(0L, told.placeOf("cleo")!!.weekTotal)
        assertEquals("told once", 0, run.run())
        // And it's on the phone for the chat's card and the ranking screen.
        assertEquals(listOf(monday), db.daroodDao().observeWeekResults(party).first().map { it.weekStart }.distinct())
    }

    @Test
    fun aMutedParty_isToldQuietly_andNeverLater(): Unit = runBlocking {
        val party = server.seedParty("Quiet circle", "aria", "bram")
        server.seedEntry(party, "aria", 10, monday)
        val posted = Posted()
        val mutes = Mutes(muted = setOf(party))
        assertEquals(0, WeekResultsRun(repository, { "aria" }, posted, mutes).run())
        mutes.setMuted(party, false)
        assertEquals("turned back on: the old week isn't told after all", 0, WeekResultsRun(repository, { "aria" }, posted, mutes).run())
        assertTrue(posted.weeks.isEmpty())
    }

    @Test
    fun stillSundayInThePartysZone_nothingYet(): Unit = runBlocking {
        server.seedParty("Late circle", "aria", "bram")
        server.partyToday = { LocalDate.of(2026, 10, 18) }
        assertEquals(0, WeekResultsRun(repository, { "aria" }, Posted(), Mutes()).run())
    }

    @Test
    fun theText_saysThePlaceKindly_andTheSameForTheLeader() {
        assertEquals(
            listOf("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st", "22nd", "111th"),
            listOf(1, 2, 3, 4, 11, 12, 13, 21, 22, 111).map { WeekRankingText.ordinal(context, it) }
        )
        val w = week(WeekPlace("aria", 1, 3200, 4), WeekPlace("bram", 2, 3200 - 1, 3), WeekPlace("cleo", 3, 40, 1), WeekPlace("dara", 3, 40, 1))
        assertEquals("Your position this week: 2nd of 4 with 3,199 Darood", WeekRankingText.line(context, w, "bram"))
        assertEquals(WeekTone.TOP, WeekTone.of(w, "aria"))
        assertEquals(WeekTone.NONE, WeekTone.of(w, "bram"))
        assertEquals("tied last, both encouraged", listOf(WeekTone.ENCOURAGE, WeekTone.ENCOURAGE), listOf(WeekTone.of(w, "cleo"), WeekTone.of(w, "dara")))
        assertEquals(WeekTone.ENCOURAGE, WeekTone.of(week(WeekPlace("solo", 1, 0, 0)), "solo"))
        assertTrue(WeekRankingText.tone(context, w, "cleo")!!.startsWith("Every Darood counts"))
        // Nothing in the text depends on who leads: the same place reads the same.
        val led = week(WeekPlace("leader", 1, 10, 1), WeekPlace("member", 1, 10, 1))
        assertEquals(WeekRankingText.line(context, led, "leader"), WeekRankingText.line(context, led, "member"))
        assertEquals(WeekRankingText.tone(context, led, "leader"), WeekRankingText.tone(context, led, "member"))
        val expanded = WeekRankingText.expanded(context, w, "bram", mapOf("aria" to "aria_k", "cleo" to "cleo", "dara" to null))
        assertTrue(expanded, expanded.contains("1st aria_k 3,200") && expanded.contains("2nd You 3,199") && expanded.contains("3rd Someone 40"))
    }

    @Test
    fun theSheet_andTheScreen_showTheWeeks() {
        val older = WeekResult("p", monday.minusWeeks(1), listOf(WeekPlace("aria", 4, 10, 1), WeekPlace("bram", 1, 99, 2)), Instant.EPOCH)
        val latest = week(WeekPlace("bram", 1, 500, 2), WeekPlace("aria", 2, 300, 1))
        val state = WeeklyRankingState(listOf(latest, older), mapOf("bram" to "bram"), "aria", refreshed = true)
        var screen by androidx.compose.runtime.mutableStateOf(false)
        composeTestRule.setContent {
            PixelQuestTheme {
                if (screen) WeeklyRankingContent(state, onNavigateBack = {}) else WeeklyRankingSheet(state, onDismiss = {}, onSeeAll = {})
            }
        }
        composeTestRule.onNodeWithTag(WeeklyRankingTags.SHEET).assertExists()
        composeTestRule.onNodeWithTag(WeeklyRankingTags.row("aria")).assertExists()
        composeTestRule.onNodeWithText("Your position this week: 2nd of 2 with 300 Darood").assertExists()
        composeTestRule.onNodeWithTag(WeeklyRankingTags.ALL_WEEKS).assertExists()
        screen = true
        composeTestRule.onNodeWithTag(WeeklyScreenTags.TREND).assertExists()
        composeTestRule.onNodeWithText("Up from 4th to 2nd. MashaAllah.").assertExists()
    }

    @Test
    fun noWeekYet_saysSo() {
        composeTestRule.setContent { PixelQuestTheme { WeeklyRankingSheet(WeeklyRankingState(me = "aria", refreshed = true), onDismiss = {}) } }
        composeTestRule.onNodeWithTag(WeeklyRankingTags.EMPTY).assertExists()
    }

    private class Told : GoalToldStore {
        val map = mutableMapOf<String, Pair<Int, Boolean>>()
        override fun told(partyId: String) = map[partyId]
        override fun setTold(partyId: String, sequence: Int, reached: Boolean) { map[partyId] = sequence to reached }
    }

    @Test
    fun goals_reached_extended_andTold(): Unit = runBlocking {
        val party = server.seedParty("Goal circle", "aria", "bram", milestone = 100)
        repository.refreshParties()
        val told = Told()
        val asBram = mutableListOf<GoalNews>()
        val asAria = mutableListOf<GoalNews>()
        fun bram() = runBlocking { server.me = "bram"; GoalNotifyRun(repository, { "bram" }, told, { asBram += it }).run().also { server.me = "aria" } }
        val ariaTold = Told()
        fun aria() = runBlocking { GoalNotifyRun(repository, { "aria" }, ariaTold, { asAria += it }).run() }

        assertEquals("the first look tells nothing old", 0, bram())
        aria()
        server.seedEntry(party, "bram", 100)
        // As the server does when an entry reaches it.
        server.parties[party] = server.parties[party]!!.copy(completedAt = server.now.toString())
        server.goals[party]!![0] = server.goals[party]!![0].copy(reachedAt = server.now)
        assertEquals(1, bram())
        assertTrue(asBram.single() is GoalNews.Reached)

        // The leader extends: +25%, +50% or double of the goal reached, above the total.
        assertEquals(listOf(25 to 125L, 50 to 150L, 100 to 200L), quickGoals(total = 100, reached = 100))
        assertTrue(repository.setMilestone(party, 50) is com.pixelquest.app.data.remote.SupabaseResult.ServerError)
        assertTrue(repository.setMilestone(party, 200) is com.pixelquest.app.data.remote.SupabaseResult.Success)
        assertEquals(listOf(1, 2), server.goals[party]!!.map(PartyGoal::sequence))
        assertEquals("open again", null, server.parties[party]!!.completedAt)
        assertEquals(1, bram())
        assertEquals(200L, (asBram.last() as GoalNews.NewGoal).goal.target)
        aria()
        assertTrue("the leader isn't told of the goal they set", asAria.none { it is GoalNews.NewGoal })
    }

    @Test
    fun onlyAPartysOwnRoutes_openFromANotification() {
        val id = "6f1c2d3e-4a5b-4c6d-8e9f-0a1b2c3d4e5f"
        assertTrue(MainActivity.isOpenable(Screen.DaroodWeek.createRoute(id)))
        assertTrue(MainActivity.isOpenable(Screen.DaroodRoom.createRoute(id)))
        assertFalse(MainActivity.isOpenable(Screen.DaroodWeek.createRoute("../settings")))
        assertFalse(MainActivity.isOpenable(Screen.DaroodRoom.createRoute("$id/x")))
        assertFalse(MainActivity.isOpenable(Screen.DaroodWeeks.createRoute(id)))
    }
}
