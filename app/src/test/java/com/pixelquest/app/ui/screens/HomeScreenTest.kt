package com.pixelquest.app.ui.screens

import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.QuestUi
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.screens.today.TodayTaskItem
import com.pixelquest.app.ui.screens.today.TodayUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Home (docs/ui-v2.md, mocks 2A and 2B): the header, today's progress or the perfect day, and the
 * quests still to do, which a circle completes and a swipe (or TalkBack's action) skips after asking.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val gym = QuestUi(1, "Go to Gym", "17:30", QuestCategory.Fitness, repeatLabel = "Daily", overdue = true)
    private val read = QuestUi(2, "Read 15 Pages", "21:00", QuestCategory.Learning)
    private val pushUps = QuestUi(3, "20 Push-ups", "08:00", QuestCategory.Fitness, done = true, xp = 20)

    private fun state(
        upNext: List<QuestUi> = listOf(gym, read),
        done: List<QuestUi> = listOf(pushUps),
        perfect: Boolean = false,
        simple: Boolean = false,
        total: Int = upNext.size + done.size,
    ) = HomeUiState(
        heroName = "Aria", level = 3, avatar = AvatarClass.Mage, streakDays = 5,
        doneCount = done.size, totalCount = total, targetPercent = 70, xpToday = 20,
        upNext = upNext, doneToday = done, isPerfectDay = perfect, isSimpleMode = simple,
    )

    private val completed = mutableListOf<QuestUi>()
    private val skipped = mutableListOf<QuestUi>()
    private var newQuest = 0

    private fun show(state: HomeUiState, partiesDot: Boolean = false) {
        composeTestRule.setContent {
            HomeContent(
                state = state,
                greeting = "Good evening",
                onCompleteQuest = { completed += it },
                onSkipQuest = { skipped += it },
                onQuestClick = {},
                onSeeAllQuests = {},
                onNewQuest = { newQuest++ },
                onAvatarClick = {},
                onParties = {},
                partiesDot = partiesDot,
            )
        }
    }

    private fun scrollTo(tag: String) {
        composeTestRule.onNodeWithTag(HomeTags.SCREEN).performScrollToNode(hasTestTag(tag))
    }

    /** TalkBack's "Skip for today" action on a quest's row. */
    private fun skipWithTalkBack(id: Long) {
        val actions = composeTestRule.onNodeWithTag(HomeTags.quest(id)).fetchSemanticsNode().config[SemanticsActions.CustomActions]
        composeTestRule.runOnIdle { actions.first { it.label == "Skip for today" }.action() }
        composeTestRule.waitForIdle()
    }

    @Test
    fun theHeader_andTodaysProgress() {
        show(state())

        composeTestRule.onNodeWithText("Good evening").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aria · LVL 3").assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("5 day streak")).assertIsDisplayed()
        composeTestRule.onNodeWithText("QUEST PROGRESS").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 of 3 done · target 70%").assertIsDisplayed()
        composeTestRule.onNodeWithText("+20 XP today").assertIsDisplayed()
    }

    @Test
    fun aQuestsCircle_completesIt() {
        show(state())
        scrollTo(HomeTags.quest(read.id))

        composeTestRule.onNode(isToggleable() and hasAnyAncestor(hasTestTag(HomeTags.quest(read.id)))).performClick()

        assertEquals(listOf(read), completed)
    }

    @Test
    fun skipping_asksFirst_thenSkips() {
        show(state())
        scrollTo(HomeTags.quest(gym.id))

        skipWithTalkBack(gym.id)
        composeTestRule.waitForIdle()
        assertTrue("not before the answer", skipped.isEmpty())
        composeTestRule.onNodeWithText("Skip quest?").assertIsDisplayed()

        composeTestRule.onNodeWithText("SKIP").performClick()
        assertEquals(listOf(gym), skipped)
    }

    @Test
    fun cancellingTheSkip_keepsTheQuest() {
        show(state())
        scrollTo(HomeTags.quest(gym.id))
        skipWithTalkBack(gym.id)
        composeTestRule.onNodeWithText("CANCEL").performClick()

        assertTrue(skipped.isEmpty())
        assertEquals(0, composeTestRule.onAllNodes(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString("Skip quest?")))).fetchSemanticsNodes().size)
    }

    @Test
    fun aPerfectDay_celebrates_andListsWhatsDone() {
        show(state(upNext = emptyList(), done = listOf(pushUps, pushUps.copy(id = 4, title = "Stretch")), perfect = true))

        composeTestRule.onNodeWithTag(HomeTags.PERFECT).assertIsDisplayed()
        composeTestRule.onNodeWithText("ALL 2 QUESTS DONE").assertIsDisplayed()
        composeTestRule.onNodeWithText("PERFECT DAY!").assertIsDisplayed()
        scrollTo(HomeTags.quest(4))
        composeTestRule.onNodeWithText("DONE TODAY · 2").assertExists()
    }

    @Test
    fun aPerfectDayBeforeEveryQuest_saysHowMany_andKeepsTheRestToDo() {
        show(state(upNext = listOf(read), done = listOf(pushUps, pushUps.copy(id = 4), pushUps.copy(id = 5)), perfect = true))

        composeTestRule.onNodeWithText("3 OF 4 DONE").assertIsDisplayed()
        scrollTo(HomeTags.quest(read.id))
        composeTestRule.onNodeWithTag(HomeTags.quest(read.id)).assertExists()
    }

    @Test
    fun noQuestsToday_offersANewOne() {
        show(state(upNext = emptyList(), done = emptyList()))

        composeTestRule.onNodeWithTag(HomeTags.EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithText("NEW QUEST").performClick()
        assertEquals(1, newQuest)
    }

    @Test
    fun simpleMode_plainWords_noLevelXpOrStreak() {
        show(state(simple = true))

        composeTestRule.onNodeWithText("Aria").assertIsDisplayed()
        composeTestRule.onNodeWithText("TODAY'S PROGRESS").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodes(hasContentDescription("5 day streak")).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodes(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString("+20 XP today")))).fetchSemanticsNodes().size)
    }

    @Test
    fun aWaitingInvitation_dotsThePartiesButton() {
        show(state(), partiesDot = true)
        composeTestRule.onNode(hasContentDescription("Circles, something waits for you here")).assertIsDisplayed()
    }

    @Test
    fun todaysState_mapsToHome() {
        val today = LocalDate.of(2026, 10, 21)
        fun task(id: Long, name: String, at: LocalTime, recurrence: RecurrenceType = RecurrenceType.DAILY) =
            TaskEntity(id = id, name = name, description = "", scheduledTime = at, scheduledDay = today, category = TaskCategory.FITNESS, recurrenceType = recurrence)
        val success = TodayUiState.Success(
            tasks = listOf(
                TodayTaskItem(task(1, "Gym", LocalTime.of(17, 30)), TaskItemStatus.GRACE_PERIOD),
                TodayTaskItem(task(2, "Read", LocalTime.of(21, 0), RecurrenceType.ONE_TIME), TaskItemStatus.PENDING),
                TodayTaskItem(task(3, "Push-ups", LocalTime.of(8, 0)), TaskItemStatus.DONE, xp = 20),
                TodayTaskItem(task(4, "Stretch", LocalTime.of(9, 0)), TaskItemStatus.MISSED),
            ),
            currentStreak = 5,
            level = 3,
            targetThreshold = 0.7f,
            heroName = "Aria",
            avatarId = "avatar_mage",
            xpToday = 20,
        )

        val home = success.toHomeUi(DateTimeFormatter.ofPattern("HH:mm")) { if (it == RecurrenceType.DAILY) "Daily" else null }

        assertEquals(AvatarClass.Mage, home.avatar)
        assertEquals(1, home.doneCount)
        assertEquals(4, home.totalCount)
        assertEquals(70, home.targetPercent)
        assertEquals(listOf(1L, 2L, 4L), home.upNext.map { it.id })
        assertEquals("past its time, or skipped: overdue", listOf(true, false, true), home.upNext.map { it.overdue })
        assertEquals(listOf("Daily", null, "Daily"), home.upNext.map { it.repeatLabel })
        assertEquals("17:30", home.upNext.first().time)
        assertEquals(QuestCategory.Fitness, home.upNext.first().category)
        assertEquals(listOf(20), home.doneToday.map { it.xp })
    }
}
