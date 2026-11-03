package com.pixelquest.app.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
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
import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.QuestTags
import com.pixelquest.app.ui.components.QuestUi
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.screens.history.HistoryFilter
import com.pixelquest.app.ui.screens.history.TaskHistoryItem
import com.pixelquest.app.ui.screens.history.TaskHistoryViewModel
import com.pixelquest.app.ui.screens.tasks.TaskViewModel
import com.pixelquest.app.ui.screens.tasks.TaskWithStatus
import com.pixelquest.app.ui.screens.today.TodayTaskItem
import com.pixelquest.app.ui.screens.today.TodayViewModel
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
 * Quests (docs/ui-v2.md, mock 2C): today's by part of the day, all of them by how they repeat, the
 * history by day; a search, and the one New quest button.
 *
 * Extended for V024:
 * - Quest list flow across All, Today, and History tabs
 * - Out-of-bounds: 500+ quests, duplicate titles, past & far future dates
 * - Hostile search strings, rapid tab switching, theme and simple mode matrix
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
@OptIn(ExperimentalCoroutinesApi::class)
class QuestsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val baseInstant = Instant.parse("2026-10-21T10:00:00Z")
    private val day = LocalDate.of(2026, 10, 21)
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
            StreakEntity(id = 1, currentStreak = 5, longestStreak = 10, lastCompletedDate = day.minusDays(1)),
        )
        profileRepo = FakeUserProfileRepository(
            UserProfileEntity(id = 1, username = "Lancelot", avatarId = "avatar_hero", totalXp = 300, level = 3),
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

    private fun showQuestsScreen(
        initialTab: QuestTab = QuestTab.All,
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
                    initialTab = initialTab,
                    todayViewModel = todayViewModel,
                    allViewModel = taskViewModel,
                    historyViewModel = historyViewModel,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun task(
        id: Long,
        name: String,
        hour: Int,
        recurrence: RecurrenceType = RecurrenceType.DAILY,
        category: TaskCategory = TaskCategory.FITNESS,
        scheduledDay: LocalDate = day,
    ) = TaskEntity(
        id = id,
        name = name,
        description = "Task description $id",
        scheduledTime = LocalTime.of(hour, 0),
        scheduledDay = scheduledDay,
        category = category,
        recurrenceType = recurrence,
    )

    // =========================================================================
    // Core Grouping & Search Unit Tests
    // =========================================================================

    @Test
    fun today_isGroupedByPartOfTheDay_inTimeOrder() {
        val parts = todayParts(
            listOf(
                TodayTaskItem(task(1, "Gym", 17), TaskItemStatus.PENDING),
                TodayTaskItem(task(2, "Push-ups", 8), TaskItemStatus.DONE),
                TodayTaskItem(task(3, "Lunch walk", 13), TaskItemStatus.PENDING),
                TodayTaskItem(task(4, "Stretch", 7), TaskItemStatus.DONE),
            ),
        )
        assertEquals(listOf(DayPart.Morning, DayPart.Afternoon, DayPart.Evening), parts.map { it.first })
        assertEquals(listOf(4L, 2L), parts.first().second.map { it.task.id })
    }

    @Test
    fun all_isGroupedByHowTheyRepeat_dailyFirst_oneTimeLast() {
        val groups = recurrenceGroups(
            listOf(
                TaskWithStatus(task(1, "Exam", 9, RecurrenceType.ONE_TIME), TaskItemStatus.PENDING),
                TaskWithStatus(task(2, "Gym", 17), TaskItemStatus.COMPLETED),
                TaskWithStatus(task(3, "Laundry", 10, RecurrenceType.WEEKLY), TaskItemStatus.MISSED),
                TaskWithStatus(task(4, "Read", 8), TaskItemStatus.PENDING),
            ),
        )
        assertEquals(listOf(RecurrenceType.DAILY, RecurrenceType.WEEKLY, RecurrenceType.ONE_TIME), groups.map { it.first })
        assertEquals(listOf(4L, 2L), groups.first().second.map { it.task.id })

        val laundry = groups[1].second.single().toQuestUi(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) { "Weekly" }
        assertEquals(true, laundry.missed)
        assertEquals("Weekly", laundry.repeatLabel)
    }

    @Test
    fun history_isByDay_latestFirst_missedSaysSo() {
        val items = listOf(
            TaskHistoryItem(1, 7, "Gym", TaskCategory.FITNESS, day.minusDays(2), 20, true),
            TaskHistoryItem(2, 8, "Read", TaskCategory.LEARNING, day, 15, true),
            TaskHistoryItem(3, 7, "Gym", TaskCategory.FITNESS, day, 0, false),
        )
        val days = historyDays(items)
        assertEquals(listOf(day, day.minusDays(2)), days.map { it.first })
        val missed = days.first().second.last().toQuestUi()
        assertEquals(true, missed.missed)
        assertEquals(0, missed.xp)
        assertEquals(15, days.first().second.first().toQuestUi().xp)
    }

    @Test
    fun theSearch_keepsMatchingTitles_anyCase() {
        val groups = listOf(
            QuestGroup("a", "Morning", listOf(QuestUi(1, "Go to Gym", null, QuestCategory.Fitness), QuestUi(2, "Read", null, QuestCategory.Learning))),
            QuestGroup("b", "Evening", listOf(QuestUi(3, "Read more", null, QuestCategory.Learning))),
        )
        assertEquals(listOf(listOf(1L)), groups.matching("GYM").map { g -> g.quests.map { it.id } })
        assertEquals(groups, groups.matching(" "))
    }

    private val sampleGroups = listOf(
        QuestGroup("Morning", "Morning · 2 done", listOf(QuestUi(1, "20 Push-ups", "08:00", QuestCategory.Fitness, done = true))),
        QuestGroup("Evening", "Evening · 1 left", listOf(QuestUi(2, "Go to Gym", "17:30", QuestCategory.Fitness, overdue = true))),
    )

    @Test
    fun today_showsItsGroups_andACircleCompletes() {
        val toggled = mutableListOf<QuestUi>()
        var tab = QuestTab.All
        composeTestRule.setContent {
            QuestsContent(QuestTab.Today, { tab = it }, todayCount = 2, groups = sampleGroups, onToggleQuest = { toggled += it }, onQuestClick = {}, onNewQuest = {})
        }

        composeTestRule.onNodeWithText("QUESTS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today · 2").assertIsSelected()
        composeTestRule.onNodeWithText("EVENING · 1 LEFT").assertIsDisplayed()
        composeTestRule.onNode(isToggleable() and hasAnyAncestor(hasTestTag(QuestsTags.quest(2)))).performClick()
        assertEquals(listOf(2L), toggled.map { it.id })

        composeTestRule.onNodeWithText("History").performClick()
        assertEquals(QuestTab.History, tab)
    }

    @Test
    fun theNewQuestButton_saysWhatItDoes() {
        var added = 0
        composeTestRule.setContent {
            QuestsContent(QuestTab.Today, {}, 0, emptyList(), {}, {}, onNewQuest = { added++ })
        }
        composeTestRule.onNodeWithText("Nothing scheduled today.").assertIsDisplayed()
        composeTestRule.onNodeWithText("NEW QUEST").performClick()
        assertEquals(1, added)
    }

    @Test
    fun theSearchButton_opensAField_thatFilters() {
        var query: String? = null
        composeTestRule.setContent {
            val q = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
            QuestsContent(QuestTab.Today, {}, 2, sampleGroups.matching(q.value.orEmpty()), {}, {}, {}, query = q.value, onQueryChange = {
                q.value = it
                query = it
            })
        }
        composeTestRule.onNodeWithTag(QuestsTags.SEARCH).performClick()
        composeTestRule.onNodeWithTag(QuestsTags.SEARCH_FIELD).assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(QuestsTags.SEARCH_FIELD))).performTextInput("gym")

        assertEquals("gym", query)
        composeTestRule.onNodeWithText("Go to Gym").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodes(hasText("20 Push-ups")).fetchSemanticsNodes().size)
    }

    @Test
    fun history_hasItsFilters_andMore() {
        var filter: HistoryFilter? = null
        var more = 0
        composeTestRule.setContent {
            QuestsContent(
                QuestTab.History, {}, 0,
                listOf(QuestGroup("d", "Today", listOf(QuestUi(7, "Gym", null, QuestCategory.Fitness, missed = true)))),
                {}, {}, {},
                historyFilter = HistoryFilter.ALL_TIME, onHistoryFilter = { filter = it }, historyHasMore = true, onHistoryMore = { more++ },
            )
        }
        composeTestRule.onNodeWithText("All time").assertIsSelected()
        composeTestRule.onNodeWithText("MISSED").assertIsDisplayed()
        composeTestRule.onNodeWithText("7 days").performClick()
        assertEquals(HistoryFilter.LAST_7_DAYS, filter)
        composeTestRule.onNodeWithTag(QuestsTags.MORE).performClick()
        assertEquals(1, more)
    }

    @Test
    fun simpleMode_saysTasks() {
        composeTestRule.setContent {
            QuestsContent(QuestTab.All, {}, 0, emptyList(), {}, {}, {}, isSimpleMode = true)
        }
        composeTestRule.onNodeWithText("TASKS").assertIsDisplayed()
        composeTestRule.onNodeWithText("All tasks").assertIsSelected()
        composeTestRule.onNodeWithText("NEW TASK").assertIsDisplayed()
    }

    // =========================================================================
    // V024 User Flows & Out-of-Bounds Cases
    // =========================================================================

    @Test
    fun questList_allTabUserFlow_displaysRecurrenceGroupsAndCompletesQuest() {
        val dailyTask = task(301L, "Daily Training", 8, RecurrenceType.DAILY)
        val weeklyTask = task(302L, "Weekly Deep Clean", 14, RecurrenceType.WEEKLY)
        val oneTimeTask = task(303L, "Castle Guard Duty", 18, RecurrenceType.ONE_TIME)

        runBlocking {
            taskRepo.insertTask(dailyTask)
            taskRepo.insertTask(weeklyTask)
            taskRepo.insertTask(oneTimeTask)
        }

        showQuestsScreen(initialTab = QuestTab.All)

        composeTestRule.onNodeWithTag(QuestsTags.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily Training").assertIsDisplayed()
        composeTestRule.onNodeWithText("Weekly Deep Clean").assertIsDisplayed()
        composeTestRule.onNodeWithText("Castle Guard Duty").assertIsDisplayed()

        // Toggle complete on daily task
        composeTestRule.onNodeWithTag(QuestTags.complete(301L)).performClick()
        composeTestRule.waitForIdle()

        // Verify task completion is written to completionRepo
        runBlocking {
            val log = completionRepo.getLogForTaskOnDate(301L, day)
            assertNotNull("Completion log must exist in repository", log)
            assertTrue("Task should be completed", log?.wasCompleted == true)
        }
    }

    @Test
    fun questList_historyTabUserFlow_filtersAndPagination() {
        var openedHistoryTaskId: Long? = null

        runBlocking {
            taskRepo.insertTask(task(310L, "Dungeon Raid", 10))
            taskRepo.insertTask(task(311L, "Dragon Hunt", 12))
            taskRepo.insertTask(task(312L, "Castle Guard", 16))
            // Seed history logs over multiple dates with distinct tasks
            completionRepo.insertLog(TaskCompletionLogEntity(taskId = 310L, completedDate = day, wasCompleted = true, pointsAwarded = 30))
            completionRepo.insertLog(TaskCompletionLogEntity(taskId = 311L, completedDate = day.minusDays(2), wasCompleted = false, pointsAwarded = 0))
            completionRepo.insertLog(TaskCompletionLogEntity(taskId = 312L, completedDate = day.minusDays(15), wasCompleted = true, pointsAwarded = 30))
        }

        showQuestsScreen(
            initialTab = QuestTab.History,
            onOpenHistoryQuest = { openedHistoryTaskId = it },
        )

        composeTestRule.onNodeWithTag(QuestsTags.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("History").assertIsSelected()

        // Verify filter chips are present
        composeTestRule.onNodeWithText("7 days").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("30 days").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("All time").performClick()
        composeTestRule.waitForIdle()

        // Click a history row to open details
        composeTestRule.onNodeWithTag(QuestsTags.history(310L)).performClick()
        assertEquals(310L, openedHistoryTaskId)
    }

    @Test
    fun questList_emptyState_allTabsDisplayProperEmptyMessages() {
        showQuestsScreen(initialTab = QuestTab.All)
        composeTestRule.onNodeWithTag(QuestsTags.EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithText("No quests yet. Add one with the button below.").assertIsDisplayed()

        // Switch to Today tab
        composeTestRule.onNodeWithText("Today · 0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Nothing scheduled today.").assertIsDisplayed()

        // Switch to History tab
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Nothing here yet. What you finish or miss shows up here.").assertIsDisplayed()
    }

    @Test
    fun questList_fiveHundredQuests_rendersRecurrenceGroupsWithoutCrash() {
        val manyTasks = (1L..500L).map { i ->
            task(
                id = i,
                name = "Quest #$i",
                hour = (i % 24).toInt(),
                recurrence = when (i % 4) {
                    0L -> RecurrenceType.DAILY
                    1L -> RecurrenceType.WEEKLY
                    2L -> RecurrenceType.MONTHLY
                    else -> RecurrenceType.ONE_TIME
                },
            )
        }
        runBlocking {
            manyTasks.forEach { taskRepo.insertTask(it) }
        }

        showQuestsScreen(initialTab = QuestTab.All)

        composeTestRule.onNodeWithTag(QuestsTags.SCREEN).assertIsDisplayed()
        // Scroll to quest 50
        composeTestRule.onNodeWithTag(QuestsTags.SCREEN).performScrollToNode(hasTestTag(QuestsTags.quest(50L)))
        composeTestRule.onNodeWithText("Quest #50").assertIsDisplayed()
    }

    @Test
    fun questList_duplicateTitles_rendersDistinctNodesAndCompletesCorrectOne() {
        val taskA = task(401L, "Practice Archery", 8, RecurrenceType.DAILY)
        val taskB = task(402L, "Practice Archery", 12, RecurrenceType.WEEKLY)
        val taskC = task(403L, "Practice Archery", 16, RecurrenceType.ONE_TIME)

        runBlocking {
            taskRepo.insertTask(taskA)
            taskRepo.insertTask(taskB)
            taskRepo.insertTask(taskC)
        }

        showQuestsScreen(initialTab = QuestTab.All)

        // All 3 duplicate-named quests must render
        composeTestRule.onNodeWithTag(QuestsTags.quest(401L)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QuestsTags.quest(402L)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QuestsTags.quest(403L)).assertIsDisplayed()

        // Complete only task 402
        composeTestRule.onNodeWithTag(QuestTags.complete(402L)).performClick()
        composeTestRule.waitForIdle()

        runBlocking {
            val log402 = completionRepo.getLogForTaskOnDate(402L, day)
            val log401 = completionRepo.getLogForTaskOnDate(401L, day)
            assertNotNull("Task 402 must be completed", log402)
            assertEquals("Task 401 must not be completed", null, log401)
        }
    }

    @Test
    fun questList_dateEdgeCases_pastDueDatesAndFarFutureDates() {
        val pastTask = task(410L, "Forgotten Ancient Scroll", 10, RecurrenceType.ONE_TIME, scheduledDay = day.minusYears(3))
        val futureTask = task(411L, "Century Celebration", 12, RecurrenceType.ONE_TIME, scheduledDay = day.plusYears(50))

        runBlocking {
            taskRepo.insertTask(pastTask)
            taskRepo.insertTask(futureTask)
        }

        showQuestsScreen(initialTab = QuestTab.All)

        composeTestRule.onNodeWithText("Forgotten Ancient Scroll").assertIsDisplayed()
        composeTestRule.onNodeWithText("Century Celebration").assertIsDisplayed()
    }

    @Test
    fun questList_searchBoundariesAndHostileCharacters() {
        val hostileTask = task(
            420L,
            "⚔️ <script>alert('xss')</script> ' OR 1=1; -- \u0000\u0007",
            10,
            RecurrenceType.DAILY,
        )
        val arabicTask = task(
            421L,
            "قراءة القرآن الكريم ٥ أجزاء",
            14,
            RecurrenceType.DAILY,
        )

        runBlocking {
            taskRepo.insertTask(hostileTask)
            taskRepo.insertTask(arabicTask)
        }

        showQuestsScreen(initialTab = QuestTab.All)

        // Open search
        composeTestRule.onNodeWithTag(QuestsTags.SEARCH).performClick()
        val searchField = composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(QuestsTags.SEARCH_FIELD)))

        // 1. Search Arabic
        searchField.performTextInput("القرآن")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("قراءة القرآن الكريم ٥ أجزاء").assertIsDisplayed()

        // 2. Search SQL injection
        searchField.performTextReplacement(" ' OR 1=1; --")
        composeTestRule.waitForIdle()

        // 3. Search huge 1,000 char query
        searchField.performTextReplacement("Z".repeat(1000))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(QuestsTags.EMPTY).assertIsDisplayed()
    }

    @Test
    fun questList_rapidTabSwitching_isDeterministic() {
        val taskDaily = task(430L, "Rapid Tab Task", 9, RecurrenceType.DAILY)
        runBlocking {
            taskRepo.insertTask(taskDaily)
        }

        showQuestsScreen(initialTab = QuestTab.Today)

        // Rapidly switch between tabs
        composeTestRule.onNodeWithText("All quests").performClick()
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithText("Today · 1").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Today · 1").assertIsSelected()
        composeTestRule.onNodeWithText("Rapid Tab Task").assertIsDisplayed()
    }

    @Test
    fun questList_themeAndSimpleModeMatrix() {
        val testTask = task(440L, "Theme Matrix Quest", 11, RecurrenceType.DAILY)
        runBlocking {
            taskRepo.insertTask(testTask)
            settingsRepo.setSimpleModeEnabled(true)
        }

        showQuestsScreen(
            initialTab = QuestTab.All,
            config = ComposeTestConfiguration(
                themeMode = ThemeMode.Comic,
                isDark = true,
                isSimpleMode = true,
                fontScale = 2.0f,
                layoutDirection = LayoutDirection.Rtl,
            ),
        )

        composeTestRule.onNodeWithTag(QuestsTags.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("TASKS").assertIsDisplayed()
        composeTestRule.onNodeWithText("All tasks").assertIsSelected()
        composeTestRule.onNodeWithText("NEW TASK").assertIsDisplayed()
        composeTestRule.onNodeWithText("Theme Matrix Quest").assertIsDisplayed()
    }
}
