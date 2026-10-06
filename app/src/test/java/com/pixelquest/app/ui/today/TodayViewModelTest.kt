package com.pixelquest.app.ui.today

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.screens.today.TodayUiState
import com.pixelquest.app.ui.screens.today.TodayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/*
 * The Today tests share these: the in-memory repositories from com.pixelquest.app.testing, under
 * the flow names the tests were written with, seeded with a level 3 hero on a 5-day streak.
 */
class FakeTaskRepository : com.pixelquest.app.testing.FakeTaskRepository() {
    val tasksFlow get() = tasks
}

class FakeTaskCompletionRepository : com.pixelquest.app.testing.FakeTaskCompletionRepository() {
    val logsFlow get() = logs
}

class FakeStreakRepository : com.pixelquest.app.testing.FakeStreakRepository(
    StreakEntity(id = 1, currentStreak = 5, longestStreak = 10)
) {
    val streakFlow get() = streak
}

class FakeUserProfileRepository : com.pixelquest.app.testing.FakeUserProfileRepository(
    UserProfileEntity(id = 1, username = "Hero", avatarId = "avatar_hero", totalXp = 500, level = 3, perfectDaysTowardNextLevel = 2)
) {
    val profileFlow get() = profile
}

class FakeDifficultyRepo : com.pixelquest.app.testing.FakeDifficultySettingsRepository(
    DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, perfectDayThreshold = 0.7f, daysRequiredPerLevel = 7)
) {
    val difficultyFlow get() = settings
}

/** Noon today: before an 18:00 quest, after an 08:00 one. */
fun todayAtNoon() = java.time.LocalDate.now().atTime(12, 0)

@RunWith(RobolectricTestRunner::class)
// A plain Application: the real one builds the real database, whose background seeding outlived the test
@org.robolectric.annotation.Config(sdk = [34], application = android.app.Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

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
            taskAlarmScheduler = alarmScheduler,
            taskResultRecorder = com.pixelquest.app.domain.TaskResultRecorder(completionRepo, profileRepo, streakRepo),
            appClock = com.pixelquest.app.testing.FixedClock(todayAtNoon())
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun combineLogic_mapsTaskStatusCorrectly() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val today = LocalDate.now()
        val task1 = TaskEntity(id = 1, description = "", name = "Morning Workout", scheduledTime = LocalTime.of(8, 0), scheduledDay = today, category = TaskCategory.FITNESS, recurrenceType = RecurrenceType.DAILY)
        val task2 = TaskEntity(id = 2, description = "", name = "Read Book", scheduledTime = LocalTime.of(12, 0), scheduledDay = today, category = TaskCategory.LEARNING, recurrenceType = RecurrenceType.DAILY)
        val task3 = TaskEntity(id = 3, description = "", name = "Clean Desk", scheduledTime = LocalTime.of(18, 0), scheduledDay = today, category = TaskCategory.CHORES, recurrenceType = RecurrenceType.DAILY)

        taskRepo.tasksFlow.value = listOf(task1, task2, task3)
        completionRepo.logsFlow.value = listOf(
            TaskCompletionLogEntity(id = 10, taskId = 1, completedDate = today, wasCompleted = true, pointsAwarded = 0),
            TaskCompletionLogEntity(id = 11, taskId = 2, completedDate = today, wasCompleted = false, pointsAwarded = 0)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TodayUiState.Success)
        val successState = state as TodayUiState.Success

        assertEquals(3, successState.tasks.size)
        assertEquals(TaskItemStatus.DONE, successState.tasks.find { it.task.id == 1L }?.status)
        assertEquals(TaskItemStatus.MISSED, successState.tasks.find { it.task.id == 2L }?.status)
        assertEquals(TaskItemStatus.PENDING, successState.tasks.find { it.task.id == 3L }?.status)
        assertEquals(5, successState.currentStreak)
        assertEquals(500, successState.totalXp)
        assertEquals(3, successState.level)
    }

    @Test
    fun combineLogic_sortsPendingFirstByScheduledTime() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val today = LocalDate.now()
        val task1 = TaskEntity(id = 1, description = "", name = "Late Pending", scheduledTime = LocalTime.of(18, 0), scheduledDay = today, category = TaskCategory.FITNESS, recurrenceType = RecurrenceType.DAILY)
        val task2 = TaskEntity(id = 2, description = "", name = "Early Completed", scheduledTime = LocalTime.of(8, 0), scheduledDay = today, category = TaskCategory.LEARNING, recurrenceType = RecurrenceType.DAILY)
        val task3 = TaskEntity(id = 3, description = "", name = "Early Pending", scheduledTime = LocalTime.of(9, 0), scheduledDay = today, category = TaskCategory.CHORES, recurrenceType = RecurrenceType.DAILY)

        taskRepo.tasksFlow.value = listOf(task1, task2, task3)
        completionRepo.logsFlow.value = listOf(
            TaskCompletionLogEntity(id = 10, taskId = 2, completedDate = today, wasCompleted = true, pointsAwarded = 0)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as TodayUiState.Success
        assertEquals(3, state.tasks.size)
        assertEquals(3L, state.tasks[0].task.id) // Early Pending (9:00)
        assertEquals(1L, state.tasks[1].task.id) // Late Pending (18:00)
        assertEquals(2L, state.tasks[2].task.id) // Early Completed (8:00, deprioritized)
    }

    @Test
    fun completeTask_updatesCompletionPercentageAndIsPerfectDay() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val today = LocalDate.now()
        val task1 = TaskEntity(id = 1, description = "", name = "Task 1", scheduledTime = LocalTime.of(8, 0), scheduledDay = today, category = TaskCategory.FITNESS, recurrenceType = RecurrenceType.DAILY)
        val task2 = TaskEntity(id = 2, description = "", name = "Task 2", scheduledTime = LocalTime.of(12, 0), scheduledDay = today, category = TaskCategory.LEARNING, recurrenceType = RecurrenceType.DAILY)

        taskRepo.tasksFlow.value = listOf(task1, task2)
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value as TodayUiState.Success
        assertEquals(0f, state.completionPercentage, 0.001f)
        assertFalse(state.isPerfectDay)

        // Complete task1 (1 of 2 completed = 50%, below Medium 70% threshold)
        completionRepo.logsFlow.value = listOf(
            TaskCompletionLogEntity(id = 1, taskId = 1, completedDate = today, wasCompleted = true, pointsAwarded = 50)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value as TodayUiState.Success
        assertEquals(0.5f, state.completionPercentage, 0.001f)
        assertFalse(state.isPerfectDay)

        // Complete task2 (2 of 2 completed = 100%, >= 70% threshold)
        completionRepo.logsFlow.value = listOf(
            TaskCompletionLogEntity(id = 1, taskId = 1, completedDate = today, wasCompleted = true, pointsAwarded = 50),
            TaskCompletionLogEntity(id = 2, taskId = 2, completedDate = today, wasCompleted = true, pointsAwarded = 50)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value as TodayUiState.Success
        assertEquals(1.0f, state.completionPercentage, 0.001f)
        assertTrue(state.isPerfectDay)
    }
}
