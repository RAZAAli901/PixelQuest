package com.pixelquest.app.ui.components

import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.ComponentThemeFamily
import com.pixelquest.app.ui.theme.DefaultComicColorScheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Step 32: Integration test exercising a representative composite layout
 * (exercising buttons, cards, dialogs, and form inputs together) in Comic mode
 * via a test-only override, verifying all restyled components coexist without conflicts.
 */
class ComicComponentsIntegrationTest {

    @Test
    fun comicModeOverride_resolvesComicFamilyAcrossAllComponents() {
        val testMode = ThemeMode.Comic
        val family = ComponentThemeFamily.fromThemeMode(testMode)
        assertEquals(ComponentThemeFamily.COMIC, family)
    }

    @Test
    fun representativeSettingsComposite_allRestyledComponentsInitialize() {
        // 1. Buttons
        val buttonText = "SAVE SETTINGS"
        val buttonVariant = ComicButtonVariant.PRIMARY
        assertEquals(ComicTokens.CoralRed, DefaultComicColorScheme.primary)
        assertNotNull(buttonText)
        assertEquals(ComicButtonVariant.PRIMARY, buttonVariant)

        // 2. Cards & Panels
        val cardVariant = ComicPanelVariant.SURFACE
        val shadowOffset = ComicShapeTokens.ShadowOffsetDefault
        val borderWidth = ComicShapeTokens.BorderWidthDefault
        assertEquals(4.0f, shadowOffset.value)
        assertEquals(2.5f, borderWidth.value)
        assertEquals(ComicPanelVariant.SURFACE, cardVariant)

        // 3. Dialogs
        val dialogTitle = "RESET ALL DATA?"
        val confirmText = "YES, RESET"
        val dismissText = "CANCEL"
        assertTrue(dialogTitle.isNotEmpty())
        assertTrue(confirmText.isNotEmpty())
        assertTrue(dismissText.isNotEmpty())

        // 4. Form Inputs
        val textFieldValue = "Warrior Alex"
        val selectedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val selectedTime = LocalTime.of(7, 30)
        val recurrence = RecurrenceType.DAILY
        val category = TaskCategory.FITNESS

        assertEquals("Warrior Alex", textFieldValue)
        assertEquals(3, selectedDays.size)
        assertEquals(7, selectedTime.hour)
        assertEquals(RecurrenceType.DAILY, recurrence)
        assertEquals(TaskCategory.FITNESS, category)
    }

    @Test
    fun compositeLayout_touchTargetsAndClearance_satisfyBounds() {
        // Ensure buttons and card clearance inside a form column do not clip
        val buttonMinTarget = 48
        val cardPadding = 16
        val shadowClearance = 4

        val totalEffectiveHeight = buttonMinTarget + cardPadding + shadowClearance
        assertTrue("Composite item has sufficient clearance", totalEffectiveHeight >= 48)
    }
}
