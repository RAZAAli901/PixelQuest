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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class FakeTaskRepository : TaskRepository {
    val tasksFlow = MutableStateFlow<List<TaskEntity>>(emptyList())
    override fun getAllTasks(): Flow<List<TaskEntity>> = tasksFlow
    override fun getTaskById(id: Long): Flow<TaskEntity?> = MutableStateFlow(null)
    override fun getTasksForDay(day: LocalDate): Flow<List<TaskEntity>> = tasksFlow
    override suspend fun insertTask(task: TaskEntity): Long = 1L
    override suspend fun updateTask(task: TaskEntity) {}
    override suspend fun deleteTask(task: TaskEntity) {}
}

class FakeTaskCompletionRepository : TaskCompletionRepository {
    val logsFlow = MutableStateFlow<List<TaskCompletionLogEntity>>(emptyList())
    override suspend fun insertLog(log: TaskCompletionLogEntity): Long = 1L
    override fun getLogsForDate(date: LocalDate): Flow<List<TaskCompletionLogEntity>> = logsFlow
    override fun getLogsForTask(taskId: Long): Flow<List<TaskCompletionLogEntity>> = logsFlow
    override fun getCompletionHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskCompletionLogEntity>> = logsFlow
}

class FakeStreakRepository : StreakRepository {
    val streakFlow = MutableStateFlow<StreakEntity?>(StreakEntity(id = 1, currentStreak = 5, longestStreak = 10))
    override fun getCurrentStreak(): Flow<StreakEntity?> = streakFlow
    override suspend fun insertStreak(streak: StreakEntity) {}
    override suspend fun updateStreak(streak: StreakEntity) {}
}

class FakeUserProfileRepository : UserProfileRepository {
    val profileFlow = MutableStateFlow<UserProfileEntity?>(
        UserProfileEntity(id = 1, username = "Hero", totalXp = 500, level = 3, perfectDaysTowardNextLevel = 2)
    )
    override fun getProfile(): Flow<UserProfileEntity?> = profileFlow
    override suspend fun insertProfile(profile: UserProfileEntity) {}
    override suspend fun updateProfile(profile: UserProfileEntity) {}
    override suspend fun performLevelUp(): UserProfileEntity? = null
}

class FakeDifficultyRepo : DifficultySettingsRepository {
    val difficultyFlow = MutableStateFlow<DifficultySettingsEntity?>(
        DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, perfectDayThreshold = 0.7f, daysRequiredPerLevel = 7)
    )
    override fun getCurrentDifficulty(): Flow<DifficultySettingsEntity?> = difficultyFlow
    override suspend fun insertSettings(settings: DifficultySettingsEntity) {}
    override suspend fun updateSettings(settings: DifficultySettingsEntity) {}
}

import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
            taskAlarmScheduler = alarmScheduler
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun combineLogic_mapsTaskStatusCorrectly() = runTest {
        val today = LocalDate.now()
        val task1 = TaskEntity(id = 1, name = "Morning Workout", scheduledTime = LocalTime.of(8, 0), scheduledDay = today, category = TaskCategory.FITNESS, recurrenceType = RecurrenceType.DAILY)
        val task2 = TaskEntity(id = 2, name = "Read Book", scheduledTime = LocalTime.of(12, 0), scheduledDay = today, category = TaskCategory.LEARNING, recurrenceType = RecurrenceType.DAILY)
        val task3 = TaskEntity(id = 3, name = "Clean Desk", scheduledTime = LocalTime.of(18, 0), scheduledDay = today, category = TaskCategory.CHORES, recurrenceType = RecurrenceType.DAILY)

        taskRepo.tasksFlow.value = listOf(task1, task2, task3)
        completionRepo.logsFlow.value = listOf(
            TaskCompletionLogEntity(id = 10, taskId = 1, completionDate = today, wasCompleted = true),
            TaskCompletionLogEntity(id = 11, taskId = 2, completionDate = today, wasCompleted = false)
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
        val today = LocalDate.now()
        val task1 = TaskEntity(id = 1, name = "Late Pending", scheduledTime = LocalTime.of(18, 0), scheduledDay = today, category = TaskCategory.FITNESS, recurrenceType = RecurrenceType.DAILY)
        val task2 = TaskEntity(id = 2, name = "Early Completed", scheduledTime = LocalTime.of(8, 0), scheduledDay = today, category = TaskCategory.LEARNING, recurrenceType = RecurrenceType.DAILY)
        val task3 = TaskEntity(id = 3, name = "Early Pending", scheduledTime = LocalTime.of(9, 0), scheduledDay = today, category = TaskCategory.CHORES, recurrenceType = RecurrenceType.DAILY)

        taskRepo.tasksFlow.value = listOf(task1, task2, task3)
        completionRepo.logsFlow.value = listOf(
            TaskCompletionLogEntity(id = 10, taskId = 2, completionDate = today, wasCompleted = true)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as TodayUiState.Success
        assertEquals(3, state.tasks.size)
        assertEquals(3L, state.tasks[0].task.id) // Early Pending (9:00)
        assertEquals(1L, state.tasks[1].task.id) // Late Pending (18:00)
        assertEquals(2L, state.tasks[2].task.id) // Early Completed (8:00, deprioritized)
    }
}
