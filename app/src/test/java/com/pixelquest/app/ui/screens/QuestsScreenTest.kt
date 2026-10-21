package com.pixelquest.app.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.QuestUi
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.screens.history.HistoryFilter
import com.pixelquest.app.ui.screens.history.TaskHistoryItem
import com.pixelquest.app.ui.screens.tasks.TaskWithStatus
import com.pixelquest.app.ui.screens.today.TodayTaskItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * Quests (docs/ui-v2.md, mock 2C): today's by part of the day, all of them by how they repeat, the
 * history by day; a search, and the one New quest button.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class QuestsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val day = LocalDate.of(2026, 10, 21)
    private fun task(id: Long, name: String, hour: Int, recurrence: RecurrenceType = RecurrenceType.DAILY) =
        TaskEntity(id = id, name = name, description = "", scheduledTime = LocalTime.of(hour, 0), scheduledDay = day, category = TaskCategory.FITNESS, recurrenceType = recurrence)

    @Test
    fun today_isGroupedByPartOfTheDay_inTimeOrder() {
        val parts = todayParts(
            listOf(
                TodayTaskItem(task(1, "Gym", 17), TaskItemStatus.PENDING),
                TodayTaskItem(task(2, "Push-ups", 8), TaskItemStatus.DONE),
                TodayTaskItem(task(3, "Lunch walk", 13), TaskItemStatus.PENDING),
                TodayTaskItem(task(4, "Stretch", 7), TaskItemStatus.DONE),
            )
        )
        assertEquals(listOf(DayPart.Morning, DayPart.Afternoon, DayPart.Evening), parts.map { it.first })
        assertEquals(listOf(4L, 2L), parts.first().second.map { it.task.id })
    }

    @Test
    fun all_isGroupedByHowTheyRepeat_dailyFirst_oneTimeLast() {
        val groups = recurrenceGroups(
            listOf(
                TaskWithStatus(task(1, "Exam", 9, RecurrenceType.ONE_TIME), TaskItemStatus.PENDING),
                TaskWithStatus(task(2, "Gym", 17), TaskItemStatus.COMPLETED),
                TaskWithStatus(task(3, "Laundry", 10, RecurrenceType.WEEKLY), TaskItemStatus.MISSED),
                TaskWithStatus(task(4, "Read", 8), TaskItemStatus.PENDING),
            )
        )
        assertEquals(listOf(RecurrenceType.DAILY, RecurrenceType.WEEKLY, RecurrenceType.ONE_TIME), groups.map { it.first })
        assertEquals(listOf(4L, 2L), groups.first().second.map { it.task.id })

        val laundry = groups[1].second.single().toQuestUi(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) { "Weekly" }
        assertEquals(true, laundry.missed)
        assertEquals("Weekly", laundry.repeatLabel)
    }

    @Test
    fun history_isByDay_latestFirst_missedSaysSo() {
        val items = listOf(
            TaskHistoryItem(1, 7, "Gym", TaskCategory.FITNESS, day.minusDays(2), 20, true),
            TaskHistoryItem(2, 8, "Read", TaskCategory.LEARNING, day, 15, true),
            TaskHistoryItem(3, 7, "Gym", TaskCategory.FITNESS, day, 0, false),
        )
        val days = historyDays(items)
        assertEquals(listOf(day, day.minusDays(2)), days.map { it.first })
        val missed = days.first().second.last().toQuestUi()
        assertEquals(true, missed.missed)
        assertEquals(0, missed.xp)
        assertEquals(15, days.first().second.first().toQuestUi().xp)
    }

    @Test
    fun theSearch_keepsMatchingTitles_anyCase() {
        val groups = listOf(
            QuestGroup("a", "Morning", listOf(QuestUi(1, "Go to Gym", null, QuestCategory.Fitness), QuestUi(2, "Read", null, QuestCategory.Learning))),
            QuestGroup("b", "Evening", listOf(QuestUi(3, "Read more", null, QuestCategory.Learning))),
        )
        assertEquals(listOf(listOf(1L)), groups.matching("GYM").map { g -> g.quests.map { it.id } })
        assertEquals(groups, groups.matching(" "))
    }

    private val groups = listOf(
        QuestGroup("Morning", "Morning · 2 done", listOf(QuestUi(1, "20 Push-ups", "08:00", QuestCategory.Fitness, done = true))),
        QuestGroup("Evening", "Evening · 1 left", listOf(QuestUi(2, "Go to Gym", "17:30", QuestCategory.Fitness, overdue = true))),
    )

    @Test
    fun today_showsItsGroups_andACircleCompletes() {
        val toggled = mutableListOf<QuestUi>()
        var tab = QuestTab.All
        composeTestRule.setContent {
            QuestsContent(QuestTab.Today, { tab = it }, todayCount = 2, groups = groups, onToggleQuest = { toggled += it }, onQuestClick = {}, onNewQuest = {})
        }

        composeTestRule.onNodeWithText("QUESTS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today · 2").assertIsSelected()
        composeTestRule.onNodeWithText("EVENING · 1 LEFT").assertIsDisplayed()
        composeTestRule.onNode(isToggleable() and hasAnyAncestor(hasTestTag(QuestsTags.quest(2)))).performClick()
        assertEquals(listOf(2L), toggled.map { it.id })

        composeTestRule.onNodeWithText("History").performClick()
        assertEquals(QuestTab.History, tab)
    }

    @Test
    fun theNewQuestButton_saysWhatItDoes() {
        var added = 0
        composeTestRule.setContent {
            QuestsContent(QuestTab.Today, {}, 0, emptyList(), {}, {}, onNewQuest = { added++ })
        }
        composeTestRule.onNodeWithText("Nothing scheduled today.").assertIsDisplayed()
        composeTestRule.onNodeWithText("NEW QUEST").performClick()
        assertEquals(1, added)
    }

    @Test
    fun theSearchButton_opensAField_thatFilters() {
        var query: String? = null
        composeTestRule.setContent {
            var q by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
            QuestsContent(QuestTab.Today, {}, 2, groups.matching(q.orEmpty()), {}, {}, {}, query = q, onQueryChange = { q = it; query = it })
        }
        composeTestRule.onNodeWithTag(QuestsTags.SEARCH).performClick()
        composeTestRule.onNodeWithTag(QuestsTags.SEARCH_FIELD).assertIsDisplayed()
        composeTestRule.onNode(androidx.compose.ui.test.hasSetTextAction() and hasAnyAncestor(hasTestTag(QuestsTags.SEARCH_FIELD))).performTextInput("gym")

        assertEquals("gym", query)
        composeTestRule.onNodeWithText("Go to Gym").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("20 Push-ups")).fetchSemanticsNodes().size)
    }

    @Test
    fun history_hasItsFilters_andMore() {
        var filter: HistoryFilter? = null
        var more = 0
        composeTestRule.setContent {
            QuestsContent(
                QuestTab.History, {}, 0,
                listOf(QuestGroup("d", "Today", listOf(QuestUi(7, "Gym", null, QuestCategory.Fitness, missed = true)))),
                {}, {}, {},
                historyFilter = HistoryFilter.ALL_TIME, onHistoryFilter = { filter = it }, historyHasMore = true, onHistoryMore = { more++ },
            )
        }
        composeTestRule.onNodeWithText("All time").assertIsSelected()
        composeTestRule.onNodeWithText("MISSED").assertIsDisplayed()
        composeTestRule.onNodeWithText("7 days").performClick()
        assertEquals(HistoryFilter.LAST_7_DAYS, filter)
        composeTestRule.onNodeWithTag(QuestsTags.MORE).performClick()
        assertEquals(1, more)
    }

    @Test
    fun simpleMode_saysTasks() {
        composeTestRule.setContent {
            QuestsContent(QuestTab.All, {}, 0, emptyList(), {}, {}, {}, isSimpleMode = true)
        }
        composeTestRule.onNodeWithText("TASKS").assertIsDisplayed()
        composeTestRule.onNodeWithText("All tasks").assertIsSelected()
        composeTestRule.onNodeWithText("NEW TASK").assertIsDisplayed()
    }
}
