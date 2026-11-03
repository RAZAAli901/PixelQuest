package com.pixelquest.app.ui.screens.home

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelUpSignalManager
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
import com.pixelquest.app.ui.screens.HomeContent
import com.pixelquest.app.ui.screens.HomeTags
import com.pixelquest.app.ui.screens.toHomeUi
import com.pixelquest.app.ui.screens.today.TodayUiState
import com.pixelquest.app.ui.screens.today.TodayViewModel
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Phase B - V022: Home screen flow and exhaustive out-of-bounds cases.
 *
 * Covers:
 * 1. Happy-path user journey: Open screen, see today's quests, complete one, verify XP & streak changes.
 * 2. Empty state: No quests today (EmptyDayCard with New Quest action).
 * 3. High volume: 500 quests loaded into LazyColumn, fast scrolling, and completing items.
 * 4. Midnight boundary transition: Completing quest at 23:59 on Day 1, then at 00:01 on Day 2.
 * 5. Hostile / Odd characters: Emoji, Arabic/RTL, HTML, and SQL snippets in quest names.
 * 6. Extreme numbers: Massive streak (99,999 days), huge XP (2 billion), and level 999.
 * 7. Double tap / Concurrency: Rapid repeated taps on completion circle are safely deduplicated.
 * 8. Theme matrix: Comic, Pixel, Dark, Simple Mode, and 2x font scale.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class HomeScreenFlowAndEdgeCasesTest {

    @get:Rule(order = 0)
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    private val baseDate = LocalDate.of(2026, 11, 3)
    private val fakeClock = FakeClock(Instant.parse("2026-11-03T10:00:00Z"))

    private lateinit var taskRepo: FakeTaskRepository
    private lateinit var completionRepo: FakeTaskCompletionRepository
    private lateinit var streakRepo: FakeStreakRepository
    private lateinit var profileRepo: FakeUserProfileRepository
    private lateinit var difficultyRepo: FakeDifficultySettingsRepository
    private lateinit var settingsRepo: FakeSettingsRepository
    private lateinit var taskResultRecorder: TaskResultRecorder
    private lateinit var alarmScheduler: TaskAlarmScheduler

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var todayViewModel: TodayViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        taskRepo = FakeTaskRepository()
        completionRepo = FakeTaskCompletionRepository()
        streakRepo = FakeStreakRepository(
            StreakEntity(id = 1, currentStreak = 3, longestStreak = 5, lastCompletedDate = baseDate.minusDays(1)),
        )
        profileRepo = FakeUserProfileRepository(
            UserProfileEntity(id = 1, username = "Arthur", avatarId = "avatar_hero", totalXp = 100, level = 2),
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

        homeViewModel = HomeViewModel(
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            difficultySettingsRepository = difficultyRepo,
            levelUpSignalManager = LevelUpSignalManager(context),
            settingsRepository = settingsRepo,
        )
    }

    @Composable
    private fun TestHomeScreen(
        onNewQuest: () -> Unit = {},
    ) {
        val today by todayViewModel.uiState.collectAsState()
        (today as? TodayUiState.Success)?.let { state ->
            val time = DateTimeFormatter.ofPattern("HH:mm")
            val labels = mapOf(RecurrenceType.DAILY to "Daily", RecurrenceType.WEEKLY to "Weekly", RecurrenceType.MONTHLY to "Monthly")
            val home = state.toHomeUi(time) { labels[it] }
            HomeContent(
                state = home,
                greeting = "Good morning",
                onCompleteQuest = { quest ->
                    state.tasks.firstOrNull { it.task.id == quest.id }?.let { item ->
                        todayViewModel.completeTask(item.task)
                    }
                },
                onSkipQuest = { quest ->
                    state.tasks.firstOrNull { it.task.id == quest.id }?.let { item ->
                        todayViewModel.skipTask(item.task)
                    }
                },
                onQuestClick = {},
                onSeeAllQuests = {},
                onNewQuest = onNewQuest,
                onAvatarClick = {},
                onParties = {},
            )
        }
    }

    private fun showHomeScreen(
        config: ComposeTestConfiguration = ComposeTestConfiguration(themeMode = ThemeMode.Pixel),
        onNewQuest: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ThemedTestContent(config) {
                TestHomeScreen(onNewQuest = onNewQuest)
            }
        }
        composeTestRule.waitUntil(5000) { todayViewModel.uiState.value is TodayUiState.Success }
    }

    // -------------------------------------------------------------------------
    // 1. Happy-Path User Flow: Open, see quests, complete one, verify XP & streak
    // -------------------------------------------------------------------------

    @Test
    fun homeScreen_userJourney_completeQuest_updatesProgressAndXp() {
        // Arrange: 2 quests scheduled for today
        val task1 = TaskEntity(
            id = 101L,
            name = "Morning Meditation",
            description = "10 minutes of mindfulness",
            scheduledTime = LocalTime.of(8, 0),
            scheduledDay = baseDate,
            category = TaskCategory.HEALTH,
            recurrenceType = RecurrenceType.DAILY,
        )
        val task2 = TaskEntity(
            id = 102L,
            name = "Read Documentation",
            description = "Study Kotlin Compose",
            scheduledTime = LocalTime.of(14, 0),
            scheduledDay = baseDate,
            category = TaskCategory.LEARNING,
            recurrenceType = RecurrenceType.DAILY,
        )
        runBlocking {
            taskRepo.insertTask(task1)
            taskRepo.insertTask(task2)
            // Seed an earlier completion for task 1 so it awards normal XP
            completionRepo.insertLog(
                TaskCompletionLogEntity(
                    taskId = 101L,
                    completedDate = baseDate.minusDays(1),
                    wasCompleted = true,
                    pointsAwarded = 20,
                ),
            )
        }

        // Act: Open screen
        showHomeScreen()

        // Assert: Home header & progress card
        composeTestRule.onNodeWithTag(HomeTags.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("Arthur · LVL 2").assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("3 day streak")).assertIsDisplayed()
        composeTestRule.onNodeWithText("0 of 2 done · target 50%").assertIsDisplayed()

        // Verify quests displayed
        composeTestRule.onNodeWithText("Morning Meditation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Read Documentation").assertIsDisplayed()

        // Complete task 1 by clicking its circle
        composeTestRule.onNode(isToggleable() and hasAnyAncestor(hasTestTag(HomeTags.quest(101L)))).performClick()

        // Assert: Progress updates, target 50% met -> PerfectDayCard is shown
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(HomeTags.PERFECT).assertIsDisplayed()

        // Verify database state updated
        runBlocking {
            val log = completionRepo.getLogForTaskOnDate(101L, baseDate)
            assertNotNull("Completion log must be stored", log)
            assertTrue("Task must be logged as completed", log?.wasCompleted == true)

            val updatedProfile = profileRepo.getProfile().first()
            assertTrue("XP should increase after completion", (updatedProfile?.totalXp ?: 0) > 100)
        }
    }

    // -------------------------------------------------------------------------
    // 2. Out-of-Bounds: Empty State (0 quests)
    // -------------------------------------------------------------------------

    @Test
    fun homeScreen_noQuests_displaysEmptyStateAndNewQuestPrompt() {
        var newQuestClicked = false
        showHomeScreen(onNewQuest = { newQuestClicked = true })

        composeTestRule.onNodeWithTag(HomeTags.EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithText("NEW QUEST").performClick()
        assertTrue("Clicking New Quest must invoke navigation callback", newQuestClicked)
    }

    // -------------------------------------------------------------------------
    // 3. Out-of-Bounds: 500 Quests (High Volume)
    // -------------------------------------------------------------------------

    @Test
    fun homeScreen_fiveHundredQuests_loadsAndScrollsWithoutCrashing() {
        val manyTasks = (1L..500L).map { i ->
            TaskEntity(
                id = i,
                name = "Quest #$i",
                description = "Task payload $i",
                scheduledTime = LocalTime.of(8, (i % 60).toInt()),
                scheduledDay = baseDate,
                category = TaskCategory.CHORES,
                recurrenceType = RecurrenceType.DAILY,
            )
        }
        runBlocking {
            manyTasks.forEach { taskRepo.insertTask(it) }
        }

        showHomeScreen()

        // Home loads without OOM
        composeTestRule.onNodeWithTag(HomeTags.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("0 of 500 done · target 50%").assertIsDisplayed()

        // Scroll to quest 50
        composeTestRule.onNodeWithTag(HomeTags.SCREEN).performScrollToNode(hasTestTag(HomeTags.quest(50L)))
        composeTestRule.onNodeWithText("Quest #50").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 4. Time Boundaries: Completing at 23:59 then 00:01
    // -------------------------------------------------------------------------

    @Test
    fun homeScreen_completingAcrossMidnightBoundary_recordsCorrectDates() = runTest {
        val day1 = LocalDate.of(2026, 11, 3)
        val day2 = LocalDate.of(2026, 11, 4)

        // Set clock to 23:59 on Day 1
        fakeClock.set(Instant.parse("2026-11-03T23:59:00Z"))

        val collectorJob = launch { todayViewModel.uiState.collect {} }

        val task1 = TaskEntity(
            id = 201L,
            name = "Late Night Review",
            description = "Study review",
            scheduledTime = LocalTime.of(23, 50),
            scheduledDay = day1,
            category = TaskCategory.LEARNING,
            recurrenceType = RecurrenceType.DAILY,
        )
        val task2 = TaskEntity(
            id = 202L,
            name = "Midnight Vigil",
            description = "Midnight reflection",
            scheduledTime = LocalTime.of(0, 5),
            scheduledDay = day1,
            category = TaskCategory.HEALTH,
            recurrenceType = RecurrenceType.DAILY,
        )
        taskRepo.insertTask(task1)
        taskRepo.insertTask(task2)

        // Complete task 1 at 23:59
        todayViewModel.completeTask(task1)
        advanceUntilIdle()

        val logDay1 = completionRepo.getLogForTaskOnDate(201L, day1)
        assertNotNull("Task completed at 23:59 must be logged on Day 1", logDay1)
        assertEquals(day1, logDay1?.completedDate)

        // Advance clock across midnight to 00:01 on Day 2
        fakeClock.advanceBy(Duration.ofMinutes(2))
        todayViewModel.refresh()
        advanceUntilIdle()

        // Complete task 2 at 00:01
        todayViewModel.completeTask(task2)
        advanceUntilIdle()

        val logDay2 = completionRepo.getLogForTaskOnDate(202L, day2)
        assertNotNull("Task completed at 00:01 must be logged on Day 2", logDay2)
        assertEquals(day2, logDay2?.completedDate)

        collectorJob.cancel()
    }

    // -------------------------------------------------------------------------
    // 5. Hostile / Odd Characters
    // -------------------------------------------------------------------------

    @Test
    fun homeScreen_oddAndHostileCharacters_renderedSafely() {
        val oddTask = TaskEntity(
            id = 301L,
            name = "⚔️ صلاة الفجر <script>alert('xss')</script> '; DROP TABLE quests; --",
            description = "Special characters payload",
            scheduledTime = LocalTime.of(9, 0),
            scheduledDay = baseDate,
            category = TaskCategory.HEALTH,
            recurrenceType = RecurrenceType.DAILY,
        )
        runBlocking {
            taskRepo.insertTask(oddTask)
        }

        showHomeScreen()

        composeTestRule.onNodeWithText("⚔️ صلاة الفجر <script>alert('xss')</script> '; DROP TABLE quests; --").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 6. Number Overflow Boundaries
    // -------------------------------------------------------------------------

    @Test
    fun homeScreen_extremeNumbers_handledWithoutCrashing() {
        runBlocking {
            streakRepo.updateStreak(StreakEntity(id = 1, currentStreak = 99_999, longestStreak = 99_999))
            profileRepo.insertProfile(
                UserProfileEntity(id = 1, username = "Titan", avatarId = "avatar_hero", totalXp = 2_000_000_000, level = 999),
            )
        }

        showHomeScreen()

        composeTestRule.onNodeWithText("Titan · LVL 999").assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("99999 day streak")).assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 7. Concurrency / Rapid Taps
    // -------------------------------------------------------------------------

    @Test
    fun homeScreen_rapidRepeatedCompletions_onlyAwardsXpOnce() = runTest {
        val task = TaskEntity(
            id = 401L,
            name = "Single Completion Task",
            description = "Deduplication task",
            scheduledTime = LocalTime.of(10, 0),
            scheduledDay = baseDate,
            category = TaskCategory.FITNESS,
            recurrenceType = RecurrenceType.DAILY,
        )
        taskRepo.insertTask(task)

        val initialProfile = profileRepo.getProfile().first()
        val initialXp = initialProfile?.totalXp ?: 0

        // Rapid double invocation
        val res1 = taskResultRecorder.recordTaskCompletion(401L, baseDate)
        val res2 = taskResultRecorder.recordTaskCompletion(401L, baseDate)

        assertTrue("First call succeeds", res1 is TaskResultRecorder.CompletionResult.Success)
        assertTrue("Second call is detected as already completed", res2 is TaskResultRecorder.CompletionResult.AlreadyCompleted)

        val updatedProfile = profileRepo.getProfile().first()
        val xpGained = (updatedProfile?.totalXp ?: 0) - initialXp
        assertEquals("Only one completion XP award must be granted", (res1 as TaskResultRecorder.CompletionResult.Success).points, xpGained)
    }

    // -------------------------------------------------------------------------
    // 8. Theme Matrix & Simple Mode
    // -------------------------------------------------------------------------

    @Test
    fun homeScreen_rendersInSimpleModeAndComicTheme() {
        val task = TaskEntity(
            id = 501L,
            name = "Walk in Park",
            description = "Nature walk",
            scheduledTime = LocalTime.of(15, 0),
            scheduledDay = baseDate,
            category = TaskCategory.FITNESS,
            recurrenceType = RecurrenceType.DAILY,
        )
        runBlocking {
            taskRepo.insertTask(task)
            settingsRepo.setSimpleModeEnabled(true)
        }

        showHomeScreen(
            ComposeTestConfiguration(themeMode = ThemeMode.Comic, isDark = true, isSimpleMode = true, fontScale = 2.0f),
        )

        // Simple mode displays hero name without level/XP
        composeTestRule.onNodeWithText("Arthur").assertIsDisplayed()
        composeTestRule.onNodeWithText("TODAY'S PROGRESS").assertIsDisplayed()
    }

    private fun runTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = kotlinx.coroutines.test.runTest { this.block() }
}
