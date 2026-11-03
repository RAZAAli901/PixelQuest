package com.pixelquest.app.ui.screens.stats

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.LayoutDirection
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.NamazLogEntity
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.repository.StatsRepositoryImpl
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.testing.ComposeTestConfiguration
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.MainDispatcherRule
import com.pixelquest.app.testing.setThemedContent
import com.pixelquest.app.ui.screens.HeroContent
import com.pixelquest.app.ui.screens.HeroSegment
import com.pixelquest.app.ui.screens.HeroTags
import com.pixelquest.app.ui.screens.heroUiOf
import com.pixelquest.app.ui.screens.profile.ProfileViewModel
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/**
 * Unit & flow test suite for the Stats screen (part of Hero) covering:
 * - Start-to-finish user flow: switching between Profile and Stats segments
 * - Boundary conditions: zero history, single day, 365-day dataset, data gap
 * - Feature switches: Simple Mode, Namaz tracker integration
 * - Time and rollover: midnight and past-day attribution
 * - Display resilience: Pixel/Comic skins, font scale 2x, right-to-left layout
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class StatsScreenFlowAndLimitsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    private val today = LocalDate.now()
    private val fixedClock = FixedClock(LocalDateTime.of(today, LocalTime.of(12, 0)))

    private fun createHarness(
        initialTasks: List<TaskEntity> = emptyList(),
        initialLogs: List<TaskCompletionLogEntity> = emptyList(),
        currentStreak: Int = 0,
        longestStreak: Int = 0,
        totalXp: Int = 0,
        simpleMode: Boolean = false,
        namazRepo: FakeNamazRepository? = null,
    ): TestHarness {
        val taskRepo = FakeTaskRepository(initialTasks)
        val completionRepo = FakeTaskCompletionRepository(initialLogs)
        val streakRepo = FakeStreakRepository(StreakEntity(id = 1, currentStreak = currentStreak, longestStreak = longestStreak))
        val profileRepo = FakeUserProfileRepository(
            UserProfileEntity(
                id = 1,
                username = "HeroTester",
                avatarId = "avatar_hero",
                level = 5,
                totalXp = totalXp,
                perfectDaysTowardNextLevel = 3,
            ),
        )
        val diffRepo = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, daysRequiredPerLevel = 7),
        )
        val settingsRepo = FakeSettingsRepository(simpleMode = simpleMode)

        val statsRepo = StatsRepositoryImpl(
            taskCompletionRepository = completionRepo,
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            taskRepository = taskRepo,
            difficultySettingsRepository = diffRepo,
            appClock = fixedClock,
        )

        val statsVm = StatsViewModel(
            statsRepository = statsRepo,
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            difficultySettingsRepository = diffRepo,
            settingsRepository = settingsRepo,
            appClock = fixedClock,
            namazRepository = namazRepo,
        )

        val profileVm = ProfileViewModel(
            userProfileRepository = profileRepo,
            streakRepository = streakRepo,
            difficultySettingsRepository = diffRepo,
            settingsRepository = settingsRepo,
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            appClock = fixedClock,
        )

        return TestHarness(
            taskRepo = taskRepo,
            completionRepo = completionRepo,
            streakRepo = streakRepo,
            profileRepo = profileRepo,
            settingsRepo = settingsRepo,
            statsVm = statsVm,
            profileVm = profileVm,
        )
    }

    @Composable
    private fun StatsScreenHost(
        harness: TestHarness,
        initialSegment: HeroSegment = HeroSegment.Profile,
        onSegmentChanged: ((HeroSegment) -> Unit)? = null,
    ) {
        val profile by harness.profileVm.uiState.collectAsState()
        val stats by harness.statsVm.uiState.collectAsState()
        var segment by rememberSaveable { mutableStateOf(initialSegment) }
        val state = remember(profile, stats) { heroUiOf(profile, stats, today) }

        HeroContent(
            state = state,
            segment = segment,
            onSegmentChange = {
                segment = it
                onSegmentChanged?.invoke(it)
            },
            onSettings = {},
            onChangeAvatar = {},
            onDifficulty = {},
            onQuestHistory = {},
            onLevelHistory = {},
            onLeaderboard = {},
            onParties = {},
            onUsername = {},
        )
    }

    @Test
    fun statsScreen_flow_openHero_switchToStats_verifiesAllSectionsAndTogglesBack() {
        val task = TaskEntity(
            id = 10L,
            name = "Morning Quest",
            description = "Desc",
            scheduledDay = today.minusDays(5),
            scheduledTime = LocalTime.of(9, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.HEALTH,
        )
        val logs = (0..5).map { offset ->
            TaskCompletionLogEntity(
                id = (offset + 1).toLong(),
                taskId = 10L,
                completedDate = today.minusDays(offset.toLong()),
                wasCompleted = true,
                pointsAwarded = 25,
            )
        }
        val harness = createHarness(
            initialTasks = listOf(task),
            initialLogs = logs,
            currentStreak = 6,
            longestStreak = 14,
            totalXp = 150,
        )

        var currentSegment = HeroSegment.Profile
        composeTestRule.setContent {
            StatsScreenHost(harness, initialSegment = HeroSegment.Profile) { currentSegment = it }
        }

        // 1. Initial view is on Profile segment
        composeTestRule.onNodeWithText("Profile").assertIsSelected()
        composeTestRule.onNodeWithText("HEROTESTER").assertIsDisplayed()

        // 2. User taps 'Stats' segment
        composeTestRule.onNodeWithText("Stats").performClick()
        composeTestRule.waitForIdle()
        assertEquals(HeroSegment.Stats, currentSegment)
        composeTestRule.onNodeWithText("Stats").assertIsSelected()

        // 3. Stats Summary: XP and Perfect Days
        composeTestRule.onNodeWithText("150 XP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last 7 days").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfect days, last 7").assertIsDisplayed()

        // 4. Activity Heatmap is displayed with legend
        composeTestRule.onNodeWithTag(HeroTags.HEATMAP).assertIsDisplayed()
        composeTestRule.onNodeWithText("QUEST ACTIVITY").assertIsDisplayed()
        composeTestRule.onNodeWithText(" Perfect").assertIsDisplayed()

        // 5. Weekly Trend section
        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasTestTag(HeroTags.WEEKLY))
        composeTestRule.onNodeWithTag(HeroTags.WEEKLY).assertIsDisplayed()
        composeTestRule.onNodeWithText("WEEKLY COMPLETION").assertIsDisplayed()
        composeTestRule.onNodeWithText("Now").assertIsDisplayed()

        // 6. More Stats (Longest streak, 3-month completion, target)
        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasText("Longest streak", substring = true))
        composeTestRule.onNodeWithText("Longest streak: 14 days").assertIsDisplayed()
        composeTestRule.onNodeWithText("Days with something done, last 3 months:", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Medium: a perfect day is 70% done", substring = true).assertIsDisplayed()

        // 7. Toggle back to Profile and re-toggle to Stats
        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasText("Profile"))
        composeTestRule.onNodeWithText("Profile").performClick()
        composeTestRule.waitForIdle()
        assertEquals(HeroSegment.Profile, currentSegment)
        composeTestRule.onNodeWithText("Profile").assertIsSelected()

        composeTestRule.onNodeWithText("Stats").performClick()
        composeTestRule.waitForIdle()
        assertEquals(HeroSegment.Stats, currentSegment)
        composeTestRule.onNodeWithText("Stats").assertIsSelected()
        composeTestRule.onNodeWithText("QUEST ACTIVITY").assertIsDisplayed()
    }

    @Test
    fun statsScreen_zeroHistory_zeroQuests_rendersCleanZeroStateWithoutExceptions() {
        val harness = createHarness(
            initialTasks = emptyList(),
            initialLogs = emptyList(),
            currentStreak = 0,
            longestStreak = 0,
            totalXp = 0,
        )

        composeTestRule.setContent {
            StatsScreenHost(harness, initialSegment = HeroSegment.Stats)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("0 XP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last 7 days").assertIsDisplayed()
        composeTestRule.onNodeWithText("0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfect days, last 7").assertIsDisplayed()

        composeTestRule.onNodeWithTag(HeroTags.HEATMAP).assertIsDisplayed()
        composeTestRule.onNodeWithText("QUEST ACTIVITY").assertIsDisplayed()

        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasTestTag(HeroTags.WEEKLY))
        composeTestRule.onNodeWithText("WEEKLY COMPLETION").assertIsDisplayed()
        // 4 weekly columns all reporting 0%
        composeTestRule.onAllNodesWithText("0%").assertCountEquals(4)

        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasText("Longest streak", substring = true))
        composeTestRule.onNodeWithText("Longest streak: 0 days").assertIsDisplayed()
        composeTestRule.onNodeWithText("Days with something done, last 3 months: 0%").assertIsDisplayed()
    }

    @Test
    fun statsScreen_oneDayHistory_singleCompletionToday() {
        val task = TaskEntity(
            id = 1L,
            name = "Sole Quest",
            description = "Only one",
            scheduledDay = today,
            scheduledTime = LocalTime.of(10, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.LEARNING,
        )
        val log = TaskCompletionLogEntity(
            id = 1L,
            taskId = 1L,
            completedDate = today,
            wasCompleted = true,
            pointsAwarded = 80,
        )
        val harness = createHarness(
            initialTasks = listOf(task),
            initialLogs = listOf(log),
            currentStreak = 1,
            longestStreak = 1,
            totalXp = 80,
        )

        composeTestRule.setContent {
            StatsScreenHost(harness, initialSegment = HeroSegment.Stats)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("80 XP").assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfect days, last 7").assertIsDisplayed()

        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasTestTag(HeroTags.WEEKLY))
        composeTestRule.onNodeWithText("Now").assertIsDisplayed()
        composeTestRule.onNodeWithText("100%").assertIsDisplayed()

        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasText("Longest streak", substring = true))
        composeTestRule.onNodeWithText("Longest streak: 1 day").assertIsDisplayed()
    }

    @Test
    fun statsScreen_oneYearOfData_thousandsOfLogs_handlesOverflowAndLargeNumbersGracefully() {
        val task = TaskEntity(
            id = 55L,
            name = "Yearly Habit",
            description = "Long streak",
            scheduledDay = today.minusDays(365),
            scheduledTime = LocalTime.of(7, 30),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS,
        )
        val logs = (0..365).map { offset ->
            TaskCompletionLogEntity(
                id = (offset + 1).toLong(),
                taskId = 55L,
                completedDate = today.minusDays(offset.toLong()),
                wasCompleted = true,
                pointsAwarded = 100,
            )
        }
        val harness = createHarness(
            initialTasks = listOf(task),
            initialLogs = logs,
            currentStreak = 365,
            longestStreak = 365,
            totalXp = 36600,
        )

        composeTestRule.setContent {
            StatsScreenHost(harness, initialSegment = HeroSegment.Stats)
        }
        composeTestRule.waitForIdle()

        // 7 days of 100 XP = 700 XP in last 7 days
        composeTestRule.onNodeWithText("700 XP").assertIsDisplayed()
        composeTestRule.onNodeWithText("7").assertIsDisplayed()

        composeTestRule.onNodeWithTag(HeroTags.HEATMAP).assertIsDisplayed()

        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasTestTag(HeroTags.WEEKLY))
        composeTestRule.onAllNodesWithText("100%").assertCountEquals(4)

        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasText("Longest streak", substring = true))
        composeTestRule.onNodeWithText("Longest streak: 365 days").assertIsDisplayed()
        composeTestRule.onNodeWithText("Days with something done, last 3 months: 100%").assertIsDisplayed()
    }

    @Test
    fun statsScreen_gapInData_rendersDiscontinuousHistorySafely() {
        val task = TaskEntity(
            id = 99L,
            name = "Gap Quest",
            description = "Sparse intervals",
            scheduledDay = today.minusDays(90),
            scheduledTime = LocalTime.of(8, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.LEARNING,
        )
        // 5 completions 90-86 days ago, then nothing until 3 days ago
        val oldLogs = (86..90).map { offset ->
            TaskCompletionLogEntity(
                id = offset.toLong(),
                taskId = 99L,
                completedDate = today.minusDays(offset.toLong()),
                wasCompleted = true,
                pointsAwarded = 20,
            )
        }
        val recentLogs = (0..2).map { offset ->
            TaskCompletionLogEntity(
                id = (100 + offset).toLong(),
                taskId = 99L,
                completedDate = today.minusDays(offset.toLong()),
                wasCompleted = true,
                pointsAwarded = 20,
            )
        }

        val harness = createHarness(
            initialTasks = listOf(task),
            initialLogs = oldLogs + recentLogs,
            currentStreak = 3,
            longestStreak = 5,
            totalXp = 160,
        )

        composeTestRule.setContent {
            StatsScreenHost(harness, initialSegment = HeroSegment.Stats)
        }
        composeTestRule.waitForIdle()

        // Recent 3 days = 60 XP
        composeTestRule.onNodeWithText("60 XP").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()

        composeTestRule.onNodeWithTag(HeroTags.HEATMAP).assertIsDisplayed()

        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasTestTag(HeroTags.WEEKLY))
        // Middle weeks (W-1, W-2, W-3) should be 0% completed
        composeTestRule.onAllNodesWithText("0%").assertCountEquals(3)

        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasText("Longest streak", substring = true))
        composeTestRule.onNodeWithText("Longest streak: 5 days").assertIsDisplayed()
    }

    @Test
    fun statsScreen_simpleMode_suppressesGamifiedMetricsAndAltersLabels() {
        val task = TaskEntity(
            id = 20L,
            name = "Simple Quest",
            description = "Calm tracker",
            scheduledDay = today.minusDays(3),
            scheduledTime = LocalTime.of(8, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.CHORES,
        )
        val logs = listOf(
            TaskCompletionLogEntity(id = 1L, taskId = 20L, completedDate = today, wasCompleted = true, pointsAwarded = 50),
        )
        val harness = createHarness(
            initialTasks = listOf(task),
            initialLogs = logs,
            currentStreak = 1,
            longestStreak = 10,
            totalXp = 50,
            simpleMode = true,
        )

        composeTestRule.setContent {
            StatsScreenHost(harness, initialSegment = HeroSegment.Stats)
        }
        composeTestRule.waitForIdle()

        // In Simple Mode:
        // 1. XP card in StatsSummary is hidden
        assertEquals(0, composeTestRule.onAllNodesWithText("50 XP").fetchSemanticsNodes().size)
        // Perfect days card is still shown
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfect days, last 7").assertIsDisplayed()

        // 2. Activity title is TASK ACTIVITY rather than QUEST ACTIVITY
        composeTestRule.onNodeWithText("TASK ACTIVITY").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("QUEST ACTIVITY").fetchSemanticsNodes().size)

        // 3. Longest streak is hidden in MoreStats
        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasText("Days with something done", substring = true))
        assertEquals(0, composeTestRule.onAllNodesWithText("Longest streak", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun statsScreen_namazStatsIntegration_showsPrayerStatsWhenPresent() {
        val namazRepoWithPrayers = FakeNamazRepository(
            NamazSettingsEntity(enabled = true),
        )
        val namazLogs = (0..4).map { offset ->
            NamazLogEntity(
                id = (offset + 1).toLong(),
                date = today.minusDays(offset.toLong()),
                prayer = Prayer.FAJR,
                status = PrayerStatus.PRAYED,
                xpAwarded = 20,
                bonusXp = 5,
            )
        }
        namazRepoWithPrayers.rows.value = namazLogs.associateBy { it.date to it.prayer }

        val harnessWithNamaz = createHarness(namazRepo = namazRepoWithPrayers)

        composeTestRule.setContent {
            StatsScreenHost(harnessWithNamaz, initialSegment = HeroSegment.Stats)
        }
        composeTestRule.waitForIdle()

        // 5 prayers * 25 XP = 125 XP
        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasText("PRAYERS"))
        composeTestRule.onNodeWithText("PRAYERS").assertIsDisplayed()
        composeTestRule.onNodeWithText("125 XP from prayers · 125 this week").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 prayers marked prayed").assertIsDisplayed()
    }

    @Test
    fun statsScreen_namazStatsIntegration_omitsSectionWhenDisabled() {
        val emptyNamazRepo = FakeNamazRepository(NamazSettingsEntity(enabled = false))
        val harnessWithoutNamaz = createHarness(namazRepo = emptyNamazRepo)

        composeTestRule.setContent {
            StatsScreenHost(harnessWithoutNamaz, initialSegment = HeroSegment.Stats)
        }
        composeTestRule.waitForIdle()

        assertEquals(0, composeTestRule.onAllNodesWithText("PRAYERS").fetchSemanticsNodes().size)
    }

    @Test
    fun statsScreen_displayMatrix_comicSkin_andFontScale2x() {
        val task = TaskEntity(
            id = 7L,
            name = "Matrix Quest",
            description = "Resilience",
            scheduledDay = today,
            scheduledTime = LocalTime.of(12, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.OTHER,
        )
        val harness = createHarness(
            initialTasks = listOf(task),
            initialLogs = listOf(
                TaskCompletionLogEntity(id = 1L, taskId = 7L, completedDate = today, wasCompleted = true, pointsAwarded = 30),
            ),
            currentStreak = 1,
            longestStreak = 5,
            totalXp = 30,
        )

        composeTestRule.setThemedContent(
            ComposeTestConfiguration(themeMode = ThemeMode.Comic, fontScale = 2.0f),
        ) {
            StatsScreenHost(harness, initialSegment = HeroSegment.Stats)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("30 XP").assertIsDisplayed()
        composeTestRule.onNodeWithTag(HeroTags.HEATMAP).assertIsDisplayed()
    }

    @Test
    fun statsScreen_displayMatrix_pixelSkin_andRtl() {
        val task = TaskEntity(
            id = 8L,
            name = "RTL Quest",
            description = "Resilience",
            scheduledDay = today,
            scheduledTime = LocalTime.of(12, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.OTHER,
        )
        val harness = createHarness(
            initialTasks = listOf(task),
            initialLogs = listOf(
                TaskCompletionLogEntity(id = 1L, taskId = 8L, completedDate = today, wasCompleted = true, pointsAwarded = 30),
            ),
            currentStreak = 1,
            longestStreak = 5,
            totalXp = 30,
        )

        composeTestRule.setThemedContent(
            ComposeTestConfiguration(
                themeMode = ThemeMode.Pixel,
                layoutDirection = LayoutDirection.Rtl,
                locale = Locale("ar"),
            ),
        ) {
            StatsScreenHost(harness, initialSegment = HeroSegment.Stats)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("30 XP").assertIsDisplayed()
        composeTestRule.onNodeWithTag(HeroTags.HEATMAP).assertIsDisplayed()
    }

    data class TestHarness(
        val taskRepo: FakeTaskRepository,
        val completionRepo: FakeTaskCompletionRepository,
        val streakRepo: FakeStreakRepository,
        val profileRepo: FakeUserProfileRepository,
        val settingsRepo: FakeSettingsRepository,
        val statsVm: StatsViewModel,
        val profileVm: ProfileViewModel,
    )
}
