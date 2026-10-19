package com.pixelquest.app.darood.ui.race

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.domain.RaceLine
import com.pixelquest.app.darood.domain.rankRace
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.time.ZoneId

/** The race board: finished first, by finish, then by progress; the player's own picked out. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class RaceBoardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun line(name: String, progress: Float, finished: String? = null, mine: Boolean = false, total: Long = (progress * 1000).toLong()) =
        RaceLine(name, 5, total, 1000, progress, finished?.let(Instant::parse), mine)

    @Test
    fun finishedFirst_byWhenTheyFinished_thenTheFurthest() {
        val lines = listOf(
            line("Halfway", 0.5f),
            line("Late finish", 1f, finished = "2026-10-19T10:00:00Z"),
            line("Nearly", 0.9f, mine = true),
            line("Early finish", 1f, finished = "2026-10-12T10:00:00Z"),
            line("Level B", 0.5f, total = 400),
            line("Level A", 0.5f, total = 400)
        )
        assertEquals(
            listOf("Early finish", "Late finish", "Nearly", "Halfway", "Level A", "Level B"),
            rankRace(lines).map { it.name }
        )
    }

    @Test
    fun theBoard_placesEachParty_andPicksOutThePlayers() {
        val lines = rankRace(listOf(line("Friday circle", 0.24f, mine = true), line("Early finish", 1f, finished = "2026-10-12T10:00:00Z")))
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) { RaceBoardContent(state = RaceBoardUiState(lines = lines), onNavigateBack = {}) }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.raceRow(1, mine = false)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.raceRow(2, mine = true)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Yours", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("240 of 1,000 (24%)", substring = true, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("never anyone's name or numbers", substring = true).assertIsDisplayed()
    }

    @Test
    fun anEmptyBoard_andAnOfflineOne_sayWhy() {
        val state = androidx.compose.runtime.mutableStateOf(RaceBoardUiState(lines = emptyList()))
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) { RaceBoardContent(state = state.value, onNavigateBack = {}) }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.RACE_EMPTY).assertIsDisplayed()
        state.value = RaceBoardUiState(problem = RaceProblem.OFFLINE)
        composeTestRule.onNodeWithTag(DaroodTestTags.RACE_PROBLEM).assertIsDisplayed()
        composeTestRule.onNodeWithText("Try again").assertIsDisplayed()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun theViewModel_readsTheServersBoard_freshEachTime() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val store = androidx.lifecycle.ViewModelStore()
        try {
            val server = FakeDaroodServer(me = "aria-id").apply { usernames += mapOf("aria-id" to "aria_k", "bram-id" to "bram") }
            val mine = server.seedParty("Friday circle", "aria-id", milestone = 1000)
            val theirs = server.seedParty("Family", "bram-id", milestone = 500)
            server.seedParty("Quiet", "bram-id", milestone = 500)
            server.parties[mine] = server.parties.getValue(mine).copy(isPublicRace = true)
            server.parties[theirs] = server.parties.getValue(theirs).copy(isPublicRace = true)
            server.seedEntry(theirs, "bram-id", 400)
            val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { ZoneId.of("UTC") })
            val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = RaceBoardViewModel(repository) as T
            }
            val vm = androidx.lifecycle.ViewModelProvider(store, factory)[RaceBoardViewModel::class.java]
            run {
                repeat(150) {
                    if (vm.uiState.value.lines != null) return@run
                    Thread.sleep(20)
                }
            }
            val lines = vm.uiState.value.lines!!
            assertEquals(listOf("Family", "Friday circle"), lines.map { it.name })
            assertEquals(listOf(false, true), lines.map { it.mine })

            server.offline = true
            vm.refresh()
            Thread.sleep(100)
            assertEquals(RaceProblem.OFFLINE, vm.uiState.value.problem)
            assertEquals("the last board stays", 2, vm.uiState.value.lines?.size)
        } finally {
            store.clear()
            Dispatchers.resetMain()
            db.close()
        }
    }
}
