package com.pixelquest.app.ui

import app.cash.turbine.test
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.screens.tasks.TaskUiState
import com.pixelquest.app.ui.screens.tasks.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTurbineTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun taskViewModel_emitsSuccessState_andReactsToRepositoryChangesWithTurbine() = runTest(testDispatcher) {
        val testDate = LocalDate.of(2026, 10, 23)
        val clock = FixedClock(LocalDateTime.of(testDate, LocalTime.of(10, 0)))

        val taskRepo = FakeTaskRepository()
        val completionRepo = FakeTaskCompletionRepository()
        val difficultyRepo = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM),
        )
        val settingsRepo = FakeSettingsRepository()

        val task1 = TaskEntity(
            id = 1,
            name = "Morning Quest",
            description = "Start the day right",
            category = TaskCategory.FITNESS,
            scheduledDay = testDate,
            scheduledTime = LocalTime.of(8, 0),
            recurrenceType = RecurrenceType.DAILY,
        )
        taskRepo.insertTask(task1)

        val viewModel = TaskViewModel(
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            difficultySettingsRepository = difficultyRepo,
            appClock = clock,
            settingsRepository = settingsRepo,
        )

        viewModel.uiState.test {
            // UnconfinedTestDispatcher eagerly evaluates the combined StateFlow into Success
            val state1 = awaitItem() as TaskUiState.Success
            assertEquals(1, state1.tasks.size)
            assertEquals("Morning Quest", state1.tasks[0].task.name)
            assertEquals(TaskItemStatus.PENDING, state1.tasks[0].status)
            assertEquals(0.0f, state1.completionPercentage, 0.01f)

            // Complete the task in completion repo
            completionRepo.insertLog(
                TaskCompletionLogEntity(
                    id = 1,
                    taskId = 1,
                    completedDate = testDate,
                    wasCompleted = true,
                    pointsAwarded = 10,
                ),
            )

            // Next emission via Turbine reflects completion
            val state2 = awaitItem() as TaskUiState.Success
            assertEquals(1, state2.tasks.size)
            assertEquals(TaskItemStatus.COMPLETED, state2.tasks[0].status)
            assertEquals(1.0f, state2.completionPercentage, 0.01f)
            assertTrue(state2.isPerfectDay)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
