package com.pixelquest.app.ui.components

import android.app.Application
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Buttons drawn with a symbol (◀, 🗑️, 🔄) were read by their Unicode names ("wastebasket") or not at
 * all. They now have names: "Back", "Refresh the leaderboard".
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
}
