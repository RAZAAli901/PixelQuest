package com.pixelquest.app.ui.today

import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.screens.today.TodayUiState
import com.pixelquest.app.ui.screens.today.TodayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class TodayFullDayCycleQaTest {

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
    fun fullDayCycle_simulatesTaskCreationCountdownAndQuickCompletion() = runTest {
        val today = LocalDate.now()
        val task1 = TaskEntity(id = 1, name = "Morning Run", scheduledTime = LocalTime.of(8, 0), scheduledDay = today, category = TaskCategory.FITNESS, recurrenceType = RecurrenceType.DAILY)
        val task2 = TaskEntity(id = 2, name = "Evening Study", scheduledTime = LocalTime.of(20, 0), scheduledDay = today, category = TaskCategory.LEARNING, recurrenceType = RecurrenceType.DAILY)

        // 1. Initial State: 2 pending tasks
        taskRepo.tasksFlow.value = listOf(task1, task2)
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value as TodayUiState.Success
        assertEquals(2, state.tasks.size)
        assertEquals(0f, state.completionPercentage, 0.001f)

        // 2. Quick-complete task1
        viewModel.completeTask(task1)
        completionRepo.logsFlow.value = listOf(
            TaskCompletionLogEntity(id = 1, taskId = 1, completionDate = today, wasCompleted = true, pointsAwarded = 50)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value as TodayUiState.Success
        assertEquals(0.5f, state.completionPercentage, 0.001f)
        assertEquals(TaskItemStatus.DONE, state.tasks.find { it.task.id == 1L }?.status)

        // 3. Quick-complete task2
        viewModel.completeTask(task2)
        completionRepo.logsFlow.value = listOf(
            TaskCompletionLogEntity(id = 1, taskId = 1, completionDate = today, wasCompleted = true, pointsAwarded = 50),
            TaskCompletionLogEntity(id = 2, taskId = 2, completionDate = today, wasCompleted = true, pointsAwarded = 50)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value as TodayUiState.Success
        assertEquals(1.0f, state.completionPercentage, 0.001f)
        assertTrue(state.isPerfectDay)
    }
}
