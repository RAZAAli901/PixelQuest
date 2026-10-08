package com.pixelquest.app.worker

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.TaskResultRecorder
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Missed-quest notices follow the quest's own reminder settings. A quest with reminders switched off
 * still got a "missed" notice, and a SILENT quest's notice vibrated on the missed channel.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MissedNoticeReminderSettingsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val tasks = FakeTaskRepository()
    private val logs = FakeTaskCompletionRepository()
    private val recorder = TaskResultRecorder(logs, FakeUserProfileRepository(), FakeStreakRepository(), tasks)

    private val dueAt = LocalDateTime.now().minusHours(3)

    @Before
    fun onlyWhenTheQuestFallsToday() {
        // Notices are only posted for today's quests (yesterday's are recorded quietly), so before
        // 03:00 there's no "three hours ago" today to test with.
        assumeTrue(dueAt.toLocalDate() == LocalDate.now())
    }

    private fun overdue(id: Long, reminderEnabled: Boolean = true, style: ReminderStyle = ReminderStyle.STANDARD) = TaskEntity(
        id = id, name = "Quest $id", description = "", scheduledDay = dueAt.toLocalDate(), scheduledTime = dueAt.toLocalTime(),
        recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS, reminderEnabled = reminderEnabled, reminderStyle = style
    )

    private fun runWorker() = runBlocking {
        TestListenableWorkerBuilder<MissedTaskWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    MissedTaskWorker(appContext, workerParameters, tasks, logs, FakeSettingsRepository(), recorder)
            })
            .build()
            .doWork()
    }

    private fun notices() = shadowOf(context.getSystemService(NotificationManager::class.java)).activeNotifications
        .filter { it.tag == MissedTaskWorker.MISSED_TAG }

    @Test
    fun aQuestWithRemindersOff_isRecordedMissed_withoutANotice() = runBlocking {
        tasks.insertTask(overdue(1))
        tasks.insertTask(overdue(2, reminderEnabled = false))

        runWorker()

        assertEquals("Both misses are recorded", setOf(1L, 2L), logs.logs.value.filter { !it.wasCompleted }.map { it.taskId }.toSet())
        assertEquals("Only the quest with reminders gets a notice", listOf(1), notices().map { it.id })
    }

    @Test
    fun aSilentQuestsNotice_isSilent() = runBlocking {
        tasks.insertTask(overdue(3, style = ReminderStyle.SILENT))
        tasks.insertTask(overdue(4))

        runWorker()

        val byId = notices().associateBy { it.id }
        assertEquals(NotificationCompat.GROUP_ALERT_SUMMARY, NotificationCompat.getGroupAlertBehavior(byId.getValue(3).notification))
        assertEquals(NotificationCompat.GROUP_ALERT_ALL, NotificationCompat.getGroupAlertBehavior(byId.getValue(4).notification))
    }
}
