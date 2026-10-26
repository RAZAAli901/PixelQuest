package com.pixelquest.app.study.ui

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.StudyRepositoryImpl
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeStudyServer
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** The study parties' Home card: only for someone in one, with what they haven't finished, soonest first. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class HomeStudyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val now = Instant.parse("2026-10-21T10:00:00Z")
    private val server = FakeDaroodServer(me = "aria").apply { this.now = this@HomeStudyTest.now }
    private val study = FakeStudyServer(server)
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val parties = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { server.now.toEpochMilli() }, zone = { ZoneId.of("UTC") })
    private val repo = StudyRepositoryImpl(study, db.studyDao(), db.partyDao(), { server.me }, clock = { server.now.toEpochMilli() }, timeZone = { ZoneId.of("UTC") })
    private val viewModels = TestViewModels()

    @After
    fun tearDown() {
        viewModels.clear()
        db.closeAfterWrites()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("still waiting: $what")
    }

    @Test
    fun itShowsWhatsUnfinished_soonestFirst_onlyForSomeoneInAStudyParty() {
        val vm = viewModels.keep(HomeStudyViewModel::class.java) { HomeStudyViewModel(parties, repo) }
        vm.home.launchIn(MainScope())
        runBlocking { parties.refreshParties() }
        waitFor("nothing without a party") { true }
        assertNull(vm.home.value)

        val party = server.seedParty("Physics group", "aria", "bram", type = PartyType.STUDY)
        val later = study.seedItem(party, "bram", "Essay", dueAt = now.plus(Duration.ofDays(3)))
        val soon = study.seedItem(party, "bram", "Waves quiz", dueAt = now.plus(Duration.ofHours(3)))
        val done = study.seedItem(party, "bram", "Lab report", dueAt = now.plus(Duration.ofHours(1)))
        study.seedItem(party, "bram", "Far off", dueAt = now.plus(Duration.ofDays(10)))
        study.seedCompletion(done.id, "aria")
        runBlocking {
            parties.refreshParties()
            repo.refresh(party)
        }
        waitFor("the card") { vm.home.value?.due?.map { it.itemId } == listOf(soon.id, later.id) }
        assertEquals("Physics group", vm.home.value!!.due.first().partyName)
    }

    @Test
    fun aLine_opensItsParty() {
        var opened = false
        val line = HomeDueLine("p1", "Physics group", "q", "Waves quiz", DueState.TODAY, now.plus(Duration.ofHours(3)))
        composeTestRule.setContent {
            PixelQuestTheme { HomeStudyCard(HomeStudy(listOf(line)), onOpenParty = {}, onOpenParties = { opened = true }, zone = ZoneId.of("UTC")) }
        }
        composeTestRule.onNodeWithText("Physics group · Due today, ", substring = true, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(StudyTags.homeLine("q")).performClick()
        org.junit.Assert.assertTrue(opened)
    }
}
