package com.pixelquest.app.study.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import com.pixelquest.app.islamic.ui.IslamicReminders
import com.pixelquest.app.islamic.ui.IslamicSettingsContent
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyTab
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.quran.ui.reader.ReadingSettings
import com.pixelquest.app.study.domain.StudyCompletion
import com.pixelquest.app.study.domain.StudyItem
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.study.domain.WeekScore
import com.pixelquest.app.study.domain.WeekWinner
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.tips.LocalTipsStore
import com.pixelquest.app.ui.tips.MemoryTipsStore
import com.pixelquest.app.ui.tips.Tip
import com.pixelquest.app.ui.tips.FirstRunTip
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * The accessibility pass on every screen Phase E and F added: on each, every control TalkBack can
 * press says what it is (a label, its text, or a description), and is a 48dp target at least, at
 * normal and at large text. The study party's four tabs, an item's screen, the Add item sheet, the
 * timer, the Home card, Settings' Islamic section and the first-run tips.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class NewScreensAccessibilityTest(private val fontScale: Float) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "fontScale={0}")
        fun scales() = listOf(arrayOf<Any>(1f), arrayOf<Any>(2f))
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    private val week = LocalDate.of(2026, 10, 19)
    private val party = Party("p1", "Physics group", "aria", 10, null, null, null, Instant.EPOCH, memberCount = 2, myStatus = MemberStatus.ACTIVE, type = PartyType.STUDY)
    private val members = listOf(
        PartyMember("p1", "aria", "aria_k", PartyRole.LEADER, MemberStatus.ACTIVE, null, null),
        PartyMember("p1", "bram", "bram", PartyRole.MEMBER, MemberStatus.ACTIVE, null, null)
    )
    private val quiz = StudyItem("q", "p1", StudyItemKind.QUIZ, "Waves quiz", "Physics", "Pages 4 to 9", Instant.parse("2026-10-23T09:00:00Z"), true, "aria", Instant.EPOCH)
    private val essay = StudyItem("e", "p1", StudyItemKind.ASSIGNMENT, "Lab report", null, null, null, false, "bram", Instant.EPOCH)
    private val tick = StudyCompletion(1, "c", "e", "p1", "aria", Instant.EPOCH, week, "UTC", SyncState.SYNCED)
    private val rows = listOf(
        ItemRow(quiz, null, 0, 2, DueState.SOON, canUndo = false, canEdit = true),
        ItemRow(essay, tick, 1, 2, DueState.NONE, canUndo = true, canEdit = true)
    )
    private val state = StudyPartyUiState(
        party = party, myId = "aria", isLeader = true, members = members, items = rows, thisWeek = rows,
        weekSections = weekSectionsOf(rows, java.time.ZoneId.of("UTC")), weekProgress = WeekProgress(1, 2), pinned = rows.take(1),
        weekStart = week, today = week.plusDays(2), ticks = mapOf("e" to listOf(tick)),
        board = boardOf(listOf(WeekScore("p1", "aria", "aria_k", week, 10, 10, 0, 0, 1, 0)), members, "aria"),
        pastWeeks = listOf(PastWeek(week.minusWeeks(1), listOf(WeekWinner("p1", week.minusWeeks(1), "bram", "bram", 30)), 20)),
        winnerBanner = listOf(WeekWinner("p1", week.minusWeeks(1), "bram", "bram", 30)),
        activity = listOf(ActivityLine.Finished("aria", Instant.EPOCH, "Lab report")),
        loaded = true
    )

    /** Every control that can be pressed, in every window: its label and size. */
    private fun problems(): List<String> {
        val density = composeTestRule.density
        val min = with(density) { 48.dp.toPx() } - 1f
        return composeTestRule.onAllNodes(isRoot(), useUnmergedTree = false).fetchSemanticsNodes().flatMap { root -> walk(root) }
            .filter { it.config.getOrNull(SemanticsActions.OnClick) != null }
            .mapNotNull { node ->
                val said = listOfNotNull(
                    node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(),
                    node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text },
                    node.config.getOrNull(SemanticsActions.OnClick)?.label,
                    node.config.getOrNull(SemanticsProperties.StateDescription)
                ).joinToString(" ").trim()
                val tag = node.config.getOrNull(SemanticsProperties.TestTag) ?: "node ${node.id}"
                when {
                    said.isEmpty() -> "$tag says nothing"
                    // What there is to press: its laid-out size, or its touch area where that's larger (a control
                    // drawn smaller is given 48dp, minimumInteractiveComponentSize). The touch area is clipped to the
                    // screen, so one scrolled off has only its size to go by.
                    maxOf(node.size.height.toFloat(), node.touchBoundsInRoot.height) < min -> "$tag ('$said') is ${node.size.height}px high, under 48dp"
                    maxOf(node.size.width.toFloat(), node.touchBoundsInRoot.width) < min -> "$tag ('$said') is ${node.size.width}px wide, under 48dp"
                    else -> null
                }
            }
    }

    private fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap { walk(it) }

    private fun check(content: @Composable () -> Unit, also: () -> Unit = {}) {
        composeTestRule.setContent {
            val base = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(base.density, fontScale)) {
                PixelQuestTheme { content() }
            }
        }
        composeTestRule.waitForIdle()
        also()
        assertEquals(emptyList<String>(), problems())
    }

    @Test
    fun everyTab_ofTheStudyParty() {
        val tab = androidx.compose.runtime.mutableStateOf(PartyTab.THIS_WEEK)
        check({ StudyPartyContent(state.copy(tab = tab.value), onNavigateBack = {}, onOpenInfo = {}) })
        for (next in listOf(PartyTab.ALL_ITEMS, PartyTab.SCOREBOARD, PartyTab.PEOPLE)) {
            tab.value = next
            composeTestRule.waitForIdle()
            assertEquals(next.name, emptyList<String>(), problems())
        }
    }

    @Test
    fun anItemsScreen() = check({ StudyItemContent(state, "q", onNavigateBack = {}) })

    @Test
    fun theAddItemSheetsFields() = check({
        ItemFormContent(ItemForm(kind = StudyItemKind.QUIZ, title = "Waves quiz", dueDay = week.plusDays(2), dueTime = LocalTime.of(9, 0)), week, isLeader = true, onChange = {}, onSave = {})
    })

    @Test
    fun theHomeCard() = check({
        HomeStudyCard(HomeStudy(listOf(HomeDueLine("p1", "Physics group", "q", "Waves quiz", DueState.SOON, quiz.dueAt))), onOpenParty = {}, onOpenParties = {})
    })

    @Test
    fun settingsIslamicSection() = check({
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            IslamicSettingsContent(true, {}, ReadingSettings(), {}, IslamicReminders())
        }
    })

    @Test
    fun theFirstRunTips() = check({
        CompositionLocalProvider(LocalTipsStore provides MemoryTipsStore()) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) { Tip.entries.forEach { FirstRunTip(it) } }
        }
    })
}
