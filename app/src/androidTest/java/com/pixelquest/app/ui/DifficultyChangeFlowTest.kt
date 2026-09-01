package com.pixelquest.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.components.PixelConfirmDialog
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DifficultyChangeFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun difficultyChange_triggersWarningDialog_andUpdatesDifficultyOnConfirm() {
        var currentDifficulty by mutableStateOf(DifficultyLevel.EASY)
        var pendingDifficulty by mutableStateOf<DifficultyLevel?>(null)
        var showWarningDialog by mutableStateOf(false)

        composeTestRule.setContent {
            PixelQuestTheme {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text("Current: ${currentDifficulty.name}")
                    com.pixelquest.app.ui.components.PixelButton(
                        text = "SELECT HARD",
                        onClick = {
                            pendingDifficulty = DifficultyLevel.HARD
                            showWarningDialog = true
                        }
                    )

                    if (showWarningDialog && pendingDifficulty != null) {
                        PixelConfirmDialog(
                            title = "CHANGE DIFFICULTY?",
                            message = "Changing difficulty will update your target threshold. Confirm change?",
                            confirmText = "CONFIRM",
                            dismissText = "CANCEL",
                            onConfirm = {
                                currentDifficulty = pendingDifficulty!!
                                pendingDifficulty = null
                                showWarningDialog = false
                            },
                            onDismiss = {
                                pendingDifficulty = null
                                showWarningDialog = false
                            }
                        )
                    }
                }
            }
        }

        // Verify initial state
        composeTestRule.onNodeWithText("Current: EASY").assertIsDisplayed()

        // Click select HARD
        composeTestRule.onNodeWithText("SELECT HARD").performClick()

        // Verify dialog shows
        composeTestRule.onNodeWithText("CHANGE DIFFICULTY?").assertIsDisplayed()
        composeTestRule.onNodeWithText("CONFIRM").performClick()

        // Verify updated difficulty
        composeTestRule.onNodeWithText("Current: HARD").assertIsDisplayed()
        assertEquals(DifficultyLevel.HARD, currentDifficulty)
    }
}
