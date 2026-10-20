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
    fun aCardsButton_ticks_undoes_orRetries() {
        val item = com.pixelquest.app.study.domain.StudyItem(
            "a", "p1", com.pixelquest.app.study.domain.StudyItemKind.ASSIGNMENT, "Lab report", null, null, null, false, "bram", Instant.EPOCH
        )
        fun tick(state: com.pixelquest.app.study.domain.SyncState) = com.pixelquest.app.study.domain.StudyCompletion(
            null, "c1", "a", "p1", "bram", Instant.EPOCH, java.time.LocalDate.of(2026, 10, 21), "UTC", state
        )
        var row by mutableStateOf(ItemRow(item, null, 0, 2, DueState.NONE, canUndo = false, canEdit = false))
        val did = mutableListOf<String>()
        composeTestRule.setContent {
            PixelQuestTheme {
                ItemCard(row, onOpen = {}, onDone = { did += "done" }, onUndo = { did += "undo" }, onRetry = { did += "retry" }, onDiscard = { did += "drop" })
            }
        }
        composeTestRule.onNodeWithTag(StudyTags.itemDone("a"), useUnmergedTree = true).performClick()

        row = row.copy(mine = tick(com.pixelquest.app.study.domain.SyncState.PENDING), doneCount = 1, canUndo = true)
        composeTestRule.onNodeWithTag(StudyTags.itemDone("a"), useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(StudyTags.itemUndo("a"), useUnmergedTree = true).performClick()

        row = row.copy(mine = tick(com.pixelquest.app.study.domain.SyncState.FAILED), doneCount = 0, canUndo = true)
        composeTestRule.onNodeWithTag(StudyTags.itemRetry("a"), useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag(StudyTags.itemDiscard("a"), useUnmergedTree = true).performClick()

        // Sent more than a day ago: final, no button.
        row = row.copy(mine = tick(com.pixelquest.app.study.domain.SyncState.SYNCED), doneCount = 1, canUndo = false)
        composeTestRule.onNodeWithTag(StudyTags.itemUndo("a"), useUnmergedTree = true).assertDoesNotExist()
        assertEquals(listOf("done", "undo", "retry", "drop"), did)
    }

    @Test
    fun anItemsScreen_showsItAll_andWhoFinished_thenClosesWhenItGoes() {
        val zone = java.time.ZoneId.of("UTC")
        val item = com.pixelquest.app.study.domain.StudyItem(
            "a", "p1", com.pixelquest.app.study.domain.StudyItemKind.ASSIGNMENT, "Lab report", "Physics", "Pages 4 to 9",
            Instant.parse("2026-10-23T17:00:00Z"), false, "aria", Instant.EPOCH
        )
        val bramsTick = com.pixelquest.app.study.domain.StudyCompletion(
            7, null, "a", "p1", "bram", Instant.parse("2026-10-21T08:00:00Z"), java.time.LocalDate.of(2026, 10, 21), "UTC"
        )
        val row = ItemRow(item, null, 1, 2, DueState.LATER, canUndo = false, canEdit = true)
        var state by mutableStateOf(StudyPartyUiState(party = party, myId = "aria", isLeader = true, members = members, items = listOf(row), ticks = mapOf("a" to listOf(bramsTick)), loaded = true))
        var done: String? = null
        var closed = false
        composeTestRule.setContent {
            PixelQuestTheme {
                StudyItemContent(state, "a", onNavigateBack = { closed = true }, actions = ItemActions(onDone = { done = it }), zone = zone)
            }
        }
        composeTestRule.onNodeWithText("Lab report").assertExists()
        composeTestRule.onNodeWithTag(StudyTags.ITEM_NOTES).assertTextEquals("Pages 4 to 9")
        composeTestRule.onNodeWithText("Added by @aria_k").assertExists()
        composeTestRule.onNodeWithTag(StudyTags.doneBy("bram")).assertTextEquals("@bram, Wed 21 Oct")
        composeTestRule.onNodeWithTag(StudyTags.itemMenu("a")).assertExists()
        composeTestRule.onNodeWithTag(StudyTags.itemDone("a")).performClick()
        assertEquals("a", done)

        state = state.copy(items = emptyList())
        composeTestRule.waitForIdle()
        assertEquals(true, closed)
    }

    @Test
    fun theTimerCard_startsStops_andLogsMinutesPicked() {
        var state by mutableStateOf(StudyPartyUiState(party = party, studiedToday = 65, loaded = true))
        val did = mutableListOf<String>()
        val actions = TimerActions(onStart = { did += "start" }, onStop = { did += "stop" }, onCancel = { did += "cancel" }, onLogMinutes = { did += "log $it" })
        composeTestRule.setContent {
            PixelQuestTheme { StudyTimerCard(state, actions, clock = { 1_000_000L + 25 * 60_000L + 3_000L }) }
        }
        composeTestRule.onNodeWithTag(StudyTags.TIMER_TODAY).assertTextEquals("65 minutes today")
        composeTestRule.onNodeWithTag(StudyTags.TIMER_START).performClick()

        state = state.copy(timer = com.pixelquest.app.study.data.RunningTimer("p1", 1_000_000L))
        composeTestRule.onNodeWithTag(StudyTags.TIMER_RUNNING).assertTextEquals("Studying: 25:03")
        composeTestRule.onNodeWithTag(StudyTags.TIMER_STOP).performClick()
        composeTestRule.onNodeWithTag(StudyTags.TIMER_CANCEL).performClick()

        composeTestRule.onNodeWithTag(StudyTags.LOG_MINUTES).assertDoesNotExist()
        composeTestRule.onNodeWithTag(StudyTags.minutes(25)).performClick()
        composeTestRule.onNodeWithTag(StudyTags.LOG_MINUTES).performClick()
        assertEquals(listOf("start", "stop", "cancel", "log 25"), did)
    }

    @Test
    fun theScoreboard_ranksEveryone_withTheirBreakdown_andHowPointsWork() {
        val week = java.time.LocalDate.of(2026, 10, 19)
        val brams = com.pixelquest.app.study.domain.WeekScore("p1", "bram", "bram", week, 26, 15, 5, 6, 1, 65)
        val board = listOf(
            BoardLine("bram", "bram", 26, 1, isMe = false, score = brams),
            BoardLine("aria", "aria_k", 0, 2, isMe = true, score = null)
        )
        val state = StudyPartyUiState(party = party, myId = "aria", members = members, tab = PartyTab.SCOREBOARD, weekStart = week, board = board, loaded = true)
        composeTestRule.setContent {
            PixelQuestTheme { StudyPartyContent(state = state, onNavigateBack = {}, onOpenInfo = {}) }
        }
        composeTestRule.onNodeWithText("Mon 19 Oct to Sun 25 Oct").assertExists()
        composeTestRule.onNodeWithTag(StudyTags.boardRow("bram"))
            .assertExists()
        composeTestRule.onNodeWithContentDescription("1. @bram, 26 points 1 item, 5 bonus, 65 minutes", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithContentDescription("2. @aria_k (you), 0 points", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(StudyTags.POINTS_RULES).assertExists()
    }

    @Test
    fun theWinnerBanner_namesTies_andDismisses() {
        val last = java.time.LocalDate.of(2026, 10, 12)
        val winners = listOf(
            com.pixelquest.app.study.domain.WeekWinner("p1", last, "bram", null, 40),
            com.pixelquest.app.study.domain.WeekWinner("p1", last, "aria", null, 40)
        )
        var dismissed = false
        val state = StudyPartyUiState(party = party, myId = "aria", members = members, tab = PartyTab.SCOREBOARD, winnerBanner = winners, pastWeeks = listOf(PastWeek(last, winners, 40)), loaded = true)
        composeTestRule.setContent {
            PixelQuestTheme { StudyPartyContent(state = state, onNavigateBack = {}, onOpenInfo = {}, onDismissWinners = { dismissed = true }) }
        }
        composeTestRule.onNodeWithText("🏆 You won last week!").assertExists()
        composeTestRule.onNodeWithText("@bram, @aria_k, with 40 points each").assertExists()
        composeTestRule.onNodeWithTag(StudyTags.pastWeek(last.toString())).assertExists()
        composeTestRule.onNodeWithTag(StudyTags.WINNER_DISMISS).performClick()
        assertEquals(true, dismissed)
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
