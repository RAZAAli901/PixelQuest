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
}
