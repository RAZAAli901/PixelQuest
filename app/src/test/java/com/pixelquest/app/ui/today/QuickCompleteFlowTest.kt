package com.pixelquest.app.ui.today

import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.ui.screens.today.TodayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class QuickCompleteFlowTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var taskRepo: FakeTaskRepository
    private lateinit var completionRepo: FakeTaskCompletionRepository
    private lateinit var streakRepo: FakeStreakRepository
    private lateinit var profileRepo: FakeUserProfileRepository
    private lateinit var difficultyRepo: FakeDifficultyRepo
    private lateinit var alarmScheduler: TaskAlarmScheduler
    private lateinit var viewModel: TodayViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        taskRepo = FakeTaskRepository()
        completionRepo = FakeTaskCompletionRepository()
        streakRepo = FakeStreakRepository()
        profileRepo = FakeUserProfileRepository()
        difficultyRepo = FakeDifficultyRepo()
        alarmScheduler = TaskAlarmScheduler(ApplicationProvider.getApplicationContext())

        viewModel = TodayViewModel(
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            difficultySettingsRepository = difficultyRepo,
            taskAlarmScheduler = alarmScheduler
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun completeTask_insertsLogAndAwardsXp() = runTest {
        val today = LocalDate.now()
        val task = TaskEntity(id = 1, name = "Daily Quest", scheduledTime = LocalTime.of(10, 0), scheduledDay = today, category = TaskCategory.FITNESS, recurrenceType = RecurrenceType.DAILY)
        taskRepo.tasksFlow.value = listOf(task)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.completeTask(task)
        testDispatcher.scheduler.advanceUntilIdle()

        val logs = completionRepo.logsFlow.value
        assertEquals(1, logs.size)
        assertTrue(logs[0].wasCompleted)
        assertEquals(1L, logs[0].taskId)

        val updatedProfile = profileRepo.profileFlow.value
        assertTrue((updatedProfile?.totalXp ?: 0) > 500)
    }

    @Test
    fun skipTask_insertsMissedLogWithoutXp() = runTest {
        val today = LocalDate.now()
        val task = TaskEntity(id = 2, name = "Skipped Quest", scheduledTime = LocalTime.of(14, 0), scheduledDay = today, category = TaskCategory.CHORES, recurrenceType = RecurrenceType.DAILY)
        taskRepo.tasksFlow.value = listOf(task)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.skipTask(task)
        testDispatcher.scheduler.advanceUntilIdle()

        val logs = completionRepo.logsFlow.value
        assertEquals(1, logs.size)
        assertEquals(false, logs[0].wasCompleted)
        assertEquals(0, logs[0].pointsAwarded)

        val updatedProfile = profileRepo.profileFlow.value
        assertEquals(500, updatedProfile?.totalXp)
    }
}
