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
}
