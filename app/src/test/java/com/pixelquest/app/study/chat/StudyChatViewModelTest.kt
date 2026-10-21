package com.pixelquest.app.study.chat

import android.app.Application
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.chat.data.StudyChatRepositoryImpl
import com.pixelquest.app.study.chat.ui.ChatNotice
import com.pixelquest.app.study.chat.ui.StudyChatViewModel
import com.pixelquest.app.study.data.StudyRepositoryImpl
import com.pixelquest.app.study.ui.StudyPartyViewModel
import com.pixelquest.app.testing.FakeChatServer
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeStudyServer
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

/** A study party's Chat tab: what it shows and what each action does, on servers in memory. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyChatViewModelTest {

    private val zone = ZoneId.of("Asia/Karachi")
    private val server = FakeDaroodServer(me = "aria").apply {
        usernames += mapOf("aria" to "aria_k", "bram" to "bram")
        now = Instant.parse("2026-10-21T10:00:00Z")
    }
    private val party = server.seedParty("Physics group", "aria", "bram", type = PartyType.STUDY)
    private val chatServer = FakeChatServer(me = "aria", now = server.now).apply { members[party] = mutableSetOf("aria", "bram") }
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val parties = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { server.now.toEpochMilli() }, zone = { zone })
    private val study = StudyRepositoryImpl(FakeStudyServer(server), db.studyDao(), db.partyDao(), { server.me }, clock = { server.now.toEpochMilli() }, timeZone = { zone })
    private val chat = StudyChatRepositoryImpl(chatServer, db.chatDao(), { server.me }, clock = { chatServer.now.toEpochMilli() })
    private val viewModels = TestViewModels()
    private var watching: Job? = null

    @After
    fun tearDown() {
        watching?.cancel()
        viewModels.clear()
        db.closeAfterWrites()
    }

    private fun viewModel(): StudyChatViewModel {
        runBlocking {
            parties.refreshParties()
            parties.refreshParty(party)
        }
        return viewModels.keep(StudyChatViewModel::class.java) {
            StudyChatViewModel(SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to party)), chat, parties, study)
        }.also { vm -> watching = vm.uiState.launchIn(MainScope()) }
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(20))
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("still waiting: $what")
    }

    @Test
    fun theChat_showsNames_runsAndReplies_andSendsAsThePlayer() {
        val first = chatServer.seed(party, "bram", "Anyone done Q3?", minutesAgo = 3)
        chatServer.seed(party, "bram", "It's the hard one", minutesAgo = 2)
        val vm = viewModel()
        waitFor("loaded") { vm.uiState.value.loaded && vm.uiState.value.rows.size == 2 }
        val rows = vm.uiState.value.rows
        assertEquals(listOf("bram", "bram"), rows.map { it.senderName })
        assertEquals("one run, named once", listOf(true, false), rows.map { it.firstOfRun })
        assertTrue(rows.none { it.mine })

        vm.replyTo(rows.first())
        waitFor("replying") { vm.uiState.value.replyingTo?.message?.id == first.id }
        vm.changeDraft("Yes: it's 42")
        vm.send()
        waitFor("sent") { chatServer.messages.size == 3 && vm.uiState.value.rows.size == 3 }
        val mine = vm.uiState.value.rows.last()
        assertTrue(mine.mine)
        assertEquals("aria", chatServer.messages.last().senderId)
        assertEquals(first.id, mine.replyTo?.id)
        assertEquals("bram", mine.replyToName)
        assertEquals("", vm.uiState.value.draft)
        assertEquals(null, vm.uiState.value.replyingTo)
    }

    @Test
    fun offline_itsSavedAndSaysSo_andTheDraftStopsAtItsLimit() {
        val vm = viewModel()
        waitFor("loaded") { vm.uiState.value.loaded }
        chatServer.online = false
        vm.changeDraft("x".repeat(2100))
        waitFor("cut") { vm.uiState.value.draft.length == 2000 }
        vm.changeDraft("Back soon")
        vm.send()
        waitFor("said saved offline") { vm.notices.value == ChatNotice.SavedOffline }
        assertEquals(listOf("Back soon"), vm.uiState.value.rows.map { it.message.body })
    }

    @Test
    fun theUnreadMark_isWhereItWasWhenTheChatOpened() {
        chatServer.seed(party, "bram", "Earlier", minutesAgo = 10)
        runBlocking {
            parties.refreshParties()
            parties.refreshParty(party)
            chat.refresh(party)
            chat.markRead(party)
        }
        chatServer.seed(party, "bram", "New since")
        val vm = viewModel()
        waitFor("loaded") { vm.uiState.value.rows.size == 2 }
        assertEquals(server.now, vm.uiState.value.lastReadAt)
        vm.markRead()
        waitFor("marked") { chatServer.reads[party to "aria"] != null }
        assertEquals("kept while it's open", server.now, vm.uiState.value.lastReadAt)
    }

    @Test
    fun deleting_isTheServersToAllow() {
        val theirs = chatServer.seed(party, "bram", "Bram's")
        val vm = viewModel()
        waitFor("loaded") { vm.uiState.value.rows.size == 1 }
        vm.delete(theirs.id)
        waitFor("refused") { vm.notices.value is ChatNotice.Refused }
        chatServer.leaders[party] = "aria"
        vm.delete(theirs.id)
        waitFor("deleted") { vm.uiState.value.rows.single().message.deleted }
    }
}
