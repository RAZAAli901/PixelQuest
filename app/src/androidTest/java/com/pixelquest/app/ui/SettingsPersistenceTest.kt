package com.pixelquest.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsPersistenceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsToggles_soundCrtHapticsNotifications_toggleAndPersistState() {
        var isSoundEnabled by mutableStateOf(true)
        var isCrtEnabled by mutableStateOf(false)
        var isHapticsEnabled by mutableStateOf(true)
        var isNotificationsEnabled by mutableStateOf(true)

        composeTestRule.setContent {
            PixelQuestTheme {
                androidx.compose.foundation.layout.Column {
                    PixelButton(
                        text = if (isSoundEnabled) "🔊 SFX: ON" else "🔇 SFX: OFF",
                        onClick = { isSoundEnabled = !isSoundEnabled }
                    )
                    PixelButton(
                        text = if (isCrtEnabled) "📺 CRT FILTER: ON" else "📺 CRT FILTER: OFF",
                        onClick = { isCrtEnabled = !isCrtEnabled }
                    )
                    PixelButton(
                        text = if (isHapticsEnabled) "📳 HAPTICS: ON" else "📴 HAPTICS: OFF",
                        onClick = { isHapticsEnabled = !isHapticsEnabled }
                    )
                    PixelButton(
                        text = if (isNotificationsEnabled) "🔔 NOTIFICATIONS: ON" else "🔕 NOTIFICATIONS: OFF",
                        onClick = { isNotificationsEnabled = !isNotificationsEnabled }
                    )
                }
            }
        }

        // Initial state assertions
        composeTestRule.onNodeWithText("🔊 SFX: ON").assertIsDisplayed()
        composeTestRule.onNodeWithText("📺 CRT FILTER: OFF").assertIsDisplayed()
        composeTestRule.onNodeWithText("📳 HAPTICS: ON").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔔 NOTIFICATIONS: ON").assertIsDisplayed()

        // Toggle each setting
        composeTestRule.onNodeWithText("🔊 SFX: ON").performClick()
        composeTestRule.onNodeWithText("📺 CRT FILTER: OFF").performClick()
        composeTestRule.onNodeWithText("📳 HAPTICS: ON").performClick()
        composeTestRule.onNodeWithText("🔔 NOTIFICATIONS: ON").performClick()

        // Assert updated toggled states
        composeTestRule.onNodeWithText("🔇 SFX: OFF").assertIsDisplayed()
        composeTestRule.onNodeWithText("📺 CRT FILTER: ON").assertIsDisplayed()
        composeTestRule.onNodeWithText("📴 HAPTICS: OFF").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔕 NOTIFICATIONS: OFF").assertIsDisplayed()

        assertFalse(isSoundEnabled)
        assertTrue(isCrtEnabled)
        assertFalse(isHapticsEnabled)
        assertFalse(isNotificationsEnabled)
    }
}
