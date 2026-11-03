package com.pixelquest.app.ui.theme

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.LayoutDirection
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.testing.ComposeTestConfiguration
import com.pixelquest.app.testing.MainDispatcherRule
import com.pixelquest.app.testing.setThemedContent
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.QuestUi
import com.pixelquest.app.ui.screens.HeroContent
import com.pixelquest.app.ui.screens.HeroSegment
import com.pixelquest.app.ui.screens.HeroTags
import com.pixelquest.app.ui.screens.HomeContent
import com.pixelquest.app.ui.screens.HomeUiState
import com.pixelquest.app.ui.screens.QuestGroup
import com.pixelquest.app.ui.screens.QuestTab
import com.pixelquest.app.ui.screens.QuestsContent
import com.pixelquest.app.ui.screens.QuestsTags
import com.pixelquest.app.ui.screens.heroUiOf
import com.pixelquest.app.ui.screens.profile.ProfileUiState
import com.pixelquest.app.ui.screens.settings.SettingSwitch
import com.pixelquest.app.ui.screens.settings.SettingsScreenScaffold
import com.pixelquest.app.ui.screens.stats.StatsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.Locale

/**
 * Theme matrix test suite covering the full permutation matrix across all main screens:
 * - Pixel and Comic skins
 * - Light and Dark modes
 * - Accessibility 2.0x font scaling
 * - Right-to-Left (RTL) layout with Arabic locale
 * - Standard and Simple Mode variations
 * - Main screens tested: Home/Today, Quests, Hero (Profile/Stats), and Settings
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ThemeMatrixMainScreensTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleHomeState = HomeUiState(
        heroName = "MatrixHero",
        level = 4,
        avatar = AvatarClass.Hero,
        streakDays = 5,
        doneCount = 2,
        totalCount = 4,
        targetPercent = 70,
        xpToday = 60,
        upNext = listOf(
            QuestUi(1, "Deep Focus", "10:00", QuestCategory.Learning),
            QuestUi(2, "Cardio Sprint", "16:00", QuestCategory.Fitness),
        ),
        doneToday = listOf(
            QuestUi(3, "Morning Water", "07:30", QuestCategory.Health, done = true, xp = 20),
        ),
        isPerfectDay = false,
    )

    private val sampleQuestsGroups = listOf(
        QuestGroup(
            key = "morning",
            title = "Morning",
            quests = listOf(
                QuestUi(10L, "Morning Walk", "08:00", QuestCategory.Fitness),
                QuestUi(11L, "Read Pages", "09:00", QuestCategory.Learning),
            ),
        ),
    )

    private fun createHeroState(simple: Boolean = false): com.pixelquest.app.ui.screens.HeroUiState {
        val today = LocalDate.of(2026, 10, 24)
        val profileState = ProfileUiState(
            profile = UserProfileEntity(
                username = "MatrixHero",
                avatarId = "avatar_mage",
                level = 5,
                totalXp = 1200,
                perfectDaysTowardNextLevel = 3,
            ),
            streak = StreakEntity(currentStreak = 5, longestStreak = 14),
            difficulty = DifficultySettingsEntity(difficultyLevel = DifficultyLevel.MEDIUM, daysRequiredPerLevel = 7),
            totalTasksCompleted = 35,
            activeTasksCount = 3,
            xpLastWeek = 180,
            signedIn = true,
            isSimpleMode = simple,
        )
        val statsState = StatsUiState(
            currentStreak = 5,
            longestStreak = 14,
            overallCompletionRate = 0.75f,
            weeklyTrend = listOf("W-3" to 0.5f, "W-2" to 0.7f, "W-1" to 0.6f, "NOW" to 1.0f),
            isSimpleMode = simple,
        )
        return heroUiOf(profileState, statsState, today)
    }

    // -------------------------------------------------------------------------
    // HOME SCREEN MATRIX TESTS
    // -------------------------------------------------------------------------

    @Test
    fun themeMatrix_homeScreen_pixelLight_ltr() {
        composeTestRule.setThemedContent(
            ComposeTestConfiguration(themeMode = ThemeMode.Pixel, isDark = false, layoutDirection = LayoutDirection.Ltr),
        ) {
            HomeContent(
                state = sampleHomeState,
                greeting = "Salam, MatrixHero",
                onCompleteQuest = {},
                onSkipQuest = {},
                onQuestClick = {},
                onSeeAllQuests = {},
                onNewQuest = {},
                onAvatarClick = {},
                onParties = {},
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(PqSkin.Pixel, PqSkinState.skin)
        composeTestRule.onNodeWithText("Salam, MatrixHero").assertIsDisplayed()
        composeTestRule.onNodeWithText("Deep Focus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cardio Sprint").assertIsDisplayed()
    }

    @Test
    fun themeMatrix_homeScreen_pixelDark_rtl_andFontScale2x() {
        composeTestRule.setThemedContent(
            ComposeTestConfiguration(
                themeMode = ThemeMode.Pixel,
                isDark = true,
                fontScale = 2.0f,
                layoutDirection = LayoutDirection.Rtl,
                locale = Locale("ar"),
            ),
        ) {
            HomeContent(
                state = sampleHomeState,
                greeting = "Salam, MatrixHero",
                onCompleteQuest = {},
                onSkipQuest = {},
                onQuestClick = {},
                onSeeAllQuests = {},
                onNewQuest = {},
                onAvatarClick = {},
                onParties = {},
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(PqSkin.Pixel, PqSkinState.skin)
        composeTestRule.onNodeWithText("Salam, MatrixHero").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cardio Sprint").assertIsDisplayed()
    }

    @Test
    fun themeMatrix_homeScreen_comicLight_ltr_simpleMode() {
        val simpleState = sampleHomeState.copy(isSimpleMode = true)
        composeTestRule.setThemedContent(
            ComposeTestConfiguration(themeMode = ThemeMode.Comic, isDark = false, isSimpleMode = true),
        ) {
            HomeContent(
                state = simpleState,
                greeting = "Salam, MatrixHero",
                onCompleteQuest = {},
                onSkipQuest = {},
                onQuestClick = {},
                onSeeAllQuests = {},
                onNewQuest = {},
                onAvatarClick = {},
                onParties = {},
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(PqSkin.Comic, PqSkinState.skin)
        composeTestRule.onNodeWithText("Salam, MatrixHero").assertIsDisplayed()
        composeTestRule.onNodeWithText("Deep Focus").assertIsDisplayed()
    }

    @Test
    fun themeMatrix_homeScreen_comicDark_rtl_andFontScale2x() {
        composeTestRule.setThemedContent(
            ComposeTestConfiguration(
                themeMode = ThemeMode.Comic,
                isDark = true,
                fontScale = 2.0f,
                layoutDirection = LayoutDirection.Rtl,
                locale = Locale("ar"),
            ),
        ) {
            HomeContent(
                state = sampleHomeState,
                greeting = "Salam, MatrixHero",
                onCompleteQuest = {},
                onSkipQuest = {},
                onQuestClick = {},
                onSeeAllQuests = {},
                onNewQuest = {},
                onAvatarClick = {},
                onParties = {},
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(PqSkin.Comic, PqSkinState.skin)
        composeTestRule.onNodeWithText("Salam, MatrixHero").assertIsDisplayed()
        composeTestRule.onNodeWithText("Deep Focus").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // QUESTS SCREEN MATRIX TESTS
    // -------------------------------------------------------------------------

    @Test
    fun themeMatrix_questsScreen_pixelLight_ltr() {
        composeTestRule.setThemedContent(
            ComposeTestConfiguration(themeMode = ThemeMode.Pixel, isDark = false, layoutDirection = LayoutDirection.Ltr),
        ) {
            QuestsContent(
                tab = QuestTab.Today,
                onTabChange = {},
                todayCount = 2,
                groups = sampleQuestsGroups,
                onToggleQuest = {},
                onQuestClick = {},
                onNewQuest = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(QuestsTags.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("Morning Walk").assertIsDisplayed()
        composeTestRule.onNodeWithText("Read Pages").assertIsDisplayed()
    }

    @Test
    fun themeMatrix_questsScreen_comicDark_rtl_andFontScale2x() {
        composeTestRule.setThemedContent(
            ComposeTestConfiguration(
                themeMode = ThemeMode.Comic,
                isDark = true,
                fontScale = 2.0f,
                layoutDirection = LayoutDirection.Rtl,
                locale = Locale("ar"),
            ),
        ) {
            QuestsContent(
                tab = QuestTab.Today,
                onTabChange = {},
                todayCount = 2,
                groups = sampleQuestsGroups,
                onToggleQuest = {},
                onQuestClick = {},
                onNewQuest = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(QuestsTags.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("Morning Walk").assertIsDisplayed()
        composeTestRule.onNodeWithText("Read Pages").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // HERO SCREEN (PROFILE & STATS) MATRIX TESTS
    // -------------------------------------------------------------------------

    @Test
    fun themeMatrix_heroScreen_pixelDark_fontScale2x_statsSegment() {
        val heroState = createHeroState(simple = false)
        composeTestRule.setThemedContent(
            ComposeTestConfiguration(themeMode = ThemeMode.Pixel, isDark = true, fontScale = 2.0f),
        ) {
            HeroContent(
                state = heroState,
                segment = HeroSegment.Stats,
                onSegmentChange = {},
                onSettings = {},
                onChangeAvatar = {},
                onDifficulty = {},
                onQuestHistory = {},
                onLevelHistory = {},
                onLeaderboard = {},
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(PqSkin.Pixel, PqSkinState.skin)
        composeTestRule.onNodeWithTag(HeroTags.HEATMAP).assertIsDisplayed()
        composeTestRule.onNodeWithText("QUEST ACTIVITY").assertIsDisplayed()
        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasTestTag(HeroTags.WEEKLY))
        composeTestRule.onNodeWithTag(HeroTags.WEEKLY).assertIsDisplayed()
        composeTestRule.onNodeWithText("WEEKLY COMPLETION").assertIsDisplayed()
    }

    @Test
    fun themeMatrix_heroScreen_comicLight_rtl_simpleMode() {
        val heroState = createHeroState(simple = true)
        composeTestRule.setThemedContent(
            ComposeTestConfiguration(
                themeMode = ThemeMode.Comic,
                isDark = false,
                isSimpleMode = true,
                layoutDirection = LayoutDirection.Rtl,
                locale = Locale("ar"),
            ),
        ) {
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
        composeTestRule.waitForIdle()

        assertEquals(PqSkin.Comic, PqSkinState.skin)
        composeTestRule.onNodeWithText("PROFILE").assertIsDisplayed()
        composeTestRule.onNodeWithText("MATRIXHERO").assertIsDisplayed()
        composeTestRule.onNodeWithText("TASKS DONE").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // SETTINGS SCREEN MATRIX TESTS
    // -------------------------------------------------------------------------

    @Test
    fun themeMatrix_settingsScreen_pixelLight_rtl() {
        composeTestRule.setThemedContent(
            ComposeTestConfiguration(
                themeMode = ThemeMode.Pixel,
                isDark = false,
                layoutDirection = LayoutDirection.Rtl,
                locale = Locale("ar"),
            ),
        ) {
            SettingsScreenScaffold(
                onNavigateBack = {},
                notificationsSection = {
                    SettingSwitch("Notifications", "Reminders and nudges", true, {})
                },
                appearanceSection = {
                    SettingSwitch("Sound effects", null, true, {})
                    SettingSwitch("Haptics", null, true, {})
                },
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(PqSkin.Pixel, PqSkinState.skin)
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sound effects").assertIsDisplayed()
        composeTestRule.onNodeWithText("Haptics").assertIsDisplayed()
    }

    @Test
    fun themeMatrix_settingsScreen_comicDark_fontScale2x() {
        composeTestRule.setThemedContent(
            ComposeTestConfiguration(themeMode = ThemeMode.Comic, isDark = true, fontScale = 2.0f),
        ) {
            SettingsScreenScaffold(
                onNavigateBack = {},
                notificationsSection = {
                    SettingSwitch("Notifications", "Reminders and nudges", true, {})
                },
                appearanceSection = {
                    SettingSwitch("Sound effects", null, true, {})
                    SettingSwitch("Reduce motion", null, false, {})
                },
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(PqSkin.Comic, PqSkinState.skin)
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sound effects").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reduce motion").assertIsDisplayed()
    }
}
