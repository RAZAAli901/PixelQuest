package com.pixelquest.app.ui.screens

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.QuestUi
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardAuthState
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardContent
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardUiState
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardView
import com.pixelquest.app.ui.screens.namaz.NamazContent
import com.pixelquest.app.ui.screens.namaz.NamazUiState
import com.pixelquest.app.ui.screens.profile.ProfileUiState
import com.pixelquest.app.ui.screens.settings.SettingsScreenScaffold
import com.pixelquest.app.ui.screens.stats.StatsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * UI smoke tests verifying that each main screen renders its primary layout,
 * navigation headers, and components without crashing under Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class MainScreensSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_rendersSuccessfully() {
        val testState = HomeUiState(
            heroName = "SmokeHero",
            level = 5,
            avatar = AvatarClass.Hero,
            streakDays = 7,
            doneCount = 1,
            totalCount = 3,
            targetPercent = 70,
            xpToday = 50,
            upNext = listOf(
                QuestUi(1, "Morning Meditation", "07:00", QuestCategory.Health),
                QuestUi(2, "Read Quran", "18:00", QuestCategory.Learning),
            ),
            doneToday = listOf(
                QuestUi(3, "Daily Workout", "08:00", QuestCategory.Fitness, done = true, xp = 30),
            ),
            isPerfectDay = false,
        )

        composeTestRule.setContent {
            HomeContent(
                state = testState,
                greeting = "Salam, Hero",
                onCompleteQuest = {},
                onSkipQuest = {},
                onQuestClick = {},
                onSeeAllQuests = {},
                onNewQuest = {},
                onAvatarClick = {},
                onParties = {},
            )
        }

        composeTestRule.onNodeWithText("Salam, Hero").assertIsDisplayed()
        composeTestRule.onNodeWithText("Morning Meditation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Read Quran").assertIsDisplayed()
    }

    @Test
    fun questsScreen_rendersSuccessfully() {
        val testGroup = QuestGroup(
            key = "morning",
            title = "Morning - 1 left",
            quests = listOf(
                QuestUi(id = 101L, title = "Morning Sprint", time = "09:00", category = QuestCategory.Fitness),
            ),
        )

        composeTestRule.setContent {
            QuestsContent(
                tab = QuestTab.Today,
                onTabChange = {},
                todayCount = 1,
                groups = listOf(testGroup),
                onToggleQuest = {},
                onQuestClick = {},
                onNewQuest = {},
            )
        }

        composeTestRule.onNodeWithTag(QuestsTags.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("Morning Sprint").assertIsDisplayed()
    }

    @Test
    fun heroScreen_rendersSuccessfully() {
        val today = LocalDate.of(2026, 10, 23)
        val profileState = ProfileUiState(
            profile = UserProfileEntity(
                username = "PixelWarrior",
                avatarId = "avatar_hero",
                level = 10,
                totalXp = 2500,
                perfectDaysTowardNextLevel = 4,
            ),
            streak = StreakEntity(currentStreak = 12, longestStreak = 25),
            difficulty = DifficultySettingsEntity(difficultyLevel = DifficultyLevel.MEDIUM, daysRequiredPerLevel = 7),
            totalTasksCompleted = 88,
            activeTasksCount = 4,
            xpLastWeek = 320,
            signedIn = true,
        )
        val statsState = StatsUiState(
            longestStreak = 25,
            overallCompletionRate = 0.85f,
            heatmapStatusMap = emptyMap(),
            weeklyTrend = listOf("W-1" to 0.8f, "NOW" to 1.0f),
        )
        val heroState = heroUiOf(profileState, statsState, today)

        composeTestRule.setContent {
            HeroContent(
                state = heroState,
                segment = HeroSegment.Profile,
                onSegmentChange = {},
                onSettings = {},
                onChangeAvatar = {},
                onDifficulty = {},
                onQuestHistory = {},
                onLevelHistory = {},
                onLeaderboard = {},
            )
        }

        composeTestRule.onNodeWithTag(HeroTags.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("PIXELWARRIOR").assertIsDisplayed()
    }

    @Test
    fun leaderboardScreen_rendersSuccessfully() {
        val testEntries = listOf(
            CloudProfileDto(id = "user_alpha", displayName = "ChampionAlpha", currentStreak = 42, level = 15),
            CloudProfileDto(id = "user_beta", displayName = "RunnerBeta", currentStreak = 30, level = 12),
        )
        val uiState = LeaderboardUiState(
            authState = LeaderboardAuthState.SignedInAndOptedIn("user_alpha", "ChampionAlpha"),
            selectedView = LeaderboardView.TOP,
            streakEntries = testEntries,
            levelEntries = testEntries,
            isLoading = false,
        )

        composeTestRule.setContent {
            LeaderboardContent(
                uiState = uiState,
                onTabSelected = {},
                onLoadMore = {},
                onRefresh = {},
                onNavigateToAccount = {},
            )
        }

        composeTestRule.onNodeWithText("LEADERBOARD").assertIsDisplayed()
        composeTestRule.onNodeWithText("ChampionAlpha").assertIsDisplayed()
        composeTestRule.onNodeWithText("RunnerBeta").assertIsDisplayed()
    }

    @Test
    fun namazScreen_rendersSuccessfully() {
        val namazState = NamazUiState(
            loading = false,
            enabled = false,
        )

        composeTestRule.setContent {
            NamazContent(
                state = namazState,
            )
        }

        composeTestRule.onNodeWithText("NAMAZ").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_rendersSuccessfully() {
        composeTestRule.setContent {
            SettingsScreenScaffold(
                accountSection = {
                    Text("Account Settings Content")
                },
            )
        }

        composeTestRule.onNodeWithText("SETTINGS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Account Settings Content").assertIsDisplayed()
    }
}
