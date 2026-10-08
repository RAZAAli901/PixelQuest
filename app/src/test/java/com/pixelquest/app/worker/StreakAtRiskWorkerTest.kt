package com.pixelquest.app.worker

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.notification.NotificationHelper
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FixedClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** The evening streak-at-risk nudge (pq_progress had no sender before Day 31). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StreakAtRiskWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.of(2026, 10, 8)
    private val evening = LocalDateTime.of(today, LocalTime.of(19, 30))
    private val notifications get() = shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications

    private val settings = FakeSettingsRepository()
    private val tasks = FakeTaskRepository(
        (1L..3L).map {
            TaskEntity(
                id = it, name = "Quest $it", description = "", scheduledDay = today.minusDays(5),
                scheduledTime = LocalTime.of(9, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
            )
        }
    )
    private val logs = FakeTaskCompletionRepository()
    private val streaks = FakeStreakRepository(StreakEntity(id = 1, currentStreak = 5, longestStreak = 5, lastCompletedDate = today.minusDays(1)))

    @Before
    fun setUp() {
        NotificationHelper.createNotificationChannel(context)
        context.getSharedPreferences(StreakAtRiskWorker.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun done(vararg ids: Long) = runBlocking {
        ids.forEach { logs.insertLog(TaskCompletionLogEntity(taskId = it, completedDate = today, wasCompleted = true, pointsAwarded = 50)) }
    }

    private fun run(at: LocalDateTime = evening) = runBlocking {
        TestListenableWorkerBuilder<StreakAtRiskWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    StreakAtRiskWorker(
                        appContext, workerParameters, settings, streaks, tasks, logs,
                        FakeDifficultySettingsRepository(), FixedClock(at)
                    )
            })
            .build()
            .doWork()
    }

    @Test
    fun aStreakAtRisk_getsOneNudge_onTheProgressChannel() {
        done(1) // 1 of 3 on Medium: 2 more needed

        run()

        val posted = notifications.single()
        assertEquals(NotificationChannels.Spec.PROGRESS.id, posted.channelId)
        assertEquals("🔥 Your 5-day streak is at risk!", posted.extras.getString(NotificationCompat.EXTRA_TITLE))
        assertTrue(posted.extras.getString(NotificationCompat.EXTRA_TEXT)!!.contains("2 more quests"))

        run() // a second run the same evening
        assertEquals(1, notifications.size)
    }

    @Test
    fun simpleMode_usesPlainWording() {
        settings.simpleModeEnabled.value = true

        run()

        assertEquals("Your 5-day streak ends tonight", notifications.single().extras.getString(NotificationCompat.EXTRA_TITLE))
    }

    @Test
    fun nothingIsSent_whenSafe_tooEarly_orWithNotificationsOff() {
        done(1, 2, 3)
        run()
        assertEquals("Today already keeps the streak", 0, notifications.size)

        logs.logs.value = emptyList()
        run(at = LocalDateTime.of(today, LocalTime.of(14, 0)))
        assertEquals("Too early in the day", 0, notifications.size)

        settings.isNotificationsEnabled.value = false
        run()
        assertEquals("Notifications are off", 0, notifications.size)
    }

    @Test
    fun theNextRun_isAt19() {
        assertEquals(60L, StreakAtRiskWorker.delayUntilNextRun(LocalDateTime.of(today, LocalTime.of(18, 0))))
        assertEquals(23L * 60, StreakAtRiskWorker.delayUntilNextRun(LocalDateTime.of(today, LocalTime.of(20, 0))))
    }
}
