package com.pixelquest.app.darood.ui.create

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Creating a party: the name's rules, counted as it's typed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class CreatePartyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theName_isCountedTrimmed_between2And40() {
        assertNull(CreatePartyUiState("").nameProblem)
        assertEquals(PartyNameProblem.TOO_SHORT, CreatePartyUiState(" a ").nameProblem)
        assertEquals(2, CreatePartyUiState("  ab  ").nameLength)
        assertTrue(CreatePartyUiState("ab").nameIsValid)
        assertTrue(CreatePartyUiState("x".repeat(40)).nameIsValid)
        assertEquals(PartyNameProblem.TOO_LONG, CreatePartyUiState("x".repeat(41)).nameProblem)
        assertFalse(CreatePartyUiState("").nameIsValid)
    }

    /** The editable field inside the tagged one (PixelTextField puts the tag on its wrapper). */
    private fun nameField() = composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(DaroodTestTags.CREATE_NAME)))

    @Test
    fun theCounter_followsTheTyping_andSaysWhenItsTooLong() {
        val viewModel = CreatePartyViewModel()
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) { CreatePartyScreen(onNavigateBack = {}, viewModel = viewModel) }
        }

        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_NAME_COUNTER).assertTextEquals("0/40")
        nameField().performTextInput("Friday circle")
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_NAME_COUNTER).assertTextEquals("13/40")

        nameField().performTextReplacement("F")
        composeTestRule.onNodeWithText("At least 2 characters.").assertIsDisplayed()

        nameField().performTextReplacement("x".repeat(45))
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_NAME_COUNTER).assertTextEquals("45/40")
        composeTestRule.onNodeWithText("At most 40 characters.").assertIsDisplayed()
    }

    @Test
    fun theLimit_startsAt10_andStaysBetween2And30() {
        val viewModel = CreatePartyViewModel()
        assertEquals(10, viewModel.uiState.value.maxMembers)
        viewModel.setMaxMembers(1)
        assertEquals(2, viewModel.uiState.value.maxMembers)
        assertFalse(viewModel.uiState.value.canHaveFewer)
        viewModel.setMaxMembers(31)
        assertEquals(30, viewModel.uiState.value.maxMembers)
        assertFalse(viewModel.uiState.value.canHaveMore)
    }

    @Test
    fun theStepper_movesOneAtATime_andStopsAtItsEnds() {
        val viewModel = CreatePartyViewModel()
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) { CreatePartyScreen(onNavigateBack = {}, viewModel = viewModel) }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_LIMIT).assertTextEquals("10")
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_MORE).performClick()
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_LIMIT).assertTextEquals("11")
        repeat(20) { composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_FEWER).performClick() }
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_LIMIT).assertTextEquals("2")
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_FEWER).assertIsNotEnabled()
    }

    @Test
    fun theMilestone_isOptional_andDigitsOnly() {
        val viewModel = CreatePartyViewModel()
        assertNull(viewModel.uiState.value.milestone)

        viewModel.onMilestoneChange("100000")
        assertEquals(100_000L, viewModel.uiState.value.milestone)
        viewModel.onMilestoneChange("10,000 Darood")
        assertEquals("10000", viewModel.uiState.value.milestoneText)
        viewModel.onMilestoneChange("-5")
        assertEquals(5L, viewModel.uiState.value.milestone)
        viewModel.onMilestoneChange("1.5e3")
        assertEquals("153", viewModel.uiState.value.milestoneText)
        viewModel.onMilestoneChange("0007")
        assertEquals("7", viewModel.uiState.value.milestoneText)
        viewModel.onMilestoneChange("123456789012345")
        assertEquals("1234567890", viewModel.uiState.value.milestoneText)
        viewModel.onMilestoneChange("abc")
        assertNull(viewModel.uiState.value.milestone)
        viewModel.onMilestoneChange("000")
        assertTrue(viewModel.uiState.value.milestoneIsZero)
    }

    @Test
    fun theMilestoneField_readsTheNumberBack() {
        val viewModel = CreatePartyViewModel()
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Light, darkTheme = false) { CreatePartyScreen(onNavigateBack = {}, viewModel = viewModel) }
        }
        composeTestRule.onNodeWithText("The total the party aims for. You can set it later.").assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(DaroodTestTags.CREATE_MILESTONE))).performTextInput("100000")
        composeTestRule.onNodeWithText("The party aims for 100,000 together.").assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(DaroodTestTags.CREATE_MILESTONE))).performTextReplacement("0")
        composeTestRule.onNodeWithText("The milestone must be above 0, or left empty.").assertIsDisplayed()
    }
}
