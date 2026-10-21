package com.pixelquest.app.study.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderRepeat
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** The Create reminder sheet: type, title, When, Repeat and Who, each a choice that sticks. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ReminderSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 21)
    private val zone = ZoneId.of("Asia/Karachi")

    @Test
    fun everyChoice_isMade_andSaved() {
        var form by mutableStateOf(ReminderForm.new(today))
        var saved = 0
        composeTestRule.setContent {
            PixelQuestTheme { ReminderFormContent(form, today, zone, onChange = { form = it }, onSave = { saved++ }) }
        }
        composeTestRule.onNodeWithTag(ReminderTags.kind(ReminderKind.EXAM)).performClick()
        composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(ReminderTags.TITLE))).performTextInput("Exam on Thursday")
        composeTestRule.onNodeWithTag(ReminderTags.scope(ReminderScope.TIME)).performClick()
        composeTestRule.onNodeWithTag(ReminderTags.day(LocalDate.of(2026, 10, 22))).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(ReminderTags.time(LocalTime.of(10, 0))).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(ReminderTags.repeat(ReminderRepeat.WEEKLY)).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(ReminderTags.audience(ReminderAudience.ME)).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(ReminderTags.audience(ReminderAudience.ME)).assertIsSelected()
        composeTestRule.onNodeWithTag(ReminderTags.SAVE).performScrollTo().performClick()

        assertEquals(1, saved)
        assertEquals(ReminderKind.EXAM, form.kind)
        assertEquals("Exam on Thursday", form.title)
        assertEquals(ReminderScope.TIME, form.scope)
        assertEquals(java.time.Instant.parse("2026-10-22T05:00:00Z"), form.draft(zone).dueAt)
        assertEquals(ReminderRepeat.WEEKLY, form.repeat)
        assertEquals(ReminderAudience.ME, form.audience)
    }

    @Test
    fun aWeek_offersNoDailyRepeat_andAnEdit_keepsWhoItsFor() {
        var form by mutableStateOf(ReminderForm.new(today).copy(editingId = "r1"))
        composeTestRule.setContent {
            PixelQuestTheme { ReminderFormContent(form, today, zone, onChange = { form = it }, onSave = {}) }
        }
        composeTestRule.onNodeWithTag(ReminderTags.repeat(ReminderRepeat.DAILY)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ReminderTags.week(LocalDate.of(2026, 10, 26))).performClick()
        assertEquals(LocalDate.of(2026, 10, 26), form.weekStart)
        composeTestRule.onNodeWithTag(ReminderTags.audience(ReminderAudience.ME)).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun aProblem_showsOnlyOnceSaveIsTried() {
        var form by mutableStateOf(ReminderForm.new(today))
        composeTestRule.setContent {
            PixelQuestTheme { ReminderFormContent(form, today, zone, onChange = { form = it }, onSave = { form = form.copy(triedToSave = true) }) }
        }
        val problem = "A reminder needs 2 to 80 characters."
        composeTestRule.onNodeWithText(problem).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ReminderTags.SAVE).performScrollTo().performClick()
        composeTestRule.onNodeWithText(problem).assertExists()
    }
}
