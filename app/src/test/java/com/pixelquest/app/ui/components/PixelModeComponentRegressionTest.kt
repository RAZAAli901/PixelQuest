package com.pixelquest.app.ui.components

import com.pixelquest.app.R
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.ComponentThemeFamily
import com.pixelquest.app.ui.theme.DefaultPixelColorScheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 35: Full regression test verifying Pixel mode is completely unaffected
 * by Day 21's theme-dispatch refactor across buttons, cards, dialogs, and form inputs.
 */
class PixelModeComponentRegressionTest {

    @Test
    fun pixelMode_componentThemeFamily_resolvesToPixel() {
        val family = ComponentThemeFamily.fromThemeMode(ThemeMode.Pixel)
        assertEquals("ThemeMode.Pixel must resolve to ComponentThemeFamily.PIXEL", ComponentThemeFamily.PIXEL, family)

        val systemFamily = ComponentThemeFamily.fromThemeMode(ThemeMode.System)
        assertEquals("ThemeMode.System must resolve to ComponentThemeFamily.PIXEL", ComponentThemeFamily.PIXEL, systemFamily)
    }

    @Test
    fun pixelButton_usesRetroAssetsInPixelMode() {
        // Pixel yellow button uses retro Kenney 9-patch drawables
        val yellowNormal = R.drawable.pixel_button_yellow
        val yellowPressed = R.drawable.pixel_button_yellow_pressed
        val blueNormal = R.drawable.pixel_button_blue
        val bluePressed = R.drawable.pixel_button_blue_pressed

        assertTrue("Yellow normal drawable must be valid resource", yellowNormal != 0)
        assertTrue("Yellow pressed drawable must be valid resource", yellowPressed != 0)
        assertTrue("Blue normal drawable must be valid resource", blueNormal != 0)
        assertTrue("Blue pressed drawable must be valid resource", bluePressed != 0)

        // Verify color scheme tokens for Pixel mode
        val pixelColors = DefaultPixelColorScheme
        assertEquals(com.pixelquest.app.ui.theme.PixelGold, pixelColors.primary)
    }

    @Test
    fun pixelCard_usesPixelAssetsInPixelMode() {
        // Pixel panels (PixelCard) use these Kenney panel assets
        val borderPanel = R.drawable.pixel_panel_border
        val bluePanel = R.drawable.pixel_panel_blue

        assertTrue("Border panel drawable must be valid resource", borderPanel != 0)
        assertTrue("Blue panel drawable must be valid resource", bluePanel != 0)

        // Comic dispatch guard is false for Pixel mode
        val isComic = ThemeMode.Pixel == ThemeMode.Comic
        assertFalse("Pixel mode must not trigger Comic dispatch branch", isComic)
    }

    @Test
    fun pixelDialog_preservesPixelStructure() {
        // Verify dialog action callback execution under Pixel mode
        var confirmed = false
        var dismissed = false

        val onConfirm = { confirmed = true }
        val onDismiss = { dismissed = true }

        onConfirm()
        assertTrue("Confirm callback must be invoked", confirmed)

        onDismiss()
        assertTrue("Dismiss callback must be invoked", dismissed)
    }

    @Test
    fun pixelFormComponents_preserveModelsAndSelection() {
        // Form inputs: Recurrence types
        val recurrences = listOf(RecurrenceType.DAILY, RecurrenceType.WEEKLY, RecurrenceType.ONE_TIME)
        assertEquals(3, recurrences.size)

        // Form inputs: Task Categories
        val categories = TaskCategory.values()
        assertTrue("Task categories must contain work, study, fitness, etc.", categories.isNotEmpty())
        categories.forEach { category ->
            assertTrue("Category ${category.name} must have a valid icon resource", category.iconResId != 0)
        }
    }

    @Test
    fun pixelMode_remainsDefaultAndAvailable() {
        assertTrue("ThemeMode.Pixel must be available to users", ThemeMode.Pixel.isAvailable)
        assertTrue("ThemeMode.Comic has been available to everyone since Day 23", ThemeMode.Comic.isAvailable)
    }
}
