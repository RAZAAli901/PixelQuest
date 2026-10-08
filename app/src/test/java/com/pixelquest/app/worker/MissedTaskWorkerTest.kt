package com.pixelquest.app.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.TaskResultRecorder
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MissedTaskWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val tasks = FakeTaskRepository()
    private val logs = FakeTaskCompletionRepository()
    private val recorder = TaskResultRecorder(logs, FakeUserProfileRepository(), FakeStreakRepository(), tasks)

    /** A daily quest due three hours ago (yesterday's date if that crosses midnight). */
    private val dueAt = LocalDateTime.now().minusHours(3)
    private val dueDay: LocalDate = dueAt.toLocalDate()

    private fun overdueQuest(id: Long) = TaskEntity(
        id = id, name = "Missed Quest", description = "", scheduledDay = dueDay,
        scheduledTime = dueAt.toLocalTime(), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
    )

    private fun runWorker(): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<MissedTaskWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    MissedTaskWorker(appContext, workerParameters, tasks, logs, FakeSettingsRepository(), recorder)
            })
            .build()
            .doWork()
    }

    @Test
    fun doWork_marksOverdueUnloggedTasksAsMissed() = runBlocking {
        tasks.insertTask(overdueQuest(1))

        assertEquals(ListenableWorker.Result.success(), runWorker())

        val missed = logs.logs.value.single()
        assertEquals(1L, missed.taskId)
        assertEquals(dueDay, missed.completedDate)
        assertEquals(false, missed.wasCompleted)
    }

    @Test
    fun doWork_leavesACompletedQuestAlone() = runBlocking {
        tasks.insertTask(overdueQuest(1))
        recorder.recordCompleted(1, dueDay)

        runWorker()

        assertTrue(logs.logs.value.single().wasCompleted)
    }
}
