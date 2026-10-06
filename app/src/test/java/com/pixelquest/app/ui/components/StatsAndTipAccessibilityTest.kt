package com.pixelquest.app.ui.components

import android.app.Application
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * The ✕ that closes tip cards was a bare character with a tiny touch target, and heatmap squares were
 * unnamed buttons. Both now say what they are.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class StatsAndTipAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dismissButton_isANamedButton_withA48dpTarget() {
        var dismissed = 0
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                DismissButton(contentDescription = "Dismiss the Comic mode tip", onClick = { dismissed++ }, color = androidx.compose.ui.graphics.Color.Gray)
            }
        }

        composeTestRule.onNodeWithContentDescription("Dismiss the Comic mode tip")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(1, dismissed)
        // The bare symbol isn't read on its own.
        assertEquals(0, composeTestRule.onAllNodesWithText("✕").fetchSemanticsNodes().size)
    }

    @Test
    fun heatmapSquares_sayTheDayAndItsResult() {
        val friday = LocalDate.of(2026, 10, 2)
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PixelCalendarHeatmap(
                    statusMap = mapOf(friday to DailyStatus.PERFECT, friday.plusDays(1) to DailyStatus.MISSED),
                    startDate = friday.minusDays(6),
                    endDate = friday.plusDays(1),
                    onDayClick = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Friday 2 October: perfect day").assertHasClickAction()
        composeTestRule.onNodeWithContentDescription("Saturday 3 October: missed").assertHasClickAction()
        composeTestRule.onNodeWithContentDescription("Thursday 1 October: nothing scheduled").assertHasClickAction()
    }

    @Test
    fun theLabels() {
        assertEquals("Friday 2 October: partly done", HeatmapCellLabels.describe(LocalDate.of(2026, 10, 2), DailyStatus.PARTIAL))
    }
}
