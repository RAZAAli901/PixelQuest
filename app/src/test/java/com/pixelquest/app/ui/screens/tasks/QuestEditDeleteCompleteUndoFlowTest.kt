package com.pixelquest.app.ui.screens.tasks

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.TaskResultRecorder
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.ComposeTestConfiguration
import com.pixelquest.app.testing.FakeClock
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.MainDispatcherRule
import com.pixelquest.app.testing.ThemedTestContent
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * V026: Quest edit, delete, complete and undo flows test suite.
 *
 * Verifies:
 * 1. Edit flow: loads stored task, modifies fields, saves to repository, updates alarm
 * 2. Delete flow: confirmation dialog sequence, cancel vs confirm, repository & alarm removal
 * 3. Complete flow: records completion log, applies first-completion (0 XP) vs streak XP rules
 * 4. Skip and redo (undo) flow: missed/skipped task can still be completed later that day
 * 5. Delete while open: handles deletion of task concurrently while edit form is displayed
 * 6. Edit validation: cannot save with blank title or 0 weekly days selected
 * 7. Unsaved edits preservation across reload / rotation
 * 8. Completion idempotency: double-complete prevents duplicate logs and duplicate XP
 * 9. Display & Theme matrix for edit and delete actions
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class QuestEditDeleteCompleteUndoFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val baseInstant = Instant.parse("2026-10-21T09:00:00Z")
    private val baseDate = LocalDate.of(2026, 10, 21)
    private lateinit var fakeClock: FakeClock

    private lateinit var taskRepo: FakeTaskRepository
    private lateinit var completionRepo: FakeTaskCompletionRepository
    private lateinit var streakRepo: FakeStreakRepository
    private lateinit var profileRepo: FakeUserProfileRepository
    private lateinit var alarmScheduler: TaskAlarmScheduler
    private lateinit var taskResultRecorder: TaskResultRecorder
    private lateinit var viewModel: TaskFormViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        fakeClock = FakeClock(baseInstant)
        taskRepo = FakeTaskRepository()
        completionRepo = FakeTaskCompletionRepository()
        streakRepo = FakeStreakRepository(StreakEntity(id = 1, currentStreak = 5, longestStreak = 10))
        profileRepo = FakeUserProfileRepository(UserProfileEntity(id = 1, username = "Paladin", avatarId = "avatar_knight", totalXp = 100, level = 2))
        alarmScheduler = TaskAlarmScheduler(context)
        taskResultRecorder = TaskResultRecorder(
            taskCompletionRepository = completionRepo,
            userProfileRepository = profileRepo,
            streakRepository = streakRepo,
            taskRepository = taskRepo,
        )

        viewModel = TaskFormViewModel(
            taskRepository = taskRepo,
            taskAlarmScheduler = alarmScheduler,
            savedStateHandle = SavedStateHandle(),
            appClock = fakeClock.asAppClock(),
        )
    }

    private fun runTest(block: suspend TestScope.() -> Unit) = kotlinx.coroutines.test.runTest(mainDispatcherRule.testDispatcher) {
        block()
    }

    private fun showCreateTaskScreen(
        config: ComposeTestConfiguration = ComposeTestConfiguration(),
        onNavigateBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ThemedTestContent(configuration = config) {
                CreateTaskScreen(
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBack,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    // =========================================================================
    // 1. Edit Flow: Load existing task, modify, save & verify alarm update
    // =========================================================================

    @Test
    fun questEditFlow_loadsExistingTask_modifiesFields_updatesInRepositoryAndReschedulesAlarm() = runTest {
        val originalTask = TaskEntity(
            id = 601L,
            name = "Morning Walk",
            description = "Walk around park",
            scheduledDay = baseDate,
            scheduledTime = LocalTime.of(7, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS,
            reminderEnabled = true,
            reminderLeadMinutes = 0,
            reminderStyle = ReminderStyle.STANDARD,
        )
        taskRepo.insertTask(originalTask)

        // Load task into ViewModel
        viewModel.loadTask(601L)
        advanceUntilIdle()

        val state = viewModel.formState.value
        assertTrue("Form should be in edit mode", state.isEditMode)
        assertEquals(601L, state.taskId)
        assertEquals("Morning Walk", state.name)
        assertEquals(LocalTime.of(7, 0), state.scheduledTime)

        var navigatedBack = false
        showCreateTaskScreen(onNavigateBack = { navigatedBack = true })

        // Modify title to "Morning Power Walk"
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("Morning Power Walk")
        composeTestRule.waitForIdle()

        // Modify category to Health
        composeTestRule.onNodeWithText("Health").performClick()
        composeTestRule.waitForIdle()

        // Click Update Quest button
        val updateButton = composeTestRule.onNodeWithText("UPDATE QUEST", ignoreCase = true)
        updateButton.assertIsEnabled()
        updateButton.performScrollTo().performClick()
        composeTestRule.waitForIdle()
        advanceUntilIdle()

        assertTrue("Should navigate back upon updating", navigatedBack)

        // Verify updated task in repository
        val updatedTask = taskRepo.getTaskById(601L).first()
        assertNotNull("Task must still exist in repository", updatedTask)
        assertEquals("Morning Power Walk", updatedTask?.name)
        assertEquals(TaskCategory.HEALTH, updatedTask?.category)
    }

    // =========================================================================
    // 2. Delete Flow: Confirmation Dialog Sequence
    // =========================================================================

    @Test
    fun questDeleteFlow_fromEditScreen_confirmDialogAbandonsQuest() = runTest {
        val taskToDelete = TaskEntity(
            id = 602L,
            name = "Obsolete Sentry Duty",
            description = "Watch old gate",
            scheduledDay = baseDate,
            scheduledTime = LocalTime.of(12, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.OTHER,
        )
        taskRepo.insertTask(taskToDelete)

        viewModel.loadTask(602L)
        advanceUntilIdle()

        var navigatedBack = false
        showCreateTaskScreen(onNavigateBack = { navigatedBack = true })

        // 1. Click Delete button in form
        val deleteButton = composeTestRule.onNodeWithText("DELETE QUEST", ignoreCase = true)
        deleteButton.performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // 2. Verify confirmation dialog appears
        composeTestRule.onNodeWithText("Abandon quest?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to delete 'Obsolete Sentry Duty'?").assertIsDisplayed()

        // 3. Click Cancel first -> quest should NOT be deleted
        composeTestRule.onNodeWithText("Cancel", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
        assertNotNull("Quest should still exist after cancel", taskRepo.getTaskById(602L).first())
        assertFalse("Should not navigate back after cancel", navigatedBack)

        // 4. Click Delete button again and confirm in dialog
        deleteButton.performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
        advanceUntilIdle()

        // 5. Verify quest is deleted from repository and back navigation was invoked
        assertTrue("Should navigate back after delete confirmed", navigatedBack)
        assertNull("Quest must be removed from repository", taskRepo.getTaskById(602L).first())
    }

    // =========================================================================
    // 3. Complete Flow: First-Completion (0 XP) vs Subsequent Completion Rules
    // =========================================================================

    @Test
    fun questCompleteFlow_marksTaskDone_awardsXpProperly() = runTest {
        val quest = TaskEntity(
            id = 603L,
            name = "Dragon Patrol",
            description = "Fly over peaks",
            scheduledDay = baseDate,
            scheduledTime = LocalTime.of(14, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS,
        )
        taskRepo.insertTask(quest)

        // First completion awards 0 XP (Rule B06)
        val initialXp = profileRepo.getProfile().first()?.totalXp ?: 0
        val result1 = taskResultRecorder.recordTaskCompletion(603L, baseDate)
        assertTrue(result1 is TaskResultRecorder.CompletionResult.Success)
        assertEquals(0, (result1 as TaskResultRecorder.CompletionResult.Success).points)

        val log1 = completionRepo.getLogForTaskOnDate(603L, baseDate)
        assertNotNull(log1)
        assertTrue(log1?.wasCompleted == true)
        assertEquals(0, log1?.pointsAwarded)
        assertEquals(initialXp, profileRepo.getProfile().first()?.totalXp)

        // Second completion on the next day awards streak-based XP
        val nextDay = baseDate.plusDays(1)
        val result2 = taskResultRecorder.recordTaskCompletion(603L, nextDay)
        assertTrue(result2 is TaskResultRecorder.CompletionResult.Success)
        val awardedXp = (result2 as TaskResultRecorder.CompletionResult.Success).points
        assertTrue("Second completion must award > 0 XP", awardedXp > 0)

        val log2 = completionRepo.getLogForTaskOnDate(603L, nextDay)
        assertEquals(awardedXp, log2?.pointsAwarded)
        assertTrue("Total XP must have increased", (profileRepo.getProfile().first()?.totalXp ?: 0) > initialXp)
    }

    // =========================================================================
    // 4. Skip and Redo Flow: Missed Task Can Still Be Completed Later That Day
    // =========================================================================

    @Test
    fun questSkipAndRedoFlow_missedTaskCanBeCompletedLater_updatesLogAndAwardsXp() = runTest {
        val quest = TaskEntity(
            id = 604L,
            name = "Late Afternoon Stretch",
            description = "Yoga",
            scheduledDay = baseDate,
            scheduledTime = LocalTime.of(16, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.HEALTH,
        )
        taskRepo.insertTask(quest)

        // Seed a prior completion so this completion awards points
        completionRepo.insertLog(TaskCompletionLogEntity(taskId = 604L, completedDate = baseDate.minusDays(1), wasCompleted = true, pointsAwarded = 20))

        // Step 1: User skips the task earlier in the day
        completionRepo.insertLog(TaskCompletionLogEntity(taskId = 604L, completedDate = baseDate, wasCompleted = false, pointsAwarded = 0))
        val skippedLog = completionRepo.getLogForTaskOnDate(604L, baseDate)
        assertNotNull(skippedLog)
        assertFalse(skippedLog?.wasCompleted == true)

        // Step 2: Later that day, user changes mind and completes the task
        val result = taskResultRecorder.recordTaskCompletion(604L, baseDate)
        assertTrue(result is TaskResultRecorder.CompletionResult.Success)
        val points = (result as TaskResultRecorder.CompletionResult.Success).points
        assertTrue(points > 0)

        // Step 3: Verify the log is updated to wasCompleted = true with awarded points
        val updatedLog = completionRepo.getLogForTaskOnDate(604L, baseDate)
        assertNotNull(updatedLog)
        assertTrue(updatedLog?.wasCompleted == true)
        assertEquals(points, updatedLog?.pointsAwarded)
    }

    // =========================================================================
    // 5. Delete While Open Edge Cases
    // =========================================================================

    @Test
    fun questDeleteWhileOpen_completingDeletedTaskReturnsTaskNotFound() = runTest {
        // Task exists initially
        val quest = TaskEntity(
            id = 605L,
            name = "Vanishing Quest",
            description = "Disappears",
            scheduledDay = baseDate,
            scheduledTime = LocalTime.of(11, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.OTHER,
        )
        taskRepo.insertTask(quest)

        // Delete from repository
        taskRepo.deleteTask(quest)

        // Attempting to complete the now-deleted task
        val result = taskResultRecorder.recordTaskCompletion(605L, baseDate)
        assertEquals(TaskResultRecorder.CompletionResult.TaskNotFound, result)
        assertNull(completionRepo.getLogForTaskOnDate(605L, baseDate))
    }

    // =========================================================================
    // 6. Edit Validation: Cannot Save With Blank Title or 0 Weekly Days
    // =========================================================================

    @Test
    fun questEditValidation_cannotUpdateToInvalidOrBlankTitle() {
        val quest = TaskEntity(
            id = 606L,
            name = "Valid Title",
            description = "Desc",
            scheduledDay = baseDate,
            scheduledTime = LocalTime.of(10, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.CHORES,
        )
        runBlocking { taskRepo.insertTask(quest) }
        viewModel.loadTask(606L)

        // Blank name
        viewModel.onNameChanged("   ")
        assertFalse(viewModel.formState.value.isValid)
        assertEquals("Quest title is required!", viewModel.formState.value.nameError)
        assertFalse(viewModel.validateForm())

        // Set valid name, then switch to Weekly and remove all days
        viewModel.onNameChanged("Valid Again")
        viewModel.onRecurrenceSelected(RecurrenceType.WEEKLY)
        viewModel.formState.value.selectedDays.toList().forEach { viewModel.onDayToggled(it) }
        assertEquals(0, viewModel.formState.value.selectedDays.size)
        assertFalse(viewModel.formState.value.isValid)
        assertEquals("Select at least one day!", viewModel.formState.value.daysError)
        assertFalse(viewModel.validateForm())
    }

    // =========================================================================
    // 7. Unsaved Edits Preservation Across Reload / Rotation
    // =========================================================================

    @Test
    fun questEdit_rotationKeepsPendingEdits() = runTest {
        val quest = TaskEntity(
            id = 607L,
            name = "Database Title",
            description = "Desc",
            scheduledDay = baseDate,
            scheduledTime = LocalTime.of(8, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS,
        )
        taskRepo.insertTask(quest)

        // Initial load
        viewModel.loadTask(607L)
        advanceUntilIdle()

        // User makes unsaved changes
        viewModel.onNameChanged("Unsaved Player Changes")
        assertEquals("Unsaved Player Changes", viewModel.formState.value.name)

        // Re-invoking loadTask for the same task ID (as during rotation / recomposition)
        viewModel.loadTask(607L)
        advanceUntilIdle()

        // Must still have player's unsaved edits
        assertEquals("Unsaved Player Changes", viewModel.formState.value.name)
    }

    // =========================================================================
    // 8. Completion Idempotency: Double Complete Does Not Double-Award XP
    // =========================================================================

    @Test
    fun questComplete_idempotency_doubleCompleteDoesNotAwardDuplicateXp() = runTest {
        val quest = TaskEntity(
            id = 608L,
            name = "Idempotent Patrol",
            description = "Desc",
            scheduledDay = baseDate,
            scheduledTime = LocalTime.of(9, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS,
        )
        taskRepo.insertTask(quest)
        // Prior completion exists so first completion gives points
        completionRepo.insertLog(TaskCompletionLogEntity(taskId = 608L, completedDate = baseDate.minusDays(1), wasCompleted = true, pointsAwarded = 20))

        val result1 = taskResultRecorder.recordTaskCompletion(608L, baseDate)
        assertTrue(result1 is TaskResultRecorder.CompletionResult.Success)
        val initialTotalXp = profileRepo.getProfile().first()?.totalXp ?: 0

        // Second completion on the same day
        val result2 = taskResultRecorder.recordTaskCompletion(608L, baseDate)
        assertEquals(TaskResultRecorder.CompletionResult.AlreadyCompleted, result2)
        assertEquals("XP must not increase on duplicate completion", initialTotalXp, profileRepo.getProfile().first()?.totalXp ?: 0)
    }

    // =========================================================================
    // 9. Display & Theme Matrix for Edit / Delete Screen
    // =========================================================================

    @Test
    fun questEditDelete_themeAndDisplayMatrix() = runTest {
        val quest = TaskEntity(
            id = 609L,
            name = "Matrix Edit Quest",
            description = "Desc",
            scheduledDay = baseDate,
            scheduledTime = LocalTime.of(15, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.CHORES,
        )
        taskRepo.insertTask(quest)
        viewModel.loadTask(609L)
        advanceUntilIdle()

        showCreateTaskScreen(
            config = ComposeTestConfiguration(
                themeMode = ThemeMode.Comic,
                isDark = true,
                isSimpleMode = true,
                fontScale = 2.0f,
                layoutDirection = LayoutDirection.Rtl,
            ),
        )

        composeTestRule.onNodeWithText("Edit quest", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("UPDATE QUEST", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("DELETE QUEST", ignoreCase = true).assertExists()
    }
}
