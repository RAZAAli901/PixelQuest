package com.pixelquest.app.ui.components

import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * B09 - B14: Verifies the quest card's three distinct touch zones:
 * 1. Category Icon zone (minimum 48dp target, triggers onIconClick / edit)
 * 2. Card Body zone (minimum 48dp height target, triggers onClick and onLongClick / open)
 * 3. Complete zone (minimum 48dp target, triggers onToggle / complete)
 * 4. TalkBack custom accessibility actions on QuestRow
 * 5. Touch zones do not conflict with each other
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class QuestTouchZonesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleQuest = QuestUi(
        id = 42L,
        title = "Morning Workout",
        time = "07:30",
        category = QuestCategory.Fitness,
        done = false,
        xp = 50,
        repeatLabel = "Daily",
    )

    @Test
    fun iconZone_isAtLeast48dp_andTriggersIconClick() {
        var iconClicked = false
        var rowClicked = false
        var toggled = false

        composeTestRule.setContent {
            QuestRow(
                quest = sampleQuest,
                onToggle = { toggled = true },
                onClick = { rowClicked = true },
                onIconClick = { iconClicked = true },
            )
        }

        val iconNode = composeTestRule.onNodeWithTag(QuestTags.icon(sampleQuest.id))
        iconNode.assertWidthIsAtLeast(48.dp)
        iconNode.assertHeightIsAtLeast(48.dp)

        iconNode.performClick()

        assertTrue("Category icon tap must trigger onIconClick", iconClicked)
        assertEquals("Icon tap must not trigger row toggle", false, toggled)
        assertEquals("Icon tap must not trigger row body click", false, rowClicked)
    }

    @Test
    fun cardBodyZone_isAtLeast48dpHeight_andHandlesShortAndLongClick() {
        var clicked = false
        var longClicked = false
        var toggled = false

        composeTestRule.setContent {
            QuestRow(
                quest = sampleQuest,
                onToggle = { toggled = true },
                onClick = { clicked = true },
                onLongClick = { longClicked = true },
            )
        }

        val bodyNode = composeTestRule.onNodeWithTag(QuestTags.body(sampleQuest.id))
        bodyNode.assertHeightIsAtLeast(48.dp)

        // Short click
        bodyNode.performClick()
        assertTrue("Short click on card body must trigger onClick", clicked)
        assertEquals("Body click must not trigger toggle", false, toggled)

        // Long click
        bodyNode.performTouchInput { longClick() }
        assertTrue("Long click on card body must trigger onLongClick", longClicked)
    }

    @Test
    fun completeZone_isAtLeast48dp_andTogglesQuest() {
        var toggled = false
        var clicked = false
        var iconClicked = false

        composeTestRule.setContent {
            QuestRow(
                quest = sampleQuest,
                onToggle = { toggled = true },
                onClick = { clicked = true },
                onIconClick = { iconClicked = true },
            )
        }

        val completeNode = composeTestRule.onNodeWithTag(QuestTags.complete(sampleQuest.id))
        completeNode.assertWidthIsAtLeast(48.dp)
        completeNode.assertHeightIsAtLeast(48.dp)

        completeNode.performClick()

        assertTrue("Tapping complete circle zone must trigger onToggle", toggled)
        assertEquals("Complete toggle must not trigger body click", false, clicked)
        assertEquals("Complete toggle must not trigger icon click", false, iconClicked)
    }

    @Test
    fun questRow_providesTalkBackCustomAccessibilityActions() {
        var toggled = false
        var edited = false
        var opened = false

        composeTestRule.setContent {
            QuestRow(
                quest = sampleQuest,
                onToggle = { toggled = true },
                onClick = { edited = true },
                onIconClick = { edited = true },
                onLongClick = { opened = true },
            )
        }

        // Check custom accessibility actions on QuestRow
        val rowNode = composeTestRule.onNode(
            SemanticsMatcher("has custom accessibility actions") { node ->
                val actions = node.config.getOrNull(SemanticsActions.CustomActions)
                actions?.any { it.label == "Complete" } == true &&
                    actions.any { it.label == "Edit" } == true &&
                    actions.any { it.label == "Open" } == true
            },
        )
        rowNode.assertExists()

        val actions = rowNode.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions)
        assertNotNull(actions)

        // Execute "Complete" custom action
        val completeAction = actions!!.firstOrNull { it.label == "Complete" }
        assertNotNull(completeAction)
        completeAction!!.action()
        assertTrue("TalkBack Complete action must trigger onToggle", toggled)

        // Execute "Edit" custom action
        val editAction = actions.firstOrNull { it.label == "Edit" }
        assertNotNull(editAction)
        editAction!!.action()
        assertTrue("TalkBack Edit action must trigger onIconClick / edit", edited)

        // Execute "Open" custom action
        val openAction = actions.firstOrNull { it.label == "Open" }
        assertNotNull(openAction)
        openAction!!.action()
        assertTrue("TalkBack Open action must trigger onLongClick", opened)
    }

    @Test
    fun distinctZones_doNotInterfereWithEachOther() {
        var iconCount = 0
        var bodyCount = 0
        var toggleCount = 0

        composeTestRule.setContent {
            QuestRow(
                quest = sampleQuest,
                onToggle = { toggleCount++ },
                onClick = { bodyCount++ },
                onIconClick = { iconCount++ },
            )
        }

        // 1. Click icon zone
        composeTestRule.onNodeWithTag(QuestTags.icon(sampleQuest.id)).performClick()
        assertEquals(1, iconCount)
        assertEquals(0, bodyCount)
        assertEquals(0, toggleCount)

        // 2. Click body zone
        composeTestRule.onNodeWithTag(QuestTags.body(sampleQuest.id)).performClick()
        assertEquals(1, iconCount)
        assertEquals(1, bodyCount)
        assertEquals(0, toggleCount)

        // 3. Click complete zone
        composeTestRule.onNodeWithTag(QuestTags.complete(sampleQuest.id)).performClick()
        assertEquals(1, iconCount)
        assertEquals(1, bodyCount)
        assertEquals(1, toggleCount)
    }
}
