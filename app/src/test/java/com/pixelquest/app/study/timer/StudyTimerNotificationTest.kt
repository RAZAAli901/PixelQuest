package com.pixelquest.app.study.timer

import android.app.Application
import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.R
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.study.data.MemoryStudyTimerStore
import com.pixelquest.app.study.data.RunningTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyTimerNotificationTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var service: StudyTimerService

    @Before
    fun setUp() {
        NotificationChannels.createAll(context)
        val controller = Robolectric.buildService(StudyTimerService::class.java)
        service = controller.get().apply {
            this.timerStore = MemoryStudyTimerStore()
            this.studyRepository = io.mockk.mockk(relaxed = true)
            this.partyDao = io.mockk.mockk(relaxed = true)
        }
    }

    @Test
    fun runningTimer_buildsOngoingNotificationWithChronometer() {
        val t0 = 1_000_000L
        val timer = RunningTimer(
            partyId = "party_1",
            startedAt = t0,
            partyName = "Physics Circle",
        )

        val notification = service.buildForegroundNotification(timer)

        // 1. Channel
        assertEquals(NotificationChannels.Spec.STUDY_TIMER.id, notification.channelId)

        // 2. Title format: "Studying with <party name>"
        val title = notification.extras.getString(NotificationCompat.EXTRA_TITLE)
        assertEquals(context.getString(R.string.study_timer_notif_title, "Physics Circle"), title)

        // 3. Chronometer active
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))

        // 4. Ongoing flag
        assertTrue((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0)
    }

    @Test
    fun soloTimer_buildsOngoingNotificationWithSoloTitle() {
        val t0 = 1_000_000L
        val timer = RunningTimer(
            partyId = "party_solo",
            startedAt = t0,
            partyName = null,
        )

        val notification = service.buildForegroundNotification(timer)
        val title = notification.extras.getString(NotificationCompat.EXTRA_TITLE)
        assertEquals(context.getString(R.string.study_timer_notif_title_solo), title)
    }

    @Test
    fun runningTimer_hasPauseAndStopActions() {
        val timer = RunningTimer("party_1", 1_000_000L, "Physics Circle")
        val notification = service.buildForegroundNotification(timer)

        assertEquals(2, notification.actions.size)
        assertEquals(context.getString(R.string.study_timer_action_pause), notification.actions[0].title.toString())
        assertEquals(context.getString(R.string.study_timer_action_stop), notification.actions[1].title.toString())
        val pauseShadow = org.robolectric.Shadows.shadowOf(notification.actions[0].actionIntent)
        assertEquals(StudyTimerService.ACTION_PAUSE, pauseShadow.savedIntent.action)
        val stopShadow = org.robolectric.Shadows.shadowOf(notification.actions[1].actionIntent)
        assertEquals(StudyTimerService.ACTION_STOP, stopShadow.savedIntent.action)
    }

    @Test
    fun pausedTimer_hasResumeAndStopActions_andStaticElapsedText() {
        val timer = RunningTimer(
            partyId = "party_1",
            startedAt = 1_000_000L,
            partyName = "Physics Circle",
            pausedAt = 1_600_000L, // 10 mins active
            accumulatedPauseMs = 0L,
        )
        val notification = service.buildForegroundNotification(timer)

        assertEquals(2, notification.actions.size)
        assertEquals(context.getString(R.string.study_timer_action_resume), notification.actions[0].title.toString())
        assertEquals(context.getString(R.string.study_timer_action_stop), notification.actions[1].title.toString())
        val resumeShadow = org.robolectric.Shadows.shadowOf(notification.actions[0].actionIntent)
        assertEquals(StudyTimerService.ACTION_RESUME, resumeShadow.savedIntent.action)
        val stopShadow = org.robolectric.Shadows.shadowOf(notification.actions[1].actionIntent)
        assertEquals(StudyTimerService.ACTION_STOP, stopShadow.savedIntent.action)

        // Chronometer is disabled while paused; static text shows "Paused · 10:00"
        assertFalse(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        val body = notification.extras.getString(NotificationCompat.EXTRA_TEXT).orEmpty()
        assertTrue(body.contains("Paused"))
    }

    @Test
    fun actions_pauseAndResumeServiceIntents() {
        val store = MemoryStudyTimerStore()
        service.timerStore = store
        store.start("party_1", 1_000_000L, "Physics Circle")
        assertFalse(store.running.value?.isPaused == true)

        // Trigger ACTION_PAUSE
        val pauseIntent = android.content.Intent(service, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_PAUSE
        }
        service.onStartCommand(pauseIntent, 0, 1)
        assertTrue(store.running.value?.isPaused == true)

        // Trigger ACTION_RESUME
        val resumeIntent = android.content.Intent(service, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_RESUME
        }
        service.onStartCommand(resumeIntent, 0, 2)
        assertFalse(store.running.value?.isPaused == true)

        // Trigger ACTION_CANCEL
        val cancelIntent = android.content.Intent(service, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_CANCEL
        }
        service.onStartCommand(cancelIntent, 0, 3)
        assertNull(store.running.value)
    }

    @Test
    fun contentIntent_opensStudyPartyTimerScreen() {
        val timer = RunningTimer("party_math", 1_000_000L, "Math Circle")
        val notification = service.buildForegroundNotification(timer)

        assertNotNull(notification.contentIntent)
        val shadow = org.robolectric.Shadows.shadowOf(notification.contentIntent)
        val intent = shadow.savedIntent
        assertEquals("party_math", intent.getStringExtra("OPEN_STUDY_PARTY_ID"))
        assertEquals(com.pixelquest.app.ui.navigation.Screen.PartiesParty.createRoute("party_math"), intent.getStringExtra(com.pixelquest.app.MainActivity.EXTRA_OPEN_ROUTE))
        assertEquals("WEEK", intent.getStringExtra("FOCUS_TAB"))
    }

    @Test
    fun stop_under5Minutes_discardsSessionAndPostsNotSavedNotification() = kotlinx.coroutines.test.runTest {
        val store = MemoryStudyTimerStore()
        service.timerStore = store
        val t0 = 1_000_000L
        store.start("party_math", t0, "Math Circle")

        // Stopped after 4 minutes (< 5 mins)
        val logged = service.performStop(t0 + 4 * 60_000L)
        assertNull(logged)
        assertNull(store.running.value)

        io.mockk.coVerify(exactly = 0) { service.studyRepository.logStudy(any(), any()) }

        val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNm = org.robolectric.Shadows.shadowOf(nm)
        val notif = shadowNm.getNotification(StudyTimerService.COMPLETION_NOTIFICATION_ID)
        assertNotNull(notif)
        val text = notif.extras.getString(NotificationCompat.EXTRA_TEXT).orEmpty()
        assertTrue(text.contains("under 5 minutes"))
    }

    @Test
    fun stop_validDuration_logsToRepositoryAndPostsSavedNotification() = kotlinx.coroutines.test.runTest {
        val store = MemoryStudyTimerStore()
        service.timerStore = store
        val t0 = 1_000_000L
        store.start("party_math", t0, "Math Circle")

        // Stopped after 45 minutes
        val logged = service.performStop(t0 + 45 * 60_000L)
        assertEquals(45, logged)
        assertNull(store.running.value)

        io.mockk.coVerify(exactly = 1) { service.studyRepository.logStudy("party_math", 45) }

        val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNm = org.robolectric.Shadows.shadowOf(nm)
        val notif = shadowNm.getNotification(StudyTimerService.COMPLETION_NOTIFICATION_ID)
        assertNotNull(notif)
        val text = notif.extras.getString(NotificationCompat.EXTRA_TEXT).orEmpty()
        assertTrue(text.contains("45 mins"))
    }

    @Test
    fun stop_over4Hours_capsAt240Minutes() = kotlinx.coroutines.test.runTest {
        val store = MemoryStudyTimerStore()
        service.timerStore = store
        val t0 = 1_000_000L
        store.start("party_math", t0, "Math Circle")

        // Stopped after 300 minutes (5 hours) -> capped at 240 mins (4 hours)
        val logged = service.performStop(t0 + 300 * 60_000L)
        assertEquals(240, logged)
        assertNull(store.running.value)

        io.mockk.coVerify(exactly = 1) { service.studyRepository.logStudy("party_math", 240) }

        val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNm = org.robolectric.Shadows.shadowOf(nm)
        val notif = shadowNm.getNotification(StudyTimerService.COMPLETION_NOTIFICATION_ID)
        assertNotNull(notif)
        val text = notif.extras.getString(NotificationCompat.EXTRA_TEXT).orEmpty()
        assertTrue(text.contains("240 mins"))
    }

    @Test
    fun channelAndServiceDeclaration_areQuietLowImportance_andHavePermissions() {
        val spec = NotificationChannels.Spec.STUDY_TIMER
        assertEquals(NotificationChannels.Importance.LOW, spec.importance)
        assertFalse(spec.sound)
        assertFalse(spec.vibration)

        val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = nm.getNotificationChannel(spec.id)
        assertNotNull(channel)
        assertEquals(android.app.NotificationManager.IMPORTANCE_LOW, channel.importance)
        assertNull(channel.sound)
        assertFalse(channel.shouldVibrate())

        val app = ApplicationProvider.getApplicationContext<Application>()
        assertFalse(StudyTimerService.hasNotificationPermission(app))
        org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(StudyTimerService.hasNotificationPermission(app))
    }

    @Test
    fun restore_runningTimer_reconstructsOngoingNotification() {
        val store = MemoryStudyTimerStore()
        service.timerStore = store
        val t0 = System.currentTimeMillis() - 30 * 60_000L // 30 mins ago
        store.start("party_chem", t0, "Chemistry Circle")

        val restoreIntent = android.content.Intent(service, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_RESTORE
        }
        service.onStartCommand(restoreIntent, 0, 1)

        val notif = service.buildForegroundNotification(store.running.value!!)
        assertEquals(context.getString(R.string.study_timer_notif_title, "Chemistry Circle"), notif.extras.getString(NotificationCompat.EXTRA_TITLE))
        assertTrue(notif.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue((notif.flags and Notification.FLAG_ONGOING_EVENT) != 0)
    }

    @Test
    fun restore_pausedTimer_reconstructsPausedNotification() {
        val store = MemoryStudyTimerStore()
        service.timerStore = store
        val t0 = System.currentTimeMillis() - 40 * 60_000L
        store.start("party_bio", t0, "Biology Circle")
        store.pause(t0 + 20 * 60_000L) // Paused after 20 mins

        val restoreIntent = android.content.Intent(service, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_RESTORE
        }
        service.onStartCommand(restoreIntent, 0, 1)

        val notif = service.buildForegroundNotification(store.running.value!!)
        assertFalse(notif.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        val body = notif.extras.getString(NotificationCompat.EXTRA_TEXT).orEmpty()
        assertTrue(body.contains("Paused"))
        assertEquals(2, notif.actions.size)
        assertEquals(context.getString(R.string.study_timer_action_resume), notif.actions[0].title.toString())
    }

    @Test
    fun restore_expiredTimer_performsStopAndLogsCapped240Minutes() = kotlinx.coroutines.test.runTest {
        val store = MemoryStudyTimerStore()
        service.timerStore = store
        val t0 = System.currentTimeMillis() - 300 * 60_000L // 5 hours ago (> 4 hours)
        store.start("party_history", t0, "History Circle")

        val restoreIntent = android.content.Intent(service, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_RESTORE
        }
        service.onStartCommand(restoreIntent, 0, 1)

        // Wait for background IO to complete
        var attempts = 0
        while (store.running.value != null && attempts < 50) {
            Thread.sleep(20)
            attempts++
        }

        io.mockk.coVerify(timeout = 2000) { service.studyRepository.logStudy("party_history", 240) }
        assertNull(store.running.value)
    }

    @Test
    fun restore_nullTimer_stopsSelfWithoutCrashing() {
        val store = MemoryStudyTimerStore()
        service.timerStore = store

        val restoreIntent = android.content.Intent(service, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_RESTORE
        }
        val result = service.onStartCommand(restoreIntent, 0, 1)
        assertEquals(android.app.Service.START_STICKY, result)
        assertNull(store.running.value)
    }
}
