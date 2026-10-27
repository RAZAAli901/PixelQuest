package com.pixelquest.app.study

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.alarm.MemoryStudyReminderMutes
import com.pixelquest.app.study.alarm.StudyDueAlarmReceiver
import com.pixelquest.app.study.alarm.StudyDueAlarmScheduler
import com.pixelquest.app.study.data.StudyRepository
import com.pixelquest.app.study.data.local.StudyItemEntity
import com.pixelquest.app.study.domain.StudyItemKind
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

/**
 * Instrumented test verifying that:
 * 1. The alarm broadcast triggers StudyDueAlarmReceiver and posts the alarm notification.
 * 2. The Done notification action completes the study item and dismisses the notification.
 */
@RunWith(AndroidJUnit4::class)
class StudyDueAlarmInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val system = context.getSystemService(NotificationManager::class.java)
    private lateinit var db: AppDatabase
    private val studyRepository = mockk<StudyRepository>(relaxed = true)
    private val settingsRepository = mockk<com.pixelquest.app.domain.repository.SettingsRepository>(relaxed = true)
    private val reminderMutes = MemoryStudyReminderMutes(on = true)
    private val account = DaroodAccount { "aria" }
    private val now = Instant.parse("2026-10-21T10:00:00Z")

    private lateinit var receiver: StudyDueAlarmReceiver
    private lateinit var scheduler: StudyDueAlarmScheduler

    @Before
    fun setUp() = runBlocking {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        NotificationChannels.createAll(context)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

        db.partyDao().upsertParties(
            listOf(
                PartyEntity(
                    "party_inst_1", "Calculus Circle", "aria", 10, null, null, null, 0, 0, 0, 2, null,
                    PartyRole.LEADER, MemberStatus.ACTIVE, 0, partyType = PartyType.STUDY,
                ),
            ),
        )

        scheduler = StudyDueAlarmScheduler(
            context = context,
            dao = db.studyDao(),
            account = account,
        )

        receiver = StudyDueAlarmReceiver().apply {
            this.studyDao = db.studyDao()
            this.partyDao = db.partyDao()
            this.studyRepository = this@StudyDueAlarmInstrumentedTest.studyRepository
            this.settingsRepository = this@StudyDueAlarmInstrumentedTest.settingsRepository
            this.dueAlarmScheduler = this@StudyDueAlarmInstrumentedTest.scheduler
            this.account = this@StudyDueAlarmInstrumentedTest.account
            this.reminderMutes = this@StudyDueAlarmInstrumentedTest.reminderMutes
            this.ioDispatcher = Dispatchers.Unconfined
        }

        coEvery { settingsRepository.isNotificationsEnabled } returns flowOf(true)
        coEvery { settingsRepository.isStudyAlarmStyleEnabled } returns flowOf(true)
        coEvery { settingsRepository.studyAlarmSound } returns flowOf("alarm")
    }

    @After
    fun tearDown() {
        val notificationId = scheduler.requestCodeForItem("quiz_inst_1")
        system.cancel(notificationId)
        db.close()
    }

    private fun waitFor(condition: () -> Boolean) {
        repeat(40) {
            if (condition()) return
            Thread.sleep(50)
        }
    }

    @Test
    fun alarmFires_notificationPosted_doneActionCompletesItem() = runBlocking {
        assumeTrue("notifications must be allowed for this app", system.areNotificationsEnabled())

        val dueAt = now.plus(Duration.ofHours(24))
        val item = StudyItemEntity(
            id = "quiz_inst_1",
            partyId = "party_inst_1",
            kind = StudyItemKind.QUIZ,
            title = "Derivatives Quiz",
            subject = "Math",
            notes = "Limits and Derivatives",
            dueAt = dueAt.toEpochMilli(),
            pinned = false,
            createdBy = "aria",
            createdAt = now.toEpochMilli(),
        )
        db.studyDao().upsertItems(listOf(item))

        val notificationId = scheduler.requestCodeForItem("quiz_inst_1")

        // 1. Alarm fires -> StudyDueAlarmReceiver handles ACTION_DUE_ALARM
        val alarmIntent = Intent(context, StudyDueAlarmReceiver::class.java).apply {
            action = StudyDueAlarmScheduler.ACTION_DUE_ALARM
            putExtra(StudyDueAlarmScheduler.EXTRA_ITEM_ID, "quiz_inst_1")
            putExtra(StudyDueAlarmScheduler.EXTRA_PARTY_ID, "party_inst_1")
            putExtra(StudyDueAlarmScheduler.EXTRA_TITLE, "Derivatives Quiz")
            putExtra(StudyDueAlarmScheduler.EXTRA_KIND, "QUIZ")
            putExtra(StudyDueAlarmScheduler.EXTRA_DUE_AT, dueAt.toEpochMilli())
        }

        receiver.handleReceive(context, alarmIntent).join()

        waitFor { system.activeNotifications.any { it.id == notificationId } }
        val postedNotification = system.activeNotifications.firstOrNull { it.id == notificationId }
        assertNotNull("Expected alarm notification to be posted", postedNotification)

        // 2. User taps Done action -> handles ACTION_DONE
        val doneIntent = Intent(context, StudyDueAlarmReceiver::class.java).apply {
            action = StudyDueAlarmReceiver.ACTION_DONE
            putExtra(StudyDueAlarmScheduler.EXTRA_ITEM_ID, "quiz_inst_1")
        }

        receiver.handleReceive(context, doneIntent).join()

        // Verify studyRepository.complete was invoked for this item
        io.mockk.coVerify(exactly = 1) { studyRepository.complete(match { it.id == "quiz_inst_1" }) }

        // Verify notification is dismissed
        waitFor { system.activeNotifications.none { it.id == notificationId } }
        val remaining = system.activeNotifications.firstOrNull { it.id == notificationId }
        assertTrue("Notification should be dismissed after Done", remaining == null)
    }
}
