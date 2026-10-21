package com.pixelquest.app.study.ui

import android.app.Application
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.StudyReminderRepositoryImpl
import com.pixelquest.app.study.data.StudyRepositoryImpl
import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeStudyReminderServer
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
import java.time.LocalDate
import java.time.ZoneId

/** Making a reminder from a study party's screen: checked first, saved on the phone, then sent. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyRemindersViewModelTest {

    private val zone = ZoneId.of("Asia/Karachi")
    private val server = FakeDaroodServer(me = "aria").apply {
        usernames += mapOf("aria" to "aria_k", "bram" to "bram")
        now = Instant.parse("2026-10-21T10:00:00Z")
    }
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val parties = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { server.now.toEpochMilli() }, zone = { zone })
    private val study = StudyRepositoryImpl(FakeStudyServer(server), db.studyDao(), db.partyDao(), { server.me }, clock = { server.now.toEpochMilli() }, timeZone = { zone })
    private val reminderServer = FakeStudyReminderServer(me = "aria")
    private val reminders = StudyReminderRepositoryImpl(reminderServer, db.studyReminderDao(), { server.me }, clock = { server.now.toEpochMilli() })
    private val party = server.seedParty("Physics group", "aria", "bram", type = PartyType.STUDY)
    private val viewModels = TestViewModels()
    private var watching: Job? = null

    @After
    fun tearDown() {
        watching?.cancel()
        viewModels.clear()
        db.closeAfterWrites()
    }

    private fun viewModel(): StudyRemindersViewModel {
        runBlocking {
            parties.refreshParties()
            parties.refreshParty(party)
        }
        return viewModels.keep(StudyRemindersViewModel::class.java) {
            StudyRemindersViewModel(SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to party)), reminders, study)
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
    fun aReminder_isCheckedFirst_thenSavedForTheParty() {
        val vm = viewModel()
        vm.openCreate()
        waitFor("the sheet open") { vm.uiState.value.form != null }
        val form = vm.uiState.value.form!!
        assertEquals("this week, by default", LocalDate.of(2026, 10, 19), form.weekStart)

        vm.save()
        waitFor("the short title shown") { vm.uiState.value.form?.triedToSave == true }
        assertEquals("nothing sent", 0, reminderServer.calls)

        vm.change(vm.uiState.value.form!!.withTitle("Quiz this week"))
        vm.save()
        waitFor("the sheet closed") { vm.uiState.value.form == null }
        val saved = reminderServer.rows.single()
        assertEquals("Quiz this week", saved.title)
        assertEquals(ReminderScope.WEEK, saved.scope)
        assertEquals(ReminderAudience.PARTY, saved.audience)
        assertEquals("Asia/Karachi", saved.tz)
        assertEquals(null, vm.uiState.value.notice)
    }

    @Test
    fun offline_itsSavedOnThePhone_andSaysSo() {
        val vm = viewModel()
        reminderServer.online = false
        vm.openCreate()
        waitFor("the sheet open") { vm.uiState.value.form != null }
        vm.change(vm.uiState.value.form!!.withTitle("Exam").withScope(ReminderScope.DAY).copy(audience = ReminderAudience.ME))
        vm.save()
        waitFor("said saved offline") { vm.uiState.value.notice == ReminderNotice.SavedOffline }
        assertTrue(reminderServer.rows.isEmpty())
        assertEquals(1, runBlocking { db.studyReminderDao().pendingToSend() }.size)
    }
}
