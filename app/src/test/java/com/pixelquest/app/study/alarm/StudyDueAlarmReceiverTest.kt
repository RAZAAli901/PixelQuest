package com.pixelquest.app.study.alarm

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.StudyRepository
import com.pixelquest.app.study.data.local.StudyItemEntity
import com.pixelquest.app.study.domain.StudyItemKind
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyDueAlarmReceiverTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: AppDatabase
    private val studyRepository = mockk<StudyRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val reminderMutes = MemoryStudyReminderMutes(on = true)
    private val account = DaroodAccount { "aria" }
    private var now = Instant.parse("2026-10-21T10:00:00Z")

    private lateinit var receiver: StudyDueAlarmReceiver
    private lateinit var scheduler: StudyDueAlarmScheduler

    @Before
    fun setUp() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(app).grantPermissions(
                android.Manifest.permission.POST_NOTIFICATIONS,
                android.Manifest.permission.USE_FULL_SCREEN_INTENT,
            )
            NotificationChannels.createAll(context)
            db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries().build()

            db.partyDao().upsertParties(
                listOf(
                    PartyEntity(
                        "party_1", "Physics group", "aria", 10, null, null, null, 0, 0, 0, 2, null,
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
                this.studyRepository = this@StudyDueAlarmReceiverTest.studyRepository
                this.settingsRepository = this@StudyDueAlarmReceiverTest.settingsRepository
                this.dueAlarmScheduler = this@StudyDueAlarmReceiverTest.scheduler
                this.account = this@StudyDueAlarmReceiverTest.account
                this.reminderMutes = this@StudyDueAlarmReceiverTest.reminderMutes
                this.ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
            }

            every { settingsRepository.isNotificationsEnabled } returns flowOf(true)
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun receivesAlarm_postsAlarmStyleNotificationWithActionsAndFullScreenIntent() = runBlocking {
        val dueAt = now.plus(Duration.ofHours(24))
        val item = StudyItemEntity(
            id = "quiz_101",
            partyId = "party_1",
            kind = StudyItemKind.QUIZ,
            title = "Mechanics Quiz",
            subject = "Physics",
            notes = "Chapter 4",
            dueAt = dueAt.toEpochMilli(),
            pinned = false,
            createdBy = "aria",
            createdAt = now.toEpochMilli(),
        )
        db.studyDao().upsertItems(listOf(item))

        val intent = Intent(context, StudyDueAlarmReceiver::class.java).apply {
            action = StudyDueAlarmScheduler.ACTION_DUE_ALARM
            putExtra(StudyDueAlarmScheduler.EXTRA_ITEM_ID, "quiz_101")
            putExtra(StudyDueAlarmScheduler.EXTRA_PARTY_ID, "party_1")
            putExtra(StudyDueAlarmScheduler.EXTRA_TITLE, "Mechanics Quiz")
            putExtra(StudyDueAlarmScheduler.EXTRA_KIND, "QUIZ")
            putExtra(StudyDueAlarmScheduler.EXTRA_DUE_AT, dueAt.toEpochMilli())
        }

        receiver.handleReceive(context, intent).join()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadow = shadowOf(notificationManager)
        val notifications = shadow.allNotifications

        assertEquals(1, notifications.size)
        val notification = notifications.first()

        // High importance alarm channel & category
        assertEquals(NotificationChannels.Spec.STUDY_DUE_ALARMS.id, notification.channelId)
        assertEquals(NotificationCompat.CATEGORY_ALARM, notification.category)

        // Actions: Done, Snooze 1 hour, Open
        assertEquals(3, notification.actions.size)
        assertEquals("Done", notification.actions[0].title.toString())
        assertEquals("Snooze 1 hour", notification.actions[1].title.toString())
        assertEquals("Open", notification.actions[2].title.toString())

        // Full-screen intent
        assertNotNull(notification.fullScreenIntent)
    }

    @Test
    fun receivesDoneAction_completesItemAndDismissesNotification() = runBlocking {
        val item = StudyItemEntity(
            id = "quiz_102",
            partyId = "party_1",
            kind = StudyItemKind.QUIZ,
            title = "Thermodynamics Quiz",
            subject = "Physics",
            notes = "Chapter 5",
            dueAt = now.plus(Duration.ofHours(24)).toEpochMilli(),
            pinned = false,
            createdBy = "aria",
            createdAt = now.toEpochMilli(),
        )
        db.studyDao().upsertItems(listOf(item))

        val intent = Intent(context, StudyDueAlarmReceiver::class.java).apply {
            action = StudyDueAlarmReceiver.ACTION_DONE
            putExtra(StudyDueAlarmScheduler.EXTRA_ITEM_ID, "quiz_102")
        }

        receiver.handleReceive(context, intent).join()

        io.mockk.coVerify(exactly = 1) { studyRepository.complete(match { it.id == "quiz_102" }) }
    }

    @Test
    fun receivesSnoozeAction_armsAlarmForOneHourLater() = runBlocking {
        val item = StudyItemEntity(
            id = "quiz_103",
            partyId = "party_1",
            kind = StudyItemKind.QUIZ,
            title = "Optics Quiz",
            subject = "Physics",
            notes = "Chapter 6",
            dueAt = now.plus(Duration.ofHours(24)).toEpochMilli(),
            pinned = false,
            createdBy = "aria",
            createdAt = now.toEpochMilli(),
        )
        db.studyDao().upsertItems(listOf(item))

        val intent = Intent(context, StudyDueAlarmReceiver::class.java).apply {
            action = StudyDueAlarmReceiver.ACTION_SNOOZE
            putExtra(StudyDueAlarmScheduler.EXTRA_ITEM_ID, "quiz_103")
            putExtra(StudyDueAlarmScheduler.EXTRA_PARTY_ID, "party_1")
            putExtra(StudyDueAlarmScheduler.EXTRA_TITLE, "Optics Quiz")
            putExtra(StudyDueAlarmScheduler.EXTRA_KIND, "QUIZ")
            putExtra(StudyDueAlarmScheduler.EXTRA_DUE_AT, item.dueAt)
        }

        receiver.handleReceive(context, intent).join()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val shadow = shadowOf(alarmManager)
        val scheduledAlarms = shadow.scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
    }
}
