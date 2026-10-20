package com.pixelquest.app.study.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
    fun aCard_saysItsKind_whenItsDue_whosDone_andThePlayersTick() {
        val zone = java.time.ZoneId.of("UTC")
        val quiz = com.pixelquest.app.study.domain.StudyItem(
            "q", "p1", com.pixelquest.app.study.domain.StudyItemKind.QUIZ, "Waves quiz", "Physics", null,
            Instant.parse("2026-10-19T09:00:00Z"), true, "aria", Instant.EPOCH
        )
        val waiting = com.pixelquest.app.study.domain.StudyCompletion(
            null, "c1", "q", "p1", "aria", Instant.EPOCH, java.time.LocalDate.of(2026, 10, 21), "UTC", com.pixelquest.app.study.domain.SyncState.PENDING
        )
        var opened = false
        composeTestRule.setContent {
            PixelQuestTheme {
                ItemCard(ItemRow(quiz, waiting, 2, 5, DueState.OVERDUE, canUndo = true, canEdit = true), onOpen = { opened = true }, zone = zone)
            }
        }
        composeTestRule.onNodeWithText("❓ Quiz", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Physics", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithContentDescription("Pinned by the leader", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(StudyTags.itemDue("q"), useUnmergedTree = true).assertTextContains("Overdue: due Mon 19 Oct", substring = true)
        composeTestRule.onNodeWithText("2 of 5 finished", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(StudyTags.itemMine("q"), useUnmergedTree = true).assertTextEquals("✓ Waiting to send")
        composeTestRule.onNodeWithTag(StudyTags.item("q")).performClick()
        assertEquals(true, opened)
        // No ⋯ menu without anything the player may do.
        composeTestRule.onNodeWithTag(StudyTags.itemMenu("q"), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun aCardsMenu_isThereForItsCreatorOrTheLeader() {
        val item = com.pixelquest.app.study.domain.StudyItem(
            "a", "p1", com.pixelquest.app.study.domain.StudyItemKind.ASSIGNMENT, "Lab report", null, null, null, false, "bram", Instant.EPOCH
        )
        val row = ItemRow(item, null, 0, 2, DueState.NONE, canUndo = false, canEdit = true)
        val state = StudyPartyUiState(party = party, myId = "bram", members = members, items = listOf(row), tab = PartyTab.ALL_ITEMS, loaded = true)
        composeTestRule.setContent {
            PixelQuestTheme { StudyPartyContent(state = state, onNavigateBack = {}, onOpenInfo = {}) }
        }
        composeTestRule.onNodeWithTag(StudyTags.itemMenu("a"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun pinnedItems_stripAcrossTheTop_andOpen() {
        val pinned = com.pixelquest.app.study.domain.StudyItem(
            "q", "p1", com.pixelquest.app.study.domain.StudyItemKind.QUIZ, "Waves quiz", null, null, null, true, "aria", Instant.EPOCH
        )
        val row = ItemRow(pinned, null, 0, 2, DueState.NONE, canUndo = false, canEdit = true)
        var opened: String? = null
        val state = StudyPartyUiState(party = party, myId = "aria", members = members, items = listOf(row), thisWeek = listOf(row), pinned = listOf(row), loaded = true)
        composeTestRule.setContent {
            PixelQuestTheme { StudyPartyContent(state = state, onNavigateBack = {}, onOpenInfo = {}, onOpenItem = { opened = it }) }
        }
        composeTestRule.onNodeWithTag(StudyTags.PINNED).assertExists()
        composeTestRule.onNodeWithTag(StudyTags.pinned("q")).performClick()
        assertEquals("q", opened)
    }

    @Test
    fun theItemForm_picksKindAndDay_showsItsProblem_andThePinOnlyForTheLeader() {
        val today = java.time.LocalDate.of(2026, 10, 21)
        var form by mutableStateOf(ItemForm(kind = com.pixelquest.app.study.domain.StudyItemKind.QUIZ, title = "Waves quiz", triedToSave = true))
        var leader by mutableStateOf(false)
        composeTestRule.setContent {
            PixelQuestTheme { ItemFormContent(form, today, leader, onChange = { form = it }, onSave = {}) }
        }
        composeTestRule.onNodeWithTag(StudyTags.FORM_PROBLEM).assertExists()
        composeTestRule.onNodeWithTag(StudyTags.FORM_PIN).assertDoesNotExist()

        composeTestRule.onNodeWithTag(StudyTags.formDay(today.plusDays(1))).performClick()
        assertEquals(today.plusDays(1), form.dueDay)
        composeTestRule.onNodeWithTag(StudyTags.FORM_PROBLEM).assertDoesNotExist()
        composeTestRule.onNodeWithTag(StudyTags.formTime(java.time.LocalTime.of(9, 0))).performClick()
        assertEquals(java.time.LocalTime.of(9, 0), form.dueTime)

        composeTestRule.onNodeWithTag(StudyTags.formKind(com.pixelquest.app.study.domain.StudyItemKind.ASSIGNMENT)).performClick()
        assertEquals(com.pixelquest.app.study.domain.StudyItemKind.ASSIGNMENT, form.kind)

        leader = true
        composeTestRule.onNodeWithTag(StudyTags.FORM_PIN).performClick()
        assertEquals(true, form.pinned)
    }

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
