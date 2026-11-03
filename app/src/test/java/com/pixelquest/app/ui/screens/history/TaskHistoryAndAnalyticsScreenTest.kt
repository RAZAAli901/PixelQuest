package com.pixelquest.app.ui.screens.history

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
import com.pixelquest.app.domain.TaskResultRecorder
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.PerTaskStats
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.ui.screens.QuestTab
import com.pixelquest.app.ui.screens.QuestsScreen
import com.pixelquest.app.ui.screens.QuestsTags
import com.pixelquest.app.ui.screens.analytics.TaskAnalyticsContent
import com.pixelquest.app.ui.screens.tasks.TaskViewModel
import com.pixelquest.app.ui.screens.today.TodayViewModel
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqSkin
import com.pixelquest.app.ui.theme.PqSkinState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * V030: User flows and out-of-bounds cases for Task History and Task Analytics screens.
 * Covers:
 * - History tab: zero history, one day, year of data, gap in data, range switching, pagination
 * - Analytics screen: zero history, full metrics, 14-day history window, null task, back navigation
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TaskHistoryAndAnalyticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val today = LocalDate.now()
    private val appClock = FixedClock(LocalDateTime.of(today, LocalTime.of(12, 0)))

    private lateinit var taskRepository: FakeTaskRepository
    private lateinit var completionRepository: FakeTaskCompletionRepository
    private lateinit var historyViewModel: TaskHistoryViewModel
    private lateinit var allViewModel: TaskViewModel
    private lateinit var todayViewModel: TodayViewModel
    private var originalSkin: PqSkin = PqSkin.Comic

    @Before
    fun setup() {
        originalSkin = PqSkinState.skin
        taskRepository = FakeTaskRepository()
        completionRepository = FakeTaskCompletionRepository()
        historyViewModel = TaskHistoryViewModel(completionRepository, taskRepository)
        val diffRepo = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, perfectDayThreshold = 0.7f, daysRequiredPerLevel = 7),
        )
        allViewModel = TaskViewModel(taskRepository, completionRepository, diffRepo, appClock)

        val profileRepo = FakeUserProfileRepository(UserProfileEntity(id = 1, username = "Hero", avatarId = "avatar"))
        val streakRepo = FakeStreakRepository(StreakEntity(id = 1, currentStreak = 3, longestStreak = 5, lastCompletedDate = today.minusDays(1)))
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val alarmScheduler = TaskAlarmScheduler(context)
        val recorder = TaskResultRecorder(completionRepository, profileRepo, streakRepo, taskRepository)

        todayViewModel = TodayViewModel(
            taskRepository = taskRepository,
            taskCompletionRepository = completionRepository,
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            difficultySettingsRepository = diffRepo,
            taskAlarmScheduler = alarmScheduler,
            taskResultRecorder = recorder,
            appClock = appClock,
        )
    }

    @After
    fun tearDown() {
        PqSkinState.skin = originalSkin
    }

    @Test
    fun historyTab_zeroHistory_displaysEmptyMessage() {
        composeTestRule.setContent {
            PixelQuestTheme {
                QuestsScreen(
                    initialTab = QuestTab.History,
                    onNewQuest = {},
                    onEditQuest = {},
                    onOpenHistoryQuest = {},
                    todayViewModel = todayViewModel,
                    historyViewModel = historyViewModel,
                    allViewModel = allViewModel,
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(QuestsTags.EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithText("Nothing here yet. What you finish or miss shows up here.").assertIsDisplayed()
    }

    @Test
    fun historyTab_oneDayHistory_displaysDateGroupAndClickOpensQuest() {
        runBlocking {
            val task = TaskEntity(
                id = 101L,
                name = "Morning Jog",
                description = "Run 5k",
                scheduledDay = today,
                scheduledTime = LocalTime.of(8, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.FITNESS,
            )
            taskRepository.insertTask(task)
            completionRepository.insertLog(
                TaskCompletionLogEntity(
                    id = 1L,
                    taskId = task.id,
                    completedDate = today,
                    wasCompleted = true,
                    pointsAwarded = 50,
                ),
            )

            var openedTaskId: Long? = null
            composeTestRule.setContent {
                PixelQuestTheme {
                    QuestsScreen(
                        initialTab = QuestTab.History,
                        onNewQuest = {},
                        onEditQuest = {},
                        onOpenHistoryQuest = { openedTaskId = it },
                        todayViewModel = todayViewModel,
                        historyViewModel = historyViewModel,
                        allViewModel = allViewModel,
                    )
                }
            }
            composeTestRule.waitForIdle()

            // Displays Today group title and task title
            composeTestRule.onNodeWithText("Today", ignoreCase = true).assertIsDisplayed()
            composeTestRule.onNodeWithText("Morning Jog").assertIsDisplayed()

            // Tapping history quest opens analytics/quest history
            composeTestRule.onNodeWithTag(QuestsTags.history(task.id)).performClick()
            composeTestRule.waitForIdle()
            assertEquals(task.id, openedTaskId)
        }
    }

    @Test
    fun historyTab_gapInData_groupsAccuratelyWithoutCrashing() {
        runBlocking {
            val task = TaskEntity(
                id = 201L,
                name = "Sporadic Quest",
                description = "",
                scheduledDay = today,
                scheduledTime = LocalTime.of(9, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.LEARNING,
            )
            taskRepository.insertTask(task)

            // One log today, one log 100 days ago (gap of 99 days)
            val oldDate = today.minusDays(100)
            completionRepository.insertLog(
                TaskCompletionLogEntity(id = 1L, taskId = task.id, completedDate = today, wasCompleted = true, pointsAwarded = 50),
            )
            completionRepository.insertLog(
                TaskCompletionLogEntity(id = 2L, taskId = task.id, completedDate = oldDate, wasCompleted = false, pointsAwarded = 0),
            )

            composeTestRule.setContent {
                PixelQuestTheme {
                    QuestsScreen(
                        initialTab = QuestTab.History,
                        onNewQuest = {},
                        onEditQuest = {},
                        onOpenHistoryQuest = {},
                        todayViewModel = todayViewModel,
                        historyViewModel = historyViewModel,
                        allViewModel = allViewModel,
                    )
                }
            }
            composeTestRule.waitForIdle()

            // Both date entries are present without crash
            composeTestRule.onNodeWithText("Today", ignoreCase = true).assertIsDisplayed()
            composeTestRule.onAllNodesWithText("Sporadic Quest").assertCountEquals(2)
        }
    }

    @Test
    fun historyTab_filterRangeSwitching_filtersCorrectly() {
        runBlocking {
            val task = TaskEntity(
                id = 301L,
                name = "Range Quest",
                description = "",
                scheduledDay = today,
                scheduledTime = LocalTime.of(10, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.HEALTH,
            )
            taskRepository.insertTask(task)

            // Seed logs:
            // Log 1: 3 days ago (within 7 days)
            completionRepository.insertLog(
                TaskCompletionLogEntity(id = 1L, taskId = task.id, completedDate = today.minusDays(3), wasCompleted = true, pointsAwarded = 50),
            )
            // Log 2: 15 days ago (within 30 days, outside 7 days)
            completionRepository.insertLog(
                TaskCompletionLogEntity(id = 2L, taskId = task.id, completedDate = today.minusDays(15), wasCompleted = true, pointsAwarded = 50),
            )
            // Log 3: 45 days ago (outside 30 days)
            completionRepository.insertLog(
                TaskCompletionLogEntity(id = 3L, taskId = task.id, completedDate = today.minusDays(45), wasCompleted = true, pointsAwarded = 50),
            )

            composeTestRule.setContent {
                PixelQuestTheme {
                    QuestsScreen(
                        initialTab = QuestTab.History,
                        onNewQuest = {},
                        onEditQuest = {},
                        onOpenHistoryQuest = {},
                        todayViewModel = todayViewModel,
                        historyViewModel = historyViewModel,
                        allViewModel = allViewModel,
                    )
                }
            }
            composeTestRule.waitForIdle()

            // Default: ALL_TIME shows 3 items
            assertEquals(3, historyViewModel.uiState.value.items.size)

            // Switch to "7 days"
            composeTestRule.onNodeWithText("7 days").performClick()
            composeTestRule.waitForIdle()
            assertEquals(HistoryFilter.LAST_7_DAYS, historyViewModel.uiState.value.selectedFilter)
            assertEquals(1, historyViewModel.uiState.value.items.size)

            // Switch to "30 days"
            composeTestRule.onNodeWithText("30 days").performClick()
            composeTestRule.waitForIdle()
            assertEquals(HistoryFilter.LAST_30_DAYS, historyViewModel.uiState.value.selectedFilter)
            assertEquals(2, historyViewModel.uiState.value.items.size)

            // Switch back to "All time"
            composeTestRule.onNodeWithText("All time").performClick()
            composeTestRule.waitForIdle()
            assertEquals(HistoryFilter.ALL_TIME, historyViewModel.uiState.value.selectedFilter)
            assertEquals(3, historyViewModel.uiState.value.items.size)
        }
    }

    @Test
    fun historyTab_pagination_showsMoreButtonWhenExceedingPageSize() {
        runBlocking {
            val task = TaskEntity(
                id = 401L,
                name = "Paginated Quest",
                description = "",
                scheduledDay = today,
                scheduledTime = LocalTime.of(11, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.CHORES,
            )
            taskRepository.insertTask(task)

            // Seed 35 logs (default page size is 30)
            for (i in 1..35) {
                completionRepository.insertLog(
                    TaskCompletionLogEntity(
                        id = i.toLong(),
                        taskId = task.id,
                        completedDate = today.minusDays(i.toLong()),
                        wasCompleted = true,
                        pointsAwarded = 50,
                    ),
                )
            }

            composeTestRule.setContent {
                PixelQuestTheme {
                    QuestsScreen(
                        initialTab = QuestTab.History,
                        onNewQuest = {},
                        onEditQuest = {},
                        onOpenHistoryQuest = {},
                        todayViewModel = todayViewModel,
                        historyViewModel = historyViewModel,
                        allViewModel = allViewModel,
                    )
                }
            }
            composeTestRule.waitForIdle()

            // Has more items should be true initially (30 items displayed)
            assertTrue(historyViewModel.uiState.value.hasMoreItems)
            assertEquals(30, historyViewModel.uiState.value.items.size)

            // Scroll to and click MORE button
            composeTestRule.onNodeWithTag(QuestsTags.SCREEN).performScrollToNode(hasTestTag(QuestsTags.MORE))
            composeTestRule.onNodeWithTag(QuestsTags.MORE).performClick()
            composeTestRule.waitForIdle()

            // Page expanded to show all 35 items
            assertEquals(35, historyViewModel.uiState.value.items.size)
            assertEquals(false, historyViewModel.uiState.value.hasMoreItems)
        }
    }

    @Test
    fun analyticsScreen_zeroHistory_rendersSafeZerosWithoutCrashing() {
        val stats = PerTaskStats(
            taskId = 501L,
            completionCount = 0,
            totalScheduledCount = 0,
            completionRate = 0f,
            currentStreak = 0,
            longestStreak = 0,
            recentHistory = emptyList(),
        )

        composeTestRule.setContent {
            PixelQuestTheme {
                TaskAnalyticsContent(
                    task = null,
                    stats = stats,
                    onBackClick = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("QUEST STATS").assertIsDisplayed()
        composeTestRule.onNodeWithText("0").assertIsDisplayed()
        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("0 days").assertCountEquals(2)
    }

    @Test
    fun analyticsScreen_fullMetricsAndRecentHistory_displaysAllCardsAndIndicators() {
        val task = TaskEntity(
            id = 601L,
            name = "Mastering Kotlin",
            description = "Deep dive coding",
            scheduledDay = today,
            scheduledTime = LocalTime.of(14, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.LEARNING,
        )

        val recentDays = (1..14).map { i ->
            today.minusDays(14L - i) to (i % 2 == 0)
        }

        val stats = PerTaskStats(
            taskId = 601L,
            completionCount = 42,
            totalScheduledCount = 50,
            completionRate = 0.84f,
            currentStreak = 6,
            longestStreak = 12,
            recentHistory = recentDays,
        )

        var backClicked = false
        composeTestRule.setContent {
            PixelQuestTheme {
                TaskAnalyticsContent(
                    task = task,
                    stats = stats,
                    onBackClick = { backClicked = true },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("MASTERING KOTLIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("Learning").assertIsDisplayed()
        composeTestRule.onNodeWithText("42").assertIsDisplayed()
        composeTestRule.onNodeWithText("84%").assertIsDisplayed()
        composeTestRule.onNodeWithText("6 days").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 days").assertIsDisplayed()

        // 14-day section
        composeTestRule.onNodeWithText("Last 14 days", ignoreCase = true).assertIsDisplayed()

        // Test one of the recent days accessibility descriptions
        val sampleDoneDay = recentDays.first { it.second }
        composeTestRule.onNodeWithContentDescription("done, ${sampleDoneDay.first}").assertExists()

        // Back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue("Back button click should invoke onBackClick", backClicked)
    }

    @Test
    fun analyticsScreen_nullTask_rendersMetricsSafely() {
        val stats = PerTaskStats(
            taskId = 701L,
            completionCount = 10,
            totalScheduledCount = 10,
            completionRate = 1.0f,
            currentStreak = 10,
            longestStreak = 10,
            recentHistory = emptyList(),
        )

        composeTestRule.setContent {
            PixelQuestTheme {
                TaskAnalyticsContent(
                    task = null,
                    stats = stats,
                    onBackClick = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("10").assertIsDisplayed()
        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
    }
}
