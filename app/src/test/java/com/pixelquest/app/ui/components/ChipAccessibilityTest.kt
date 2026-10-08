package com.pixelquest.app.ui.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek

/**
 * TalkBack used to hear "T" for both Tuesday and Thursday, and no chip said whether it was selected.
 * Day chips are now checkboxes named with the full day; recurrence and category chips are radio buttons.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChipAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun dayChips(mode: ThemeMode) {
        var days by mutableStateOf(setOf(DayOfWeek.TUESDAY))
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                PixelDaySelector(selectedDays = days, onDayToggled = { day -> days = if (day in days) days - day else days + day })
            }
        }

        composeTestRule.onNodeWithContentDescription("Tuesday").assertIsOn().assert(hasRole(Role.Checkbox))
        composeTestRule.onNodeWithContentDescription("Thursday").assertIsOff()
        // The single letters are not read on their own.
        assertEquals(0, composeTestRule.onAllNodesWithContentDescription("T").fetchSemanticsNodes().size)

        composeTestRule.onNodeWithContentDescription("Thursday").performClick()
        composeTestRule.onNodeWithContentDescription("Thursday").assertIsOn()
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), days)
    }

    @Test
    fun dayChips_pixel() = dayChips(ThemeMode.Pixel)

    @Test
    fun dayChips_comic() = dayChips(ThemeMode.Comic)

    private fun choiceChips(mode: ThemeMode) {
        var recurrence by mutableStateOf(RecurrenceType.DAILY)
        var category by mutableStateOf(TaskCategory.FITNESS)
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                androidx.compose.foundation.layout.Column {
                    PixelRecurrenceSelector(selectedType = recurrence, onTypeSelected = { recurrence = it })
                    PixelCategorySelector(selectedCategory = category, onCategorySelected = { category = it })
                }
            }
        }

        composeTestRule.onNodeWithText("DAILY", substring = true, ignoreCase = true).assertIsSelected().assert(hasRole(Role.RadioButton))
        composeTestRule.onNodeWithText("WEEKLY", substring = true, ignoreCase = true).assertIsNotSelected().performClick()
        assertEquals(RecurrenceType.WEEKLY, recurrence)

        val learning = TaskCategory.LEARNING.displayName
        composeTestRule.onNodeWithText(learning).assertIsNotSelected().performClick()
        composeTestRule.onNodeWithText(learning).assertIsSelected()
        // The icon no longer repeats the name.
        assertEquals(0, composeTestRule.onAllNodesWithContentDescription(learning).fetchSemanticsNodes().size)
    }

    @Test
    fun difficultyCards_sayWhichIsSelected() {
        var level by mutableStateOf(com.pixelquest.app.domain.model.DifficultyLevel.MEDIUM)
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PixelDifficultyCards(selectedLevel = level, onLevelSelected = { level = it })
            }
        }

        composeTestRule.onNodeWithText("MEDIUM").assertIsSelected().assert(hasRole(Role.RadioButton))
        composeTestRule.onNodeWithText("HARD").assertIsNotSelected().performClick()
        assertEquals(com.pixelquest.app.domain.model.DifficultyLevel.HARD, level)
        composeTestRule.onNodeWithText("HARD").assertIsSelected()
    }

    @Test
    fun recurrenceAndCategoryChips_pixel() = choiceChips(ThemeMode.Pixel)

    @Test
    fun recurrenceAndCategoryChips_comic() = choiceChips(ThemeMode.Comic)
}
