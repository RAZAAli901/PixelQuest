package com.pixelquest.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.screens.AvatarPickerScreen
import com.pixelquest.app.ui.screens.AvatarTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Choosing an avatar (mock 2I): a tap previews it, Equip makes it the hero's. */
class AvatarSelectionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aTapPreviews_andEquipWearsIt() {
        var equipped by mutableStateOf(AvatarClass.Hero)
        var selected by mutableStateOf(AvatarClass.Hero)
        composeTestRule.setContent {
            AvatarPickerScreen(equipped, selected, onBack = {}, onSelect = { selected = it }, onEquip = { equipped = it })
        }

        composeTestRule.onNodeWithText("CHOOSE AVATAR").assertIsDisplayed()
        composeTestRule.onNodeWithTag(AvatarTags.EQUIP).assertIsNotEnabled()

        composeTestRule.onNodeWithTag(AvatarTags.tile(AvatarClass.Rogue)).performClick()
        assertEquals("a tap only previews", AvatarClass.Hero, equipped)
        composeTestRule.onNodeWithText("EQUIP ROGUE").performClick()

        assertEquals(AvatarClass.Rogue, equipped)
    }
}
