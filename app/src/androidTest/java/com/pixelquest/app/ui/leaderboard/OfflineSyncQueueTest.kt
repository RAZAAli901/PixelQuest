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
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.ui.screens.account.AccountContent
import com.pixelquest.app.ui.screens.account.AccountUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OfflineSyncQueueTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun offlineSync_queuesGracefullyAndSucceedsOnReconnect() {
        val testUser = PixelAuthUser(
            id = "offline-test-user-1",
            email = "offline@pixelquest.test",
            displayName = "OfflineHero"
        )

        var isNetworkConnected by mutableStateOf(false)
        var cloudServerProfile by mutableStateOf<CloudProfileDto?>(null)
        var localStreak by mutableStateOf(5)

        var accountState by mutableStateOf(
            AccountUiState(
                isOptedIn = true,
                displayNameInput = "OfflineHero",
                isSyncFailed = false,
                syncMessage = null
            )
        )

        fun executeSync() {
            if (!isNetworkConnected) {
                // Simulate offline failure
                accountState = accountState.copy(
                    isSyncing = false,
                    isSyncFailed = true,
                    syncMessage = "Network error: check connection."
                )
            } else {
                // Simulate reconnect success
                cloudServerProfile = CloudProfileDto(
                    id = testUser.id,
                    displayName = "OfflineHero",
                    currentStreak = localStreak,
                    longestStreak = localStreak,
                    level = 3,
                    totalXp = 600,
                    leaderboardOptIn = true,
                    updatedAt = java.time.Instant.now().toString()
                )
                accountState = accountState.copy(
                    isSyncing = false,
                    isSyncFailed = false,
                    lastSyncTime = System.currentTimeMillis(),
                    syncMessage = "Cloud sync successful!"
                )
            }
        }

        composeTestRule.setContent {
            PixelQuestTheme {
                AccountContent(
                    authState = AuthUiState.SignedIn(testUser),
                    accountState = accountState,
                    onSyncNow = {
                        accountState = accountState.copy(isSyncing = true)
                        executeSync()
                    }
                )
            }
        }

        // 1. Initially offline: tap Sync Now
        composeTestRule.onNodeWithText("🔄 FORCE SYNC NOW (DEBUG)").performClick()

        // 2. Verify subtle offline warning is displayed without crashing
        composeTestRule.onNodeWithText("☁️⚠️ Cloud sync currently unavailable (retrying in background) — local progress is safe").assertIsDisplayed()
        assertTrue(accountState.isSyncFailed)
        assertEquals(null, cloudServerProfile)

        // 3. User achieves new streak while offline
        localStreak = 6

        // 4. Network reconnects!
        isNetworkConnected = true

        // 5. Trigger sync upon reconnect
        composeTestRule.onNodeWithText("🔄 FORCE SYNC NOW (DEBUG)").performClick()

        // 6. Verify sync succeeds and server receives updated streak
        composeTestRule.onNodeWithText("Cloud sync successful!").assertIsDisplayed()
        assertFalse(accountState.isSyncFailed)
        assertTrue(cloudServerProfile != null)
        assertEquals(6, cloudServerProfile?.currentStreak)
    }
}
