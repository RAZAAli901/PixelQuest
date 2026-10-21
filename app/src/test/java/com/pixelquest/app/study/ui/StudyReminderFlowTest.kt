package com.pixelquest.app.study.ui

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.data.remote.DaroodPartyRemoteDataSource
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.alarm.MemoryStudyReminderLedger
import com.pixelquest.app.study.alarm.MemoryStudyReminderMutes
import com.pixelquest.app.study.alarm.StudyReminderAlarmSync
import com.pixelquest.app.study.alarm.StudyReminderAlertHandler
import com.pixelquest.app.study.alarm.StudyReminderNotifier
import com.pixelquest.app.study.alarm.StudyReminderScheduler
import com.pixelquest.app.study.alarm.StudyReminderSchedulerTest
import com.pixelquest.app.study.data.MemoryStudyTimerStore
import com.pixelquest.app.study.data.StudyReminderRepositoryImpl
import com.pixelquest.app.study.data.StudyRepositoryImpl
import com.pixelquest.app.study.data.remote.StudyReminderRemoteDataSource
import com.pixelquest.app.study.data.remote.StudyRemoteDataSource
import com.pixelquest.app.study.domain.ReminderAlert
import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.StudyReminder
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeStudyReminderServer
import com.pixelquest.app.testing.FakeStudyServer
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * A study reminder through the screens, on two phones sharing one server: Aria makes a party-wide
 * "Quiz this week" from This week; Bram's phone brings it, shows it on his banner, arms its
 * alerts, and shows him the alert when it comes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class StudyReminderFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val zone = ZoneId.of("Asia/Karachi")
    private val server = FakeDaroodServer(me = null).apply {
        usernames += mapOf("aria" to "aria_k", "bram" to "bram")
        // Wednesday 21 October 2026, 10:00 in Karachi.
        now = Instant.parse("2026-10-21T05:00:00Z")
    }
    private val study = FakeStudyServer(server)
    private val reminderServer = FakeStudyReminderServer(me = "aria", now = server.now)
    private val party = server.seedParty("Physics group", "aria", "bram", type = PartyType.STUDY)
    private val databases = mutableListOf<AppDatabase>()
    private val viewModels = TestViewModels()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @After
    fun tearDown() {
        scope.cancel()
        viewModels.clear()
        databases.forEach { it.closeAfterWrites() }
    }

    private inline fun <reified T> asPlayer(userId: String, target: T, crossinline be: (String) -> Unit): T = java.lang.reflect.Proxy.newProxyInstance(
        T::class.java.classLoader, arrayOf(T::class.java)
    ) { _, method, args ->
        synchronized(server) {
            be(userId)
            method.invoke(target, *(args ?: emptyArray()))
        }
    } as T

    /** A player's phone: its own cache and alarms, and the server answering it as its player. */
    private inner class Phone(userId: String) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build().also { databases += it }
        val parties = DaroodPartyRepositoryImpl(
            asPlayer<DaroodPartyRemoteDataSource>(userId, server) { server.me = it }, db.daroodDao(), { userId },
            now = { server.now.toEpochMilli() }, zone = { zone }
        )
        val repo = StudyRepositoryImpl(
            asPlayer<StudyRemoteDataSource>(userId, study) { server.me = it }, db.studyDao(), db.partyDao(), { userId },
            clock = { server.now.toEpochMilli() }, timeZone = { zone }
        )
        val reminders = StudyReminderRepositoryImpl(
            asPlayer<StudyReminderRemoteDataSource>(userId, reminderServer) { reminderServer.me = it }, db.studyReminderDao(), { userId },
            clock = { server.now.toEpochMilli() }
        )
        val alarms = StudyReminderSchedulerTest.FakeAlarms()
        val mutes = MemoryStudyReminderMutes()
        val scheduler = StudyReminderScheduler(reminders, db.studyReminderDao(), alarms, MemoryStudyReminderLedger(), clock = { server.now.toEpochMilli() }, mutes = mutes)
        val shown = mutableListOf<Pair<String, ReminderAlert>>()
        val alerts = StudyReminderAlertHandler(reminders, db.studyReminderDao(), scheduler, object : StudyReminderNotifier {
            override fun show(reminder: StudyReminder, alert: ReminderAlert) {
                shown += reminder.title to alert
            }
        }, clock = { server.now.toEpochMilli() })
        val vm: StudyPartyViewModel = viewModels.keep(StudyPartyViewModel::class.java) {
            StudyPartyViewModel(SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to party)), parties, repo, null, MemoryStudyTimerStore())
        }
        val reminderVm: StudyRemindersViewModel = viewModels.keep(StudyRemindersViewModel::class.java) {
            StudyRemindersViewModel(SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to party)), reminders, repo, parties, mutes)
        }

        init {
            StudyReminderAlarmSync(db.studyReminderDao(), scheduler, mutes).start(scope)
        }

        fun armedAt() = synchronized(alarms) { alarms.armed.values.map { it.at }.sorted() }

        @Composable
        fun Screen() = StudyPartyScreen(onNavigateBack = {}, onOpenInfo = {}, onAddPeople = {}, viewModel = vm, remindersViewModel = reminderVm)
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(300) {
            composeTestRule.waitForIdle()
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(10)
        }
        throw AssertionError("still waiting: $what")
    }

    private fun shown(tag: String) = composeTestRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun aPartyWideReminder_reachesTheOtherPhone_andAlertsThere() {
        val aria = Phone("aria")
        var screen by mutableStateOf<@Composable () -> Unit>({})
        composeTestRule.setContent { PixelQuestTheme { screen() } }

        // Aria opens the party and, from This week, makes "Quiz this week" for the whole party.
        screen = { aria.Screen() }
        waitFor("Aria's party") { aria.vm.uiState.value.loaded && shown(ReminderTags.ADD) }
        composeTestRule.onNodeWithTag(ReminderTags.ADD).performSemanticsAction(SemanticsActions.OnClick)
        waitFor("the sheet") { aria.reminderVm.uiState.value.form != null }
        // The sheet's own window is out of Robolectric's reach (as dialogs are): its fields, as chosen.
        aria.reminderVm.change(aria.reminderVm.uiState.value.form!!.withTitle("Quiz this week").copy(kind = ReminderKind.QUIZ, audience = ReminderAudience.PARTY))
        aria.reminderVm.save()
        waitFor("saved") { aria.reminderVm.uiState.value.form == null && reminderServer.rows.size == 1 }
        val quiz = reminderServer.rows.single()
        assertEquals("aria", quiz.createdBy)
        waitFor("on her banner") { shown(ReminderTags.line(quiz.id)) }

        // Bram opens the party on his phone: it's on his banner, and his phone arms its alerts.
        val bram = Phone("bram")
        screen = { bram.Screen() }
        waitFor("on Bram's banner") { shown(ReminderTags.line(quiz.id)) }
        composeTestRule.onNodeWithText("Quiz: Quiz this week", useUnmergedTree = true).assertExists()
        // Monday's 09:00 has passed; Saturday's 18:00 in Karachi is still to come.
        val saturday = Instant.parse("2026-10-24T13:00:00Z")
        waitFor("armed on Bram's phone") { bram.armedAt() == listOf(saturday) }

        // Saturday evening: the alert comes, and shows.
        val alert = synchronized(bram.alarms) { bram.alarms.armed.values.single() }
        server.now = saturday
        runBlocking { bram.alerts.handle(alert.reminderId, alert.occurrence.start.toInstant().toEpochMilli(), alert.index) }
        assertEquals(listOf("Quiz this week"), bram.shown.map { it.first })
    }
}
