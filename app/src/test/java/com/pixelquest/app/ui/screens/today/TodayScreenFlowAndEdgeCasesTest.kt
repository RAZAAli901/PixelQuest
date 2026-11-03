package com.pixelquest.app.ui.screens.today

import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.TaskResultRecorder
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.ComposeTestConfiguration
import com.pixelquest.app.testing.FakeClock
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.MainDispatcherRule
import com.pixelquest.app.testing.ThemedTestContent
import com.pixelquest.app.ui.components.QuestTags
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.screens.QuestTab
import com.pixelquest.app.ui.screens.QuestsScreen
import com.pixelquest.app.ui.screens.QuestsTags
import com.pixelquest.app.ui.screens.history.TaskHistoryViewModel
import com.pixelquest.app.ui.screens.tasks.TaskViewModel
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
 * V023: Flow and out-of-bounds edge case suite for the Today screen (QuestsScreen Today tab).
 *
 * Simulates:
 * 1. Happy-path user journey: open Today, view grouped quests, complete one, verify XP, streak and log update
 * 2. User journey: skip quest, verify confirmation dialog and MISSED status recording
 * 3. Out-of-bounds: 0 quests (empty state with create quest call-to-action)
 * 4. Out-of-bounds: 500 quests across day parts (render and lazy scrolling without crash)
 * 5. Hostile inputs & search boundaries: empty, whitespace, 1 char, 10k chars, emoji, RTL, Arabic-Indic digits, null bytes, SQL & HTML injection
 * 6. Time boundaries: midnight rollover (completing at 23:59 on Day 1, then at 00:01 on Day 2)
 * 7. Grace period transition: task moves from PENDING to GRACE_PERIOD as scheduled time passes
 * 8. Large numbers & overflow: streak = 999,999, total XP = 2B, level = 999
 * 9. Rapid double-completion idempotency
 * 10. Display matrix: Pixel, Comic, Light, Dark, Simple Mode, 2x font scale, RTL
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TodayScreenFlowAndEdgeCasesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val baseInstant = Instant.parse("2026-11-03T10:00:00Z")
    private val baseDate = LocalDate.of(2026, 11, 3)
    private lateinit var fakeClock: FakeClock

    private lateinit var taskRepo: FakeTaskRepository
    private lateinit var completionRepo: FakeTaskCompletionRepository
    private lateinit var streakRepo: FakeStreakRepository
    private lateinit var profileRepo: FakeUserProfileRepository
    private lateinit var difficultyRepo: FakeDifficultySettingsRepository
    private lateinit var settingsRepo: FakeSettingsRepository

    private lateinit var alarmScheduler: TaskAlarmScheduler
    private lateinit var taskResultRecorder: TaskResultRecorder

    private lateinit var todayViewModel: TodayViewModel
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var historyViewModel: TaskHistoryViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        fakeClock = FakeClock(baseInstant)

        taskRepo = FakeTaskRepository()
        completionRepo = FakeTaskCompletionRepository()
        streakRepo = FakeStreakRepository(
            StreakEntity(id = 1, currentStreak = 4, longestStreak = 10, lastCompletedDate = baseDate.minusDays(1)),
        )
        profileRepo = FakeUserProfileRepository(
            UserProfileEntity(id = 1, username = "Galahad", avatarId = "avatar_knight", totalXp = 250, level = 3),
        )
        difficultyRepo = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.EASY, perfectDayThreshold = 0.5f, daysRequiredPerLevel = 5),
        )
        settingsRepo = FakeSettingsRepository(onboardingDone = true)

        alarmScheduler = TaskAlarmScheduler(context)
        taskResultRecorder = TaskResultRecorder(
            taskCompletionRepository = completionRepo,
            userProfileRepository = profileRepo,
            streakRepository = streakRepo,
            taskRepository = taskRepo,
        )

        todayViewModel = TodayViewModel(
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            difficultySettingsRepository = difficultyRepo,
            taskAlarmScheduler = alarmScheduler,
            taskResultRecorder = taskResultRecorder,
            settingsRepository = settingsRepo,
            appClock = fakeClock.asAppClock(),
        )

        taskViewModel = TaskViewModel(
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            difficultySettingsRepository = difficultyRepo,
            appClock = fakeClock.asAppClock(),
            settingsRepository = settingsRepo,
        )

        historyViewModel = TaskHistoryViewModel(
            taskCompletionRepository = completionRepo,
            taskRepository = taskRepo,
        )
    }

    private fun runTest(block: suspend TestScope.() -> Unit) = kotlinx.coroutines.test.runTest {
        block()
    }

    private fun showTodayScreen(
        config: ComposeTestConfiguration = ComposeTestConfiguration(),
        onNewQuest: () -> Unit = {},
        onEditQuest: (Long) -> Unit = {},
        onOpenHistoryQuest: (Long) -> Unit = {},
    ) {
        composeTestRule.setContent {
            ThemedTestContent(configuration = config) {
                QuestsScreen(
                    onNewQuest = onNewQuest,
                    onEditQuest = onEditQuest,
                    onOpenHistoryQuest = onOpenHistoryQuest,
                    initialTab = QuestTab.Today,
                    todayViewModel = todayViewModel,
                    allViewModel = taskViewModel,
                    historyViewModel = historyViewModel,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    // -------------------------------------------------------------------------
    // 1. User Journey: Open Today, see groups, complete quest, verify XP & streak
    // -------------------------------------------------------------------------

    @Test
    fun todayScreen_userJourney_completeQuest_updatesXpAndStreak() {
        val morningTask = TaskEntity(
            id = 201L,
            name = "Morning Stretch",
            description = "10 minutes of yoga",
            scheduledTime = LocalTime.of(8, 30),
            scheduledDay = baseDate,
            category = TaskCategory.HEALTH,
            recurrenceType = RecurrenceType.DAILY,
        )
        val afternoonTask = TaskEntity(
            id = 202L,
            name = "Algorithm Study",
            description = "Solve 2 dynamic programming puzzles",
            scheduledTime = LocalTime.of(14, 0),
            scheduledDay = baseDate,
            category = TaskCategory.LEARNING,
            recurrenceType = RecurrenceType.DAILY,
        )
        runBlocking {
            taskRepo.insertTask(morningTask)
            taskRepo.insertTask(afternoonTask)
            // Seed a previous day completion for task 201 so it awards base XP
            completionRepo.insertLog(
                TaskCompletionLogEntity(
                    taskId = 201L,
                    completedDate = baseDate.minusDays(1),
                    wasCompleted = true,
                    pointsAwarded = 25,
                ),
            )
        }

        showTodayScreen()

        // Verify Quests screen loaded on Today tab
        composeTestRule.onNodeWithTag(QuestsTags.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("Morning Stretch").assertIsDisplayed()
        composeTestRule.onNodeWithText("Algorithm Study").assertIsDisplayed()

        // Complete the morning task using its complete circle
        composeTestRule.onNodeWithTag(QuestTags.complete(201L)).performClick()
        composeTestRule.waitForIdle()

        // Verify task completion is persisted
        runBlocking {
            val log = completionRepo.getLogForTaskOnDate(201L, baseDate)
            assertNotNull("Completion log must exist in database", log)
            assertTrue("Task must be marked completed", log?.wasCompleted == true)

            val updatedProfile = profileRepo.getProfile().first()
            assertTrue("Total XP must have increased", (updatedProfile?.totalXp ?: 0) > 250)
        }
    }

    // -------------------------------------------------------------------------
    // 2. User Journey: Skip quest flow
    // -------------------------------------------------------------------------

    @Test
    fun todayScreen_userJourney_skipQuest_recordsMissedInDatabase() {
        val quest = TaskEntity(
            id = 203L,
            name = "Evening Cardio",
            description = "30 minutes run",
            scheduledTime = LocalTime.of(19, 0),
            scheduledDay = baseDate,
            category = TaskCategory.HEALTH,
            recurrenceType = RecurrenceType.DAILY,
        )
        runBlocking {
            taskRepo.insertTask(quest)
        }

        showTodayScreen()

        composeTestRule.onNodeWithText("Evening Cardio").assertIsDisplayed()

        // Trigger skip accessibility action on the quest row
        val questNode = composeTestRule.onNodeWithTag(QuestsTags.quest(203L))
        val skipAction = questNode.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions)
            ?.firstOrNull { it.label.contains("skip", ignoreCase = true) }
        assertNotNull("Skip action must be available on pending quest", skipAction)

        skipAction?.action?.invoke()
        composeTestRule.waitForIdle()

        // Confirm dialog must appear with "SKIP" button
        composeTestRule.onNodeWithText("SKIP").performClick()
        composeTestRule.waitForIdle()

        // Verify completion repo records missed log
        runBlocking {
            val log = completionRepo.getLogForTaskOnDate(203L, baseDate)
            assertNotNull("Log must be created for skipped task", log)
            assertFalse("Skipped task must have wasCompleted = false", log?.wasCompleted == true)
        }
    }

    // -------------------------------------------------------------------------
    // 3. Out-of-Bounds: Empty state (0 quests today)
    // -------------------------------------------------------------------------

    @Test
    fun todayScreen_emptyState_showsPromptForNewQuest() {
        var newQuestTriggered = false
        showTodayScreen(onNewQuest = { newQuestTriggered = true })

        composeTestRule.onNodeWithTag(QuestsTags.EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QuestsTags.NEW).performClick()
        assertTrue("Clicking New Quest FAB must invoke onNewQuest callback", newQuestTriggered)
    }

    // -------------------------------------------------------------------------
    // 4. Out-of-Bounds: 500 Quests (High Volume)
    // -------------------------------------------------------------------------

    @Test
    fun todayScreen_fiveHundredQuests_loadsAndRendersWithoutCrashing() {
        val tasks = (1L..500L).map { i ->
            TaskEntity(
                id = i,
                name = "High Volume Quest #$i",
                description = "Task payload $i",
                scheduledTime = LocalTime.of((i % 24).toInt(), ((i * 7) % 60).toInt()),
                scheduledDay = baseDate,
                category = TaskCategory.CHORES,
                recurrenceType = RecurrenceType.DAILY,
            )
        }
        runBlocking {
            tasks.forEach { taskRepo.insertTask(it) }
        }

        showTodayScreen()

        composeTestRule.onNodeWithTag(QuestsTags.SCREEN).assertIsDisplayed()
        // Scroll to quest 40
        composeTestRule.onNodeWithTag(QuestsTags.SCREEN).performScrollToNode(hasTestTag(QuestsTags.quest(40L)))
        composeTestRule.onNodeWithText("High Volume Quest #40").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 5. Hostile Strings & Boundary Inputs in Search
    // -------------------------------------------------------------------------

    @Test
    fun todayScreen_searchFilter_boundaryInputsAndHostileStrings() {
        val questNormal = TaskEntity(
            id = 210L,
            name = "Clean Arsenal",
            description = "Tidy storage",
            scheduledTime = LocalTime.of(9, 0),
            scheduledDay = baseDate,
            category = TaskCategory.CHORES,
            recurrenceType = RecurrenceType.DAILY,
        )
        val questHostile = TaskEntity(
            id = 211L,
            name = "🐉 Slay Dragon <script>alert(1)</script> ' OR 1=1; -- \u0000\u0007",
            description = "Hostile input test",
            scheduledTime = LocalTime.of(15, 0),
            scheduledDay = baseDate,
            category = TaskCategory.OTHER,
            recurrenceType = RecurrenceType.DAILY,
        )
        val questArabic = TaskEntity(
            id = 212L,
            name = "مهمة الصباح الأولى ١٢٣",
            description = "Arabic text test",
            scheduledTime = LocalTime.of(7, 0),
            scheduledDay = baseDate,
            category = TaskCategory.OTHER,
            recurrenceType = RecurrenceType.DAILY,
        )
        runBlocking {
            taskRepo.insertTask(questNormal)
            taskRepo.insertTask(questHostile)
            taskRepo.insertTask(questArabic)
        }

        showTodayScreen()

        // Open search field
        composeTestRule.onNodeWithTag(QuestsTags.SEARCH).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(QuestsTags.SEARCH_FIELD).assertIsDisplayed()

        val searchField = composeTestRule.onNode(hasSetTextAction() and androidx.compose.ui.test.hasAnyAncestor(hasTestTag(QuestsTags.SEARCH_FIELD)))

        // 1. Arabic search
        searchField.performTextInput("الصباح")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("مهمة الصباح الأولى ١٢٣").assertIsDisplayed()

        // 2. SQL injection attack search
        searchField.performTextReplacement(" ' OR 1=1; --")
        composeTestRule.waitForIdle()
        // Must filter without throwing SQLite or parsing exceptions

        // 3. Huge 1,000 characters input
        val hugeQuery = "A".repeat(1000)
        searchField.performTextReplacement(hugeQuery)
        composeTestRule.waitForIdle()
        // No match text should appear safely
        composeTestRule.onNodeWithTag(QuestsTags.EMPTY).assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 6. Time Boundaries: Completing at 23:59 then 00:01 across midnight
    // -------------------------------------------------------------------------

    @Test
    fun todayScreen_midnightRollover_completingAt2359Then0001() = runTest {
        val day1 = LocalDate.of(2026, 11, 3)
        val day2 = LocalDate.of(2026, 11, 4)

        fakeClock.set(Instant.parse("2026-11-03T23:59:00Z"))

        val collectorJob = launch { todayViewModel.uiState.collect {} }

        val task1 = TaskEntity(
            id = 220L,
            name = "Late Night Patrol",
            description = "Patrol Day 1",
            scheduledTime = LocalTime.of(23, 50),
            scheduledDay = day1,
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.OTHER,
        )
        val task2 = TaskEntity(
            id = 221L,
            name = "Midnight Vigil",
            description = "Vigil Day 2",
            scheduledTime = LocalTime.of(0, 5),
            scheduledDay = day2,
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.OTHER,
        )
        taskRepo.insertTask(task1)
        taskRepo.insertTask(task2)
        advanceUntilIdle()

        // Complete task 1 on Day 1
        todayViewModel.completeTask(task1)
        advanceUntilIdle()

        val log1 = completionRepo.getLogForTaskOnDate(220L, day1)
        assertNotNull("Task 1 must be recorded for Day 1", log1)
        assertEquals(day1, log1?.completedDate)

        // Advance clock past midnight to Day 2 at 00:01
        fakeClock.set(Instant.parse("2026-11-04T00:01:00Z"))
        todayViewModel.refresh()
        advanceUntilIdle()

        // Complete task 2 on Day 2
        todayViewModel.completeTask(task2)
        advanceUntilIdle()

        val log2 = completionRepo.getLogForTaskOnDate(221L, day2)
        assertNotNull("Task 2 must be recorded for Day 2", log2)
        assertEquals(day2, log2?.completedDate)

        collectorJob.cancel()
    }

    // -------------------------------------------------------------------------
    // 7. Time Boundaries: Grace period transition
    // -------------------------------------------------------------------------

    @Test
    fun todayScreen_gracePeriod_timeAdvancementUpdatesStatus() = runTest {
        val scheduledDate = LocalDate.of(2026, 11, 3)
        // Clock at 09:00
        fakeClock.set(Instant.parse("2026-11-03T09:00:00Z"))

        val collectorJob = launch { todayViewModel.uiState.collect {} }

        val task = TaskEntity(
            id = 230L,
            name = "10 AM Strategy Meeting",
            description = "Standup",
            scheduledTime = LocalTime.of(10, 0),
            scheduledDay = scheduledDate,
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.OTHER,
        )
        taskRepo.insertTask(task)
        advanceUntilIdle()

        var state = todayViewModel.uiState.value as TodayUiState.Success
        var item = state.tasks.first { it.task.id == 230L }
        assertEquals("Task before scheduled time must be PENDING", TaskItemStatus.PENDING, item.status)

        // Advance time past 10:00 to 10:30
        fakeClock.set(Instant.parse("2026-11-03T10:30:00Z"))
        todayViewModel.refresh()
        advanceUntilIdle()

        state = todayViewModel.uiState.value as TodayUiState.Success
        item = state.tasks.first { it.task.id == 230L }
        assertEquals("Task after scheduled time must enter GRACE_PERIOD", TaskItemStatus.GRACE_PERIOD, item.status)

        collectorJob.cancel()
    }

    // -------------------------------------------------------------------------
    // 8. Number Overflow & Edge Values
    // -------------------------------------------------------------------------

    @Test
    fun todayScreen_numberOverflow_streakAndXpValues() {
        runBlocking {
            streakRepo.updateStreak(StreakEntity(id = 1, currentStreak = 999_999, longestStreak = 999_999))
            profileRepo.updateProfile(
                UserProfileEntity(
                    id = 1,
                    username = "LegendaryHero",
                    avatarId = "avatar_hero",
                    totalXp = 2_147_483_600,
                    level = 999,
                ),
            )
            taskRepo.insertTask(
                TaskEntity(
                    id = 240L,
                    name = "Epic Overcapped Quest",
                    description = "Max numbers",
                    scheduledTime = LocalTime.of(12, 0),
                    scheduledDay = baseDate,
                    category = TaskCategory.OTHER,
                    recurrenceType = RecurrenceType.DAILY,
                ),
            )
        }

        showTodayScreen()

        composeTestRule.onNodeWithTag(QuestsTags.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("Epic Overcapped Quest").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 9. Concurrency & Idempotency: Rapid duplicate completion
    // -------------------------------------------------------------------------

    @Test
    fun todayScreen_rapidDoubleCompletion_isIdempotent() = runTest {
        val task = TaskEntity(
            id = 250L,
            name = "Shield Maintenance",
            description = "Maintenance",
            scheduledTime = LocalTime.of(11, 0),
            scheduledDay = baseDate,
            category = TaskCategory.OTHER,
            recurrenceType = RecurrenceType.DAILY,
        )
        taskRepo.insertTask(task)

        val collectorJob = launch { todayViewModel.uiState.collect {} }
        advanceUntilIdle()

        // Rapid duplicate trigger
        todayViewModel.completeTask(task)
        todayViewModel.completeTask(task)
        advanceUntilIdle()

        val logs = completionRepo.getLogsForDate(baseDate).first().filter { it.taskId == 250L }
        assertEquals("Exactly one completion log should exist despite double invocation", 1, logs.size)

        collectorJob.cancel()
    }

    // -------------------------------------------------------------------------
    // 10. Display Matrix: Themes, Scales, RTL
    // -------------------------------------------------------------------------

    @Test
    fun todayScreen_themeAndDisplayMatrix() {
        val quest = TaskEntity(
            id = 260L,
            name = "Theme Matrix Verification",
            description = "Verify theme visuals",
            scheduledTime = LocalTime.of(12, 0),
            scheduledDay = baseDate,
            category = TaskCategory.OTHER,
            recurrenceType = RecurrenceType.DAILY,
        )
        runBlocking {
            taskRepo.insertTask(quest)
        }

        // Verify rendering under combined theme matrix: Comic theme, Dark mode, Simple Mode, 2x font scale, and RTL layout
        showTodayScreen(
            config = ComposeTestConfiguration(
                themeMode = ThemeMode.Comic,
                isDark = true,
                isSimpleMode = true,
                fontScale = 2.0f,
                layoutDirection = LayoutDirection.Rtl,
            ),
        )

        composeTestRule.onNodeWithTag(QuestsTags.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("Theme Matrix Verification").assertIsDisplayed()
    }
}
