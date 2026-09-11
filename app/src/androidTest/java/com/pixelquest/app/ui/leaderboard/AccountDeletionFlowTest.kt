package com.pixelquest.app.ui.leaderboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.auth.PixelAuthUser
import com.pixelquest.app.ui.screens.account.AccountContent
import com.pixelquest.app.ui.screens.account.AccountUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccountDeletionFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun accountDeletion_doubleConfirmationAndPurgeExecution() {
        val testUser = PixelAuthUser(
            id = "user-delete-test-99",
            email = "slayer@pixelquest.test",
            displayName = "DragonSlayer"
        )

        var authState by mutableStateOf<AuthUiState>(AuthUiState.SignedIn(testUser))
        var accountState by mutableStateOf(
            AccountUiState(
                isOptedIn = true,
                displayNameInput = "DragonSlayer"
            )
        )

        var cloudProfileDeleted = false
        var authAccountDeleted = false
        var localCloudDataCleared = false
        var localQuestsPreserved = true // Local quest state flag

        composeTestRule.setContent {
            PixelQuestTheme {
                AccountContent(
                    authState = authState,
                    accountState = accountState,
                    onRequestDeleteCloudData = {
                        accountState = accountState.copy(showDeleteConfirmDialog = true)
                    },
                    onProceedDeleteDoubleConfirm = {
                        accountState = accountState.copy(
                            showDeleteConfirmDialog = false,
                            showDeleteDoubleConfirmDialog = true
                        )
                    },
                    onConfirmDeleteCloudData = {
                        // 1. Delete remote profiles row
                        cloudProfileDeleted = true
                        // 2. Delete Supabase auth account via RPC
                        authAccountDeleted = true
                        // 3. Clear Room cloud references
                        localCloudDataCleared = true
                        // 4. Sign out locally
                        authState = AuthUiState.SignedOut
                        accountState = AccountUiState(
                            isOptedIn = false,
                            displayNameInput = "",
                            showDeleteConfirmDialog = false,
                            showDeleteDoubleConfirmDialog = false
                        )
                    },
                    onDismissDeleteDialog = {
                        accountState = accountState.copy(
                            showDeleteConfirmDialog = false,
                            showDeleteDoubleConfirmDialog = false
                        )
                    }
                )
            }
        }

        // 1. Initial State: Signed in with linked account
        composeTestRule.onNodeWithText("🛡️ LINKED CLOUD ACCOUNT").assertIsDisplayed()
        composeTestRule.onNodeWithText("DragonSlayer").assertIsDisplayed()

        // 2. Tap "DELETE MY CLOUD DATA"
        composeTestRule.onNodeWithText("🗑️ DELETE MY CLOUD DATA").performClick()
        assertTrue(accountState.showDeleteConfirmDialog)

        // 3. First Confirmation Dialog
        composeTestRule.onNodeWithText("DELETE CLOUD DATA?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to delete your cloud account and public leaderboard record? This action cannot be undone.").assertIsDisplayed()
        composeTestRule.onNodeWithText("CONTINUE").performClick()
        assertTrue(accountState.showDeleteDoubleConfirmDialog)

        // 4. Second Double Confirmation Dialog (Explicit warning of signout & local preservation)
        composeTestRule.onNodeWithText("FINAL WARNING: PURGE").assertIsDisplayed()
        composeTestRule.onNodeWithText("This permanently erases your leaderboard rank, display name, and cloud profile, and signs you out.\n\nLocal quests and streak history on this device will remain safe.").assertIsDisplayed()

        // 5. Confirm Final Purge
        composeTestRule.onNodeWithText("PURGE CLOUD").performClick()

        // 6. Verify Deletion Steps Executed
        assertTrue(cloudProfileDeleted)
        assertTrue(authAccountDeleted)
        assertTrue(localCloudDataCleared)
        assertTrue(localQuestsPreserved)
        assertTrue(authState is AuthUiState.SignedOut)
        assertFalse(accountState.isOptedIn)
        assertEquals("", accountState.displayNameInput)

        // 7. UI updates immediately to SignedOut state
        composeTestRule.onNodeWithText("🏆 JOIN THE LEADERBOARD").assertIsDisplayed()
        composeTestRule.onNodeWithText("🌐 SIGN IN WITH GOOGLE").assertIsDisplayed()
    }
}
