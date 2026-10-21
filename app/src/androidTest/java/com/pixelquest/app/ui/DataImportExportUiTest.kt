package com.pixelquest.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqConfirmDialog
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DataImportExportUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dataExportImport_triggersConfirmationDialog_andRestoresData() {
        var isExportTriggered by mutableStateOf(false)
        var isImportTriggered by mutableStateOf(false)
        var showRestoreDialog by mutableStateOf(false)
        var dataRestored by mutableStateOf(false)

        composeTestRule.setContent {
            PixelQuestTheme {
                androidx.compose.foundation.layout.Column {
                    PqButton(
                        text = "📤 EXPORT QUEST DATA (JSON)",
                        onClick = { isExportTriggered = true }
                    )
                    PqButton(
                        text = "📥 IMPORT QUEST DATA (JSON)",
                        onClick = {
                            isImportTriggered = true
                            showRestoreDialog = true
                        }
                    )

                    if (showRestoreDialog) {
                        PqConfirmDialog(
                            title = "RESTORE DATA?",
                            message = "Importing quest data will replace your current progress. Confirm restore?",
                            confirm = "RESTORE",
                            dismiss = "CANCEL",
                            onConfirm = {
                                dataRestored = true
                                showRestoreDialog = false
                            },
                            onDismiss = { showRestoreDialog = false }
                        )
                    }
                }
            }
        }

        // Test Export
        composeTestRule.onNodeWithText("📤 EXPORT QUEST DATA (JSON)").performClick()
        assertTrue(isExportTriggered)

        // Test Import
        composeTestRule.onNodeWithText("📥 IMPORT QUEST DATA (JSON)").performClick()
        assertTrue(isImportTriggered)

        // Confirm restore dialog
        composeTestRule.onNodeWithText("RESTORE DATA?").assertIsDisplayed()
        composeTestRule.onNodeWithText("RESTORE").performClick()
        assertTrue(dataRestored)
    }
}
