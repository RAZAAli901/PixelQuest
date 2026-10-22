package com.pixelquest.app.domain.tasks

import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.TaskResultRecorder
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

class TestTaskRepo : com.pixelquest.app.testing.FakeTaskRepository() {
    val tasksFlow get() = tasks
}

class TestTaskCompletionRepo : com.pixelquest.app.testing.FakeTaskCompletionRepository() {
    val logsFlow get() = logs
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class TaskUseCasesTest {

    private lateinit var taskRepo: TestTaskRepo
    private lateinit var completionRepo: TestTaskCompletionRepo
    private lateinit var streakRepo: com.pixelquest.app.testing.FakeStreakRepository
    private lateinit var profileRepo: com.pixelquest.app.testing.FakeUserProfileRepository
    private lateinit var recorder: TaskResultRecorder
    private lateinit var alarmScheduler: TaskAlarmScheduler
    private lateinit var completeTaskUseCase: CompleteTaskUseCase
    private lateinit var skipTaskUseCase: SkipTaskUseCase

    private val testDate = LocalDate.of(2026, 10, 22)
    private val recurringTask = TaskEntity(
        id = 1,
        name = "Morning Run",
        description = "",
        category = TaskCategory.HEALTH,
        scheduledTime = LocalTime.of(8, 0),
        scheduledDay = testDate,
        recurrenceType = RecurrenceType.DAILY,
    )
    private val oneTimeTask = TaskEntity(
        id = 2,
        name = "Dentist Appointment",
        description = "",
        category = TaskCategory.HEALTH,
        scheduledTime = LocalTime.of(14, 0),
        scheduledDay = testDate,
        recurrenceType = RecurrenceType.ONE_TIME,
    )

    @Before
    fun setUp() = runBlocking {
        taskRepo = TestTaskRepo()
        completionRepo = TestTaskCompletionRepo()
        streakRepo = com.pixelquest.app.testing.FakeStreakRepository(StreakEntity(id = 1, currentStreak = 3, longestStreak = 5))
        profileRepo = com.pixelquest.app.testing.FakeUserProfileRepository(UserProfileEntity(id = 1, username = "Hero", avatarId = "avatar_hero", totalXp = 100))
        alarmScheduler = TaskAlarmScheduler(ApplicationProvider.getApplicationContext())
        recorder = TaskResultRecorder(completionRepo, profileRepo, streakRepo, taskRepo)
        completeTaskUseCase = CompleteTaskUseCase(recorder, alarmScheduler)
        skipTaskUseCase = SkipTaskUseCase(recorder, alarmScheduler)

        taskRepo.tasksFlow.value = listOf(recurringTask, oneTimeTask)
    }

    @Test
    fun completeTask_recordsCompletion_forRecurringTask() = runBlocking {
        val result = completeTaskUseCase(recurringTask, testDate)
        assertTrue(result)

        // Completing same task on same day again should return false (already done)
        val secondResult = completeTaskUseCase(recurringTask, testDate)
        assertFalse(secondResult)
    }

    @Test
    fun completeTask_recordsCompletion_forOneTimeTask() = runBlocking {
        val result = completeTaskUseCase(oneTimeTask, testDate)
        assertTrue(result)
    }

    @Test
    fun skipTask_recordsSkipped() = runBlocking {
        val result = skipTaskUseCase(recurringTask, testDate)
        assertTrue(result)

        // Skipped task can still be completed later
        val completeResult = completeTaskUseCase(recurringTask, testDate)
        assertTrue(completeResult)
    }
}
