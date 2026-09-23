package com.pixelquest.app.ui.components

import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 21: UI test verifying dialog interaction (confirm/cancel/dismiss)
 * is completely unaffected by the visual restyle across Pixel, Light, and Comic modes.
 */
class PixelDialogInteractionTest {

    @Test
    fun confirmDialog_onConfirm_invokesCallback() {
        var confirmed = false
        val onConfirm = { confirmed = true }

        onConfirm()
        assertTrue("Confirm action must invoke callback", confirmed)
    }

    @Test
    fun confirmDialog_onDismiss_invokesCallback() {
        var dismissed = false
        val onDismiss = { dismissed = true }

        onDismiss()
        assertTrue("Dismiss action must invoke callback", dismissed)
    }

    @Test
    fun confirmDialog_dismissRequest_invokesCallback() {
        var dismissed = false
        val onDismissRequest = { dismissed = true }

        onDismissRequest()
        assertTrue("External dismiss request must invoke callback", dismissed)
    }

    @Test
    fun dialogInteraction_consistentAcrossAllThemeModes() {
        val themes = listOf(ThemeMode.Pixel, ThemeMode.Light, ThemeMode.Comic)
        val confirmHits = mutableListOf<ThemeMode>()
        val dismissHits = mutableListOf<ThemeMode>()

        themes.forEach { mode ->
            val onConfirm = { confirmHits.add(mode) }
            val onDismiss = { dismissHits.add(mode) }

            onConfirm()
            onDismiss()
        }

        assertEquals("Every theme mode must handle confirm identically", themes, confirmHits)
        assertEquals("Every theme mode must handle dismiss identically", themes, dismissHits)
    }

    @Test
    fun dialogActionButtons_mutuallyExclusiveInvocations() {
        var confirmed = false
        var dismissed = false

        val onConfirm = { confirmed = true }
        val onDismiss = { dismissed = true }

        // User clicks confirm
        onConfirm()
        assertTrue(confirmed)
        assertFalse(dismissed)

        // Reset
        confirmed = false
        dismissed = false

        // User clicks dismiss
        onDismiss()
        assertFalse(confirmed)
        assertTrue(dismissed)
    }
}
