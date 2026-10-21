package com.pixelquest.app.study.ui

import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithText
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyTab
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.domain.StudyItem
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.domain.WeekScore
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/** The study party in each look: every tab draws, its chips and tabs take taps, and the board has its medals. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class StudyLookTest(private val mode: ThemeMode) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun modes() = listOf(ThemeMode.Pixel, ThemeMode.Comic).map { arrayOf<Any>(it) }
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    private val week = LocalDate.of(2026, 10, 19)
    private val party = Party("p1", "Physics group", "aria", 10, null, null, null, Instant.EPOCH, memberCount = 3, myStatus = MemberStatus.ACTIVE, type = PartyType.STUDY)
    private val members = listOf("aria", "bram", "cleo").map { PartyMember("p1", it, it, if (it == "aria") PartyRole.LEADER else PartyRole.MEMBER, MemberStatus.ACTIVE, null, null) }
    private val item = StudyItem("q", "p1", StudyItemKind.QUIZ, "Waves quiz", "Physics", null, null, true, "aria", Instant.EPOCH)
    private val row = ItemRow(item, null, 1, 3, DueState.NONE, canUndo = false, canEdit = true)
    private fun score(user: String, points: Int) = WeekScore("p1", user, user, week, points, points, 0, 0, 1, 0)

    @Test
    fun everyTabDraws_andTakesTaps() {
        var state by mutableStateOf(
            StudyPartyUiState(
                party = party, myId = "aria", isLeader = true, members = members, items = listOf(row), thisWeek = listOf(row),
                weekSections = listOf(WeekSection.NoDate to listOf(row)), pinned = listOf(row), weekStart = week, today = week.plusDays(2),
                board = boardOf(listOf(score("bram", 30), score("aria", 20), score("cleo", 10)), members, "aria"), loaded = true
            )
        )
        var opened: String? = null
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                StudyPartyContent(state = state, onNavigateBack = {}, onOpenInfo = {}, onSelectTab = { state = state.copy(tab = it) }, onOpenItem = { opened = it })
            }
        }
        composeTestRule.onNodeWithTag(StudyTags.pinned("q")).performClick()
        assertEquals("q", opened)
        composeTestRule.onNodeWithTag(StudyTags.minutes(25)).performClick()
        composeTestRule.onNodeWithTag(StudyTags.LOG_MINUTES).assertExists()

        PartyTab.entries.filter { it != PartyTab.CHAT }.forEach { tab ->
            composeTestRule.onNodeWithTag(StudyTags.tabButton(tab)).performScrollTo().performClick()
            composeTestRule.onNodeWithTag(StudyTags.tab(tab)).assertExists()
        }
        composeTestRule.onNodeWithTag(StudyTags.tabButton(PartyTab.SCOREBOARD)).performClick()
        // The first three on their medals, by number.
        for (place in listOf("1", "2", "3")) composeTestRule.onAllNodesWithText(place, useUnmergedTree = true).onFirst().assertExists()
    }

    @Test
    fun inSimpleMode_noPointsAnywhere_andNoSundaySummary() {
        val state = StudyPartyUiState(
            party = party, myId = "aria", members = members, items = listOf(row), thisWeek = listOf(row),
            weekSections = listOf(WeekSection.NoDate to listOf(row)), weekProgress = WeekProgress(0, 1), weekStart = week, today = week,
            tabs = listOf(PartyTab.THIS_WEEK, PartyTab.ALL_ITEMS, PartyTab.PEOPLE), simpleMode = true, loaded = true
        )
        var current by mutableStateOf(state)
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                StudyPartyContent(state = current, onNavigateBack = {}, onOpenInfo = {}, onSelectTab = { current = current.copy(tab = it) })
            }
        }
        composeTestRule.onNodeWithTag(StudyTags.WEEK_SUMMARY).assertExists()
        composeTestRule.onNodeWithTag(StudyTags.WEEK_POINTS).assertDoesNotExist()
        composeTestRule.onNodeWithTag(StudyTags.tabButton(PartyTab.SCOREBOARD)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(StudyTags.tabButton(PartyTab.PEOPLE)).performClick()
        composeTestRule.onNodeWithTag(StudyTags.REMINDERS_DUE).assertExists()
        composeTestRule.onNodeWithTag(StudyTags.REMINDERS_WEEKLY).assertDoesNotExist()
    }

    @Test
    fun medals_areForTheFirstThreeWithPoints() {
        assertEquals(Medal.GOLD, medalFor(1, 10))
        assertEquals(Medal.SILVER, medalFor(2, 5))
        assertEquals(Medal.BRONZE, medalFor(3, 1))
        assertNull(medalFor(4, 50))
        assertNull("no medal for nothing", medalFor(1, 0))
    }
}
