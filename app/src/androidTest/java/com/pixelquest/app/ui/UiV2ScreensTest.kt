package com.pixelquest.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.QuestUi
import com.pixelquest.app.ui.navigation.PqBottomBar
import com.pixelquest.app.ui.navigation.PqNavTags
import com.pixelquest.app.ui.navigation.PqTab
import com.pixelquest.app.ui.screens.HomeContent
import com.pixelquest.app.ui.screens.HomeTags
import com.pixelquest.app.ui.screens.HomeUiState
import com.pixelquest.app.ui.screens.QuestGroup
import com.pixelquest.app.ui.screens.QuestTab
import com.pixelquest.app.ui.screens.QuestsContent
import com.pixelquest.app.ui.theme.PqSkin
import com.pixelquest.app.ui.theme.PqSkinState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * UI v2 on a device (docs/ui-v2.md): the bottom bar's tabs and trophy in both skins, a quest done
 * from Home, and Quests' lists. Robolectric covers the rest; this is the same, drawn for real.
 */
class UiV2ScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun resetSkin() {
        PqSkinState.skin = PqSkin.Comic
    }

    private fun theBar_switchesTabs_andTheTrophyOpensTheLeaderboard(skin: PqSkin) {
        PqSkinState.skin = skin
        var screen by mutableStateOf("HOME")
        composeTestRule.setContent {
            Column(Modifier.fillMaxSize()) {
                Text("SCREEN_$screen", Modifier.weight(1f))
                PqBottomBar(
                    current = PqTab.entries.firstOrNull { it.name.uppercase() == screen },
                    onSelect = { screen = it.name.uppercase() },
                    onCenterClick = { screen = "LEADERBOARD" },
                )
            }
        }
        for (tab in listOf(PqTab.Quests, PqTab.Deen, PqTab.Hero, PqTab.Home)) {
            composeTestRule.onNodeWithTag(PqNavTags.tab(tab)).performClick()
            composeTestRule.onNodeWithText("SCREEN_${tab.name.uppercase()}").assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag(PqNavTags.TROPHY).performClick()
        composeTestRule.onNodeWithText("SCREEN_LEADERBOARD").assertIsDisplayed()
    }

    @Test
    fun comic_theBarWorks() = theBar_switchesTabs_andTheTrophyOpensTheLeaderboard(PqSkin.Comic)

    @Test
    fun pixel_theBarWorks() = theBar_switchesTabs_andTheTrophyOpensTheLeaderboard(PqSkin.Pixel)

    @Test
    fun home_aQuestsCircle_completesIt() {
        val read = QuestUi(2, "Read 10 Pages", "21:00", QuestCategory.Learning)
        var completed: QuestUi? = null
        composeTestRule.setContent {
            HomeContent(
                state = HomeUiState(
                    heroName = "Aria", level = 1, avatar = AvatarClass.Hero, streakDays = 0, doneCount = 0, totalCount = 1,
                    targetPercent = 70, xpToday = 0, upNext = listOf(read), doneToday = emptyList(), isPerfectDay = false,
                ),
                greeting = "Good morning",
                onCompleteQuest = { completed = it }, onSkipQuest = {}, onQuestClick = {}, onSeeAllQuests = {},
                onNewQuest = {}, onAvatarClick = {}, onParties = {},
            )
        }
        composeTestRule.onNodeWithText("Read 10 Pages").assertIsDisplayed()
        composeTestRule.onNode(isToggleable() and hasAnyAncestor(hasTestTag(HomeTags.quest(2)))).performClick()
        assertEquals(read, completed)
    }

    @Test
    fun quests_listTodays_andTheHistory() {
        var tab by mutableStateOf(QuestTab.Today)
        composeTestRule.setContent {
            QuestsContent(
                tab = tab, onTabChange = { tab = it }, todayCount = 1,
                groups = if (tab == QuestTab.History) {
                    listOf(QuestGroup("d", "Today", listOf(QuestUi(5, "Read 20 Pages", null, QuestCategory.Learning, done = true, xp = 25))))
                } else {
                    listOf(QuestGroup("m", "Morning · 1 left", listOf(QuestUi(1, "Morning Pushups", "07:00", QuestCategory.Fitness))))
                },
                onToggleQuest = {}, onQuestClick = {}, onNewQuest = {},
            )
        }
        composeTestRule.onNodeWithText("Morning Pushups").assertIsDisplayed()
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithText("Read 20 Pages").assertIsDisplayed()
    }
}
