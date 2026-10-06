package com.pixelquest.app.ui.today

import androidx.test.core.app.ApplicationProvider
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
import kotlinx.coroutines.launch
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
// A plain Application: the real one builds the real database, whose background seeding outlived the test
@org.robolectric.annotation.Config(sdk = [34], application = android.app.Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TodayGracePeriodTest {

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
    fun noTasksToday_returnsEmptyTaskListInState() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        taskRepo.tasksFlow.value = emptyList()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as TodayUiState.Success
        assertTrue(state.tasks.isEmpty())
        assertEquals(0f, state.completionPercentage, 0.001f)
    }

    @Test
    fun overdueTaskWithoutLog_mapsToGracePeriodStatus() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val today = LocalDate.now()
        val pastTask = TaskEntity(
            id = 1,
            name = "Morning Quest",
            description = "",
            scheduledTime = LocalTime.of(0, 1), // 00:01 AM (past)
            scheduledDay = today,
            category = TaskCategory.FITNESS,
            recurrenceType = RecurrenceType.DAILY
        )

        taskRepo.tasksFlow.value = listOf(pastTask)
        completionRepo.logsFlow.value = emptyList()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as TodayUiState.Success
        assertEquals(1, state.tasks.size)
        assertEquals(TaskItemStatus.GRACE_PERIOD, state.tasks[0].status)
    }
}
