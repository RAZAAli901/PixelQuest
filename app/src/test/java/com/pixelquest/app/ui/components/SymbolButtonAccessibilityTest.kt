package com.pixelquest.app.ui.components

import android.app.Application
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.pixelquest.app.data.local.entity.TaskEntity
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
import java.time.LocalDate
import java.time.LocalTime

/**
 * Buttons drawn with a symbol (◀, 🗑️, 🔄) were read by their Unicode names ("wastebasket") or not at
 * all. They now have names: "Back", "Delete Morning Run", "Refresh the leaderboard".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class SymbolButtonAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aSymbolButton_isReadByItsName_notItsSymbol() {
        var clicks = 0
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                IconButton(onClick = { clicks++ }) {
                    SymbolIcon("◀", contentDescription = "Back", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").assertHasClickAction().performClick()
        assertEquals(1, clicks)
        assertEquals(0, composeTestRule.onAllNodesWithText("◀").fetchSemanticsNodes().size)
    }

    @Test
    fun aQuestsDeleteButton_namesTheQuest() {
        val task = TaskEntity(
            id = 1, name = "Morning Run", description = "", scheduledDay = LocalDate.of(2026, 10, 6),
            scheduledTime = LocalTime.of(7, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
        )
        var deleted = false
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                PixelTaskListItem(task = task, onClick = {}, onDeleteClick = { deleted = true })
            }
        }

        composeTestRule.onNodeWithContentDescription("Delete Morning Run").assertHasClickAction().performClick()
        assertEquals(true, deleted)
    }
}
