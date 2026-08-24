package com.pixelquest.app.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StreakXpSummaryStripTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun streakXpSummaryStrip_displaysDataAndTriggersClick() {
        var clicked = false
        composeTestRule.setContent {
            StreakXpSummaryStrip(
                currentStreak = 7,
                totalXp = 1250,
                level = 4,
                onClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("7 DAYS").assertExists()
        composeTestRule.onNodeWithText("1250 XP").assertExists()
        composeTestRule.onNodeWithText("LVL 4").assertExists()

        composeTestRule.onNodeWithText("7 DAYS").performClick()
        assertTrue(clicked)
    }
}
