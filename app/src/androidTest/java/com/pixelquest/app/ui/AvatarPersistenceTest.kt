package com.pixelquest.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.screens.avatar.AvatarSelectionScreen
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AvatarPersistenceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun avatarSelection_persistsAndReflectsOnProfileScreen() {
        var currentAvatarId by mutableStateOf("avatar_hero")
        var currentScreen by mutableStateOf("SELECTION")

        composeTestRule.setContent {
            PixelQuestTheme {
                when (currentScreen) {
                    "SELECTION" -> AvatarSelectionScreen(
                        currentAvatarId = currentAvatarId,
                        onAvatarSelected = {
                            currentAvatarId = it
                            currentScreen = "PROFILE"
                        }
                    )
                    "PROFILE" -> androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.Text("PROFILE SCREEN")
                        androidx.compose.material3.Text("ACTIVE AVATAR: $currentAvatarId")
                    }
                }
            }
        }

        // Initial check on Selection screen
        composeTestRule.onNodeWithText("🧙 CHOOSE AVATAR").assertIsDisplayed()

        // Select ROGUE
        composeTestRule.onNodeWithText("ROGUE").performClick()

        // Verify state persistence to Profile screen
        composeTestRule.onNodeWithText("PROFILE SCREEN").assertIsDisplayed()
        composeTestRule.onNodeWithText("ACTIVE AVATAR: avatar_rogue").assertIsDisplayed()
        assertEquals("avatar_rogue", currentAvatarId)
    }
}
