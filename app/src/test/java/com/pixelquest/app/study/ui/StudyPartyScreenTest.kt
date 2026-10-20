package com.pixelquest.app.study.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyTab
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** The study party's screen: its four tabs, each showing its own, and People with the leader's way to add. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class StudyPartyScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val party = Party(
        id = "p1", name = "Physics group", leaderId = "aria", maxMembers = 10, milestone = null, milestoneSetAt = null,
        completedAt = null, createdAt = Instant.EPOCH, total = 0, memberCount = 2, myStatus = MemberStatus.ACTIVE, type = PartyType.STUDY
    )
    private val members = listOf(
        PartyMember("p1", "aria", "aria_k", PartyRole.LEADER, MemberStatus.ACTIVE, null, null),
        PartyMember("p1", "bram", "bram", PartyRole.MEMBER, MemberStatus.ACTIVE, null, null)
    )

    @Test
    fun theTabs_switch_andEachShowsItsOwn() {
        var state by mutableStateOf(StudyPartyUiState(party = party, myId = "aria", isLeader = true, members = members, loaded = true))
        composeTestRule.setContent {
            PixelQuestTheme {
                StudyPartyContent(state = state, onNavigateBack = {}, onOpenInfo = {}, onSelectTab = { state = state.copy(tab = it) })
            }
        }
        composeTestRule.onNodeWithTag(StudyTags.tabButton(PartyTab.THIS_WEEK)).assertIsSelected()
        composeTestRule.onNodeWithTag(StudyTags.EMPTY).assertExists()

        composeTestRule.onNodeWithTag(StudyTags.tabButton(PartyTab.PEOPLE)).performClick()
        assertEquals(PartyTab.PEOPLE, state.tab)
        composeTestRule.onNodeWithTag(StudyTags.tab(PartyTab.PEOPLE)).assertExists()
        composeTestRule.onNodeWithTag(StudyTags.ADD_PEOPLE).assertExists()

        state = state.copy(isLeader = false)
        composeTestRule.onNodeWithTag(StudyTags.ADD_PEOPLE).assertDoesNotExist()
    }
}
