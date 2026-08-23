package com.pixelquest.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.screens.avatar.AvatarSelectionScreen
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AvatarSelectionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun avatarSelectionScreen_displaysAvatarsAndTriggersSelection() {
        var selectedAvatarId: String? = null

        composeTestRule.setContent {
            PixelQuestTheme {
                AvatarSelectionScreen(
                    currentAvatarId = "avatar_hero",
                    onAvatarSelected = { selectedAvatarId = it }
                )
            }
        }

        composeTestRule.onNodeWithText("🧙 CHOOSE AVATAR").assertIsDisplayed()
        composeTestRule.onNodeWithText("HERO").assertIsDisplayed()
        composeTestRule.onNodeWithText("MAGE").assertIsDisplayed()
        composeTestRule.onNodeWithText("WARRIOR").assertIsDisplayed()

        composeTestRule.onNodeWithText("MAGE").performClick()

        assertEquals("avatar_mage", selectedAvatarId)
    }
}
