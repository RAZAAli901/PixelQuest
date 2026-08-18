package com.pixelquest.app.worker

import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.ui.FakeTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

class FakeTaskCompletionRepository : TaskCompletionRepository {
    private val logs = MutableStateFlow<List<TaskCompletionLogEntity>>(emptyList())

    override fun getLogsForDate(date: LocalDate): Flow<List<TaskCompletionLogEntity>> =
        logs.map { list -> list.filter { it.completedAt.toLocalDate() == date } }

    override fun getLogsForTask(taskId: Long): Flow<List<TaskCompletionLogEntity>> =
        logs.map { list -> list.filter { it.taskId == taskId } }

    override fun getAllLogs(): Flow<List<TaskCompletionLogEntity>> = logs

    override suspend fun logTaskCompletion(log: TaskCompletionLogEntity): Long {
        val newId = (logs.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val newLog = log.copy(id = newId)
        logs.value = logs.value + newLog
        return newId
    }

    override suspend fun getCompletionCountBetween(
        taskId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): Int {
        return logs.value.count {
            it.taskId == taskId &&
            it.wasCompleted &&
            !it.completedAt.toLocalDate().isBefore(startDate) &&
            !it.completedAt.toLocalDate().isAfter(endDate)
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class MissedTaskWorkerTest {

    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeCompletionRepo: FakeTaskCompletionRepository

    @Before
    fun setUp() {
        fakeTaskRepo = FakeTaskRepository()
        fakeCompletionRepo = FakeTaskCompletionRepository()
    }

    @Test
    fun doWork_marksOverdueUnloggedTasksAsMissed() = runBlocking {
        val pastTime = LocalTime.now().minusHours(3)
        fakeTaskRepo.insertTask(
            TaskEntity(
                id = 1,
                name = "Missed Quest",
                description = "",
                scheduledDay = LocalDate.now(),
                scheduledTime = pastTime,
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.FITNESS
            )
        )

        val params = mock(WorkerParameters::class.java)
        val worker = MissedTaskWorker(
            ApplicationProvider.getApplicationContext(),
            params,
            fakeTaskRepo,
            fakeCompletionRepo
        )

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)

        val logs = fakeCompletionRepo.getLogsForDate(LocalDate.now()).first()
        assertEquals(1, logs.size)
        assertEquals(1L, logs.first().taskId)
        assertEquals(false, logs.first().wasCompleted)
    }
}
