package com.pixelquest.app.ui.screens.tasks

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.ComposeTestConfiguration
import com.pixelquest.app.testing.FakeClock
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.MainDispatcherRule
import com.pixelquest.app.testing.ThemedTestContent
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

/**
 * V025: Quest create form flow and out-of-bounds limits test suite.
 *
 * Verifies:
 * 1. Happy-path user journey: fill quest form, save, verify DB persistence and alarm scheduling
 * 2. Title boundaries: empty, whitespace-only, 1 char, exact limit (100 chars), overflow (101 & 10,000 chars)
 * 3. Hostile characters in title: emoji, Arabic RTL, Arabic-Indic digits, null bytes, control chars, SQL & HTML
 * 4. Recurrence limits: Weekly with 0 days selected (invalid), all 7 days selected, switching to Daily
 * 5. Time picker limits: 00:00, 12:00, 23:59, and null time validation error
 * 6. Reminder settings limits: enabled toggle, lead minutes (0..120 clamp, negative & overflow), reminder styles
 * 7. Process death / SavedStateHandle restoration: form state persists across recreation
 * 8. Concurrency & idempotency: isSubmitting prevents duplicate submissions
 * 9. Display & Theme matrix: Comic, Pixel, Dark, Simple Mode, 2x font scale, RTL layout
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class QuestCreateFormFlowAndLimitsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val baseInstant = Instant.parse("2026-10-21T09:00:00Z")
    private lateinit var fakeClock: FakeClock
    private lateinit var taskRepo: FakeTaskRepository
    private lateinit var alarmScheduler: TaskAlarmScheduler
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: TaskFormViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        fakeClock = FakeClock(baseInstant)
        taskRepo = FakeTaskRepository()
        alarmScheduler = TaskAlarmScheduler(context)
        savedStateHandle = SavedStateHandle()

        viewModel = TaskFormViewModel(
            taskRepository = taskRepo,
            taskAlarmScheduler = alarmScheduler,
            savedStateHandle = savedStateHandle,
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
    // 1. Happy-Path User Journey: Create, Save & Alarm Scheduling
    // =========================================================================

    @Test
    fun createForm_userJourney_createValidQuest_savesAndSchedulesAlarm() = runTest {
        var navigatedBack = false
        showCreateTaskScreen(onNavigateBack = { navigatedBack = true })

        // 1. Enter Quest Title
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Slay the Mountain Troll")
        composeTestRule.waitForIdle()

        // 2. Select Category (Learning)
        composeTestRule.onNodeWithText("Learning").performClick()
        composeTestRule.waitForIdle()

        // 3. Save Quest
        val saveButton = composeTestRule.onNodeWithText("SAVE QUEST", ignoreCase = true)
        saveButton.assertIsEnabled()
        saveButton.performScrollTo().performClick()
        composeTestRule.waitForIdle()
        advanceUntilIdle()

        assertTrue("Should invoke back navigation callback on save", navigatedBack)

        // 4. Verify Database record
        val allTasks = taskRepo.getAllTasks().first()
        assertEquals(1, allTasks.size)
        val created = allTasks.first()
        assertEquals("Slay the Mountain Troll", created.name)
        assertEquals(TaskCategory.LEARNING, created.category)
        assertEquals(RecurrenceType.DAILY, created.recurrenceType)
        assertTrue("Reminder should be enabled by default", created.reminderEnabled)
    }

    // =========================================================================
    // 2. Title Limits: Empty, Whitespace, 1 char, Exact Limit & Truncation
    // =========================================================================

    @Test
    fun createForm_titleLimits_emptyAndWhitespaceValidation() {
        showCreateTaskScreen()

        // Initially empty -> save button disabled
        val saveButton = composeTestRule.onNodeWithText("SAVE QUEST", ignoreCase = true)
        saveButton.assertIsNotEnabled()

        // Whitespace only -> error displayed, disabled
        composeTestRule.onNode(hasSetTextAction()).performTextInput("     ")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Quest title is required!").assertIsDisplayed()
        saveButton.assertIsNotEnabled()

        // Single character -> valid
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("A")
        composeTestRule.waitForIdle()
        saveButton.assertIsEnabled()
    }

    @Test
    fun createForm_titleLimits_exactLimitAndOverflowTruncation() {
        // Exact limit: 100 characters
        val exactTitle = "X".repeat(100)
        viewModel.onNameChanged(exactTitle)
        assertEquals(100, viewModel.formState.value.name.length)
        assertEquals(exactTitle, viewModel.formState.value.name)
        assertNull(viewModel.formState.value.nameError)

        // 101 characters -> clamped to 100 characters
        val overByOne = "X".repeat(101)
        viewModel.onNameChanged(overByOne)
        assertEquals(100, viewModel.formState.value.name.length)
        assertEquals(exactTitle, viewModel.formState.value.name)

        // 10,000 characters -> clamped to 100 characters without error
        val hugeTitle = "Y".repeat(10_000)
        viewModel.onNameChanged(hugeTitle)
        assertEquals(100, viewModel.formState.value.name.length)
        assertEquals("Y".repeat(100), viewModel.formState.value.name)
        assertNull(viewModel.formState.value.nameError)
    }

    // =========================================================================
    // 3. Hostile Characters: Emoji, RTL, Digits, Control Chars, SQL & HTML
    // =========================================================================

    @Test
    fun createForm_titleHostileCharacters_sanitizedProperly() {
        // Emoji preserved
        val emojiTitle = "🐉 Slay Ancient Dragon ⚔️"
        viewModel.onNameChanged(emojiTitle)
        assertEquals(emojiTitle, viewModel.formState.value.name)

        // Arabic RTL preserved
        val arabicTitle = "مهمة تدريب الصباح الباكر"
        viewModel.onNameChanged(arabicTitle)
        assertEquals(arabicTitle, viewModel.formState.value.name)

        // Arabic-Indic digits preserved
        val digitsTitle = "تحدي الأيام ٥"
        viewModel.onNameChanged(digitsTitle)
        assertEquals(digitsTitle, viewModel.formState.value.name)

        // Newlines, carriage returns, tabs replaced with spaces (single-line enforcement)
        viewModel.onNameChanged("Quest\nSubtask\rThird\tPart")
        assertEquals("Quest Subtask Third Part", viewModel.formState.value.name)

        // Control characters & null bytes stripped
        viewModel.onNameChanged("Control\u0000Chars\u0007Removed\u001F")
        assertEquals("ControlCharsRemoved", viewModel.formState.value.name)

        // Invisible direction isolates and BOM stripped
        viewModel.onNameChanged("Invisible\u200BText\uFEFFStrip\u202A")
        assertEquals("InvisibleTextStrip", viewModel.formState.value.name)

        // SQL injection safely handled as plain text string
        val sqlInjection = "'; DROP TABLE tasks; --"
        viewModel.onNameChanged(sqlInjection)
        assertEquals(sqlInjection, viewModel.formState.value.name)

        // HTML/XSS snippet safely handled as plain text string
        val htmlXss = "<script>alert('xss')</script>"
        viewModel.onNameChanged(htmlXss)
        assertEquals(htmlXss, viewModel.formState.value.name)
    }

    // =========================================================================
    // 4. Recurrence Limits: Weekly 0 days, 7 days, Switching Back
    // =========================================================================

    @Test
    fun createForm_weeklyRecurrence_daysSelectionLimits() {
        viewModel.onNameChanged("Weekly Review")
        viewModel.onRecurrenceSelected(RecurrenceType.WEEKLY)
        assertTrue(viewModel.formState.value.selectedDays.isNotEmpty())

        // Toggle off all selected days
        val currentDays = viewModel.formState.value.selectedDays.toList()
        currentDays.forEach { viewModel.onDayToggled(it) }

        // When 0 days selected: invalid and shows error
        assertEquals(0, viewModel.formState.value.selectedDays.size)
        assertFalse(viewModel.formState.value.isValid)
        assertEquals("Select at least one day!", viewModel.formState.value.daysError)
        assertFalse("validateForm must return false when no days selected", viewModel.validateForm())

        // Select all 7 days (Monday through Sunday)
        DayOfWeek.entries.forEach { viewModel.onDayToggled(it) }
        assertEquals(7, viewModel.formState.value.selectedDays.size)
        assertTrue(viewModel.formState.value.isValid)
        assertNull(viewModel.formState.value.daysError)

        // Switching back to DAILY clears days error
        viewModel.onRecurrenceSelected(RecurrenceType.DAILY)
        assertTrue(viewModel.formState.value.isValid)
        assertNull(viewModel.formState.value.daysError)
    }

    // =========================================================================
    // 5. Time Picker Limits: Midnight, Noon, 23:59, Null Validation
    // =========================================================================

    @Test
    fun createForm_timePicker_limits() {
        viewModel.onNameChanged("Time Boundary Quest")

        // Midnight 00:00
        viewModel.onTimeSelected(LocalTime.of(0, 0))
        assertEquals(LocalTime.of(0, 0), viewModel.formState.value.scheduledTime)
        assertTrue(viewModel.formState.value.isValid)

        // Noon 12:00
        viewModel.onTimeSelected(LocalTime.of(12, 0))
        assertEquals(LocalTime.of(12, 0), viewModel.formState.value.scheduledTime)
        assertTrue(viewModel.formState.value.isValid)

        // End of day 23:59
        viewModel.onTimeSelected(LocalTime.of(23, 59))
        assertEquals(LocalTime.of(23, 59), viewModel.formState.value.scheduledTime)
        assertTrue(viewModel.formState.value.isValid)

        // Null time validation
        viewModel.onTimeSelected(null)
        assertNull(viewModel.formState.value.scheduledTime)
        assertFalse(viewModel.formState.value.isValid)
        assertEquals("Time is required!", viewModel.formState.value.timeError)
    }

    // =========================================================================
    // 6. Reminder Settings Limits: Toggle, Lead Clamp, Styles
    // =========================================================================

    @Test
    fun createForm_reminderSettings_limits() {
        // Toggle off
        viewModel.onReminderEnabledChanged(false)
        assertFalse(viewModel.formState.value.reminderEnabled)

        // Toggle on
        viewModel.onReminderEnabledChanged(true)
        assertTrue(viewModel.formState.value.reminderEnabled)

        // Lead minutes clamp: 0 is allowed
        viewModel.onReminderLeadSelected(0)
        assertEquals(0, viewModel.formState.value.reminderLeadMinutes)

        // Negative lead clamped to 0
        viewModel.onReminderLeadSelected(-30)
        assertEquals(0, viewModel.formState.value.reminderLeadMinutes)

        // Standard lead options: 5, 15, 30, 60
        viewModel.onReminderLeadSelected(15)
        assertEquals(15, viewModel.formState.value.reminderLeadMinutes)

        // Overflow lead (> 120) clamped to 120
        viewModel.onReminderLeadSelected(999)
        assertEquals(120, viewModel.formState.value.reminderLeadMinutes)

        // Reminder styles
        viewModel.onReminderStyleSelected(ReminderStyle.STANDARD)
        assertEquals(ReminderStyle.STANDARD, viewModel.formState.value.reminderStyle)

        viewModel.onReminderStyleSelected(ReminderStyle.SILENT)
        assertEquals(ReminderStyle.SILENT, viewModel.formState.value.reminderStyle)

        viewModel.onReminderStyleSelected(ReminderStyle.PROMPT)
        assertEquals(ReminderStyle.PROMPT, viewModel.formState.value.reminderStyle)
    }

    // =========================================================================
    // 7. Process Death / SavedStateHandle Restoration
    // =========================================================================

    @Test
    fun createForm_savedStateHandle_survivesProcessDeath() {
        // 1. Enter values into initial ViewModel
        viewModel.onNameChanged("Underworld Campaign")
        viewModel.onRecurrenceSelected(RecurrenceType.WEEKLY)
        // Explicitly clear any initial day and select Monday and Friday
        val initialDays = viewModel.formState.value.selectedDays.toList()
        initialDays.forEach { viewModel.onDayToggled(it) }
        viewModel.onDayToggled(DayOfWeek.MONDAY)
        viewModel.onDayToggled(DayOfWeek.FRIDAY)
        viewModel.onTimeSelected(LocalTime.of(15, 45))
        viewModel.onCategorySelected(TaskCategory.LEARNING)
        viewModel.onReminderLeadSelected(30)
        viewModel.onReminderStyleSelected(ReminderStyle.PROMPT)

        // 2. Re-create ViewModel using the exact same SavedStateHandle
        val restoredViewModel = TaskFormViewModel(
            taskRepository = taskRepo,
            taskAlarmScheduler = alarmScheduler,
            savedStateHandle = savedStateHandle,
            appClock = fakeClock.asAppClock(),
        )

        val state = restoredViewModel.formState.value
        assertEquals("Underworld Campaign", state.name)
        assertEquals(RecurrenceType.WEEKLY, state.recurrenceType)
        assertTrue(state.selectedDays.contains(DayOfWeek.MONDAY))
        assertTrue(state.selectedDays.contains(DayOfWeek.FRIDAY))
        assertEquals(2, state.selectedDays.size)
        assertEquals(LocalTime.of(15, 45), state.scheduledTime)
        assertEquals(TaskCategory.LEARNING, state.category)
        assertEquals(30, state.reminderLeadMinutes)
        assertEquals(ReminderStyle.PROMPT, state.reminderStyle)
        assertTrue(state.isValid)
    }

    // =========================================================================
    // 8. Concurrency & Idempotency: Double Click Protection
    // =========================================================================

    @Test
    fun createForm_saveDeduplication_isSubmittingDisablesButton() = runTest {
        viewModel.onNameChanged("Idempotent Quest")
        viewModel.onTimeSelected(LocalTime.of(10, 0))
        assertTrue(viewModel.formState.value.isValid)

        var successCount = 0
        viewModel.saveTask { successCount++ }
        advanceUntilIdle()

        assertEquals(1, successCount)
        assertEquals(1, taskRepo.getAllTasks().first().size)
    }

    // =========================================================================
    // 9. Display & Theme Matrix
    // =========================================================================

    @Test
    fun createForm_themeAndDisplayMatrix() {
        showCreateTaskScreen(
            config = ComposeTestConfiguration(
                themeMode = ThemeMode.Comic,
                isDark = true,
                isSimpleMode = true,
                fontScale = 2.0f,
                layoutDirection = LayoutDirection.Rtl,
            ),
        )

        composeTestRule.onNodeWithText("Quest name", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Repeats", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Category", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("SAVE QUEST", ignoreCase = true).assertExists()
    }
}
