package com.pixelquest.app.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.AvatarTier
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.components.DayStatus
import com.pixelquest.app.ui.screens.profile.ProfileUiState
import com.pixelquest.app.ui.screens.stats.StatsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Hero (docs/ui-v2.md, mocks 2G and 2H): the old Profile and Stats in one tab, with the side menu's
 * Quest history, Leaderboard, Parties and Settings in its list and its gear.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class HeroScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 21) // a Wednesday

    private fun profile(simple: Boolean = false, signedIn: Boolean = false) = ProfileUiState(
        profile = UserProfileEntity(username = "Aria", avatarId = "avatar_mage", level = 6, totalXp = 1240, perfectDaysTowardNextLevel = 2),
        streak = StreakEntity(currentStreak = 3, longestStreak = 12),
        difficulty = DifficultySettingsEntity(difficultyLevel = DifficultyLevel.MEDIUM, daysRequiredPerLevel = 7),
        isSimpleMode = simple,
        totalTasksCompleted = 41,
        activeTasksCount = 5,
        xpLastWeek = 120,
        signedIn = signedIn,
    )

    private val stats = StatsUiState(
        longestStreak = 12,
        overallCompletionRate = 0.64f,
        heatmapStatusMap = mapOf(today to DailyStatus.PERFECT, today.minusDays(1) to DailyStatus.PARTIAL, today.minusDays(8) to DailyStatus.PERFECT),
        weeklyTrend = listOf("W-3" to 0f, "W-2" to 0.5f, "W-1" to 0.25f, "NOW" to 1f),
    )

    private val opened = mutableListOf<String>()
    private var segment = HeroSegment.Profile

    private fun show(state: HeroUiState, start: HeroSegment = HeroSegment.Profile) {
        segment = start
        composeTestRule.setContent {
            var s by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(start) }
            HeroContent(
                state = state, segment = s, onSegmentChange = { s = it; segment = it },
                onSettings = { opened += "settings" }, onChangeAvatar = { opened += "avatar" }, onDifficulty = { opened += "difficulty" },
                onQuestHistory = { opened += "history" }, onLevelHistory = { opened += "levels" }, onLeaderboard = { opened += "leaderboard" },
                onParties = { opened += "parties" },
            )
        }
    }

    @Test
    fun theProfileAndStats_mapToHero() {
        val hero = heroUiOf(profile(), stats, today)
        assertEquals(AvatarClass.Mage, hero.avatar)
        assertEquals("level 6 is silver", AvatarTier.SILVER, hero.tier)
        assertEquals(7, hero.perfectDaysNeeded)
        assertEquals(3, hero.streak)
        assertEquals(41, hero.questsDone)
        assertEquals(true, hero.leaderboardLocked)
        assertEquals(120, hero.stats.xpLast7Days)
        assertEquals("only the last 7 days' perfect ones", 1, hero.stats.perfectDays)
        assertEquals(listOf(0, 50, 25, 100), hero.stats.weekly)
        assertEquals(70, hero.stats.targetPercent)
        assertEquals(64, hero.stats.completionPercent)
        assertEquals(false, heroUiOf(profile(signedIn = true), stats, today).leaderboardLocked)
    }

    @Test
    fun theHeatmap_isMondayFirstWeeks_endingThisWeek() {
        val days = heatmapDays(mapOf(today to DailyStatus.PERFECT, today.minusDays(2) to DailyStatus.MISSED), today, weeks = 2)
        assertEquals(14, days.size)
        // This week starts Monday the 19th: Monday is MISSED, Wednesday (today) PERFECT, the rest to come.
        assertEquals(listOf(DayStatus.Missed, DayStatus.None, DayStatus.Perfect, DayStatus.None, DayStatus.None, DayStatus.None, DayStatus.None), days.takeLast(7))
    }

    @Test
    fun profile_hasTheCard_andTheMenusPlaces() {
        show(heroUiOf(profile(), stats, today))

        composeTestRule.onNodeWithText("HERO").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profile").assertIsSelected()
        composeTestRule.onNodeWithText("ARIA").assertIsDisplayed()
        composeTestRule.onNodeWithText("SILVER").assertIsDisplayed()
        composeTestRule.onNodeWithText("LVL 6 → 7").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 / 7 perfect days").assertIsDisplayed()
        composeTestRule.onNodeWithText("1240").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Quest history").performClick()
        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(hasText("Leaderboard"))
        composeTestRule.onNodeWithText("Locked").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leaderboard").performClick()
        composeTestRule.onNodeWithText("Parties").performClick()
        assertEquals(listOf("settings", "history", "leaderboard", "parties"), opened)
    }

    @Test
    fun stats_hasTheSummary_heatmap_andWeeks() {
        show(heroUiOf(profile(), stats, today), HeroSegment.Stats)

        composeTestRule.onNodeWithText("120 XP").assertIsDisplayed()
        composeTestRule.onNodeWithTag(HeroTags.HEATMAP).assertIsDisplayed()
        composeTestRule.onNodeWithTag(HeroTags.SCREEN).performScrollToNode(androidx.compose.ui.test.hasTestTag(HeroTags.WEEKLY))
        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Now").assertIsDisplayed()
        composeTestRule.onNodeWithText("W-3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profile").performClick()
        assertEquals(HeroSegment.Profile, segment)
    }

    @Test
    fun simpleMode_plainWords_noLevelOrRank_andDifficultyStaysLocked() {
        show(heroUiOf(profile(simple = true), stats, today))

        composeTestRule.onNodeWithText("PROFILE").assertIsDisplayed()
        composeTestRule.onNodeWithText("TASKS DONE").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodes(hasText("LVL 6 → 7")).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodes(hasText("SILVER")).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithText("Difficulty").performClick()
        assertEquals("locked in Simple Mode", emptyList<String>(), opened)
    }
}
