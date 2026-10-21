package com.pixelquest.app.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.components.AvatarClass
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Choosing an avatar (mock 2I), and the Leaderboard while signed out (mock 2J). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class AvatarAndLeaderboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aTapPreviews_equipWearsIt() {
        var equipped by mutableStateOf(AvatarClass.Hero)
        var selected by mutableStateOf(AvatarClass.Hero)
        composeTestRule.setContent {
            AvatarPickerScreen(equipped, selected, onBack = {}, onSelect = { selected = it }, onEquip = { equipped = it })
        }

        composeTestRule.onNodeWithText("Equipped").assertIsDisplayed()
        composeTestRule.onNodeWithTag(AvatarTags.EQUIP).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(AvatarTags.tile(AvatarClass.Hero)).assertIsSelected()

        composeTestRule.onNodeWithTag(AvatarTags.tile(AvatarClass.Mage)).performClick()
        composeTestRule.onNodeWithTag(AvatarTags.tile(AvatarClass.Mage)).assertIsSelected()
        composeTestRule.onNodeWithText("Currently equipped: Hero").assertIsDisplayed()
        assertEquals("a tap only previews", AvatarClass.Hero, equipped)

        composeTestRule.onNodeWithText("EQUIP MAGE").performClick()
        assertEquals(AvatarClass.Mage, equipped)
    }

    @Test
    fun signedOut_theBoardIsLocked_withBothWaysIn() {
        var google = 0
        var email = 0
        composeTestRule.setContent {
            LeaderboardLockedScreen(onBack = null, onGoogleSignIn = { google++ }, onEmailCode = { email++ }, cloudAvailable = true)
        }

        composeTestRule.onNodeWithText("HALL OF FAME LOCKED").assertIsDisplayed()
        composeTestRule.onNodeWithTag(LockedBoardTags.GOOGLE).performClick()
        composeTestRule.onNodeWithTag(LockedBoardTags.EMAIL).performClick()
        assertEquals(1, google)
        assertEquals(1, email)
    }

    @Test
    fun whileSigningIn_theButtonWaits_andAFailureSaysWhy() {
        composeTestRule.setContent {
            LeaderboardLockedScreen(onBack = null, onGoogleSignIn = {}, onEmailCode = {}, signingIn = true, error = "No internet connection", cloudAvailable = true)
        }
        composeTestRule.onNodeWithText("SIGNING IN…").assertIsDisplayed()
        composeTestRule.onNodeWithTag(LockedBoardTags.GOOGLE).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(LockedBoardTags.ERROR).assertIsDisplayed()
    }
}
